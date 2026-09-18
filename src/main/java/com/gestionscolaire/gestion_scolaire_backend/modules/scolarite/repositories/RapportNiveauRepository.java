package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportNiveau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RapportNiveauRepository extends JpaRepository<RapportNiveau, Long> {
    List<RapportNiveau> findByEtablissementIdOrderByDateCreationDesc(Long etablissementId);
    List<RapportNiveau> findByAuteurIdOrderByDateCreationDesc(Long auteurId);
}
