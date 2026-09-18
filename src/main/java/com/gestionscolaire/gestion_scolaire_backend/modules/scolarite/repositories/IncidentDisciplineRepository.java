package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.IncidentDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IncidentDisciplineRepository extends JpaRepository<IncidentDiscipline, Long> {
    List<IncidentDiscipline> findByEleveIdOrderByDateDescHeureDesc(Long eleveId);
    List<IncidentDiscipline> findByClasseIdAndDate(Long classeId, LocalDate date);
}
