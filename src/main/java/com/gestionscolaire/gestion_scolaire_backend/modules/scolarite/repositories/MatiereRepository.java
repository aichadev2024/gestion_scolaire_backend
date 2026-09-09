package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {
    Optional<Matiere> findByCode(String code);
    List<Matiere> findByEtablissementId(Long etablissementId);
}


