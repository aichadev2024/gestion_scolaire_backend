package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.storage.StorageService;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.SujetDevoirResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.SujetDevoir;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.SujetDevoirRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Sujets de devoir/examen qu'un enseignant transmet à la direction avant de les donner aux élèves. */
@Service
@Transactional
public class SujetDevoirService {

    private static final Logger log = LoggerFactory.getLogger(SujetDevoirService.class);
    private static final Set<String> TYPES = Set.of("DEVOIR", "EXAMEN");
    private static final Set<String> STATUTS = Set.of("VALIDE", "REJETE");
    private static final long POIDS_MAX_OCTETS = 15L * 1024 * 1024;

    private final SujetDevoirRepository sujetRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final EnseignantRepository enseignantRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilRepository profilRepository;
    private final ObjectProvider<StorageService> storageProvider;
    private final NotificationService notificationService;
    private final TenantGuard tenantGuard;

    public SujetDevoirService(SujetDevoirRepository sujetRepository,
                               ClasseMatiereRepository classeMatiereRepository,
                               EnseignantRepository enseignantRepository,
                               UtilisateurRepository utilisateurRepository,
                               ProfilRepository profilRepository,
                               ObjectProvider<StorageService> storageProvider,
                               NotificationService notificationService,
                               TenantGuard tenantGuard) {
        this.sujetRepository = sujetRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.enseignantRepository = enseignantRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.profilRepository = profilRepository;
        this.storageProvider = storageProvider;
        this.notificationService = notificationService;
        this.tenantGuard = tenantGuard;
    }

    private StorageService storage() {
        StorageService s = storageProvider.getIfAvailable();
        if (s == null) {
            throw new BadRequestException(
                    "Le stockage de documents n'est pas configuré. Définissez les variables R2_* (voir docs/deploiement-production.md).");
        }
        return s;
    }

