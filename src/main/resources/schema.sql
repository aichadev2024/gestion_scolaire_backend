-- SCRIPT D'INITIALISATION DE LA BASE DE DONNÉES POSTGRESQL EN FRANÇAIS (GESTION SCOLAIRE)

-- 1. Rôles des utilisateurs
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE
);

-- Insertion des rôles par défaut
INSERT INTO roles (nom) VALUES 
('ADMIN'), 
('DIRECTEUR'), 
('SECRETAIRE'), 
('COMPTABLE'), 
('ENSEIGNANT'), 
('ELEVE'), 
('PARENT');

-- 2. Utilisateurs
CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    est_actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Profils utilisateurs (Informations civiles)
CREATE TABLE profils (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT UNIQUE REFERENCES utilisateurs(id) ON DELETE CASCADE,
    prenom VARCHAR(100) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20),
    photo_url TEXT,
    genre VARCHAR(10),
    date_naissance DATE,
    adresse TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Niveaux d'enseignement
CREATE TABLE niveaux (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE
);

-- 5. Classes
CREATE TABLE classes (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    niveau_id INT NOT NULL REFERENCES niveaux(id) ON DELETE RESTRICT,
    capacite_max INT NOT NULL DEFAULT 40,
    enseignant_principal_id BIGINT, -- Clé étrangère ajoutée après la création de la table enseignants
    annee_scolaire VARCHAR(20) NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Parents
CREATE TABLE parents (
    id BIGSERIAL PRIMARY KEY,
    profil_id BIGINT UNIQUE NOT NULL REFERENCES profils(id) ON DELETE CASCADE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Élèves
CREATE TABLE eleves (
    id BIGSERIAL PRIMARY KEY,
    profil_id BIGINT UNIQUE NOT NULL REFERENCES profils(id) ON DELETE CASCADE,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    classe_id BIGINT REFERENCES classes(id) ON DELETE SET NULL,
    parent_id BIGINT REFERENCES parents(id) ON DELETE SET NULL,
    statut VARCHAR(20) DEFAULT 'ACTIF', -- ACTIF, ARCHIVE (Soft delete)
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Enseignants
CREATE TABLE enseignants (
    id BIGSERIAL PRIMARY KEY,
    profil_id BIGINT UNIQUE NOT NULL REFERENCES profils(id) ON DELETE CASCADE,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    biographie TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Liaison tardive pour le professeur principal de la classe
ALTER TABLE classes 
ADD CONSTRAINT fk_classes_enseignant_principal 
FOREIGN KEY (enseignant_principal_id) REFERENCES enseignants(id) ON DELETE SET NULL;

-- 9. Matières
CREATE TABLE matieres (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. Table de jointure Classe-Matière-Enseignant (avec coefficient)
CREATE TABLE classes_matieres (
    id BIGSERIAL PRIMARY KEY,
    classe_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    matiere_id BIGINT NOT NULL REFERENCES matieres(id) ON DELETE CASCADE,
    enseignant_id BIGINT REFERENCES enseignants(id) ON DELETE SET NULL,
    coefficient DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    CONSTRAINT uq_classe_matiere UNIQUE (classe_id, matiere_id)
);

-- 11. Notes
CREATE TABLE notes (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id) ON DELETE CASCADE,
    classe_matiere_id BIGINT NOT NULL REFERENCES classes_matieres(id) ON DELETE CASCADE,
    periode VARCHAR(20) NOT NULL, -- ex: TRIMESTRE_1, SEMESTRE_1
    type_evaluation VARCHAR(50) NOT NULL, -- ex: DEVOIR, EXAMEN
    valeur DOUBLE PRECISION NOT NULL, -- Note de 0 à 20
    note_max DOUBLE PRECISION DEFAULT 20.0,
    appreciation TEXT,
    cree_par BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. Présences / Absences
CREATE TABLE presences (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id) ON DELETE CASCADE,
    classe_matiere_id BIGINT REFERENCES classes_matieres(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    statut VARCHAR(20) NOT NULL, -- PRESENT, ABSENT, RETARD
    est_justifie BOOLEAN DEFAULT FALSE,
    notes_justification TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 13. Structure tarifaire (Frais de scolarité par classe)
CREATE TABLE frais_scolarite (
    id BIGSERIAL PRIMARY KEY,
    classe_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    titre VARCHAR(100) NOT NULL, -- ex: Frais de Scolarité 1ère tranche, Inscription
    montant DOUBLE PRECISION NOT NULL,
    date_echeance DATE NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 14. Historique des paiements
CREATE TABLE paiements (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id) ON DELETE CASCADE,
    frais_id BIGINT REFERENCES frais_scolarite(id) ON DELETE SET NULL,
    montant_paye DOUBLE PRECISION NOT NULL,
    mode_paiement VARCHAR(50) NOT NULL, -- ESPECES, MOBILE_MONEY, VIREMENT, CHEQUE
    reference_transaction VARCHAR(100),
    numero_recu VARCHAR(100) NOT NULL UNIQUE,
    recu_par BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    date_paiement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 15. Emploi du temps
CREATE TABLE emplois_du_temps (
    id BIGSERIAL PRIMARY KEY,
    classe_id BIGINT REFERENCES classes(id) ON DELETE CASCADE,
    classe_matiere_id BIGINT REFERENCES classes_matieres(id) ON DELETE CASCADE,
    type_creneau VARCHAR(30) DEFAULT 'COURS',
    libelle_pause VARCHAR(100),
    jour_semaine INT NOT NULL, -- 1 pour Lundi, 7 pour Dimanche
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    salle VARCHAR(50)
);

-- 16. Notifications et Messagerie
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    expediteur_id BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    destinataire_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    titre VARCHAR(150) NOT NULL,
    contenu TEXT NOT NULL,
    est_lu BOOLEAN DEFAULT FALSE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 17. Logs d'audit (Journal d'activité)
CREATE TABLE journaux_activites (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    description TEXT,
    adresse_ip VARCHAR(45),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
