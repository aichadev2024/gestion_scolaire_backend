package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {
    Optional<Etablissement> findByCode(String code);
    Boolean existsByCode(String code);

    @Modifying
    @Query("update Etablissement e set e.planTarifaire = :nouveau where e.planTarifaire = :ancien")
    int changerPlan(@Param("ancien") String ancien, @Param("nouveau") String nouveau);
}


