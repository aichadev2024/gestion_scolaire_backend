package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite;

import com.gestionscolaire.gestion_scolaire_backend.AbstractPostgresIntegrationTest;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantContext;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EleveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie le cloisonnement des données entre établissements (ADR 0002) sur une vraie
 * base PostgreSQL (Testcontainers). Nécessite Docker — s'exécute en CI.
 */
@Transactional
class TenantIsolationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired EleveService eleveService;
    @Autowired EleveRepository eleveRepository;
    @Autowired EtablissementRepository etablissementRepository;
    @Autowired ProfilRepository profilRepository;

    private Etablissement ecoleA;
    private Etablissement ecoleB;
    private Eleve eleveA;
    private Eleve eleveB;

    @BeforeEach
    void seed() {
        ecoleA = nouvelEtablissement("École A");
        ecoleB = nouvelEtablissement("École B");
        eleveA = nouvelEleve("Awa", "Traoré", ecoleA);
        eleveB = nouvelEleve("Bakary", "Coulibaly", ecoleB);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listerTous_neRenvoieQueLesElevesDeSonEtablissement() {
        TenantContext.set(ecoleA.getId(), false);

        List<Eleve> vus = eleveService.listerTous();

        assertEquals(1, vus.size());
        assertEquals(eleveA.getId(), vus.get(0).getId());
    }

    @Test
    void trouverParId_dUnEleveDUnAutreEtablissement_renvoieVide() {
        TenantContext.set(ecoleA.getId(), false);

        assertTrue(eleveService.trouverParId(eleveB.getId()).isEmpty(),
                "un élève d'un autre établissement doit être traité comme inexistant");
        assertTrue(eleveService.trouverParId(eleveA.getId()).isPresent());
    }

    @Test
    void superAdmin_voitTousLesEtablissements() {
        TenantContext.set(null, true);

        assertEquals(2, eleveService.listerTous().size());
    }

    @Test
    void tenantEntityListener_rattacheAutomatiquementLEtablissementCourant() {
        TenantContext.set(ecoleB.getId(), false);

        Profil profil = profilRepository.save(Profil.builder().prenom("Auto").nom("Rattaché").build());
        Eleve sansEtab = eleveRepository.save(
                Eleve.builder().profil(profil).matricule("E-AUTO-" + suffixe()).statut("ACTIF").build());
        eleveRepository.flush();

        Eleve recharge = eleveRepository.findById(sansEtab.getId()).orElseThrow();
        assertNotNull(recharge.getEtablissement(), "le TenantEntityListener aurait dû rattacher l'établissement");
        assertEquals(ecoleB.getId(), recharge.getEtablissement().getId());
    }

    // ---- helpers ----

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Etablissement nouvelEtablissement(String nom) {
        return etablissementRepository.save(Etablissement.builder()
                .nom(nom)
                .code(nom.toLowerCase().replace(" ", "-").replace("é", "e") + "-" + suffixe())
                .statut(StatutEtablissement.ACTIF)
                .build());
    }

    private Eleve nouvelEleve(String prenom, String nom, Etablissement etab) {
        Profil profil = profilRepository.save(Profil.builder().prenom(prenom).nom(nom).build());
        return eleveRepository.save(Eleve.builder()
                .profil(profil)
                .matricule("E-" + prenom.toUpperCase() + "-" + suffixe())
                .etablissement(etab)
                .statut("ACTIF")
                .build());
    }
}
