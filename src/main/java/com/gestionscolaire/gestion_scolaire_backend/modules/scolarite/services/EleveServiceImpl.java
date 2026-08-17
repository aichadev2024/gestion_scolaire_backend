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

        // 4. Fallback : Si des parents existent en base, lier au premier parent disponible
        List<Parent> tousParents = parentRepository.findAll();
        if (!tousParents.isEmpty()) {
            return tousParents.get(0);
        }

        throw new ResourceNotFoundException("Parent introuvable pour l'identifiant: " + parentId);
    }

    @Override
    public Eleve inscrireEleve(Eleve eleve, Profil profil, Long parentId, Long classeId) {
        if (parentId != null) {
            eleve.setParent(resoudreParent(parentId));
        }
        if (classeId != null) {
            Classe classe = classeRepository.findById(classeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
            
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

            String email = (profil.getEmail() != null && !profil.getEmail().isBlank()) ? profil.getEmail() : username + "@netaa-ecole.ml";

            com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etablissement = null;
            try {
                etablissement = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
            } catch (Exception ignored) {}

            Utilisateur user = Utilisateur.builder()
                    .username(username)
                    .email(email)
                    .motDePasse("123456")
                    .estActif(true)
                    .estPremierLogin(true)
                    .etablissement(etablissement)
                    .build();

            Utilisateur savedUser = utilisateurService.inscrire(user, profil, "ELEVE");
            profil = profilRepository.findByUtilisateurId(savedUser.getId()).orElse(profil);
        } else {
            profil = profilRepository.save(profil);
        }

        eleve.setProfil(profil);
        eleve.setStatut("ACTIF");

        return eleveRepository.save(eleve);
    }

    @Override
    public Eleve modifierEleve(Long id, Eleve eleveDetails, Profil profilDetails) {
        Eleve eleve = eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));
        
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
            Classe nouvelleClasse = classeRepository.findById(eleveDetails.getClasse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
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
        if (eleve.isPresent()) return eleve;

        // Fallback 1: ID du compte utilisateur de l'élève
        eleve = eleveRepository.findByProfilUtilisateurId(id);
        if (eleve.isPresent()) return eleve;

        // Fallback 2: ID du compte utilisateur du parent
        List<Eleve> enfantsParUser = eleveRepository.findByParentProfilUtilisateurId(id);
        if (!enfantsParUser.isEmpty()) return Optional.of(enfantsParUser.get(0));

        // Fallback 3: ID direct de la fiche Parent
        List<Eleve> enfantsParParent = eleveRepository.findByParentId(id);
        if (!enfantsParParent.isEmpty()) return Optional.of(enfantsParParent.get(0));

        return Optional.empty();
    }

    @Override
    public Optional<Eleve> trouverParMatricule(String matricule) {
        return eleveRepository.findByMatricule(matricule);
    }

    @Override
    public List<Eleve> listerElevesParClasse(Long classeId) {
        return eleveRepository.findByClasseId(classeId);
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

        // 4. Si la recherche par ID est partielle, matcher par téléphone ou email du profil parent
        Optional<Utilisateur> parentUserOpt = utilisateurRepository.findById(parentId);
        if (parentUserOpt.isPresent()) {
            Utilisateur parentUser = parentUserOpt.get();
            Profil userProfil = profilRepository.findByUtilisateurId(parentUser.getId()).orElse(null);
            String pPhone = (userProfil != null) ? userProfil.getTelephone() : null;
            String pEmail = parentUser.getEmail();

            List<Eleve> tousEleves = eleveRepository.findAll();
            for (Eleve e : tousEleves) {
                if (e.getParent() != null && e.getParent().getProfil() != null) {
                    Profil prof = e.getParent().getProfil();
                    if ((pPhone != null && !pPhone.trim().isEmpty() && pPhone.equalsIgnoreCase(prof.getTelephone())) ||
                        (pEmail != null && !pEmail.trim().isEmpty() && pEmail.equalsIgnoreCase(prof.getEmail()))) {
                        resultats.add(e);
                    }
                }
            }
        }

        return new ArrayList<>(resultats);
    }

    @Override
    public List<Eleve> listerTous() {
        try {
            com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails current = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser();
            if (current != null && current.getUtilisateur() != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(current.getUtilisateur().getRole().getNom())) {
                    return eleveRepository.findAll();
                }
                if (current.getUtilisateur().getEtablissement() != null) {
                    Long etabId = current.getUtilisateur().getEtablissement().getId();
                    return eleveRepository.findAll().stream()
                            .filter(e -> {
                                if (e.getClasse() != null && e.getClasse().getEtablissement() != null) {
                                    return etabId.equals(e.getClasse().getEtablissement().getId());
                                }
                                if (e.getProfil() != null && e.getProfil().getUtilisateur() != null && e.getProfil().getUtilisateur().getEtablissement() != null) {
                                    return etabId.equals(e.getProfil().getUtilisateur().getEtablissement().getId());
                                }
                                return true;
                            })
                            .toList();
                }
            }
        } catch (Exception ignored) {}
        return eleveRepository.findAll();
    }

    @Override
    public void archiverEleve(Long id) {
        Eleve eleve = eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));
        eleve.setStatut("ARCHIVE");
        eleveRepository.save(eleve);
    }
}


