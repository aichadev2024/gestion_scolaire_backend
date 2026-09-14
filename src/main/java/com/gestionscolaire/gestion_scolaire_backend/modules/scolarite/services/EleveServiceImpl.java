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
import java.util.Comparator;

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
            tenantGuard.requireSameNiveau(classe, c -> c.getNiveau() != null ? c.getNiveau().getId() : null);

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

    private Integer niveauDe(Eleve e) {
        return (e.getClasse() != null && e.getClasse().getNiveau() != null) ? e.getClasse().getNiveau().getId() : null;
    }

    @Override
    public Eleve modifierEleve(Long id, Eleve eleveDetails, Profil profilDetails) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        // Élève sans classe encore : rien à protéger. Élève déjà affecté : doit être de mon niveau.
        if (eleve.getClasse() != null) {
            tenantGuard.requireSameNiveau(eleve, this::niveauDe);
        }

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
            tenantGuard.requireSameNiveau(nouvelleClasse, c -> c.getNiveau() != null ? c.getNiveau().getId() : null);
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

        // Cloisonnement : un élève d'un autre établissement, ou hors du niveau supervisé, est traité comme inexistant.
        return eleve.filter(tenantGuard::appartientAuTenantCourant)
                .filter(e -> tenantGuard.correspondAuNiveauCourant(niveauDe(e)));
    }

    @Override
    public Optional<Eleve> trouverParMatricule(String matricule) {
        return eleveRepository.findByMatricule(matricule);
    }

    @Override
    public List<Eleve> listerElevesParClasse(Long classeId) {
        return tenantGuard.filterSameNiveau(tenantGuard.filterSameTenant(eleveRepository.findByClasseId(classeId)), this::niveauDe);
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
        List<Eleve> eleves;
        if (tenantGuard.crossTenant()) {
            eleves = eleveRepository.findAll();
        } else {
            eleves = eleveRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
        }
        return tenantGuard.filterSameNiveau(eleves, this::niveauDe);
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
        tenantGuard.requireSameNiveau(eleve, this::niveauDe);
        eleve.setStatutInscription(statutInscription.toUpperCase());
        return eleveRepository.save(eleve);
    }

    private static final java.util.Set<String> STATUTS_PEDAGOGIQUES = java.util.Set.of("REGULIER", "REDOUBLANT", "CL");

    @Override
    public Eleve modifierStatutPedagogique(Long id, String statutPedagogique) {
        if (statutPedagogique == null || !STATUTS_PEDAGOGIQUES.contains(statutPedagogique.toUpperCase())) {
            throw new BadRequestException(
                    "Statut pédagogique invalide. Valeurs acceptées : REGULIER, REDOUBLANT, CL.");
        }
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable ID : " + id)));
        tenantGuard.requireSameNiveau(eleve, this::niveauDe);
        eleve.setStatutPedagogique(statutPedagogique.toUpperCase());
        return eleveRepository.save(eleve);
    }

    @Override
    public void archiverEleve(Long id) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        tenantGuard.requireSameNiveau(eleve, this::niveauDe);
        eleve.setStatut("ARCHIVE");
        eleveRepository.save(eleve);
    }

    @Override
    public void supprimerEleve(Long id) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable ID : " + id)));
        tenantGuard.requireSameNiveau(eleve, this::niveauDe);

        // Suppression des dépendances (aucune contrainte FK n'est ON DELETE CASCADE)
        // avant la fiche élève, sinon la base rejette la suppression.
        noteRepository.deleteAll(noteRepository.findByEleveId(id));
        bulletinRepository.deleteAll(bulletinRepository.findByEleveId(id));
        presenceRepository.deleteAll(presenceRepository.findByEleveId(id));
        paiementRepository.deleteAll(paiementRepository.findByEleveId(id));

        eleveRepository.delete(eleve);
    }

    // Positions par défaut (celles du modèle qu'on fournit) — utilisées uniquement quand
    // l'en-tête de la colonne n'a pas pu être reconnue (fichier sans en-tête, ou libellé
    // qu'aucun synonyme ne couvre).
    private static final int COL_PRENOM = 0;
    private static final int COL_NOM = 1;
    private static final int COL_GENRE = 2;
    private static final int COL_DATE_NAISSANCE = 3;
    private static final int COL_TELEPHONE_ELEVE = 4;
    private static final int COL_EMAIL_ELEVE = 5;
    private static final int COL_CLASSE = 6;
    private static final int COL_TELEPHONE_PARENT = 7;

    // Synonymes reconnus par champ (comparés après suppression des accents et mise en
    // minuscule) — permet d'importer un fichier que l'école a DÉJÀ, avec ses propres
    // intitulés/ordre de colonnes, sans passer obligatoirement par notre modèle exact.
    // Prénom avant Nom : "prénom" normalisé contient "nom" comme sous-chaîne, donc la
    // colonne qu'il revendique est exclue avant que Nom ne cherche la sienne.
    private static final java.util.List<String> SYN_PRENOM = java.util.List.of("prenom", "first name", "firstname", "given name");
    private static final java.util.List<String> SYN_NOM = java.util.List.of("nom de famille", "last name", "lastname", "surname", "nom");
    private static final java.util.List<String> SYN_GENRE = java.util.List.of("genre", "sexe", "gender", "sex");
    private static final java.util.List<String> SYN_DATE_NAISSANCE = java.util.List.of("date de naissance", "date naissance", "naissance", "birth date", "date of birth", "ddn");
    private static final java.util.List<String> SYN_TELEPHONE_ELEVE = java.util.List.of("telephone eleve", "tel eleve", "contact eleve", "telephone", "tel", "phone", "contact");
    private static final java.util.List<String> SYN_EMAIL_ELEVE = java.util.List.of("email eleve", "e-mail", "email", "mail");
    private static final java.util.List<String> SYN_CLASSE = java.util.List.of("classe", "class");
    // Volontairement sans synonyme générique ("telephone" seul) : une colonne "Téléphone"
    // ambiguë doit toujours être comprise comme celle de l'élève, pas du parent.
    private static final java.util.List<String> SYN_TELEPHONE_PARENT = java.util.List.of("telephone du parent", "telephone parent", "tel parent", "contact parent", "numero parent");

    /** Accents retirés, minuscules, espaces normalisés — pour comparer des libellés d'en-tête. */
    private static String normaliserEntete(String s) {
        if (s == null) return "";
        String sansAccents = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Cherche, parmi les colonnes pas encore attribuées, celle dont l'en-tête contient un des
     * synonymes (le plus long synonyme en premier, pour préférer un intitulé précis à un vague).
     * À défaut, retombe sur la position du modèle standard — sauf si cette position est déjà
     * prise par un autre champ (en-têtes partiellement reconnus) : dans ce cas le champ est
     * considéré absent (-1) plutôt que de lire par erreur la colonne d'un autre champ.
     */
    private static int resoudreColonne(Row entete, DataFormatter formatter, java.util.List<String> synonymes,
                                        java.util.Set<Integer> dejaAttribuees, int positionParDefaut) {
        if (entete != null) {
            java.util.List<String> tries = new ArrayList<>(synonymes);
            tries.sort(java.util.Comparator.comparingInt(String::length).reversed());
            int dernierIndex = entete.getLastCellNum();
            for (String syn : tries) {
                for (int i = 0; i < dernierIndex; i++) {
                    if (dejaAttribuees.contains(i)) continue;
                    Cell cell = entete.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) continue;
                    String texte = normaliserEntete(formatter.formatCellValue(cell));
                    if (texte.contains(syn)) {
                        dejaAttribuees.add(i);
                        return i;
                    }
                }
            }
        }
        if (positionParDefaut >= 0 && dejaAttribuees.add(positionParDefaut)) {
            return positionParDefaut;
        }
        return -1;
    }

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

            // Détection des colonnes à partir de la ligne d'en-tête (ligne 1) — tolère un fichier
            // que l'école possède déjà, avec ses propres intitulés et son propre ordre de colonnes.
            Row ligneEntete = sheet.getRow(0);
            java.util.Set<Integer> colonnesAttribuees = new java.util.HashSet<>();
            int colPrenom = resoudreColonne(ligneEntete, formatter, SYN_PRENOM, colonnesAttribuees, COL_PRENOM);
            int colNom = resoudreColonne(ligneEntete, formatter, SYN_NOM, colonnesAttribuees, COL_NOM);
            int colGenre = resoudreColonne(ligneEntete, formatter, SYN_GENRE, colonnesAttribuees, COL_GENRE);
            int colDateNaissance = resoudreColonne(ligneEntete, formatter, SYN_DATE_NAISSANCE, colonnesAttribuees, COL_DATE_NAISSANCE);
            int colEmailEleve = resoudreColonne(ligneEntete, formatter, SYN_EMAIL_ELEVE, colonnesAttribuees, COL_EMAIL_ELEVE);
            int colClasse = resoudreColonne(ligneEntete, formatter, SYN_CLASSE, colonnesAttribuees, COL_CLASSE);
            // Téléphone du parent AVANT téléphone élève : le parent n'a que des synonymes
            // qualifiés ("téléphone du parent"...), l'élève accepte aussi un "Téléphone" nu —
            // si l'élève cherchait en premier, il pourrait voler la colonne du parent.
            int colTelephoneParent = resoudreColonne(ligneEntete, formatter, SYN_TELEPHONE_PARENT, colonnesAttribuees, COL_TELEPHONE_PARENT);
            int colTelephoneEleve = resoudreColonne(ligneEntete, formatter, SYN_TELEPHONE_ELEVE, colonnesAttribuees, COL_TELEPHONE_ELEVE);

            for (int i = 1; i <= derniereLigne; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String prenom = valeurCellule(row, colPrenom, formatter);
                String nom = valeurCellule(row, colNom, formatter);
                if (prenom.isBlank() && nom.isBlank()) continue; // ligne vide

                int numeroLigne = i + 1;
                try {
                    if (prenom.isBlank() || nom.isBlank()) {
                        throw new BadRequestException("Le prénom et le nom sont obligatoires.");
                    }

                    String genreRaw = valeurCellule(row, colGenre, formatter);
                    String genre = "F".equalsIgnoreCase(genreRaw) ? "F" : "M";

                    LocalDate dateNaissance = parserDateCellule(row, colDateNaissance, formatter);
                    String telephoneEleve = valeurCellule(row, colTelephoneEleve, formatter);
                    String emailEleve = valeurCellule(row, colEmailEleve, formatter);
                    String classeNom = valeurCellule(row, colClasse, formatter);
                    String telephoneParent = valeurCellule(row, colTelephoneParent, formatter);

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
        if (idx < 0) return "";
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate parserDateCellule(Row row, int idx, DataFormatter formatter) {
        if (idx < 0) return null;
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

    @Override
    public com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.PromotionRapport promouvoir(
            Long classeDestinationId, List<Long> eleveIds) {
        Classe destination = tenantGuard.requireSameTenant(classeRepository.findById(classeDestinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe de destination introuvable")));

        long effectifActuel = eleveRepository.findByClasseId(classeDestinationId).size();
        int capacite = destination.getCapaciteMax();

        List<com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.PromotionLigneResultat> resultats = new ArrayList<>();
        int succes = 0;
        for (Long eleveId : eleveIds) {
            String nomComplet = null;
            try {
                // La classe de destination n'est volontairement PAS restreinte au niveau de
                // l'appelant : un directeur de primaire doit pouvoir faire passer ses élèves
                // en 6ème (collège) au moment du passage — c'est justement le rôle de cette action.
                Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                        .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable ID : " + eleveId)));
                tenantGuard.requireSameNiveau(eleve, this::niveauDe);
                nomComplet = eleve.getProfil() != null
                        ? (eleve.getProfil().getPrenom() + " " + eleve.getProfil().getNom()).trim()
                        : null;

                if (effectifActuel >= capacite) {
                    throw new BadRequestException("Capacité de la classe de destination atteinte (" + capacite + ")");
                }

                eleve.setClasse(destination);
                // Un élève qui passe en classe supérieure repart "régulier" dans sa nouvelle classe —
                // l'étiquette "redoublant" ne concernait que son année précédente.
                eleve.setStatutPedagogique("REGULIER");
                eleveRepository.save(eleve);
                effectifActuel++;
                succes++;
                resultats.add(new com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.PromotionLigneResultat(
                        eleveId, true, nomComplet, null));
            } catch (Exception e) {
                resultats.add(new com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.PromotionLigneResultat(
                        eleveId, false, nomComplet, e.getMessage()));
            }
        }

        return new com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.PromotionRapport(
                eleveIds.size(), succes, eleveIds.size() - succes, resultats);
    }

    @Override
    public byte[] genererRecapitulatifAnnuel(Long classeId, String anneeScolaire) {
        List<Eleve> eleves = classeId != null ? listerElevesParClasse(classeId) : listerTous();

        List<com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.RecapitulatifLigne> lignes = new ArrayList<>();
        for (Eleve e : eleves) {
            List<Bulletin> bulletinsAnnee = bulletinRepository.findByEleveId(e.getId()).stream()
                    .filter(b -> anneeScolaire.equalsIgnoreCase(b.getAnneeScolaire()))
                    .toList();
            Double moyenneAnnuelle = bulletinsAnnee.isEmpty()
                    ? null
                    : bulletinsAnnee.stream().mapToDouble(Bulletin::getMoyenneGenerale).average().orElse(0);

            List<Presence> presences = presenceRepository.findByEleveId(e.getId());
            long total = presences.size();
            long presents = presences.stream().filter(p -> "PRESENT".equalsIgnoreCase(p.getStatut())).count();
            long absences = presences.stream().filter(p -> "ABSENT".equalsIgnoreCase(p.getStatut())).count();
            long retards = presences.stream().filter(p -> "RETARD".equalsIgnoreCase(p.getStatut())).count();
            Double tauxPresence = total == 0 ? null : (presents * 100.0 / total);

            lignes.add(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.RecapitulatifLigne.builder()
                    .matricule(e.getMatricule())
                    .nom(e.getProfil() != null ? e.getProfil().getNom() : null)
                    .prenom(e.getProfil() != null ? e.getProfil().getPrenom() : null)
                    .classeNom(e.getClasse() != null ? e.getClasse().getNom() : null)
                    .moyenneAnnuelle(moyenneAnnuelle)
                    .tauxPresence(tauxPresence)
                    .nbAbsences((int) absences)
                    .nbRetards((int) retards)
                    .statut(e.getStatut())
                    .statutInscription(e.getStatutInscription())
                    .statutPedagogique(e.getStatutPedagogique())
                    .build());
        }

        lignes.sort(Comparator
                .comparing((com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.RecapitulatifLigne l) ->
                        l.getClasseNom() == null ? "" : l.getClasseNom())
                .thenComparing(l -> l.getNom() == null ? "" : l.getNom()));

        return genererExcelRecapitulatif(lignes, anneeScolaire);
    }

    private byte[] genererExcelRecapitulatif(
            List<com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.RecapitulatifLigne> lignes,
            String anneeScolaire) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Récapitulatif " + anneeScolaire);
            String[] entetes = {
                    "Matricule", "Nom", "Prénom", "Classe", "Moyenne annuelle",
                    "Taux de présence (%)", "Absences", "Retards", "Statut", "Statut inscription", "Statut pédagogique"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < entetes.length; i++) {
                header.createCell(i).setCellValue(entetes[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int ligneIdx = 1;
            for (com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.RecapitulatifLigne l : lignes) {
                Row row = sheet.createRow(ligneIdx++);
                row.createCell(0).setCellValue(l.getMatricule());
                row.createCell(1).setCellValue(l.getNom());
                row.createCell(2).setCellValue(l.getPrenom());
                row.createCell(3).setCellValue(l.getClasseNom());
                if (l.getMoyenneAnnuelle() != null) row.createCell(4).setCellValue(l.getMoyenneAnnuelle());
                if (l.getTauxPresence() != null) row.createCell(5).setCellValue(l.getTauxPresence());
                row.createCell(6).setCellValue(l.getNbAbsences());
                row.createCell(7).setCellValue(l.getNbRetards());
                row.createCell(8).setCellValue(l.getStatut());
                row.createCell(9).setCellValue(l.getStatutInscription());
                row.createCell(10).setCellValue(l.getStatutPedagogique());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du récapitulatif : " + e.getMessage(), e);
        }
    }
}


