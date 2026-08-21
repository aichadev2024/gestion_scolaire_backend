package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.CreateEtablissementWithAdminRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.EtablissementResponse;
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

    @Override
    @Transactional
    public EtablissementResponse creerEtablissementAvecAdmin(CreateEtablissementWithAdminRequest request) {
        if (etablissementRepository.existsByCode(request.getCodeEtablissement())) {
            throw new BadRequestException("Un établissement avec ce code existe déjà : " + request.getCodeEtablissement());
        }

        LocalDateTime expiryDate = request.getDateExpirationAbonnement() != null 
                ? request.getDateExpirationAbonnement() 
                : java.time.LocalDateTime.now().plusYears(1);

        Etablissement etablissement = Etablissement.builder()
                .nom(request.getNomEtablissement())
                .code(request.getCodeEtablissement().toLowerCase().trim())
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
        utilisateurService.inscrire(admin, profil, "ADMIN");

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

    private EtablissementResponse mapToResponse(Etablissement etablissement) {
        String adminUsername = null;
        String adminNomComplet = null;
        String adminEmail = null;

        List<Utilisateur> users = utilisateurRepository.findByEtablissementId(etablissement.getId());
        Utilisateur admin = users.stream()
                .filter(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().getNom()))
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


