package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NiveauRepository extends JpaRepository<Niveau, Integer> {
    Optional<Niveau> findByNom(String nom);
}


