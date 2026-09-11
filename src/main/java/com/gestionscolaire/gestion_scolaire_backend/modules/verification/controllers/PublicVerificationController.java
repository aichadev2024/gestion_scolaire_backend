package com.gestionscolaire.gestion_scolaire_backend.modules.verification.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Bulletin;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.BulletinRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vérification publique (sans authentification) des documents officiels
 * générés par l'application : carte scolaire, reçu de paiement, bulletin,
 * fiche d'abonnement établissement. Chaque document imprimé porte un QR qui
 * pointe ici — le scan affiche la donnée réelle en base, ce qui rend une
 * copie modifiée détectable (le papier peut être trafiqué, pas la base).
 *
 * <p>Toujours une réponse {@code 200} avec {@code valide: true/false} plutôt
 * qu'un 404, pour ne pas distinguer « code inconnu » d'une autre erreur.</p>
 */
@RestController
@RequestMapping("/api/public/verify")
public class PublicVerificationController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EleveRepository eleveRepository;
    private final PaiementRepository paiementRepository;
    private final BulletinRepository bulletinRepository;
    private final EtablissementRepository etablissementRepository;

    public PublicVerificationController(EleveRepository eleveRepository,
                                         PaiementRepository paiementRepository,
                                         BulletinRepository bulletinRepository,
                                         EtablissementRepository etablissementRepository) {
        this.eleveRepository = eleveRepository;
        this.paiementRepository = paiementRepository;
        this.bulletinRepository = bulletinRepository;
        this.etablissementRepository = etablissementRepository;
    }

    private static Map<String, Object> invalide() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valide", false);
        return m;
    }

    @GetMapping("/eleve/{matricule}")
    public ResponseEntity<Map<String, Object>> verifierEleve(@PathVariable String matricule) {
        return eleveRepository.findByMatricule(matricule).map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("valide", true);
            m.put("type", "carte_scolaire");
            m.put("nom", nom(e));
            m.put("prenom", prenom(e));
            m.put("matricule", e.getMatricule());
            m.put("classe", e.getClasse() != null ? e.getClasse().getNom() : "—");
            m.put("statut", e.getStatut());
            m.put("etablissement", e.getEtablissement() != null ? e.getEtablissement().getNom() : "—");
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.ok(invalide()));
    }

    @GetMapping("/recu/{numeroRecu}")
    public ResponseEntity<Map<String, Object>> verifierRecu(@PathVariable String numeroRecu) {
        return paiementRepository.findByNumeroRecu(numeroRecu).map(p -> {
            Eleve e = p.getEleve();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("valide", true);
            m.put("type", "recu_paiement");
            m.put("numeroRecu", p.getNumeroRecu());
            m.put("eleveNom", e != null ? nom(e) : "—");
            m.put("elevePrenom", e != null ? prenom(e) : "—");
            m.put("matricule", e != null ? e.getMatricule() : "—");
            m.put("montantPaye", p.getMontantPaye());
            m.put("datePaiement", p.getDatePaiement() != null ? p.getDatePaiement().format(DATE_FMT) : null);
            m.put("modePaiement", p.getModePaiement());
            m.put("fraisTitre", p.getFraisScolarite() != null ? p.getFraisScolarite().getTitre() : "—");
            m.put("etablissement", p.getEtablissement() != null ? p.getEtablissement().getNom() : "—");
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.ok(invalide()));
    }

    @GetMapping("/bulletin/{code}")
    public ResponseEntity<Map<String, Object>> verifierBulletin(@PathVariable String code) {
        return bulletinRepository.findByCodeVerification(code).map(b -> {
            Eleve e = b.getEleve();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("valide", true);
            m.put("type", "bulletin");
            m.put("eleveNom", e != null ? nom(e) : "—");
            m.put("elevePrenom", e != null ? prenom(e) : "—");
            m.put("matricule", e != null ? e.getMatricule() : "—");
            m.put("classe", b.getClasse() != null ? b.getClasse().getNom() : "—");
            m.put("periode", b.getPeriode());
            m.put("anneeScolaire", b.getAnneeScolaire());
            m.put("moyenneGenerale", b.getMoyenneGenerale());
            m.put("estVerrouille", b.getEstVerrouille());
            m.put("etablissement", b.getEtablissement() != null ? b.getEtablissement().getNom() : "—");
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.ok(invalide()));
    }

    @GetMapping("/etablissement/{code}")
    public ResponseEntity<Map<String, Object>> verifierEtablissement(@PathVariable String code) {
        return etablissementRepository.findByCode(code).map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("valide", true);
            m.put("type", "abonnement");
            m.put("nom", e.getNom());
            m.put("code", e.getCode());
            m.put("planTarifaire", e.getPlanTarifaire());
            m.put("statut", e.getStatut());
            m.put("dateExpirationAbonnement",
                    e.getDateExpirationAbonnement() != null ? e.getDateExpirationAbonnement().format(DATE_FMT) : null);
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.ok(invalide()));
    }

    private static String nom(Eleve e) {
        return e.getProfil() != null ? e.getProfil().getNom() : "—";
    }

    private static String prenom(Eleve e) {
        return e.getProfil() != null ? e.getProfil().getPrenom() : "—";
    }
}
