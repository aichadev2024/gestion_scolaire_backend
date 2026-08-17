package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByEleveId(Long eleveId);
    Optional<Paiement> findByNumeroRecu(String numeroRecu);
}


