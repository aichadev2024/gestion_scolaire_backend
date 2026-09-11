package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EleveService;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveImportLigneResultat;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveImportRapport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;

@Service
@Transactional
public class EleveServiceImpl implements EleveService {

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private ProfilRepository profilRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ClasseRepository classeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService utilisateurService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private BulletinRepository bulletinRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    private Parent resoudreParent(Long parentId) {
        if (parentId == null) return null;

        // 1. D'ABORD : Recherche par ID du compte utilisateur du parent (profil.utilisateur.id)
        Optional<Parent> parentOpt = parentRepository.findByProfilUtilisateurId(parentId);
        if (parentOpt.isPresent()) return parentOpt.get();

        // 2. ENSUITE : Recherche par ID direct de la fiche Parent
        parentOpt = parentRepository.findById(parentId);
        if (parentOpt.isPresent()) return parentOpt.get();

        // 3. Si non trouvé, on cherche le Utilisateur correspondant et on crée la fiche Parent à la volée
        Optional<Utilisateur> userOpt = utilisateurRepository.findById(parentId);
        if (userOpt.isPresent()) {
            Utilisateur user = userOpt.get();
            Profil profil = profilRepository.findByUtilisateurId(user.getId()).orElse(null);
            if (profil != null) {
                Parent nParent = Parent.builder().profil(profil).build();
                return parentRepository.save(nParent);
            }
        }

        // Aucun fallback « premier parent disponible » : en multi-établissements, rattacher
        // un élève à un parent arbitraire provoquerait un croisement de données entre écoles.
        throw new ResourceNotFoundException("Parent introuvable pour l'identifiant: " + parentId);
    }

