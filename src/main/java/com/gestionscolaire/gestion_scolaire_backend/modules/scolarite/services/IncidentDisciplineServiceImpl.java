package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class IncidentDisciplineServiceImpl implements IncidentDisciplineService {

    private static final Logger log = LoggerFactory.getLogger(IncidentDisciplineServiceImpl.class);
    private static final java.time.format.DateTimeFormatter FMT_DATE = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> STATUTS_VALIDES = Set.of("RETARD", "ABSENT", "TENUE_NON_PORTEE", "REFUS_EXERCICE");

    @Autowired
    private IncidentDisciplineRepository incidentDisciplineRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private ClasseRepository classeRepository;

    @Autowired
    private ClasseMatiereRepository classeMatiereRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Override
    public IncidentDiscipline enregistrerIncident(IncidentDiscipline incident, Long eleveId, Long classeId, Long classeMatiereId) {
        if (!STATUTS_VALIDES.contains(incident.getStatut())) {
            throw new BadRequestException("Statut d'incident invalide : " + incident.getStatut());
        }

        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        tenantGuard.requireSameNiveau(eleve, this::niveauDe);

        Classe classe = tenantGuard.requireSameTenant(classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));

        ClasseMatiere classeMatiere = null;
        if (classeMatiereId != null) {
            classeMatiere = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClasseMatiere introuvable")));
        }

        incident.setEleve(eleve);
        incident.setClasse(classe);
        incident.setClasseMatiere(classeMatiere);
        incident.setEtablissement(eleve.getEtablissement());

        try {
            Long utilisateurId = SecurityUtils.getCurrentUserId();
            utilisateurRepository.findById(utilisateurId).ifPresent(incident::setEnregistrePar);
        } catch (Exception ignored) {
            // pas de contexte d'authentification (ex. appel interne)
        }

        IncidentDiscipline saved = incidentDisciplineRepository.save(incident);

        // Contrairement à Presence (qui ne notifie que sur ABSENT/RETARD), toute fiche de
        // discipline notifie le(s) parent(s) — c'est le cœur de la demande client (« Contact
        // des parents » listé pour les 4 statuts).
        notifierParents(saved);

        return saved;
    }

    /** Signale à chaque parent (principal + secondaire) l'incident disciplinaire enregistré. */
    private void notifierParents(IncidentDiscipline incident) {
        Eleve eleve = incident.getEleve();
        String nomComplet = (eleve.getProfil() != null ? eleve.getProfil().getPrenom() + " " + eleve.getProfil().getNom() : "Votre enfant").trim();
        String libelleStatut = libelleStatut(incident.getStatut());
        String contexte = incident.getClasseMatiere() != null && incident.getClasseMatiere().getMatiere() != null
                ? " en " + incident.getClasseMatiere().getMatiere().getNom()
                : "";
        String titre = "Incident disciplinaire signalé";
        String contenu = String.format("%s a été signalé(e) « %s »%s le %s à %s.%s",
                nomComplet,
                libelleStatut,
                contexte,
                incident.getDate().format(FMT_DATE),
                incident.getHeure(),
                incident.getCommentaire() != null && !incident.getCommentaire().isBlank() ? " Commentaire : " + incident.getCommentaire() : "");

        Long expediteurId = null;
        try {
            expediteurId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // pas de contexte d'authentification (ex. appel interne) : notification envoyée sans expéditeur
        }

        // Arrays.asList (pas List.of) : List.of() lève une NullPointerException dès qu'un seul
        // élément est null, or la plupart des élèves n'ont pas de parent secondaire.
        for (Parent parent : java.util.Arrays.asList(eleve.getParent(), eleve.getParentSecondaire())) {
            if (parent == null || parent.getProfil() == null || parent.getProfil().getUtilisateur() == null) continue;
            try {
                Notification n = Notification.builder().titre(titre).contenu(contenu).build();
                notificationService.envoyerNotification(n, expediteurId, parent.getProfil().getUtilisateur().getId());
            } catch (Exception e) {
                log.warn("Notification d'incident non envoyée au parent {} : {}", parent.getId(), e.getMessage());
            }
        }
    }

    private String libelleStatut(String statut) {
        return switch (statut) {
            case "RETARD" -> "en retard";
            case "ABSENT" -> "absent(e)";
            case "TENUE_NON_PORTEE" -> "tenue non portée";
            case "REFUS_EXERCICE" -> "refus de faire l'exercice";
            default -> statut;
        };
    }

    private Integer niveauDe(Eleve e) {
        return (e.getClasse() != null && e.getClasse().getNiveau() != null) ? e.getClasse().getNiveau().getId() : null;
    }

    private Integer niveauDe(IncidentDiscipline i) {
        return i.getEleve() != null ? niveauDe(i.getEleve()) : null;
    }

    @Override
    public List<IncidentDiscipline> listerParEleve(Long eleveId) {
        return tenantGuard.filterSameNiveau(
                tenantGuard.filterSameTenant(incidentDisciplineRepository.findByEleveIdOrderByDateDescHeureDesc(eleveId)), this::niveauDe);
    }

    @Override
    public List<IncidentDiscipline> listerParClasseEtDate(Long classeId, LocalDate date) {
        return tenantGuard.filterSameNiveau(
                tenantGuard.filterSameTenant(incidentDisciplineRepository.findByClasseIdAndDate(classeId, date)), this::niveauDe);
    }

    @Override
    public IncidentDiscipline marquerTraite(Long id, String notesTraitement) {
        IncidentDiscipline incident = tenantGuard.requireSameTenant(incidentDisciplineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident introuvable")));
        incident.setEstTraite(true);
        incident.setNotesTraitement(notesTraitement);
        return incidentDisciplineRepository.save(incident);
    }
}
