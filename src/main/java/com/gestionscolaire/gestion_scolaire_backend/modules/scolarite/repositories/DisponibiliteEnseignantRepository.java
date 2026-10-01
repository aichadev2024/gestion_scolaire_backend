package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.DisponibiliteEnseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibiliteEnseignantRepository extends JpaRepository<DisponibiliteEnseignant, Long> {
    List<DisponibiliteEnseignant> findByEnseignantIdOrderByJourSemaineAscHeureDebutAsc(Long enseignantId);
}
