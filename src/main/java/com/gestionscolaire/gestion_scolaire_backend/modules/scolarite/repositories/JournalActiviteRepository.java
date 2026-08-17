package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.JournalActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {
    List<JournalActivite> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);
}


