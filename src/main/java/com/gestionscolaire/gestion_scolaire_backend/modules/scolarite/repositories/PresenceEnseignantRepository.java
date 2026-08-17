package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresenceEnseignantRepository extends JpaRepository<PresenceEnseignant, Long> {
    List<PresenceEnseignant> findByDate(LocalDate date);
    List<PresenceEnseignant> findByEnseignantId(Long enseignantId);
    Optional<PresenceEnseignant> findByEnseignantIdAndDate(Long enseignantId, LocalDate date);
}
