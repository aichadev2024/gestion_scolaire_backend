package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
    List<Presence> findByEleveId(Long eleveId);
    List<Presence> findByEtablissementId(Long etablissementId);
    List<Presence> findByEleveIdAndDate(Long eleveId, LocalDate date);
    List<Presence> findByClasseMatiereIdAndDate(Long classeMatiereId, LocalDate date);
    // List (pas Optional) : avant le correctif d'idempotence, chaque revalidation d'un
    // appel créait une nouvelle ligne — d'éventuels doublons historiques pour un même
    // (élève, matière, date) ne doivent pas faire planter la recherche avec une exception
    // "résultat non unique".
    List<Presence> findByEleveIdAndClasseMatiereIdAndDate(Long eleveId, Long classeMatiereId, LocalDate date);
    List<Presence> findByEleveIdAndClasseMatiereIsNullAndDate(Long eleveId, LocalDate date);
    List<Presence> findByEleve_Classe_IdAndDate(Long classeId, LocalDate date);
}


