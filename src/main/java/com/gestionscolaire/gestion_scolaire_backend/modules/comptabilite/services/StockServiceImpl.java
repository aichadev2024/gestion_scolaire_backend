package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.ArticleStockRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.MouvementStockRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.ArticleStock;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.MouvementStock;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.ArticleStockRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.MouvementStockRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class StockServiceImpl implements StockService {

    private static final String ENTREE = "ENTREE";
    private static final String SORTIE = "SORTIE";

    private final ArticleStockRepository articleRepository;
    private final MouvementStockRepository mouvementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantGuard tenantGuard;

    public StockServiceImpl(ArticleStockRepository articleRepository,
                            MouvementStockRepository mouvementRepository,
                            UtilisateurRepository utilisateurRepository,
                            TenantGuard tenantGuard) {
        this.articleRepository = articleRepository;
        this.mouvementRepository = mouvementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleStock> listerArticles() {
        return articleRepository.findByEtablissementIdOrderByNomAsc(tenantGuard.requireEtablissementId());
    }

    @Override
    public ArticleStock creerArticle(ArticleStockRequest request) {
        Long etablissementId = tenantGuard.requireEtablissementId();
        String nom = request.getNom().trim();
        if (articleRepository.existsByEtablissementIdAndNomIgnoreCase(etablissementId, nom)) {
            throw new BadRequestException("Un article portant ce nom existe déjà");
        }
        ArticleStock article = ArticleStock.builder()
                .nom(nom)
                .categorie(nettoyer(request.getCategorie()))
                .unite(uniteOuDefaut(request.getUnite()))
                .seuilAlerte(request.getSeuilAlerte() != null ? request.getSeuilAlerte() : 0)
                .build();
        return articleRepository.save(article);
    }

    @Override
    public ArticleStock modifierArticle(Long id, ArticleStockRequest request) {
        ArticleStock article = trouverArticle(id);
        String nom = request.getNom().trim();
        if (!article.getNom().equalsIgnoreCase(nom)
                && articleRepository.existsByEtablissementIdAndNomIgnoreCase(tenantGuard.requireEtablissementId(), nom)) {
            throw new BadRequestException("Un article portant ce nom existe déjà");
        }
        article.setNom(nom);
        article.setCategorie(nettoyer(request.getCategorie()));
        article.setUnite(uniteOuDefaut(request.getUnite()));
        if (request.getSeuilAlerte() != null) {
            article.setSeuilAlerte(request.getSeuilAlerte());
        }
        return articleRepository.save(article);
    }

    @Override
    public void supprimerArticle(Long id) {
        ArticleStock article = trouverArticle(id);
        mouvementRepository.deleteByArticleId(article.getId());
        articleRepository.delete(article);
    }

    @Override
    public MouvementStock enregistrerMouvement(MouvementStockRequest request) {
        String type = request.getType().trim().toUpperCase();
        if (!ENTREE.equals(type) && !SORTIE.equals(type)) {
            throw new BadRequestException("Type de mouvement invalide : " + request.getType());
        }
        ArticleStock article = trouverArticle(request.getArticleId());

        int delta = ENTREE.equals(type) ? request.getQuantite() : -request.getQuantite();
        int nouvelleQuantite = article.getQuantite() + delta;
        if (nouvelleQuantite < 0) {
            throw new BadRequestException("Stock insuffisant : il reste " + article.getQuantite()
                    + " " + article.getUnite() + " de « " + article.getNom() + " »");
        }
        article.setQuantite(nouvelleQuantite);
        articleRepository.save(article);

        MouvementStock mouvement = MouvementStock.builder()
                .article(article)
                .type(type)
                .quantite(request.getQuantite())
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .motif(nettoyer(request.getMotif()))
                .beneficiaire(nettoyer(request.getBeneficiaire()))
                .build();
        try {
            utilisateurRepository.findById(SecurityUtils.getCurrentUserId()).ifPresent(mouvement::setEnregistrePar);
        } catch (Exception ignored) {
            // pas de contexte d'authentification
        }
        return mouvementRepository.save(mouvement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStock> listerMouvements(Long articleId) {
        ArticleStock article = trouverArticle(articleId);
        return mouvementRepository.findByArticleIdOrderByDateDescIdDesc(article.getId());
    }

    private ArticleStock trouverArticle(Long id) {
        return tenantGuard.requireSameTenant(articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable")));
    }

    private static String nettoyer(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static String uniteOuDefaut(String unite) {
        return unite == null || unite.isBlank() ? "unité" : unite.trim();
    }
}
