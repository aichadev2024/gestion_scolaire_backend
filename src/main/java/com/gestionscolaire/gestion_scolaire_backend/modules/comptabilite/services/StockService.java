package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.ArticleStockRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.MouvementStockRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.ArticleStock;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.MouvementStock;

import java.util.List;

public interface StockService {
    List<ArticleStock> listerArticles();
    ArticleStock creerArticle(ArticleStockRequest request);
    ArticleStock modifierArticle(Long id, ArticleStockRequest request);
    void supprimerArticle(Long id);
    MouvementStock enregistrerMouvement(MouvementStockRequest request);
    List<MouvementStock> listerMouvements(Long articleId);
}
