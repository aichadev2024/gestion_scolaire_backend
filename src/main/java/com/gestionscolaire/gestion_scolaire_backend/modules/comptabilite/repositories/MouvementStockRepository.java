package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.MouvementStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    List<MouvementStock> findByArticleIdOrderByDateDescIdDesc(Long articleId);
    void deleteByArticleId(Long articleId);
}
