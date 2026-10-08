package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.NoteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.RapportTrimestrielRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Matiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Presence;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.IncidentDisciplineRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.PresenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Les chiffres du rapport doivent coller aux règles du bulletin (matière sans note = 0, coefficients). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RapportTrimestrielServiceTest {

    @Mock private TenantGuard tenantGuard;
    @Mock private EtablissementRepository etablissementRepository;
    @Mock private ClasseRepository classeRepository;
    @Mock private ClasseMatiereRepository classeMatiereRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private PresenceRepository presenceRepository;
    @Mock private IncidentDisciplineRepository incidentRepository;
    @Mock private RapportTrimestrielRepository rapportRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private FraisScolariteRepository fraisRepository;
    @Mock private PaiementRepository paiementRepository;

    private RapportTrimestrielServiceImpl service;
    private Classe classe;
    private ClasseMatiere maths;
    private ClasseMatiere francais;
    private Eleve awa;
    private Eleve moussa;
    private Eleve sansNote;

    @BeforeEach
    void preparer() {
        service = new RapportTrimestrielServiceImpl(tenantGuard, etablissementRepository, classeRepository,
                classeMatiereRepository, eleveRepository, noteRepository, presenceRepository, incidentRepository,
                rapportRepository, utilisateurRepository, fraisRepository, paiementRepository);

        Etablissement etab = Etablissement.builder().id(1L).nom("Lycée Test").rive("RIVE GAUCHE").cap("CAP de Bamako-Centre").build();
        when(tenantGuard.requireEtablissementId()).thenReturn(1L);
        when(tenantGuard.requireSameTenant(any(Classe.class))).thenAnswer(i -> i.getArgument(0));
        when(etablissementRepository.findById(1L)).thenReturn(Optional.of(etab));

        classe = Classe.builder().id(5L).nom("10ème CG1").build();
        when(classeRepository.findById(5L)).thenReturn(Optional.of(classe));

        maths = ClasseMatiere.builder().id(100L).classe(classe).matiere(Matiere.builder().nom("Mathématiques").build()).coefficient(4.0).build();
        francais = ClasseMatiere.builder().id(101L).classe(classe).matiere(Matiere.builder().nom("Français").build()).coefficient(2.0).build();
        when(classeMatiereRepository.findByClasseId(5L)).thenReturn(List.of(maths, francais));

        awa = eleve(1L, "F", "Traoré", "Awa");
        moussa = eleve(2L, "M", "Diarra", "Moussa");
        sansNote = eleve(3L, "M", "Keïta", "Oumar");
        when(eleveRepository.findByEtablissementId(1L)).thenReturn(List.of(awa, moussa, sansNote));

        // Awa : maths 16 (notes 14 et 18), français 12 -> (16x4 + 12x2)/6 = 14,67
        // Moussa : maths 6, français sans note -> (6x4 + 0x2)/6 = 4
        when(noteRepository.findByEleveIdInAndPeriodeIgnoreCase(anyCollection(), anyString())).thenReturn(List.of(
                note(awa, maths, 14), note(awa, maths, 18), note(awa, francais, 12), note(moussa, maths, 6)));

        when(presenceRepository.findByEtablissementIdAndDateBetween(anyLong(), any(), any())).thenReturn(List.of(
                presence(awa, "PRESENT", false), presence(awa, "RETARD", false),
                presence(moussa, "ABSENT", true), presence(moussa, "ABSENT", false)));
        // Frais de la classe : inscription 20 000 (échue) + scolarité 50 000 (à venir)
        when(fraisRepository.findByClasseId(5L)).thenReturn(List.of(
                FraisScolarite.builder().id(1L).classe(classe).titre("Inscription").montant(20000.0)
                        .dateEcheance(LocalDate.now().minusDays(30)).build(),
                FraisScolarite.builder().id(2L).classe(classe).titre("Scolarité").montant(50000.0)
                        .dateEcheance(LocalDate.now().plusDays(60)).build()));
        moussa.setArrieresMontant(10000.0);
        when(paiementRepository.findByEleveIdIn(anyCollection())).thenReturn(List.of(
                Paiement.builder().eleve(awa).montantPaye(20000.0).datePaiement(LocalDateTime.of(2026, 11, 10, 9, 0)).build(),
                Paiement.builder().eleve(moussa).montantPaye(5000.0).datePaiement(LocalDateTime.of(2026, 11, 12, 9, 0)).build()));
        when(incidentRepository.findByEtablissementIdAndDateBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(rapportRepository.findByEtablissementIdAndClasseIdAndPeriodeAndAnneeScolaire(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void commeDirecteur() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "directeur", null, List.of(new SimpleGrantedAuthority("ROLE_DIRECTEUR"))));
    }

    private Eleve eleve(long id, String genre, String nom, String prenom) {
        return Eleve.builder().id(id).classe(classe).statut("ACTIF")
                .profil(Profil.builder().nom(nom).prenom(prenom).genre(genre).build()).build();
    }

    private Note note(Eleve e, ClasseMatiere cm, double valeur) {
        return Note.builder().eleve(e).classeMatiere(cm).periode("TRIMESTRE_1").valeur(valeur).build();
    }

    private Presence presence(Eleve e, String statut, boolean justifie) {
        return Presence.builder().eleve(e).statut(statut).estJustifie(justifie).date(LocalDate.of(2026, 11, 3)).build();
    }

    @Test
    void effectifsEtResultats_suiventLaRegleDuBulletin() {
        RapportTrimestrielResponse r = service.generer(5L, "trimestre_1", "2026/2027", null, null);

        assertEquals(3, r.effectifs().total());
        assertEquals(1, r.effectifs().filles());
        assertEquals(2, r.effectifs().garcons());

        assertEquals(2, r.resultats().evalues());
        assertEquals(1, r.resultats().nonEvalues());
        assertEquals(14.67, r.resultats().moyenneMax());
        assertEquals(4.0, r.resultats().moyenneMin());
        assertEquals(9.33, r.resultats().moyenne());
        assertEquals(50.0, r.resultats().tauxReussite());
        assertEquals(1, r.resultats().mentions().get("Bien"));
        assertEquals(1, r.resultats().mentions().get("Insuffisant"));
    }

    @Test
    void classementEtEleveEnDifficulte() {
        RapportTrimestrielResponse r = service.generer(5L, "TRIMESTRE_1", "2026/2027", null, null);

        assertEquals("TRAORÉ Awa", r.meilleurs().get(0).nom());
        assertEquals(1, r.enDifficulte().size());
        assertEquals("DIARRA Moussa", r.enDifficulte().get(0).nom());
    }

    @Test
    void detailParMatiere_ignoreLesElevesSansNoteDansCetteMatiere() {
        RapportTrimestrielResponse r = service.generer(5L, "TRIMESTRE_1", "2026/2027", null, null);

        var m = r.matieres().stream().filter(l -> l.matiere().equals("Mathématiques")).findFirst().orElseThrow();
        assertEquals(11.0, m.moyenne());
        assertEquals(2, m.eleves());
        var f = r.matieres().stream().filter(l -> l.matiere().equals("Français")).findFirst().orElseThrow();
        assertEquals(12.0, f.moyenne());
        assertEquals(1, f.eleves());
    }

    @Test
    void presences_retardsComptentCommePresents() {
        RapportTrimestrielResponse r = service.generer(5L, "TRIMESTRE_1", "2026/2027", null, null);

        assertEquals(1, r.presences().presents());
        assertEquals(1, r.presences().retards());
        assertEquals(1, r.presences().absencesJustifiees());
        assertEquals(1, r.presences().absencesNonJustifiees());
        assertEquals(50.0, r.presences().tauxPresence());
    }

    @Test
    void enteteOfficielEtDatesParDefautDuTrimestre() {
        RapportTrimestrielResponse r = service.generer(5L, "TRIMESTRE_2", "2026/2027", null, null);

        assertEquals("RIVE GAUCHE", r.entete().rive());
        assertEquals("CAP de Bamako-Centre", r.entete().cap());
        assertEquals(LocalDate.of(2027, 1, 1), r.dateDebut());
        assertEquals(LocalDate.of(2027, 3, 31), r.dateFin());
        assertEquals("Classe de 10ème CG1", r.portee());
        assertNull(r.commentaire());
        assertTrue(r.classes().isEmpty());
    }

    @Test
    void finances_reserveesALaDirection() {
        assertNull(service.generer(5L, "TRIMESTRE_1", "2026/2027", null, null).finances(),
                "sans rôle directeur, aucun chiffre financier");
    }

    @Test
    void finances_attenduEncaisseEtRetards() {
        commeDirecteur();

        var f = service.generer(5L, "TRIMESTRE_1", "2026/2027", null, null).finances();

        assertNotNull(f);
        assertEquals(220000.0, f.attendu());          // 3 x 70 000 + 10 000 d'arriérés
        assertEquals(25000.0, f.encaisse());
        assertEquals(195000.0, f.reste());
        assertEquals(11.36, f.tauxRecouvrement());
        assertEquals(10000.0, f.arrieres());
        assertEquals(45000.0, f.montantEnRetard());    // Moussa 30 000 - 5 000 ; Oumar 20 000 ; Awa soldée
        assertEquals(2, f.elevesEnRetard());
        assertEquals(25000.0, f.encaisseSurPeriode());
        assertEquals("FCFA", f.devise());
    }
}
