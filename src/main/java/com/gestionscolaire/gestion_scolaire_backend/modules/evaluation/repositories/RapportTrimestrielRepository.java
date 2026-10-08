package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.RapportTrimestriel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RapportTrimestrielRepository extends JpaRepository<RapportTrimestriel, Long> {
    Optional<RapportTrimestriel> findByEtablissementIdAndClasseIdAndPeriodeAndAnneeScolaire(
            Long etablissementId, Long classeId, String periode, String anneeScolaire);

    Optional<RapportTrimestriel> findByEtablissementIdAndClasseIsNullAndPeriodeAndAnneeScolaire(
            Long etablissementId, String periode, String anneeScolaire);
}
