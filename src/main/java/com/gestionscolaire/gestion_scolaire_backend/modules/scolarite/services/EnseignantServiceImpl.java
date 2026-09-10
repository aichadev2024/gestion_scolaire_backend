package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.PresenceEnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EnseignantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EnseignantServiceImpl implements EnseignantService {

    @Autowired
    private EnseignantRepository enseignantRepository;

    @Autowired
    private ProfilRepository profilRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService utilisateurService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository utilisateurRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Autowired
    private ClasseRepository classeRepository;

    @Autowired
    private ClasseMatiereRepository classeMatiereRepository;

    @Autowired
    private PresenceEnseignantRepository presenceEnseignantRepository;

    @Override
    public Enseignant creerEnseignant(Enseignant enseignant, Profil profil, String motDePasseInitial) {
        // 1. Génération automatique du matricule unique pour l'enseignant
        String seq = String.format("%04d", enseignantRepository.count() + 1);
        String matricule = "T-GEN-" + seq;
        enseignant.setMatricule(matricule);

        // 2. Génération du nom d'utilisateur / login unique
        String prenomClean = (profil.getPrenom() != null ? profil.getPrenom().trim() : "enseignant").toLowerCase().replaceAll("\\s+", "");
        String nomClean = (profil.getNom() != null ? profil.getNom().trim() : "prof").toLowerCase().replaceAll("\\s+", "");
        String baseUsername = prenomClean + "." + nomClean;
        String username = baseUsername;
        int counter = 1;
        while (utilisateurRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }

        String email = (profil.getEmail() != null && !profil.getEmail().isBlank()) 
                ? profil.getEmail().trim() 
                : null;

        // Récupérer l'établissement de l'utilisateur connecté (Admin)
        com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etablissement = null;
        try {
            etablissement = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
        } catch (Exception ignored) {}

        // 3. Création automatique du compte Utilisateur avec rôle ENSEIGNANT
        com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur user = com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur.builder()
                .username(username)
                .email(email)
                .motDePasse(motDePasseInitial)
                .estActif(true)
                .estPremierLogin(true)
                .etablissement(etablissement)
                .build();

        com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur savedUser = utilisateurService.inscrire(user, profil, "ENSEIGNANT");
        profil = profilRepository.findByUtilisateurId(savedUser.getId()).orElse(profil);
        enseignant.setProfil(profil);
        if (etablissement != null) enseignant.setEtablissement(etablissement);

        Enseignant saved = enseignantRepository.save(enseignant);
        saved.setMotDePasseInitial(motDePasseInitial);
        return saved;
    }

    @Override
    public Enseignant modifierEnseignant(Long id, Enseignant enseignantDetails, Profil profilDetails) {
        Enseignant enseignant = tenantGuard.requireSameTenant(enseignantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable")));

        Profil profil = enseignant.getProfil();
        profil.setPrenom(profilDetails.getPrenom());
        profil.setNom(profilDetails.getNom());
        profil.setTelephone(profilDetails.getTelephone());
        if (profilDetails.getEmail() != null && !profilDetails.getEmail().isBlank()) {
            profil.setEmail(profilDetails.getEmail().trim());
            if (profil.getUtilisateur() != null) {
                profil.getUtilisateur().setEmail(profilDetails.getEmail().trim());
                utilisateurRepository.save(profil.getUtilisateur());
            }
        }
        profil.setPhotoUrl(profilDetails.getPhotoUrl());
        profil.setGenre(profilDetails.getGenre());
        profil.setDateNaissance(profilDetails.getDateNaissance());
        profil.setAdresse(profilDetails.getAdresse());
        profilRepository.save(profil);

        enseignant.setBiographie(enseignantDetails.getBiographie());

        return enseignantRepository.save(enseignant);
    }

    @Override
    public Optional<Enseignant> trouverParId(Long id) {
        return enseignantRepository.findById(id).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    public Optional<Enseignant> trouverParMatricule(String matricule) {
        return enseignantRepository.findByMatricule(matricule).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    public List<Enseignant> listerTous() {
        if (tenantGuard.crossTenant()) {
            return enseignantRepository.findAll();
        }
        return enseignantRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
    }

    @Override
    public void supprimerEnseignant(Long id) {
        Enseignant enseignant = tenantGuard.requireSameTenant(enseignantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable ID : " + id)));

        // Détacher l'enseignant partout où il est référencé (colonnes nullables)
        // puis retirer ses présences, sinon la base rejette la suppression.
        for (Classe c : classeRepository.findByEnseignantPrincipalId(id)) {
            c.setEnseignantPrincipal(null);
            classeRepository.save(c);
        }
        for (ClasseMatiere cm : classeMatiereRepository.findByEnseignantId(id)) {
            cm.setEnseignant(null);
            classeMatiereRepository.save(cm);
        }
        presenceEnseignantRepository.deleteAll(presenceEnseignantRepository.findByEnseignantId(id));

        enseignantRepository.delete(enseignant);
    }
}


