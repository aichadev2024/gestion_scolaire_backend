package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportJournalierRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportJournalier;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.RapportJournalierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class RapportJournalierServiceImpl implements RapportJournalierService {

    private static final Logger log = LoggerFactory.getLogger(RapportJournalierServiceImpl.class);
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private RapportJournalierRepository rapportRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Override
    public RapportJournalier enregistrer(RapportJournalierRequest request) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(request.getEleveId())
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        RapportJournalier rapport = rapportRepository.findByEleveIdAndDate(eleve.getId(), request.getDate())
                .orElse(null);
        boolean nouveau = rapport == null;
        if (rapport == null) {
            rapport = RapportJournalier.builder()
                    .eleve(eleve)
                    .etablissement(eleve.getEtablissement())
                    .date(request.getDate())
                    .build();
        }

        rapport.setRepas(request.getRepas());
        rapport.setSiesteFaite(request.getSiesteFaite());
        rapport.setDureeSiesteMinutes(request.getDureeSiesteMinutes());
        rapport.setChangesCouches(request.getChangesCouches());
        rapport.setHumeur(request.getHumeur());
        rapport.setNotes(request.getNotes());

        Long redacteurId = null;
        try {
            redacteurId = SecurityUtils.getCurrentUserId();
            rapport.setRedigePar(utilisateurRepository.findById(redacteurId).orElse(null));
        } catch (Exception ignored) {
            // pas de contexte d'authentification (ex. appel interne)
        }

        RapportJournalier saved = rapportRepository.save(rapport);

        // On ne notifie qu'à la création : la monitrice complète souvent le
        // rapport en plusieurs fois dans la journée, pas besoin de spammer
        // les parents à chaque sauvegarde intermédiaire.
        if (nouveau) {
            notifierParents(saved, redacteurId);
        }
        return saved;
    }

    private void notifierParents(RapportJournalier rapport, Long expediteurId) {
        Eleve eleve = rapport.getEleve();
        String nomComplet = (eleve.getProfil() != null ? eleve.getProfil().getPrenom() + " " + eleve.getProfil().getNom() : "Votre enfant").trim();
        String titre = "Rapport du jour disponible";
        String contenu = String.format("Le rapport journalier de %s du %s est disponible.", nomComplet, rapport.getDate().format(FMT_DATE));

        for (Parent parent : List.of(eleve.getParent(), eleve.getParentSecondaire())) {
            if (parent == null || parent.getProfil() == null || parent.getProfil().getUtilisateur() == null) continue;
            try {
                Notification n = Notification.builder().titre(titre).contenu(contenu).build();
                notificationService.envoyerNotification(n, expediteurId, parent.getProfil().getUtilisateur().getId());
            } catch (Exception e) {
                log.warn("Notification de rapport journalier non envoyée au parent {} : {}", parent.getId(), e.getMessage());
            }
        }
    }

    @Override
    public List<RapportJournalier> listerParEleve(Long eleveId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        try {
            Utilisateur current = SecurityUtils.getCurrentUser().getUtilisateur();
            String role = current.getRole() != null ? current.getRole().getNom() : "";
            if ("PARENT".equalsIgnoreCase(role) && !estParentDe(eleve, current.getId())) {
                throw new AccessDeniedException("Ce n'est pas l'un de vos enfants.");
            }
        } catch (AccessDeniedException ade) {
            throw ade;
        } catch (Exception ignored) {
            // pas de contexte d'authentification exploitable (ex. appel interne)
        }

        return rapportRepository.findByEleveIdOrderByDateDesc(eleveId);
    }

    @Override
    public List<RapportJournalier> listerParClasseEtDate(Long classeId, LocalDate date) {
        return tenantGuard.filterSameTenant(rapportRepository.findByEleveClasseIdAndDate(classeId, date));
    }

    private boolean estParentDe(Eleve eleve, Long utilisateurId) {
        return estCeParent(eleve.getParent(), utilisateurId) || estCeParent(eleve.getParentSecondaire(), utilisateurId);
    }

    private boolean estCeParent(Parent parent, Long utilisateurId) {
        return parent != null && parent.getProfil() != null && parent.getProfil().getUtilisateur() != null
                && parent.getProfil().getUtilisateur().getId().equals(utilisateurId);
    }
}
