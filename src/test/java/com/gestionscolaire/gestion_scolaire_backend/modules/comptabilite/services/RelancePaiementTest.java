package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.security.AccesFamille;
import com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.RelancePaiementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Relance des parents en retard : qui est notifié, qui est ignoré, et la limite anti-spam. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelancePaiementTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private FraisScolariteRepository fraisScolariteRepository;
    @Mock private TenantGuard tenantGuard;
    @Mock private AccesFamille accesFamille;
    @Mock private NotificationService notificationService;
    @InjectMocks private PaiementServiceImpl service;

    private Classe classe;

    @BeforeEach
    void preparer() {
        Utilisateur direction = new Utilisateur();
        direction.setId(99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(direction), null, List.of()));

        classe = Classe.builder().id(5L).nom("10ème CG1").build();
        FraisScolarite echu = FraisScolarite.builder().id(10L).classe(classe).titre("Inscription")
                .montant(20000.0).dateEcheance(LocalDate.now().minusDays(10)).build();
        when(fraisScolariteRepository.findByClasseId(5L)).thenReturn(List.of(echu));
        when(paiementRepository.findByEleveId(anyLong())).thenReturn(List.of());

        when(tenantGuard.crossTenant()).thenReturn(false);
        when(tenantGuard.requireEtablissementId()).thenReturn(1L);
        when(tenantGuard.filterSameNiveau(any(), any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private Parent parentAvecCompte(long utilisateurId) {
        Utilisateur compte = new Utilisateur();
        compte.setId(utilisateurId);
        Profil profil = Profil.builder().nom("Traoré").prenom("Awa").utilisateur(compte).build();
        return Parent.builder().id(utilisateurId).profil(profil).build();
    }

    private Eleve eleve(long id, Parent parent, LocalDateTime derniereRelance) {
        Profil profil = Profil.builder().nom("Diarra").prenom("Moussa").build();
        Eleve e = Eleve.builder().id(id).classe(classe).profil(profil).parent(parent).statut("ACTIF")
                .derniereRelancePaiement(derniereRelance).build();
        when(eleveRepository.findById(id)).thenReturn(java.util.Optional.of(e));
        return e;
    }

    @Test
    void relanceLeParentEtMemoriseLaDate() {
        Eleve e = eleve(1L, parentAvecCompte(200L), null);
        when(eleveRepository.findByEtablissementId(1L)).thenReturn(List.of(e));

        RelancePaiementResponse r = service.relancerRetards(null);

        assertEquals(new RelancePaiementResponse(1, 0, 0), r);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).envoyerNotification(n.capture(), eq(99L), eq(200L));
        assertEquals("Rappel de paiement", n.getValue().getTitre());
        assertTrue(n.getValue().getContenu().contains("20 000"), "le montant dû figure dans le message");
        assertTrue(n.getValue().getContenu().contains("Moussa Diarra"));
        assertNotNull(e.getDerniereRelancePaiement());
        verify(eleveRepository).save(e);
    }

    @Test
    void parentSansCompte_estCompteMaisPasNotifie() {
        Parent sansCompte = Parent.builder().id(7L).profil(Profil.builder().nom("X").build()).build();
        Eleve e = eleve(1L, sansCompte, null);
        when(eleveRepository.findByEtablissementId(1L)).thenReturn(List.of(e));

        RelancePaiementResponse r = service.relancerRetards(List.of());

        assertEquals(new RelancePaiementResponse(0, 1, 0), r);
        verify(notificationService, never()).envoyerNotification(any(), any(), any());
    }

    @Test
    void familleDejaRelanceeRecemment_nEstPasRelancee() {
        Eleve e = eleve(1L, parentAvecCompte(200L), LocalDateTime.now().minusDays(1));
        when(eleveRepository.findByEtablissementId(1L)).thenReturn(List.of(e));

        RelancePaiementResponse r = service.relancerRetards(null);

        assertEquals(new RelancePaiementResponse(0, 0, 1), r);
        verify(notificationService, never()).envoyerNotification(any(), any(), any());
    }

    @Test
    void relanceAnciennePlusDeTroisJours_repart() {
        Eleve e = eleve(1L, parentAvecCompte(200L), LocalDateTime.now().minusDays(4));
        when(eleveRepository.findByEtablissementId(1L)).thenReturn(List.of(e));

        assertEquals(1, service.relancerRetards(null).envoyees());
    }

    @Test
    void listeDEleves_netoucheQueCeuxDemandes() {
        Eleve a = eleve(1L, parentAvecCompte(200L), null);
        Eleve b = eleve(2L, parentAvecCompte(201L), null);
        when(eleveRepository.findByEtablissementId(1L)).thenReturn(List.of(a, b));

        RelancePaiementResponse r = service.relancerRetards(List.of(2L));

        assertEquals(1, r.envoyees());
        verify(notificationService).envoyerNotification(any(), eq(99L), eq(201L));
        verify(notificationService, never()).envoyerNotification(any(), any(), eq(200L));
    }
}
