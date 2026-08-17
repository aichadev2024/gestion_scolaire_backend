package com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByNom(String nom);
}


