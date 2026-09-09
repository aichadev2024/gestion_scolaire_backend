# 🏫 Netaa École - Backend Spring Boot 🇲🇱

API REST Java Spring Boot pour la gestion globale d'un établissement scolaire (Administration, Élèves, Enseignants, Classes, Notes, Bulletins, Présences & Finances).

---

## 🛠️ Stack Technique

- **Langage & Framework** : Java 21 / Spring Boot 4.1
- **Sécurité** : Spring Security + Authentification JWT (JSON Web Token) + OTP e-mail au premier login
- **Base de données** : PostgreSQL + Hibernate JPA
- **Service E-mail** : Brevo API / SMTP
- **Outil de Build** : Maven (`mvnw`)

---

## 🔐 Configuration des Variables d'Environnement

Pour des raisons de sécurité, aucun mot de passe n'est stocké en dur dans le code.

1. Copiez le fichier d'exemple `.env.example` vers `.env` :
   ```bash
   cp .env.example .env
   ```

2. Adaptez les variables selon votre environnement local :
   - `DATABASE_URL` : `jdbc:postgresql://localhost:5432/gestion_scolaire_db`
   - `DB_USERNAME` : Nom d'utilisateur PostgreSQL (ex: `postgres`)
   - `DB_PASSWORD` : Mot de passe de votre base locale
   - `JWT_SECRET` : Clé secrète JWT (**obligatoire**, min 32 caractères — `openssl rand -base64 48`)
   - `SMTP_USERNAME` & `SMTP_PASSWORD` : Vos identifiants SMTP Brevo (optionnel en dev)
   - `JPA_SHOW_SQL` : `true` pour journaliser le SQL en local (défaut `false`)

> Le schéma est géré par Hibernate (`ddl-auto`). Le fichier `schema.sql` historique a été retiré
> (obsolète). Des migrations Flyway versionnées seront introduites en Phase 1.

---

## 🚀 Lancement Rapide en Local

### Sur Windows (PowerShell) :
Un script local pré-configuré `run-dev.ps1` (ignoré par Git) est disponible :
```powershell
.\run-dev.ps1
```

### Avec Maven :
```bash
./mvnw spring-boot:run
```

L'API sera accessible sur `http://localhost:8089/api`.

---

## 📌 Endpoints Principaux

- **Authentification** : `POST /api/auth/login`, `POST /api/auth/verify-otp`
- **Élèves** : `GET /api/eleves`, `GET /api/eleves/classe/{classeId}`, `GET /api/eleves/parent/{parentId}`
- **Enseignants** : `GET /api/enseignants`
- **Classes & Matières** : `GET /api/classes`, `GET /api/matieres`
- **Bulletins Scolaires** : `GET /api/bulletins/eleve/{eleveId}`
- **Paiements & Reçus** : `GET /api/paiements/eleve/{eleveId}`, `POST /api/paiements`
- **Présences** : `GET /api/presences/eleve/{eleveId}`, `POST /api/presences`
- **Emplois du Temps** : `GET /api/emplois-du-temps/classe/{classeId}`, `GET /api/emplois-du-temps/enseignant/{teacherId}`
