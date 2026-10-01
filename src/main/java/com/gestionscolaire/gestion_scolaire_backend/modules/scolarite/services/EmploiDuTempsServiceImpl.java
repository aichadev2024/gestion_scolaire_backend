package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.EmploiDuTemps;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EmploiDuTempsRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EmploiDuTempsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmploiDuTempsServiceImpl implements EmploiDuTempsService {

    private static final Logger log = LoggerFactory.getLogger(EmploiDuTempsServiceImpl.class);

    private final EmploiDuTempsRepository emploiDuTempsRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository classeRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;
    private final NotificationService notificationService;

    public EmploiDuTempsServiceImpl(
            EmploiDuTempsRepository emploiDuTempsRepository,
            ClasseMatiereRepository classeMatiereRepository,
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository classeRepository,
            com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard,
            NotificationService notificationService
    ) {
        this.emploiDuTempsRepository = emploiDuTempsRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.classeRepository = classeRepository;
        this.tenantGuard = tenantGuard;
        this.notificationService = notificationService;
    }

    @Override
    public EmploiDuTemps creerCreneau(EmploiDuTemps creneau, Long classeMatiereId) {
        if (classeMatiereId != null) {
            ClasseMatiere classeMatiere = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classe-matière introuvable")));
            creneau.setClasseMatiere(classeMatiere);
            creneau.setClasse(classeMatiere.getClasse());
            creneau.setEtablissement(classeMatiere.getEtablissement());
        } else if (creneau.getClasse() != null && creneau.getClasse().getId() != null) {
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe c = tenantGuard.requireSameTenant(classeRepository.findById(creneau.getClasse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
            creneau.setClasse(c);
            creneau.setEtablissement(c.getEtablissement());
        }
        validerCreneau(creneau);
        detecterConflits(creneau, null);
        EmploiDuTemps saved = emploiDuTempsRepository.save(creneau);
        notifierEnseignant(saved, "Nouveau cours à l'emploi du temps",
                "Un nouveau cours de " + libelleMatiere(saved) + " a été ajouté à votre emploi du temps : "
                        + libelleJour(saved) + " de " + saved.getHeureDebut() + " à " + saved.getHeureFin()
                        + " (" + libelleClasse(saved) + ").");
        return saved;
    }

    @Override
    public EmploiDuTemps modifierCreneau(Long id, EmploiDuTemps creneauDetails, Long classeMatiereId) {
        EmploiDuTemps creneau = tenantGuard.requireSameTenant(emploiDuTempsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable")));
        validerCreneau(creneauDetails);

        creneau.setJourSemaine(creneauDetails.getJourSemaine());
        creneau.setHeureDebut(creneauDetails.getHeureDebut());
        creneau.setHeureFin(creneauDetails.getHeureFin());
        creneau.setSalle(creneauDetails.getSalle());
        creneau.setTypeCreneau(creneauDetails.getTypeCreneau());
        creneau.setLibellePause(creneauDetails.getLibellePause());

        if (classeMatiereId != null) {
            ClasseMatiere classeMatiere = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classe-matière introuvable")));
            creneau.setClasseMatiere(classeMatiere);
            creneau.setClasse(classeMatiere.getClasse());
        } else if (creneauDetails.getClasse() != null && creneauDetails.getClasse().getId() != null) {
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe c = tenantGuard.requireSameTenant(classeRepository.findById(creneauDetails.getClasse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
            creneau.setClasse(c);
        }

        detecterConflits(creneau, id);
        EmploiDuTemps saved = emploiDuTempsRepository.save(creneau);
        notifierEnseignant(saved, "Emploi du temps modifié",
                "Votre cours de " + libelleMatiere(saved) + " a été modifié : "
                        + libelleJour(saved) + " de " + saved.getHeureDebut() + " à " + saved.getHeureFin()
                        + " (" + libelleClasse(saved) + ").");
        return saved;
    }

    @Override
    public Optional<EmploiDuTemps> trouverParId(Long id) {
        return emploiDuTempsRepository.findById(id).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    public List<EmploiDuTemps> listerParClasse(Long classeId) {
        return tenantGuard.filterSameTenant(emploiDuTempsRepository.findByClasseIdOrClasseMatiereClasseId(classeId, classeId));
    }

    @Override
    public List<EmploiDuTemps> listerParEnseignant(Long enseignantId) {
        return tenantGuard.filterSameTenant(emploiDuTempsRepository.findByClasseMatiereEnseignantId(enseignantId));
    }

    @Override
    public void supprimerCreneau(Long id) {
        EmploiDuTemps creneau = tenantGuard.requireSameTenant(emploiDuTempsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable")));
        notifierEnseignant(creneau, "Cours retiré de l'emploi du temps",
                "Votre cours de " + libelleMatiere(creneau) + " du " + libelleJour(creneau) + " ("
                        + creneau.getHeureDebut() + " à " + creneau.getHeureFin() + ", " + libelleClasse(creneau)
                        + ") a été retiré de l'emploi du temps.");
        emploiDuTempsRepository.delete(creneau);
    }

    /** Prévient l'enseignant concerné (base + push), sans jamais faire échouer l'opération sur l'emploi du temps. */
    private void notifierEnseignant(EmploiDuTemps creneau, String titre, String contenu) {
        if (!"COURS".equals(creneau.getTypeCreneau()) || creneau.getClasseMatiere() == null
                || creneau.getClasseMatiere().getEnseignant() == null
                || creneau.getClasseMatiere().getEnseignant().getProfil() == null
                || creneau.getClasseMatiere().getEnseignant().getProfil().getUtilisateur() == null) {
            return;
        }
        try {
            Long destinataireId = creneau.getClasseMatiere().getEnseignant().getProfil().getUtilisateur().getId();
            Long expediteurId;
            try {
                expediteurId = SecurityUtils.getCurrentUserId();
            } catch (Exception e) {
                expediteurId = null;
            }
            notificationService.envoyerNotification(
                    Notification.builder().titre(titre).contenu(contenu).build(), expediteurId, destinataireId);
        } catch (Exception e) {
            log.warn("Notification de changement d'emploi du temps non envoyée pour le créneau {} : {}", creneau.getId(), e.getMessage());
        }
    }

    private String libelleMatiere(EmploiDuTemps creneau) {
        return creneau.getClasseMatiere() != null && creneau.getClasseMatiere().getMatiere() != null
                && creneau.getClasseMatiere().getMatiere().getNom() != null
                ? creneau.getClasseMatiere().getMatiere().getNom() : "la matière";
    }

    private String libelleJour(EmploiDuTemps creneau) {
        String[] jours = {"", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
        Integer j = creneau.getJourSemaine();
        return j != null && j >= 1 && j < jours.length ? jours[j] : "un jour";
    }

    /**
     * Refuse un créneau qui chevauche, le même jour, un créneau existant du même enseignant
     * ou de la même salle — aucune vérification de ce type n'existait jusqu'ici malgré ce
     * qu'indiquait la documentation. Ne bloque que sur COURS (une pause n'a pas d'enseignant,
     * et deux classes peuvent partager la même salle pour une pause/récréation sans problème).
     */
    private void detecterConflits(EmploiDuTemps creneau, Long idAExclure) {
        if (!"COURS".equals(creneau.getTypeCreneau())) return;

        Long etablissementId = creneau.getEtablissement() != null ? creneau.getEtablissement().getId() : null;
        if (etablissementId == null) return;

        Long enseignantId = creneau.getClasseMatiere() != null && creneau.getClasseMatiere().getEnseignant() != null
                ? creneau.getClasseMatiere().getEnseignant().getId() : null;
        String salle = creneau.getSalle() != null ? creneau.getSalle().trim() : null;
        if (enseignantId == null && (salle == null || salle.isBlank())) return;

        List<EmploiDuTemps> memeJour = emploiDuTempsRepository
                .findByEtablissementIdAndJourSemaine(etablissementId, creneau.getJourSemaine());

        for (EmploiDuTemps autre : memeJour) {
            if (autre.getId().equals(idAExclure)) continue;
            if (!"COURS".equals(autre.getTypeCreneau())) continue;
            boolean chevauche = creneau.getHeureDebut().isBefore(autre.getHeureFin())
                    && autre.getHeureDebut().isBefore(creneau.getHeureFin());
            if (!chevauche) continue;

            Long autreEnseignantId = autre.getClasseMatiere() != null && autre.getClasseMatiere().getEnseignant() != null
                    ? autre.getClasseMatiere().getEnseignant().getId() : null;
            if (enseignantId != null && enseignantId.equals(autreEnseignantId)) {
                throw new BadRequestException("Conflit d'emploi du temps : cet enseignant a déjà cours en "
                        + libelleClasse(autre) + " de " + autre.getHeureDebut() + " à " + autre.getHeureFin() + ".");
            }
            String autreSalle = autre.getSalle() != null ? autre.getSalle().trim() : null;
            if (salle != null && !salle.isBlank() && salle.equalsIgnoreCase(autreSalle)) {
                throw new BadRequestException("Conflit d'emploi du temps : la salle " + salle + " est déjà occupée par "
                        + libelleClasse(autre) + " de " + autre.getHeureDebut() + " à " + autre.getHeureFin() + ".");
            }
        }
    }

    private String libelleClasse(EmploiDuTemps creneau) {
        return creneau.getClasse() != null && creneau.getClasse().getNom() != null ? creneau.getClasse().getNom() : "une autre classe";
    }

    private void validerCreneau(EmploiDuTemps creneau) {
        if (creneau.getJourSemaine() == null || creneau.getJourSemaine() < 1 || creneau.getJourSemaine() > 7) {
            throw new BadRequestException("Le jour de la semaine doit être entre 1 et 7");
        }
        if (creneau.getHeureDebut() == null || creneau.getHeureFin() == null) {
            throw new BadRequestException("Les heures de début et de fin sont obligatoires");
        }
        if (creneau.getHeureFin().isBefore(creneau.getHeureDebut()) || creneau.getHeureFin().equals(creneau.getHeureDebut())) {
            throw new BadRequestException("L'heure de fin doit être après l'heure de début");
        }
        if (creneau.getClasseMatiere() == null && creneau.getClasse() == null) {
            throw new BadRequestException("Une classe ou une classe-matière doit être associée au créneau");
        }
    }
}


