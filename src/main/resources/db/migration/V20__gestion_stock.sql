-- Gestion de stock (fournitures, consommables, tenues) tenue par le comptable.
-- La quantité courante est dénormalisée sur l'article ; l'historique complet vit dans mouvements_stock.
CREATE TABLE articles_stock (
    id BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT REFERENCES etablissements(id),
    nom VARCHAR(150) NOT NULL,
    categorie VARCHAR(80),
    unite VARCHAR(30) NOT NULL DEFAULT 'unité',
    seuil_alerte INTEGER NOT NULL DEFAULT 0,
    quantite INTEGER NOT NULL DEFAULT 0,
    date_creation TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_articles_stock_etablissement ON articles_stock(etablissement_id);

CREATE TABLE mouvements_stock (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles_stock(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    enregistre_par_id BIGINT REFERENCES utilisateurs(id),
    type VARCHAR(10) NOT NULL,
    quantite INTEGER NOT NULL,
    date DATE NOT NULL,
    motif VARCHAR(255),
    beneficiaire VARCHAR(150),
    date_creation TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_mouvements_stock_article ON mouvements_stock(article_id, date);