    private Utilisateur utilisateurCourant() {
        try {
            return SecurityUtils.getCurrentUser().getUtilisateur();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer niveauDe(ClasseMatiere cm) {
        return cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getId() : null;
    }

    @Transactional
    public SujetDevoirResponse envoyer(Long classeMatiereId, String type, String titre, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Joignez le fichier du sujet (photo ou PDF).");
        }
        if (file.getSize() > POIDS_MAX_OCTETS) {
            throw new BadRequestException("Fichier trop lourd (max 15 Mo).");
        }
        if (titre == null || titre.isBlank()) {
            throw new BadRequestException("Indiquez un titre pour ce sujet.");
        }
        String typeRetenu = type == null ? "" : type.trim().toUpperCase();
        if (!TYPES.contains(typeRetenu)) {
            throw new BadRequestException("Type invalide : indiquez DEVOIR ou EXAMEN.");
        }

        ClasseMatiere cm = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe/matière introuvable")));
        tenantGuard.requireSameNiveau(cm, this::niveauDe);

        Utilisateur u = utilisateurCourant();
        if (u == null) throw new BadRequestException("Session invalide.");
        var enseignant = enseignantRepository.findByProfilUtilisateurId(u.getId())
                .orElseThrow(() -> new BadRequestException("Aucune fiche enseignant associée à ce compte."));
        if (cm.getEnseignant() == null || !enseignant.getId().equals(cm.getEnseignant().getId())) {
            throw new BadRequestException("Vous n'êtes pas l'enseignant assigné à cette matière pour cette classe.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Lecture du fichier impossible.");
        }
        String url = storage().uploadDocument(bytes, file.getContentType(), "sujets/" + classeMatiereId);

        SujetDevoir sujet = SujetDevoir.builder()
                .classeMatiere(cm)
                .enseignant(enseignant)
                .etablissement(cm.getEtablissement())
                .type(typeRetenu)
                .titre(titre.trim())
                .description(description == null || description.isBlank() ? null : description.trim())
                .url(url)
                .contentType(file.getContentType())
                .tailleOctets(file.getSize())
                .build();
        sujet = sujetRepository.save(sujet);

        notifierDirection(sujet, cm);
        return versDto(sujet);
    }

    /** Prévient les directeurs (du niveau concerné) qu'un sujet attend leur validation. */
    private void notifierDirection(SujetDevoir sujet, ClasseMatiere cm) {
        try {
            Long etablissementId = tenantGuard.etablissementId();
            if (etablissementId == null) return;
            String enseignantNom = nomEnseignant(sujet);
            String contenu = enseignantNom + " — " + libelleClasseMatiere(cm) + " : « " + sujet.getTitre() + " » ("
                    + libelleType(sujet.getType()) + ") en attente de validation.";
            Utilisateur auteur = utilisateurCourant();
            Long expediteurId = auteur != null ? auteur.getId() : null;
            for (Utilisateur dest : utilisateurRepository.findByEtablissementId(etablissementId)) {
                if (dest.getRole() == null || !"DIRECTEUR".equalsIgnoreCase(dest.getRole().getNom())) continue;
                if (dest.getNiveauSupervise() != null && cm.getClasse() != null && cm.getClasse().getNiveau() != null
                        && !dest.getNiveauSupervise().getId().equals(cm.getClasse().getNiveau().getId())) continue;
                try {
                    notificationService.envoyerNotification(
                            Notification.builder().titre("Nouveau sujet à valider").contenu(contenu).build(),
                            expediteurId, dest.getId());
                } catch (Exception ignored) {
                    // un destinataire en échec ne bloque pas les autres
                }
            }
        } catch (Exception e) {
            log.warn("Notification de la direction impossible pour le sujet {} : {}", sujet.getId(), e.getMessage());
        }
    }

    /** Prévient l'enseignant que son sujet a été validé ou rejeté. */
    private void notifierEnseignant(SujetDevoir sujet) {
        try {
            var profil = sujet.getEnseignant().getProfil();
            if (profil == null || profil.getUtilisateur() == null) return;
            boolean valide = "VALIDE".equals(sujet.getStatut());
            String contenu = "« " + sujet.getTitre() + " » a été " + (valide ? "validé" : "rejeté") + " par la direction."
                    + (sujet.getCommentaireDirection() != null ? " Commentaire : " + sujet.getCommentaireDirection() : "");
            Utilisateur auteur = utilisateurCourant();
            Long expediteurId = auteur != null ? auteur.getId() : null;
            notificationService.envoyerNotification(
                    Notification.builder().titre(valide ? "Sujet validé" : "Sujet rejeté").contenu(contenu).build(),
                    expediteurId, profil.getUtilisateur().getId());
        } catch (Exception e) {
            log.warn("Notification de l'enseignant impossible pour le sujet {} : {}", sujet.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SujetDevoirResponse> mesSujets() {
        Utilisateur u = utilisateurCourant();
        if (u == null) return List.of();
        var enseignant = enseignantRepository.findByProfilUtilisateurId(u.getId()).orElse(null);
        if (enseignant == null) return List.of();
        return sujetRepository.findByEnseignantIdOrderByDateEnvoiDesc(enseignant.getId()).stream()
                .map(this::versDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SujetDevoirResponse> lister(String statut) {
        Long etablissementId = tenantGuard.requireEtablissementId();
        List<SujetDevoir> sujets = tenantGuard.filterSameNiveau(
                sujetRepository.findByEtablissementIdOrderByDateEnvoiDesc(etablissementId),
                s -> niveauDe(s.getClasseMatiere()));
        if (statut != null && !statut.isBlank()) {
            String s = statut.trim().toUpperCase();
            sujets = sujets.stream().filter(x -> x.getStatut().equalsIgnoreCase(s)).toList();
        }
        return sujets.stream().map(this::versDto).toList();
    }

    @Transactional
    public SujetDevoirResponse traiter(Long id, String statut, String commentaire) {
        String statutRetenu = statut == null ? "" : statut.trim().toUpperCase();
        if (!STATUTS.contains(statutRetenu)) {
            throw new BadRequestException("Statut invalide : indiquez VALIDE ou REJETE.");
        }
        SujetDevoir sujet = tenantGuard.requireSameTenant(sujetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable")));
        tenantGuard.requireSameNiveau(sujet.getClasseMatiere(), this::niveauDe);

        sujet.setStatut(statutRetenu);
        sujet.setCommentaireDirection(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        sujet.setDateTraitement(java.time.LocalDateTime.now());
        Utilisateur u = utilisateurCourant();
        if (u != null) utilisateurRepository.findById(u.getId()).ifPresent(sujet::setTraitePar);

        sujet = sujetRepository.save(sujet);
        notifierEnseignant(sujet);
        return versDto(sujet);
    }

    private static String libelleType(String type) {
        return "EXAMEN".equals(type) ? "examen" : "devoir";
    }

    private static String libelleClasseMatiere(ClasseMatiere cm) {
        String classe = cm.getClasse() != null ? cm.getClasse().getNom() : "Classe";
        String matiere = cm.getMatiere() != null ? cm.getMatiere().getNom() : "matière";
        return classe + " — " + matiere;
    }

    private String nomTraitePar(SujetDevoir sujet) {
        if (sujet.getTraitePar() == null) return null;
        return profilRepository.findByUtilisateurId(sujet.getTraitePar().getId())
                .map(p -> (p.getPrenom() + " " + p.getNom()).trim())
                .orElse(null);
    }

    private static String nomEnseignant(SujetDevoir sujet) {
        if (sujet.getEnseignant() == null || sujet.getEnseignant().getProfil() == null) return "Un enseignant";
        var profil = sujet.getEnseignant().getProfil();
        return (profil.getPrenom() + " " + profil.getNom()).trim();
    }

    private SujetDevoirResponse versDto(SujetDevoir s) {
        ClasseMatiere cm = s.getClasseMatiere();
        return new SujetDevoirResponse(
                s.getId(),
                cm != null ? cm.getId() : null,
                cm != null && cm.getClasse() != null ? cm.getClasse().getNom() : null,
                cm != null && cm.getMatiere() != null ? cm.getMatiere().getNom() : null,
                nomEnseignant(s),
                s.getType(),
                s.getTitre(),
                s.getDescription(),
                s.getUrl(),
                s.getContentType(),
                s.getTailleOctets(),
                s.getStatut(),
                s.getCommentaireDirection(),
                nomTraitePar(s),
                s.getDateEnvoi(),
                s.getDateTraitement()
        );
    }
}
