package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.DocumentEleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentEleveRepository extends JpaRepository<DocumentEleve, Long> {
    List<DocumentEleve> findByEleveIdOrderByDateAjoutDesc(Long eleveId);
}
