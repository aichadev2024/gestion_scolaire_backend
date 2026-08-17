package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FraisScolariteRepository extends JpaRepository<FraisScolarite, Long> {
    List<FraisScolarite> findByClasseId(Long classeId);
}


