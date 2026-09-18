package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportNiveauEleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RapportNiveauEleveRepository extends JpaRepository<RapportNiveauEleve, Long> {
    List<RapportNiveauEleve> findByRapportId(Long rapportId);
}
