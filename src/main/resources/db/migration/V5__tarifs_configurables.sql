CREATE TABLE tarifs_plan (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    prix_mensuel NUMERIC(12, 2) NOT NULL,
    date_modification TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO tarifs_plan (code, prix_mensuel) VALUES
    ('STARTER', 50000),
    ('PRO', 75000);