    @Override
    public Eleve inscrireEleve(Eleve eleve, Profil profil, Long parentId, Long classeId, String motDePasseInitial) {
        // Rattachement explicite à l'établissement de l'utilisateur courant (le TenantEntityListener sert de filet de sécurité).
        try {
            Etablissement etabCourant = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
            if (etabCourant != null) eleve.setEtablissement(etabCourant);
        } catch (Exception ignored) {}

        if (parentId != null) {
            eleve.setParent(resoudreParent(parentId));
        }
        if (classeId != null) {
            Classe classe = tenantGuard.requireSameTenant(
                    classeRepository.findById(classeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
            
            // Vérification de la capacité maximale de la classe
            long effectifActuel = eleveRepository.findByClasseId(classeId).size();
            if (effectifActuel >= classe.getCapaciteMax()) {
                throw new BadRequestException("La classe a atteint sa capacité maximale");
            }
            eleve.setClasse(classe);
        }

        // Génération automatique du matricule unique
        String anneeScolaire = (eleve.getClasse() != null) ? eleve.getClasse().getAnneeScolaire() : "GEN";
        anneeScolaire = anneeScolaire.replace("/", "-");
        String seq = String.format("%04d", eleveRepository.count() + 1);
        eleve.setMatricule("E-" + anneeScolaire + "-" + seq);

        // Création du compte utilisateur élève si absent
        if (profil.getUtilisateur() == null) {
            String prenomClean = (profil.getPrenom() != null ? profil.getPrenom().trim() : "eleve").toLowerCase().replaceAll("\\s+", "");
            String nomClean = (profil.getNom() != null ? profil.getNom().trim() : "eleve").toLowerCase().replaceAll("\\s+", "");
            String baseUsername = prenomClean + "." + nomClean;
            String username = baseUsername;
            int counter = 1;
            while (utilisateurRepository.existsByUsername(username)) {
                username = baseUsername + counter++;
            }

            String email = (profil.getEmail() != null && !profil.getEmail().isBlank()) ? profil.getEmail().trim() : null;

            com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etablissement = null;
            try {
                etablissement = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
            } catch (Exception ignored) {}

            Utilisateur user = Utilisateur.builder()
                    .username(username)
                    .email(email)
                    .motDePasse(motDePasseInitial)
                    .estActif(true)
                    .estPremierLogin(true)
                    .etablissement(etablissement)
                    .build();

            Utilisateur savedUser = utilisateurService.inscrire(user, profil, "ELEVE");
            profil = profilRepository.findByUtilisateurId(savedUser.getId()).orElse(profil);
            eleve.setMotDePasseInitial(motDePasseInitial);
        } else {
            profil = profilRepository.save(profil);
        }

        eleve.setProfil(profil);
        eleve.setStatut("ACTIF");

        return eleveRepository.save(eleve);
    }

    @Override
    public Eleve modifierEleve(Long id, Eleve eleveDetails, Profil profilDetails) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        Profil profil = eleve.getProfil();
        profil.setPrenom(profilDetails.getPrenom());
        profil.setNom(profilDetails.getNom());
        profil.setTelephone(profilDetails.getTelephone());
        profil.setPhotoUrl(profilDetails.getPhotoUrl());
        profil.setGenre(profilDetails.getGenre());
        profil.setDateNaissance(profilDetails.getDateNaissance());
        profil.setAdresse(profilDetails.getAdresse());
        profilRepository.save(profil);

        if (eleveDetails.getClasse() != null) {
            Classe nouvelleClasse = tenantGuard.requireSameTenant(
                    classeRepository.findById(eleveDetails.getClasse().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
            eleve.setClasse(nouvelleClasse);
        }

        if (eleveDetails.getParent() != null && eleveDetails.getParent().getId() != null) {
            eleve.setParent(resoudreParent(eleveDetails.getParent().getId()));
        }

        return eleveRepository.save(eleve);
    }

    @Override
    public Optional<Eleve> trouverParId(Long id) {
        Optional<Eleve> eleve = eleveRepository.findById(id);

        if (eleve.isEmpty()) {
            // Fallback 1: ID du compte utilisateur de l'élève
            eleve = eleveRepository.findByProfilUtilisateurId(id);
        }
        if (eleve.isEmpty()) {
            // Fallback 2: ID du compte utilisateur du parent
            List<Eleve> enfantsParUser = eleveRepository.findByParentProfilUtilisateurId(id);
            if (!enfantsParUser.isEmpty()) eleve = Optional.of(enfantsParUser.get(0));
        }
        if (eleve.isEmpty()) {
            // Fallback 3: ID direct de la fiche Parent
            List<Eleve> enfantsParParent = eleveRepository.findByParentId(id);
            if (!enfantsParParent.isEmpty()) eleve = Optional.of(enfantsParParent.get(0));
        }

        // Cloisonnement : un élève d'un autre établissement est traité comme inexistant.
        return eleve.filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    public Optional<Eleve> trouverParMatricule(String matricule) {
        return eleveRepository.findByMatricule(matricule);
    }

    @Override
    public List<Eleve> listerElevesParClasse(Long classeId) {
        return tenantGuard.filterSameTenant(eleveRepository.findByClasseId(classeId));
    }

    @Override
    public List<Eleve> listerElevesParParent(Long parentId) {
        Set<Eleve> resultats = new LinkedHashSet<>();

        if (parentId == null) return new ArrayList<>();

        // 1. D'abord chercher si parentId est un Utilisateur.id (le cas le plus courant du JWT mobile)
        Optional<Parent> parentByUserId = parentRepository.findByProfilUtilisateurId(parentId);
        if (parentByUserId.isPresent()) {
            Long pId = parentByUserId.get().getId();
            resultats.addAll(eleveRepository.findByParentIdOrParentSecondaireId(pId, pId));
        }

        // 2. Chercher si parentId est un Parent.id direct
        Optional<Parent> parentById = parentRepository.findById(parentId);
        if (parentById.isPresent()) {
            Long pId = parentById.get().getId();
            resultats.addAll(eleveRepository.findByParentIdOrParentSecondaireId(pId, pId));
            if (parentById.get().getProfil() != null && parentById.get().getProfil().getUtilisateur() != null) {
                Long uId = parentById.get().getProfil().getUtilisateur().getId();
                resultats.addAll(eleveRepository.findByParentProfilUtilisateurIdOrParentSecondaireProfilUtilisateurId(uId, uId));
            }
        }

        // 3. Fallbacks de sécurité directes sur les requêtes brutes
        resultats.addAll(eleveRepository.findByParentIdOrParentSecondaireId(parentId, parentId));
        resultats.addAll(eleveRepository.findByParentProfilUtilisateurIdOrParentSecondaireProfilUtilisateurId(parentId, parentId));

        // 4. Si la recherche par ID est partielle, matcher par téléphone ou email du profil parent.
        //    Recherche restreinte à l'établissement courant (jamais sur toute la base).
        Optional<Utilisateur> parentUserOpt = utilisateurRepository.findById(parentId);
        if (parentUserOpt.isPresent()) {
            Utilisateur parentUser = parentUserOpt.get();
            Profil userProfil = profilRepository.findByUtilisateurId(parentUser.getId()).orElse(null);
            String pPhone = (userProfil != null) ? userProfil.getTelephone() : null;
            String pEmail = parentUser.getEmail();

            List<Eleve> perimetre = tenantGuard.crossTenant()
                    ? eleveRepository.findAll()
                    : eleveRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
            for (Eleve e : perimetre) {
                if (e.getParent() != null && e.getParent().getProfil() != null) {
                    Profil prof = e.getParent().getProfil();
                    if ((pPhone != null && !pPhone.trim().isEmpty() && pPhone.equalsIgnoreCase(prof.getTelephone())) ||
                        (pEmail != null && !pEmail.trim().isEmpty() && pEmail.equalsIgnoreCase(prof.getEmail()))) {
                        resultats.add(e);
                    }
                }
            }
        }

        // Cloisonnement final : ne renvoyer que les élèves de l'établissement courant.
        return new ArrayList<>(tenantGuard.filterSameTenant(resultats));
    }

    @Override
    public List<Eleve> listerTous() {
        if (tenantGuard.crossTenant()) {
            return eleveRepository.findAll();
        }
        return eleveRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
    }

    private static final java.util.Set<String> STATUTS_INSCRIPTION =
            java.util.Set.of("VALIDEE", "EN_ATTENTE", "ANNULEE");

    @Override
    public Eleve modifierStatutInscription(Long id, String statutInscription) {
        if (statutInscription == null || !STATUTS_INSCRIPTION.contains(statutInscription.toUpperCase())) {
            throw new BadRequestException(
                    "Statut d'inscription invalide. Valeurs acceptées : VALIDEE, EN_ATTENTE, ANNULEE.");
        }
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable ID : " + id)));
        eleve.setStatutInscription(statutInscription.toUpperCase());
        return eleveRepository.save(eleve);
    }

    @Override
    public void archiverEleve(Long id) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        eleve.setStatut("ARCHIVE");
        eleveRepository.save(eleve);
    }

    @Override
    public void supprimerEleve(Long id) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable ID : " + id)));

        // Suppression des dépendances (aucune contrainte FK n'est ON DELETE CASCADE)
        // avant la fiche élève, sinon la base rejette la suppression.
        noteRepository.deleteAll(noteRepository.findByEleveId(id));
        bulletinRepository.deleteAll(bulletinRepository.findByEleveId(id));
        presenceRepository.deleteAll(presenceRepository.findByEleveId(id));
        paiementRepository.deleteAll(paiementRepository.findByEleveId(id));

        eleveRepository.delete(eleve);
    }

    private static final int COL_PRENOM = 0;
    private static final int COL_NOM = 1;
    private static final int COL_GENRE = 2;
    private static final int COL_DATE_NAISSANCE = 3;
    private static final int COL_TELEPHONE_ELEVE = 4;
    private static final int COL_EMAIL_ELEVE = 5;
    private static final int COL_CLASSE = 6;
    private static final int COL_TELEPHONE_PARENT = 7;

    @Override
    public EleveImportRapport importerDepuisExcel(MultipartFile fichier, Long classeIdParDefaut) {
        Long etablissementId = tenantGuard.requireEtablissementId();

        Classe classeParDefaut = null;
        if (classeIdParDefaut != null) {
            classeParDefaut = tenantGuard.requireSameTenant(
                    classeRepository.findById(classeIdParDefaut)
                            .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
        }
        List<Classe> classesEtablissement = classeRepository.findByEtablissementId(etablissementId);

        List<EleveImportLigneResultat> resultats = new ArrayList<>();
        int succes = 0;

        try (Workbook workbook = WorkbookFactory.create(fichier.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int derniereLigne = sheet.getLastRowNum();

            for (int i = 1; i <= derniereLigne; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String prenom = valeurCellule(row, COL_PRENOM, formatter);
                String nom = valeurCellule(row, COL_NOM, formatter);
                if (prenom.isBlank() && nom.isBlank()) continue; // ligne vide

                int numeroLigne = i + 1;
                try {
                    if (prenom.isBlank() || nom.isBlank()) {
                        throw new BadRequestException("Le prénom et le nom sont obligatoires.");
                    }

                    String genreRaw = valeurCellule(row, COL_GENRE, formatter);
                    String genre = "F".equalsIgnoreCase(genreRaw) ? "F" : "M";

                    LocalDate dateNaissance = parserDateCellule(row, COL_DATE_NAISSANCE, formatter);
                    String telephoneEleve = valeurCellule(row, COL_TELEPHONE_ELEVE, formatter);
                    String emailEleve = valeurCellule(row, COL_EMAIL_ELEVE, formatter);
                    String classeNom = valeurCellule(row, COL_CLASSE, formatter);
                    String telephoneParent = valeurCellule(row, COL_TELEPHONE_PARENT, formatter);

                    Classe classe = classeParDefaut;
                    if (!classeNom.isBlank()) {
                        String cible = classeNom.trim();
                        classe = classesEtablissement.stream()
                                .filter(c -> c.getNom().equalsIgnoreCase(cible))
                                .findFirst()
                                .orElseThrow(() -> new BadRequestException("Classe introuvable : " + classeNom));
                    }

                    Parent parent = null;
                    if (!telephoneParent.isBlank()) {
                        parent = parentRepository.findByProfilTelephoneAndEtablissementId(telephoneParent.trim(), etablissementId)
                                .orElse(null);
                    }

                    Profil profil = Profil.builder()
                            .prenom(prenom.trim())
                            .nom(nom.trim())
                            .telephone(telephoneEleve.isBlank() ? null : telephoneEleve.trim())
                            .email(emailEleve.isBlank() ? null : emailEleve.trim())
                            .genre(genre)
                            .dateNaissance(dateNaissance)
                            .build();

                    Eleve eleve = Eleve.builder().build();
                    String motDePasse = com.gestionscolaire.gestion_scolaire_backend.core.security.PasswordGenerator.generer();

                    Eleve saved = inscrireEleve(
                            eleve, profil,
                            parent != null ? parent.getId() : null,
                            classe != null ? classe.getId() : null,
                            motDePasse);

                    succes++;
                    resultats.add(new EleveImportLigneResultat(
                            numeroLigne, true, saved.getMatricule(),
                            prenom.trim() + " " + nom.trim(), saved.getMotDePasseInitial(), null));
                } catch (Exception rowEx) {
                    resultats.add(new EleveImportLigneResultat(
                            numeroLigne, false, null, (prenom + " " + nom).trim(), null, rowEx.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Fichier Excel illisible : " + e.getMessage());
        }

        return new EleveImportRapport(resultats.size(), succes, resultats.size() - succes, resultats);
    }

    private String valeurCellule(Row row, int idx, DataFormatter formatter) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate parserDateCellule(Row row, int idx, DataFormatter formatter) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String raw = formatter.formatCellValue(cell).trim();
        if (raw.isBlank()) return null;
        for (String pattern : new String[]{"dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd"}) {
            try {
                return LocalDate.parse(raw, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // essai du format suivant
            }
        }
        throw new BadRequestException("Date de naissance invalide : " + raw + " (format attendu JJ/MM/AAAA)");
    }

    @Override
    public byte[] genererModeleImportExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Élèves");
            String[] entetes = {
                    "Prénom", "Nom", "Genre (M/F)", "Date de naissance (JJ/MM/AAAA)",
                    "Téléphone élève", "Email élève", "Classe", "Téléphone du parent"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < entetes.length; i++) {
                header.createCell(i).setCellValue(entetes[i]);
                sheet.setColumnWidth(i, 22 * 256);
            }

            Row exemple = sheet.createRow(1);
            String[] valeursExemple = {
                    "Fatoumata", "Diarra", "F", "12/03/2015",
                    "", "", "6ème A", "+223 70 00 00 00"
            };
            for (int i = 0; i < valeursExemple.length; i++) {
                exemple.createCell(i).setCellValue(valeursExemple[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du modèle d'import : " + e.getMessage(), e);
        }
    }
}


