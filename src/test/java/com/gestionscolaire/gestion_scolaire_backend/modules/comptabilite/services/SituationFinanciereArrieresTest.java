package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.security.AccesFamille;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Les arriérés d'un élève sont une dette plus ancienne que l'année en cours : un paiement global les solde d'abord. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SituationFinanciereArrieresTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private FraisScolariteRepository fraisScolariteRepository;
    @Mock private TenantGuard tenantGuard;
    @Mock private AccesFamille accesFamille;
    @InjectMocks private PaiementServiceImpl service;

    private Eleve eleve;

    @BeforeEach
    void preparer() {
        Classe classe = Classe.builder().id(5L).nom("10ème CG1").build();
        eleve = Eleve.builder().id(1L).classe(classe).build();
        when(eleveRepository.findById(1L)).thenReturn(Optional.of(eleve));
        when(tenantGuard.requireSameTenant(any(Eleve.class))).thenAnswer(i -> i.getArgument(0));

        FraisScolarite inscription = FraisScolarite.builder().id(10L).classe(classe).titre("Inscription")
                .montant(20000.0).dateEcheance(LocalDate.now().plusDays(30)).build();
        FraisScolarite scolarite = FraisScolarite.builder().id(11L).classe(classe).titre("Scolarité tranche 1")
                .montant(50000.0).dateEcheance(LocalDate.now().plusDays(60)).build();
        when(fraisScolariteRepository.findByClasseId(5L)).thenReturn(List.of(inscription, scolarite));
    }

    private Paiement paiementLibre(double montant) {
        return Paiement.builder().eleve(eleve).montantPaye(montant).modePaiement("ESPECES")
                .numeroRecu("R-" + montant).datePaiement(LocalDateTime.now()).build();
    }

    @Test
    void sansArrieres_aucuneLigneArrieres() {
        when(paiementRepository.findByEleveId(1L)).thenReturn(List.of());

        SituationFinanciereResponse s = service.situationEleve(1L);

        assertEquals(2, s.lignes().size());
        assertEquals(70000.0, s.totalDu());
        assertTrue(s.lignes().stream().noneMatch(l -> "ARRIERES".equals(l.type())));
    }

    @Test
    void arrieres_apparaissentEnPremierEtAugmententLeTotalDu() {
        eleve.setArrieresMontant(30000.0);
        eleve.setArrieresLibelle("Arriérés 2025-2026");
        when(paiementRepository.findByEleveId(1L)).thenReturn(List.of());

        SituationFinanciereResponse s = service.situationEleve(1L);

        assertEquals(3, s.lignes().size());
        SituationFinanciereResponse.LigneFrais arrieres = s.lignes().get(0);
        assertEquals("ARRIERES", arrieres.type());
        assertEquals("Arriérés 2025-2026", arrieres.titre());
        assertEquals(30000.0, arrieres.reste());
        assertEquals("EN_RETARD", arrieres.statut());
        assertNull(arrieres.fraisId());
        assertEquals(100000.0, s.totalDu());
        assertEquals(100000.0, s.reste());
    }

    @Test
    void paiementGlobal_soldeLesArrieresAvantLesFraisDeLAnnee() {
        eleve.setArrieresMontant(30000.0);
        when(paiementRepository.findByEleveId(1L)).thenReturn(List.of(paiementLibre(35000.0)));

        SituationFinanciereResponse s = service.situationEleve(1L);

        SituationFinanciereResponse.LigneFrais arrieres = s.lignes().get(0);
        assertEquals("PAYE", arrieres.statut());
        assertEquals(0.0, arrieres.reste());
        // Les 5 000 F restants du paiement vont à l'échéance la plus proche (inscription : 20 000 F).
        SituationFinanciereResponse.LigneFrais inscription = s.lignes().get(1);
        assertEquals(5000.0, inscription.paye());
        assertEquals("PARTIEL", inscription.statut());
        assertEquals(35000.0, s.totalPaye());
        assertEquals(65000.0, s.reste());
    }

    @Test
    void arrieresSeuls_sansFraisDefinis_nePrennentPasLAnneeCouranteEnCompte() {
        eleve.setArrieresMontant(12000.0);
        when(fraisScolariteRepository.findByClasseId(5L)).thenReturn(List.of());
        when(paiementRepository.findByEleveId(1L)).thenReturn(List.of());

        SituationFinanciereResponse s = service.situationEleve(1L);

        assertFalse(s.aucunFraisDefini(), "des arriérés sont une dette à afficher même sans frais définis");
        assertFalse(s.scolariteDefinie(), "les arriérés ne comptent pas comme scolarité de l'année");
        assertEquals(12000.0, s.reste());
    }
}
