package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DecisionPassageRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.PerformanceClasseResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.PerformanceClasseResponse.ElevePerf;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.PerformanceClasseResponse.MatierePerf;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.DecisionPassage;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.DecisionPassageRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.NoteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Indicateurs de performance d'une classe et proposition de passage / redoublement. */
@Service
@Transactional
public class PerformanceService {

    public static final String PERIODE_ANNUELLE = "ANNUEL";
    private static final double SEUIL_PASSAGE_DEFAUT = 10.0;
    private static final double SEUIL_REDOUBLEMENT_DEFAUT = 8.0;

    private final ClasseRepository classeRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final EleveRepository eleveRepository;
    private final NoteRepository noteRepository;
    private final DecisionPassageRepository decisionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantGuard tenantGuard;

    public PerformanceService(ClasseRepository classeRepository,
                              ClasseMatiereRepository classeMatiereRepository,
                              EleveRepository eleveRepository,
                              NoteRepository noteRepository,
                              DecisionPassageRepository decisionRepository,
                              UtilisateurRepository utilisateurRepository,
                              TenantGuard tenantGuard) {
        this.classeRepository = classeRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.eleveRepository = eleveRepository;
        this.noteRepository = noteRepository;
        this.decisionRepository = decisionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.tenantGuard = tenantGuard;
    }

    private Integer niveauDe(Classe c) {
        return c.getNiveau() != null ? c.getNiveau().getId() : null;
    }

