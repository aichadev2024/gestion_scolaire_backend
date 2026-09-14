package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
    List<Presence> findByEleveId(Long eleveId);
    List<Presence> findByEtablissementId(Long etablissementId);
    List<Presence> findByEleveIdAndDate(Long eleveId, LocalDate date);
    List<Presence> findByClasseMatiereIdAndDate(Long classeMatiereId, LocalDate date);
    Optional<Presence> findByEleveIdAndClasseMatiereIdAndDate(Long eleveId, Long classeMatiereId, LocalDate date);
    Optional<Presence> findByEleveIdAndClasseMatiereIsNullAndDate(Long eleveId, LocalDate date);
}


