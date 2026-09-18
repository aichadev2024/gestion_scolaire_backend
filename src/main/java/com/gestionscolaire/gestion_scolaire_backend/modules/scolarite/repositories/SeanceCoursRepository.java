package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.SeanceCours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SeanceCoursRepository extends JpaRepository<SeanceCours, Long> {
    List<SeanceCours> findByClasseMatiereClasseIdAndDateBetweenOrderByDateDescHeureDebutDesc(Long classeId, LocalDate debut, LocalDate fin);
    List<SeanceCours> findByEtablissementIdAndDateBetween(Long etablissementId, LocalDate debut, LocalDate fin);
}
