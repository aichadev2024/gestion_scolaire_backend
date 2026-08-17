package com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfilRepository extends JpaRepository<Profil, Long> {
    java.util.Optional<Profil> findByUtilisateurId(Long utilisateurId);
}


