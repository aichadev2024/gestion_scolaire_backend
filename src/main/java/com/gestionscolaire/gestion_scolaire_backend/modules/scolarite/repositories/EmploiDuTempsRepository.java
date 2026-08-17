package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.EmploiDuTemps;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmploiDuTempsRepository extends JpaRepository<EmploiDuTemps, Long> {
    List<EmploiDuTemps> findByClasseMatiereClasseId(Long classeId);
    List<EmploiDuTemps> findByClasseId(Long classeId);
    List<EmploiDuTemps> findByClasseIdOrClasseMatiereClasseId(Long classeId, Long classeMatiereClasseId);
    List<EmploiDuTemps> findByClasseMatiereEnseignantId(Long enseignantId);
}


