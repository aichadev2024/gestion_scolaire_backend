package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Bulletin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinRepository extends JpaRepository<Bulletin, Long> {
    Optional<Bulletin> findByEleveIdAndPeriodeAndAnneeScolaire(Long eleveId, String periode, String anneeScolaire);
    List<Bulletin> findByEleveId(Long eleveId);
    List<Bulletin> findByClasseIdAndPeriodeAndAnneeScolaire(Long classeId, String periode, String anneeScolaire);
}


