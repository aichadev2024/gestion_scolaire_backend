package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.ArticleStockRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.MouvementStockRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.ArticleStock;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.MouvementStock;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasAnyRole('DIRECTEUR', 'COMPTABLE')")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/articles")
    public ResponseEntity<List<ArticleStock>> listerArticles() {
        return ResponseEntity.ok(stockService.listerArticles());
    }

    @PostMapping("/articles")
    public ResponseEntity<ArticleStock> creerArticle(@Valid @RequestBody ArticleStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.creerArticle(request));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<ArticleStock> modifierArticle(@PathVariable Long id, @Valid @RequestBody ArticleStockRequest request) {
        return ResponseEntity.ok(stockService.modifierArticle(id, request));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> supprimerArticle(@PathVariable Long id) {
        stockService.supprimerArticle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/articles/{id}/mouvements")
    public ResponseEntity<List<MouvementStock>> listerMouvements(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.listerMouvements(id));
    }

    @PostMapping("/mouvements")
    public ResponseEntity<MouvementStock> enregistrerMouvement(@Valid @RequestBody MouvementStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.enregistrerMouvement(request));
    }
}
