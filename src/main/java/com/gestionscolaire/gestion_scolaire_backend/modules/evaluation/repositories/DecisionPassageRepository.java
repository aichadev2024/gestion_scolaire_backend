package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.DecisionPassage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionPassageRepository extends JpaRepository<DecisionPassage, Long> {
    Optional<DecisionPassage> findByEleveIdAndAnneeScolaire(Long eleveId, String anneeScolaire);
    List<DecisionPassage> findByEleveClasseIdAndAnneeScolaire(Long classeId, String anneeScolaire);
}
