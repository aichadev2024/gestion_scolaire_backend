package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportJournalier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RapportJournalierRepository extends JpaRepository<RapportJournalier, Long> {
    List<RapportJournalier> findByEleveIdOrderByDateDesc(Long eleveId);
    Optional<RapportJournalier> findByEleveIdAndDate(Long eleveId, LocalDate date);
    List<RapportJournalier> findByEleveClasseIdAndDate(Long classeId, LocalDate date);
}
