package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalleRepository extends JpaRepository<Salle, Long> {
    List<Salle> findByEtablissementIdOrderByNomAsc(Long etablissementId);
    boolean existsByEtablissementIdAndNomIgnoreCase(Long etablissementId, String nom);
}
