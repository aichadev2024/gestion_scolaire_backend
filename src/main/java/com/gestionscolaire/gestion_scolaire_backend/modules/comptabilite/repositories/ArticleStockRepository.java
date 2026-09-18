package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.ArticleStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleStockRepository extends JpaRepository<ArticleStock, Long> {
    List<ArticleStock> findByEtablissementIdOrderByNomAsc(Long etablissementId);
    boolean existsByEtablissementIdAndNomIgnoreCase(Long etablissementId, String nom);
}
