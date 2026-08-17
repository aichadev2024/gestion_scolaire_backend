package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EleveRepository extends JpaRepository<Eleve, Long> {
    Optional<Eleve> findByMatricule(String matricule);
    List<Eleve> findByClasseId(Long classeId);
    List<Eleve> findByParentId(Long parentId);
    List<Eleve> findByParentIdOrParentSecondaireId(Long p1, Long p2);
    Optional<Eleve> findByProfilUtilisateurId(Long utilisateurId);
    List<Eleve> findByParentProfilUtilisateurId(Long utilisateurId);
    List<Eleve> findByParentProfilUtilisateurIdOrParentSecondaireProfilUtilisateurId(Long u1, Long u2);
}


