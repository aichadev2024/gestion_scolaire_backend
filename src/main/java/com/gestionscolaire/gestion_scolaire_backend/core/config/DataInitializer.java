package com.gestionscolaire.gestion_scolaire_backend.core.config;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Role;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.NiveauRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final List<String> ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "DIRECTEUR", "SECRETAIRE", "COMPTABLE", "ENSEIGNANT", "ELEVE", "PARENT"
    );

    private static final List<String> NIVEAUX = List.of(
            "Maternelle", "Primaire", "Collège", "Lycée"
    );

    private final RoleRepository roleRepository;
    private final NiveauRepository niveauRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository profilRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository utilisateurRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DataInitializer(
            RoleRepository roleRepository,
            NiveauRepository niveauRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository profilRepository,
            com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository utilisateurRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.niveauRepository = niveauRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.profilRepository = profilRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        modifierTypeColonnePhotoUrl();
        initialiserRoles();
        initialiserNiveaux();
        reparerComptesEnseignantsSansUtilisateur();
    }

    private void reparerComptesEnseignantsSansUtilisateur() {
        try {
            Role roleEnseignant = roleRepository.findByNom("ENSEIGNANT").orElse(null);
            if (roleEnseignant == null) return;

            List<com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil> profilsSansUtilisateur = profilRepository.findAll().stream()
                    .filter(p -> p.getUtilisateur() == null)
                    .toList();

            for (com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil profil : profilsSansUtilisateur) {
                String prenomClean = (profil.getPrenom() != null ? profil.getPrenom().trim() : "enseignant").toLowerCase().replaceAll("\\s+", "");
                String nomClean = (profil.getNom() != null ? profil.getNom().trim() : "prof").toLowerCase().replaceAll("\\s+", "");
                String baseUsername = prenomClean + "." + nomClean;
                String username = baseUsername;
                int counter = 1;
                while (utilisateurRepository.existsByUsername(username)) {
                    username = baseUsername + counter++;
                }

                String email = username + "@netaa-ecole.ml";

                com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur user = com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur.builder()
                        .username(username)
                        .email(email)
                        .motDePasse(passwordEncoder.encode("123456"))
                        .role(roleEnseignant)
                        .estActif(true)
                        .build();

                com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur savedUser = utilisateurRepository.save(user);
                profil.setUtilisateur(savedUser);
                profilRepository.save(profil);
            }
        } catch (Exception e) {
            System.err.println("Notice: Auto-repair profiles check: " + e.getMessage());
        }
    }

    private void modifierTypeColonnePhotoUrl() {
        try {
            jdbcTemplate.execute("ALTER TABLE profils ALTER COLUMN photo_url TYPE TEXT");
        } catch (Exception e) {
            // Ignorer si la colonne est déjà en TEXT ou si la table n'existe pas encore
        }
    }

    private void initialiserRoles() {
        for (String nom : ROLES) {
            roleRepository.findByNom(nom).orElseGet(() ->
                    roleRepository.save(Role.builder().nom(nom).build()));
        }
    }

    private void initialiserNiveaux() {
        for (String nom : NIVEAUX) {
            niveauRepository.findByNom(nom).orElseGet(() ->
                    niveauRepository.save(Niveau.builder().nom(nom).build()));
        }
    }
}