    /** Moyenne d'un élève dans une matière : moyenne des notes de la période, ou moyenne des moyennes de période pour l'année. */
    private static double moyenneMatiere(List<Note> notes, String periode) {
        if (PERIODE_ANNUELLE.equalsIgnoreCase(periode)) {
            Map<String, Double> parPeriode = notes.stream().collect(Collectors.groupingBy(
                    Note::getPeriode, Collectors.averagingDouble(Note::getValeur)));
            return parPeriode.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        return notes.stream().filter(n -> n.getPeriode().equalsIgnoreCase(periode))
                .mapToDouble(Note::getValeur).average().orElse(0.0);
    }

    private static boolean aDesNotes(List<Note> notes, String periode) {
        return PERIODE_ANNUELLE.equalsIgnoreCase(periode)
                ? !notes.isEmpty()
                : notes.stream().anyMatch(n -> n.getPeriode().equalsIgnoreCase(periode));
    }

    private static double arrondi(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public PerformanceClasseResponse performanceClasse(Long classeId, String periode, Double seuilPassageParam, Double seuilRedoublementParam) {
        Classe classe = tenantGuard.requireSameTenant(classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
        tenantGuard.requireSameNiveau(classe, this::niveauDe);

        String p = periode == null || periode.isBlank() ? PERIODE_ANNUELLE : periode.trim().toUpperCase();
        double seuilPassage = seuilPassageParam != null ? seuilPassageParam : SEUIL_PASSAGE_DEFAUT;
        double seuilRedoublement = seuilRedoublementParam != null ? seuilRedoublementParam : SEUIL_REDOUBLEMENT_DEFAUT;
        if (seuilRedoublement > seuilPassage) {
            throw new BadRequestException("Le seuil de redoublement ne peut pas dépasser le seuil de passage.");
        }

        List<Eleve> eleves = tenantGuard.filterSameTenant(eleveRepository.findByClasseId(classeId)).stream()
                .filter(e -> e.getStatut() == null || "ACTIF".equalsIgnoreCase(e.getStatut()))
                .toList();
        List<ClasseMatiere> matieres = classeMatiereRepository.findByClasseId(classeId);

        // moyennes[eleveId][classeMatiereId] pour les matières où l'élève a des notes sur la période.
        Map<Long, Map<Long, Double>> moyennes = new HashMap<>();
        List<MatierePerf> bilanMatieres = new ArrayList<>();
        for (ClasseMatiere cm : matieres) {
            Map<Long, List<Note>> notesParEleve = noteRepository.findByClasseMatiereId(cm.getId()).stream()
                    .collect(Collectors.groupingBy(n -> n.getEleve().getId()));
            List<Double> moyennesEleves = new ArrayList<>();
            for (Eleve e : eleves) {
                List<Note> notes = notesParEleve.getOrDefault(e.getId(), List.of());
                if (!aDesNotes(notes, p)) continue;
                double m = moyenneMatiere(notes, p);
                moyennes.computeIfAbsent(e.getId(), k -> new HashMap<>()).put(cm.getId(), m);
                moyennesEleves.add(m);
            }
            if (moyennesEleves.isEmpty()) continue;
            double moy = moyennesEleves.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double reussite = 100.0 * moyennesEleves.stream().filter(m -> m >= seuilPassage).count() / moyennesEleves.size();
            bilanMatieres.add(new MatierePerf(cm.getId(),
                    cm.getMatiere() != null ? cm.getMatiere().getNom() : null,
                    com.gestionscolaire.gestion_scolaire_backend.core.dto.SeanceCoursResponse.nomEnseignant(cm),
                    arrondi(moy), Math.round(reussite * 10.0) / 10.0, moyennesEleves.size()));
        }
        bilanMatieres.sort(Comparator.comparingDouble(MatierePerf::moyenne));

        Map<Long, ClasseMatiere> parId = matieres.stream().collect(Collectors.toMap(ClasseMatiere::getId, cm -> cm));
        Map<Long, DecisionPassage> decisions = decisionRepository
                .findByEleveClasseIdAndAnneeScolaire(classeId, classe.getAnneeScolaire()).stream()
                .collect(Collectors.toMap(d -> d.getEleve().getId(), d -> d, (a, b) -> a));

        // Moyenne générale = moyenne pondérée par coefficient des matières notées.
        Map<Long, Double> generales = new HashMap<>();
        for (Eleve e : eleves) {
            Map<Long, Double> parMatiere = moyennes.get(e.getId());
            if (parMatiere == null || parMatiere.isEmpty()) continue;
            double somme = 0, coefs = 0;
            for (Map.Entry<Long, Double> en : parMatiere.entrySet()) {
                double coef = parId.get(en.getKey()).getCoefficient() != null ? parId.get(en.getKey()).getCoefficient() : 1.0;
                somme += en.getValue() * coef;
                coefs += coef;
            }
            if (coefs > 0) generales.put(e.getId(), somme / coefs);
        }

        List<Eleve> classes = new ArrayList<>(eleves);
        classes.sort(Comparator
                .comparing((Eleve e) -> generales.getOrDefault(e.getId(), -1.0)).reversed()
                .thenComparing(e -> e.getProfil() != null ? String.valueOf(e.getProfil().getNom()) : ""));

        List<ElevePerf> listeEleves = new ArrayList<>();
        int rang = 0;
        int position = 0;
        Double precedente = null;
        for (Eleve e : classes) {
            Double moy = generales.get(e.getId());
            int rangEleve = 0;
            if (moy != null) {
                position++;
                if (precedente == null || Math.abs(moy - precedente) > 1e-9) rang = position;
                precedente = moy;
                rangEleve = rang;
            }
            String proposition = moy == null ? "SANS_NOTES"
                    : moy >= seuilPassage ? "PASSAGE"
                    : moy >= seuilRedoublement ? "A_DELIBERER" : "REDOUBLEMENT";
            DecisionPassage d = decisions.get(e.getId());
            listeEleves.add(new ElevePerf(e.getId(), e.getMatricule(),
                    e.getProfil() != null ? e.getProfil().getNom() : null,
                    e.getProfil() != null ? e.getProfil().getPrenom() : null,
                    moy != null ? arrondi(moy) : null, rangEleve, proposition,
                    d != null ? d.getDecision() : null, d != null ? d.getCommentaire() : null));
        }

        List<Double> valeurs = new ArrayList<>(generales.values());
        double moyenneClasse = valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double reussite = valeurs.isEmpty() ? 0.0 : 100.0 * valeurs.stream().filter(m -> m >= seuilPassage).count() / valeurs.size();

        return new PerformanceClasseResponse(classe.getId(), classe.getNom(), classe.getAnneeScolaire(), p,
                seuilPassage, seuilRedoublement, eleves.size(), valeurs.size(),
                arrondi(moyenneClasse), Math.round(reussite * 10.0) / 10.0,
                arrondi(valeurs.stream().mapToDouble(Double::doubleValue).min().orElse(0.0)),
                arrondi(valeurs.stream().mapToDouble(Double::doubleValue).max().orElse(0.0)),
                bilanMatieres, listeEleves);
    }

    public void enregistrerDecision(DecisionPassageRequest request) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(request.getEleveId())
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        if (eleve.getClasse() == null) {
            throw new BadRequestException("Cet élève n'est affecté à aucune classe.");
        }
        tenantGuard.requireSameNiveau(eleve.getClasse(), this::niveauDe);
        String annee = eleve.getClasse().getAnneeScolaire();
        String decision = request.getDecision().trim().toUpperCase();

        if ("AUCUNE".equals(decision)) {
            decisionRepository.findByEleveIdAndAnneeScolaire(eleve.getId(), annee).ifPresent(decisionRepository::delete);
            return;
        }
        if (!"PASSAGE".equals(decision) && !"REDOUBLEMENT".equals(decision)) {
            throw new BadRequestException("Décision invalide : " + request.getDecision());
        }
        DecisionPassage d = decisionRepository.findByEleveIdAndAnneeScolaire(eleve.getId(), annee)
                .orElseGet(() -> DecisionPassage.builder().eleve(eleve).anneeScolaire(annee).build());
        d.setDecision(decision);
        d.setCommentaire(request.getCommentaire() == null || request.getCommentaire().isBlank() ? null : request.getCommentaire().trim());
        d.setDateDecision(LocalDateTime.now());
        try {
            utilisateurRepository.findById(SecurityUtils.getCurrentUserId()).ifPresent(d::setDecidePar);
        } catch (Exception ignored) {
            // pas de contexte d'authentification
        }
        decisionRepository.save(d);
    }
}
