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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        eleveRepository.delete(eleve);
    }
}


