package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportNiveauRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportNiveauResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportNiveauResponse.EleveEnDifficulte;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportNiveauResponse.EleveSignale;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.SeanceCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.NoteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportNiveau;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportNiveauEleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.RapportNiveauEleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.RapportNiveauRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Rapports de niveau : l'enseignant signale à la direction le niveau d'une classe et ses élèves en difficulté. */
@Service
@Transactional
public class RapportNiveauService {

    private static final Logger log = LoggerFactory.getLogger(RapportNiveauService.class);
    private static final Set<String> NIVEAUX = Set.of("BON", "MOYEN", "FAIBLE", "PREOCCUPANT");
    private static final double SEUIL_DIFFICULTE = 10.0;

    private final RapportNiveauRepository rapportRepository;
    private final RapportNiveauEleveRepository rapportEleveRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final EleveRepository eleveRepository;
    private final EnseignantRepository enseignantRepository;
    private final NoteRepository noteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final TenantGuard tenantGuard;

    public RapportNiveauService(RapportNiveauRepository rapportRepository,
                                RapportNiveauEleveRepository rapportEleveRepository,
                                ClasseMatiereRepository classeMatiereRepository,
                                EleveRepository eleveRepository,
                                EnseignantRepository enseignantRepository,
                                NoteRepository noteRepository,
                                UtilisateurRepository utilisateurRepository,
                                NotificationService notificationService,
                                TenantGuard tenantGuard) {
        this.rapportRepository = rapportRepository;
        this.rapportEleveRepository = rapportEleveRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.eleveRepository = eleveRepository;
        this.enseignantRepository = enseignantRepository;
        this.noteRepository = noteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.tenantGuard = tenantGuard;
    }

    private Integer niveauDe(ClasseMatiere cm) {
        return cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getId() : null;
    }

    private Utilisateur utilisateurCourant() {
        try {
            return SecurityUtils.getCurrentUser().getUtilisateur();
        } catch (Exception e) {
            return null;
        }
    }

    /** Fiche enseignant de l'appelant s'il est ENSEIGNANT (-1 si aucune fiche), sinon null. */
    private Long enseignantCourantId() {
        Utilisateur u = utilisateurCourant();
        if (u == null || u.getRole() == null || !"ENSEIGNANT".equalsIgnoreCase(u.getRole().getNom())) return null;
        return enseignantRepository.findByProfilUtilisateurId(u.getId()).map(e -> e.getId()).orElse(-1L);
    }

    private ClasseMatiere classeMatierePourEnseignant(Long classeMatiereId) {
        ClasseMatiere cm = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe/matière introuvable")));
        tenantGuard.requireSameNiveau(cm, this::niveauDe);
        Long enseignantId = enseignantCourantId();
        if (enseignantId != null && (cm.getEnseignant() == null || !enseignantId.equals(cm.getEnseignant().getId()))) {
            throw new BadRequestException("Vous n'êtes pas l'enseignant assigné à cette matière pour cette classe.");
        }
        return cm;
    }

    /** Moyenne d'un élève dans la matière : moyenne des notes de la période, ou des moyennes de période pour ANNUEL. */
    private Double moyenne(Long eleveId, Long classeMatiereId, String periode) {
        List<Note> notes = noteRepository.findByEleveIdAndClasseMatiereId(eleveId, classeMatiereId);
        if ("ANNUEL".equalsIgnoreCase(periode)) {
            return notes.stream().collect(Collectors.groupingBy(Note::getPeriode, Collectors.averagingDouble(Note::getValeur)))
                    .values().stream().mapToDouble(Double::doubleValue).average().stream().boxed().findFirst().orElse(null);
        }
        return notes.stream().filter(n -> n.getPeriode().equalsIgnoreCase(periode))
                .mapToDouble(Note::getValeur).average().stream().boxed().findFirst().orElse(null);
    }

