package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PresenceServiceImpl implements PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceServiceImpl.class);
    private static final java.time.format.DateTimeFormatter FMT_DATE = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private ClasseMatiereRepository classeMatiereRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Override
    public Presence enregistrerPresence(Presence presence, Long eleveId, Long classeMatiereId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        if (classeMatiereId != null) {
            ClasseMatiere classeMatiere = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClasseMatiere introuvable")));
            presence.setClasseMatiere(classeMatiere);
        }

        presence.setEleve(eleve);
        presence.setEtablissement(eleve.getEtablissement());
        Presence saved = presenceRepository.save(presence);

        if ("ABSENT".equalsIgnoreCase(saved.getStatut()) || "RETARD".equalsIgnoreCase(saved.getStatut())) {
            notifierParents(saved);
        }
        return saved;
    }

    @Override
    public Presence justifierAbsence(Long presenceId, String notesJustification) {
        Presence presence = tenantGuard.requireSameTenant(presenceRepository.findById(presenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Présence introuvable")));

        Long utilisateurId = SecurityUtils.getCurrentUserId();
        if (!estParentDe(presence.getEleve(), utilisateurId)) {
            throw new AccessDeniedException("Ce n'est pas l'un de vos enfants.");
        }

        presence.setNotesJustification(notesJustification);
        presence.setEstJustifie(true);
        Presence saved = presenceRepository.save(presence);

        notifierDirectionJustification(saved, notesJustification);
        return saved;
    }

    /** Signale à chaque parent (principal + secondaire) l'absence/le retard enregistré. */
    private void notifierParents(Presence presence) {
        Eleve eleve = presence.getEleve();
        boolean absent = "ABSENT".equalsIgnoreCase(presence.getStatut());
        String nomComplet = (eleve.getProfil() != null ? eleve.getProfil().getPrenom() + " " + eleve.getProfil().getNom() : "Votre enfant").trim();
        String titre = absent ? "Absence signalée" : "Retard signalé";
        String contenu = String.format("%s a été marqué(e) %s le %s.%s",
                nomComplet,
                absent ? "absent(e)" : "en retard",
                presence.getDate().format(FMT_DATE),
                presence.getEstJustifie() != null && presence.getEstJustifie() ? "" : " Vous pouvez justifier depuis l'application.");

        Long expediteurId = null;
        try {
            expediteurId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // pas de contexte d'authentification (ex. appel interne) : notification envoyée sans expéditeur
        }

        for (Parent parent : List.of(eleve.getParent(), eleve.getParentSecondaire())) {
            if (parent == null || parent.getProfil() == null || parent.getProfil().getUtilisateur() == null) continue;
            try {
                Notification n = Notification.builder().titre(titre).contenu(contenu).build();
                notificationService.envoyerNotification(n, expediteurId, parent.getProfil().getUtilisateur().getId());
            } catch (Exception e) {
                log.warn("Notification d'absence non envoyée au parent {} : {}", parent.getId(), e.getMessage());
            }
        }
    }

    /** Alerte le directeur et le secrétariat de l'établissement qu'une justification vient d'arriver. */
    private void notifierDirectionJustification(Presence presence, String texte) {
        Eleve eleve = presence.getEleve();
        if (eleve.getEtablissement() == null) return;

        String nomComplet = (eleve.getProfil() != null ? eleve.getProfil().getPrenom() + " " + eleve.getProfil().getNom() : "Un élève").trim();
        String titre = "Justification d'absence reçue";
        String contenu = String.format("%s (%s) — %s du %s justifié(e) : « %s »",
                nomComplet,
                eleve.getMatricule(),
                "ABSENT".equalsIgnoreCase(presence.getStatut()) ? "absence" : "retard",
                presence.getDate().format(FMT_DATE),
                texte);

        Long expediteurId = SecurityUtils.getCurrentUserId();
        List<Utilisateur> staff = utilisateurRepository.findByEtablissementId(eleve.getEtablissement().getId());
        for (Utilisateur u : staff) {
            String role = u.getRole() != null ? u.getRole().getNom() : "";
            if (!"DIRECTEUR".equalsIgnoreCase(role) && !"SECRETAIRE".equalsIgnoreCase(role)) continue;
            try {
                Notification n = Notification.builder().titre(titre).contenu(contenu).build();
                notificationService.envoyerNotification(n, expediteurId, u.getId());
            } catch (Exception e) {
                log.warn("Notification de justification non envoyée à {} : {}", u.getId(), e.getMessage());
            }
        }
    }

    private boolean estParentDe(Eleve eleve, Long utilisateurId) {
        return estCeParent(eleve.getParent(), utilisateurId) || estCeParent(eleve.getParentSecondaire(), utilisateurId);
    }

    private boolean estCeParent(Parent parent, Long utilisateurId) {
        return parent != null && parent.getProfil() != null && parent.getProfil().getUtilisateur() != null
                && parent.getProfil().getUtilisateur().getId().equals(utilisateurId);
    }

    @Override
    public List<Presence> listerPresencesEleve(Long eleveId) {
        return tenantGuard.filterSameTenant(presenceRepository.findByEleveId(eleveId));
    }

    @Override
    public List<Presence> listerPresencesParClasseMatiereEtDate(Long classeMatiereId, LocalDate date) {
        return tenantGuard.filterSameTenant(presenceRepository.findByClasseMatiereIdAndDate(classeMatiereId, date));
    }
}


