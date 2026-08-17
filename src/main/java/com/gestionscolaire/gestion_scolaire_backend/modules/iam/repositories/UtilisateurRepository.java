package com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByUsername(String username);
    Optional<Utilisateur> findByUsernameOrEmail(String username, String email);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByRoleNom(String roleNom);
    java.util.List<Utilisateur> findByEtablissementId(Long etablissementId);
}


