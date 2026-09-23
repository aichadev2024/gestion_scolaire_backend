package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.SujetDevoir;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SujetDevoirRepository extends JpaRepository<SujetDevoir, Long> {
    List<SujetDevoir> findByEtablissementIdOrderByDateEnvoiDesc(Long etablissementId);
    List<SujetDevoir> findByEnseignantIdOrderByDateEnvoiDesc(Long enseignantId);
}
