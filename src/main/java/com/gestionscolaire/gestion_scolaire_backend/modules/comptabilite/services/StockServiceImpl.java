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
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class StockServiceImpl implements StockService {

    private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);
    private static final String ENTREE = "ENTREE";
    private static final String SORTIE = "SORTIE";
    private static final Set<String> ROLES_ALERTE_STOCK = Set.of("DIRECTEUR", "COMPTABLE");

    private final ArticleStockRepository articleRepository;
    private final MouvementStockRepository mouvementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final TenantGuard tenantGuard;

    public StockServiceImpl(ArticleStockRepository articleRepository,
                            MouvementStockRepository mouvementRepository,
                            UtilisateurRepository utilisateurRepository,
                            NotificationService notificationService,
                            TenantGuard tenantGuard) {
        this.articleRepository = articleRepository;
        this.mouvementRepository = mouvementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
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
        int ancienneQuantite = article.getQuantite();
        int nouvelleQuantite = ancienneQuantite + delta;
        if (nouvelleQuantite < 0) {
            throw new BadRequestException("Stock insuffisant : il reste " + article.getQuantite()
                    + " " + article.getUnite() + " de « " + article.getNom() + " »");
        }
        article.setQuantite(nouvelleQuantite);
        articleRepository.save(article);

        // Notifie seulement au moment où le seuil est franchi (pas à chaque sortie suivante
        // une fois déjà en alerte, pour ne pas spammer la direction/le comptable).
        boolean etaitEnAlerte = ancienneQuantite <= article.getSeuilAlerte();
        if (SORTIE.equals(type) && !etaitEnAlerte && article.isEnAlerte()) {
            notifierAlerteStock(article);
        }

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

    /** Prévient direction et comptable(s) de l'établissement qu'un article vient de passer sous son seuil. */
    private void notifierAlerteStock(ArticleStock article) {
        try {
            Long etablissementId = tenantGuard.requireEtablissementId();
            String titre = "Stock bas : " + article.getNom();
            String contenu = "Il ne reste plus que " + article.getQuantite() + " " + article.getUnite()
                    + " de « " + article.getNom() + " » — seuil d'alerte fixé à " + article.getSeuilAlerte() + ".";
            Long expediteurId;
            try {
                expediteurId = SecurityUtils.getCurrentUserId();
            } catch (Exception e) {
                expediteurId = null;
            }
            for (Utilisateur u : utilisateurRepository.findByEtablissementId(etablissementId)) {
                if (u.getRole() == null || !ROLES_ALERTE_STOCK.contains(u.getRole().getNom().toUpperCase())) continue;
                try {
                    notificationService.envoyerNotification(
                            Notification.builder().titre(titre).contenu(contenu).build(), expediteurId, u.getId());
                } catch (Exception ignored) {
                    // un destinataire en échec ne bloque pas les autres
                }
            }
        } catch (Exception e) {
            log.warn("Notification d'alerte stock non envoyée pour l'article {} : {}", article.getId(), e.getMessage());
        }
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
