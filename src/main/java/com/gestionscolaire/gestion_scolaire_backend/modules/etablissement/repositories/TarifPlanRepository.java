package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.TarifPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TarifPlanRepository extends JpaRepository<TarifPlan, Long> {
    Optional<TarifPlan> findByCodeIgnoreCase(String code);
}