    private static double arrondi(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public List<EleveEnDifficulte> enDifficulte(Long classeMatiereId, String periode) {
        ClasseMatiere cm = classeMatierePourEnseignant(classeMatiereId);
        String p = periode == null || periode.isBlank() ? "ANNUEL" : periode.trim();
        List<EleveEnDifficulte> resultat = new ArrayList<>();
        for (Eleve e : tenantGuard.filterSameTenant(eleveRepository.findByClasseId(cm.getClasse().getId()))) {
            if (e.getStatut() != null && !"ACTIF".equalsIgnoreCase(e.getStatut())) continue;
            Double m = moyenne(e.getId(), classeMatiereId, p);
            if (m != null && m < SEUIL_DIFFICULTE) {
                resultat.add(new EleveEnDifficulte(e.getId(), e.getMatricule(),
                        e.getProfil() != null ? e.getProfil().getNom() : null,
                        e.getProfil() != null ? e.getProfil().getPrenom() : null, arrondi(m)));
            }
        }
        resultat.sort(Comparator.comparingDouble(EleveEnDifficulte::moyenne));
        return resultat;
    }

    public RapportNiveauResponse creer(RapportNiveauRequest request) {
        String niveau = request.getNiveauGlobal().trim().toUpperCase();
        if (!NIVEAUX.contains(niveau)) {
            throw new BadRequestException("Niveau général invalide : " + request.getNiveauGlobal());
        }
        ClasseMatiere cm = classeMatierePourEnseignant(request.getClasseMatiereId());
        String periode = request.getPeriode().trim().toUpperCase();

        RapportNiveau rapport = RapportNiveau.builder()
                .classeMatiere(cm)
                .periode(periode)
                .niveauGlobal(niveau)
                .commentaire(request.getCommentaire() == null || request.getCommentaire().isBlank() ? null : request.getCommentaire().trim())
                .build();
        Utilisateur auteur = utilisateurCourant();
        if (auteur != null) utilisateurRepository.findById(auteur.getId()).ifPresent(rapport::setAuteur);
        rapport = rapportRepository.save(rapport);

        if (request.getEleves() != null) {
            for (RapportNiveauRequest.EleveSignale es : request.getEleves()) {
                if (es.getEleveId() == null) continue;
                Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(es.getEleveId())
                        .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
                if (eleve.getClasse() == null || !eleve.getClasse().getId().equals(cm.getClasse().getId())) {
                    throw new BadRequestException("Un des élèves signalés n'appartient pas à cette classe.");
                }
                rapportEleveRepository.save(RapportNiveauEleve.builder()
                        .rapport(rapport)
                        .eleve(eleve)
                        .moyenne(moyenne(eleve.getId(), cm.getId(), periode))
                        .commentaire(es.getCommentaire() == null || es.getCommentaire().isBlank() ? null : es.getCommentaire().trim())
                        .build());
            }
        }

        notifierDirection(rapport, cm);
        return versReponse(rapport);
    }

    /** Prévient les directeurs de l'établissement (push + notification) qu'un rapport de niveau les attend. */
    private void notifierDirection(RapportNiveau rapport, ClasseMatiere cm) {
        try {
            Utilisateur auteur = utilisateurCourant();
            Long expediteurId = auteur != null ? auteur.getId() : null;
            Long etablissementId = tenantGuard.etablissementId();
            if (etablissementId == null) return;
            String contenu = String.format("%s — %s : niveau %s.",
                    cm.getClasse() != null ? cm.getClasse().getNom() : "Classe",
                    cm.getMatiere() != null ? cm.getMatiere().getNom() : "matière",
                    libelleNiveau(rapport.getNiveauGlobal()));
            for (Utilisateur u : utilisateurRepository.findByEtablissementId(etablissementId)) {
                if (u.getRole() == null || !"DIRECTEUR".equalsIgnoreCase(u.getRole().getNom())) continue;
                if (u.getNiveauSupervise() != null && cm.getClasse() != null && cm.getClasse().getNiveau() != null
                        && !u.getNiveauSupervise().getId().equals(cm.getClasse().getNiveau().getId())) continue;
                try {
                    notificationService.envoyerNotification(
                            Notification.builder().titre("Rapport de niveau d'un enseignant").contenu(contenu).build(),
                            expediteurId, u.getId());
                } catch (Exception e) {
                    log.warn("Notification du rapport {} non envoyée à {} : {}", rapport.getId(), u.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Notification de la direction impossible pour le rapport {} : {}", rapport.getId(), e.getMessage());
        }
    }

    private static String libelleNiveau(String n) {
        return switch (n) {
            case "BON" -> "bon";
            case "MOYEN" -> "moyen";
            case "FAIBLE" -> "faible";
            case "PREOCCUPANT" -> "préoccupant";
            default -> n.toLowerCase();
        };
    }

    @Transactional(readOnly = true)
    public List<RapportNiveauResponse> mesRapports() {
        Utilisateur u = utilisateurCourant();
        if (u == null) return List.of();
        return rapportRepository.findByAuteurIdOrderByDateCreationDesc(u.getId()).stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RapportNiveauResponse> lister(Long classeId, boolean nonTraitesSeulement) {
        return tenantGuard.filterSameNiveau(
                        tenantGuard.filterSameTenant(rapportRepository.findByEtablissementIdOrderByDateCreationDesc(tenantGuard.requireEtablissementId())),
                        r -> niveauDe(r.getClasseMatiere()))
                .stream()
                .filter(r -> classeId == null || (r.getClasseMatiere().getClasse() != null && classeId.equals(r.getClasseMatiere().getClasse().getId())))
                .filter(r -> !nonTraitesSeulement || !Boolean.TRUE.equals(r.getEstTraite()))
                .map(this::versReponse)
                .toList();
    }

    public RapportNiveauResponse traiter(Long id, String reponse) {
        RapportNiveau r = tenantGuard.requireSameTenant(rapportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable")));
        tenantGuard.requireSameNiveau(r.getClasseMatiere(), this::niveauDe);
        r.setEstTraite(true);
        r.setReponseDirection(reponse == null || reponse.isBlank() ? null : reponse.trim());
        r.setDateTraitement(LocalDateTime.now());
        r = rapportRepository.save(r);

        // L'enseignant sait que la direction a pris son rapport en compte.
        try {
            if (r.getAuteur() != null) {
                Utilisateur directeur = utilisateurCourant();
                String contenu = "Votre rapport (" + (r.getClasseMatiere().getClasse() != null ? r.getClasseMatiere().getClasse().getNom() : "") + " — "
                        + (r.getClasseMatiere().getMatiere() != null ? r.getClasseMatiere().getMatiere().getNom() : "") + ") a été pris en compte."
                        + (r.getReponseDirection() != null ? " Réponse : " + r.getReponseDirection() : "");
                notificationService.envoyerNotification(
                        Notification.builder().titre("Rapport de niveau traité").contenu(contenu).build(),
                        directeur != null ? directeur.getId() : null, r.getAuteur().getId());
            }
        } catch (Exception e) {
            log.warn("Retour à l'enseignant impossible pour le rapport {} : {}", id, e.getMessage());
        }
        return versReponse(r);
    }

    private RapportNiveauResponse versReponse(RapportNiveau r) {
        ClasseMatiere cm = r.getClasseMatiere();
        List<EleveSignale> eleves = rapportEleveRepository.findByRapportId(r.getId()).stream()
                .map(re -> new EleveSignale(re.getEleve().getId(), re.getEleve().getMatricule(),
                        re.getEleve().getProfil() != null ? re.getEleve().getProfil().getNom() : null,
                        re.getEleve().getProfil() != null ? re.getEleve().getProfil().getPrenom() : null,
                        re.getMoyenne() != null ? arrondi(re.getMoyenne()) : null, re.getCommentaire()))
                .sorted(Comparator.comparing(EleveSignale::moyenne, Comparator.nullsLast(Double::compareTo)))
                .toList();
        return new RapportNiveauResponse(
                r.getId(), cm.getId(),
                cm.getClasse() != null ? cm.getClasse().getId() : null,
                cm.getClasse() != null ? cm.getClasse().getNom() : null,
                cm.getMatiere() != null ? cm.getMatiere().getNom() : null,
                SeanceCoursResponse.nomEnseignant(cm),
                r.getAuteur() != null ? r.getAuteur().getUsername() : null,
                r.getPeriode(), r.getNiveauGlobal(), r.getCommentaire(),
                Boolean.TRUE.equals(r.getEstTraite()), r.getReponseDirection(),
                r.getDateCreation(), r.getDateTraitement(), eleves);
    }
}
