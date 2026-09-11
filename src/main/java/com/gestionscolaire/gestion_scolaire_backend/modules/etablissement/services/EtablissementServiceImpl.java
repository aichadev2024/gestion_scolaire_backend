package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.CreateEtablissementWithAdminRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.EtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.ModifierEtablissementRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.EtablissementService;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EtablissementServiceImpl implements EtablissementService {

    private final EtablissementRepository etablissementRepository;
    private final UtilisateurService utilisateurService;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository utilisateurRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository profilRepository;
    private final DtoMapper dtoMapper;
    private final RecuEtablissementPdfService recuEtablissementPdfService;
    private final com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService emailService;

    public EtablissementServiceImpl(
            EtablissementRepository etablissementRepository,
            UtilisateurService utilisateurService,
            com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository utilisateurRepository,
            com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository profilRepository,
            DtoMapper dtoMapper,
            RecuEtablissementPdfService recuEtablissementPdfService,
            com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService emailService
    ) {
        this.etablissementRepository = etablissementRepository;
        this.utilisateurService = utilisateurService;
        this.utilisateurRepository = utilisateurRepository;
        this.profilRepository = profilRepository;
        this.dtoMapper = dtoMapper;
        this.recuEtablissementPdfService = recuEtablissementPdfService;
        this.emailService = emailService;
    }

    private String genererCodeEtablissementAutomatique(String nom) {
        if (nom == null || nom.isBlank()) return "ecole-" + (System.currentTimeMillis() % 10000);
        String base = java.text.Normalizer.normalize(nom, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", "-");
        if (base.isBlank()) base = "ecole";
        String code = base;
        int counter = 1;
        while (etablissementRepository.existsByCode(code)) {
            code = base + "-" + counter++;
        }
        return code;
    }

    @Override
    @Transactional
    public EtablissementResponse creerEtablissementAvecAdmin(CreateEtablissementWithAdminRequest request) {
        String codeFinal;
        if (request.getCodeEtablissement() == null || request.getCodeEtablissement().isBlank()) {
            codeFinal = genererCodeEtablissementAutomatique(request.getNomEtablissement());
        } else {
            codeFinal = request.getCodeEtablissement().toLowerCase().trim();
            if (etablissementRepository.existsByCode(codeFinal)) {
                throw new BadRequestException("Un établissement avec ce code existe déjà : " + codeFinal);
            }
        }

        LocalDateTime expiryDate = request.getDateExpirationAbonnement() != null 
                ? request.getDateExpirationAbonnement() 
                : java.time.LocalDateTime.now().plusMonths(1);

        Etablissement etablissement = Etablissement.builder()
                .nom(request.getNomEtablissement())
                .code(codeFinal)
                .emailContact(request.getEmailContact())
                .telephone(request.getTelephone())
                .adresse(request.getAdresse())
                .planTarifaire(request.getPlanTarifaire() != null ? request.getPlanTarifaire() : "STANDARD")
                .dateExpirationAbonnement(expiryDate)
                .statut(StatutEtablissement.ACTIF)
                .build();

        Etablissement savedEtablissement = etablissementRepository.save(etablissement);

        // email de fallback si non fourni
        String adminEmail = (request.getAdminEmail() != null && !request.getAdminEmail().isBlank())
                ? request.getAdminEmail()
                : request.getAdminUsername() + "@" + savedEtablissement.getCode() + ".netaa-ecole.com";

        // Créer l'administrateur initial de cette école
        Utilisateur admin = Utilisateur.builder()
                .username(request.getAdminUsername())
                .email(adminEmail)
                .motDePasse(request.getAdminMotDePasse())
                .etablissement(savedEtablissement)
                .build();

        Profil profil = dtoMapper.toProfil(request.getAdminProfil());
        utilisateurService.inscrire(admin, profil, "DIRECTEUR");

        // Génération automatique du reçu PDF et envoi par e-mail
        try {
            byte[] pdfBytes = recuEtablissementPdfService.genererRecuAbonnementPdf(savedEtablissement.getId());
            emailService.sendEtablissementCreatedWithPdf(savedEtablissement, adminEmail, request.getAdminMotDePasse(), pdfBytes);
        } catch (Exception e) {
            System.err.println("Avertissement : Erreur lors de l'envoi du mail/PDF de reçu d'établissement : " + e.getMessage());
        }

        return mapToResponse(savedEtablissement);
    }

    @Override
    public List<EtablissementResponse> listerTous() {
        return etablissementRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public EtablissementResponse obtenirParId(Long id) {
        Etablissement etablissement = etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));
        return mapToResponse(etablissement);
    }

    @Override
    @Transactional
    public EtablissementResponse modifierStatut(Long id, StatutEtablissement statut) {
        Etablissement etablissement = etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));
        etablissement.setStatut(statut);
        Etablissement updated = etablissementRepository.save(etablissement);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public EtablissementResponse modifierInfos(Long id, ModifierEtablissementRequest request) {
        Etablissement etablissement = etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));
        etablissement.setNom(request.getNom());
        etablissement.setEmailContact(request.getEmailContact());
        etablissement.setTelephone(request.getTelephone());
        etablissement.setAdresse(request.getAdresse());
        Etablissement updated = etablissementRepository.save(etablissement);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public EtablissementResponse renouvelerAbonnement(Long id, String planTarifaire, int dureeMois) {
        if (dureeMois < 1) {
            throw new BadRequestException("La durée du renouvellement doit être d'au moins 1 mois.");
        }
        Etablissement etablissement = etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime baseDepart = (etablissement.getDateExpirationAbonnement() != null
                && etablissement.getDateExpirationAbonnement().isAfter(maintenant))
                ? etablissement.getDateExpirationAbonnement()
                : maintenant;

        etablissement.setDateExpirationAbonnement(baseDepart.plusMonths(dureeMois));
        if (planTarifaire != null && !planTarifaire.isBlank()) {
            etablissement.setPlanTarifaire(planTarifaire.trim().toUpperCase());
        }
        // Le renouvellement vaut réactivation : une suspension pour abonnement
        // expiré n'a plus lieu d'être une fois le paiement enregistré. Une
        // suspension pour un autre motif (abus, litige…) devra être relevée
        // explicitement par le Super-Admin — on ne clôture jamais ici.
        if (etablissement.getStatut() == StatutEtablissement.SUSPENDU) {
            etablissement.setStatut(StatutEtablissement.ACTIF);
        }

        Etablissement updated = etablissementRepository.save(etablissement);
        return mapToResponse(updated);
    }

    private EtablissementResponse mapToResponse(Etablissement etablissement) {
        String adminUsername = null;
        String adminNomComplet = null;
        String adminEmail = null;

        List<Utilisateur> users = utilisateurRepository.findByEtablissementId(etablissement.getId());
        Utilisateur admin = users.stream()
                .filter(u -> u.getRole() != null && "DIRECTEUR".equalsIgnoreCase(u.getRole().getNom()))
                .findFirst()
                .orElse(users.isEmpty() ? null : users.get(0));

        if (admin != null) {
            adminUsername = admin.getUsername();
            adminEmail = admin.getEmail();
            Profil profil = profilRepository.findByUtilisateurId(admin.getId()).orElse(null);
            if (profil != null) {
                adminNomComplet = (profil.getPrenom() != null ? profil.getPrenom() : "") + " " + (profil.getNom() != null ? profil.getNom() : "");
                adminNomComplet = adminNomComplet.trim();
            }
        }

        return EtablissementResponse.builder()
                .id(etablissement.getId())
                .nom(etablissement.getNom())
                .code(etablissement.getCode())
                .emailContact(etablissement.getEmailContact())
                .telephone(etablissement.getTelephone())
                .adresse(etablissement.getAdresse())
                .statut(etablissement.getStatut())
                .planTarifaire(etablissement.getPlanTarifaire())
                .dateExpirationAbonnement(etablissement.getDateExpirationAbonnement())
                .dateCreation(etablissement.getDateCreation())
                .adminUsername(adminUsername)
                .adminNomComplet(adminNomComplet)
                .adminEmail(adminEmail)
                .build();
    }
}


