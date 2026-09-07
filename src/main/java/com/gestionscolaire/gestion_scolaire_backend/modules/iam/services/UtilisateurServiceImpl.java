package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Role;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.RoleRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService;
import com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UtilisateurServiceImpl.class);

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ProfilRepository profilRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ParentRepository parentRepository;

    @Override
    public Utilisateur inscrire(Utilisateur utilisateur, Profil profil, String nomRole) {
        Role role = roleRepository.findByNom(nomRole)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + nomRole));
        
        if (utilisateur.getEtablissement() == null) {
            try {
                utilisateur.setEtablissement(com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement());
            } catch (Exception ignored) {}
        }

        if (utilisateur.getEmail() != null && utilisateur.getEmail().isBlank()) {
            utilisateur.setEmail(null);
        }
        if (profil != null && profil.getEmail() != null && profil.getEmail().isBlank()) {
            profil.setEmail(null);
        }

        String rawPassword = utilisateur.getMotDePasse();
        utilisateur.setRole(role);
        if (utilisateur.getEstPremierLogin() == null) {
            utilisateur.setEstPremierLogin(true);
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        if (profil != null) {
            if (profil.getEmail() == null || profil.getEmail().isBlank()) {
                profil.setEmail(savedUser.getEmail());
            }
            profil.setUtilisateur(savedUser);
            Profil savedProfil = profilRepository.save(profil);

        // Si le rôle créé est PARENT, on crée automatiquement l'enregistrement dans la table parents
        if ("PARENT".equalsIgnoreCase(nomRole)) {
            if (parentRepository.findByProfilUtilisateurId(savedUser.getId()).isEmpty()) {
                parentRepository.save(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent.builder()
                        .profil(savedProfil)
                        .build());
            }
        }
        }

        // Envoi automatique de l'email de bienvenue Brevo (si l'utilisateur possède un e-mail)
        if (savedUser.getEmail() != null && !savedUser.getEmail().isBlank()) {
            try {
                logger.info("📧 [ENVOI BIENVENUE] Envoi des identifiants au compte [{}] ({}) à [{}]", savedUser.getUsername(), nomRole, savedUser.getEmail());
                emailService.sendWelcomeEmail(savedUser, rawPassword);
            } catch (Exception e) {
                logger.error("❌ Erreur lors de l'envoi de l'email de bienvenue à {}: {}", savedUser.getEmail(), e.getMessage());
            }
        } else {
            logger.info("ℹ️ Compte [{}] ({}) créé sans adresse e-mail. Les identifiants doivent être transmis manuellement par l'administration.", savedUser.getUsername(), nomRole);
        }

        return savedUser;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void syncParentsOnStartup() {
        try {
            List<Utilisateur> parentsUsers = utilisateurRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "PARENT".equalsIgnoreCase(u.getRole().getNom()))
                    .toList();

            for (Utilisateur u : parentsUsers) {
                if (parentRepository.findByProfilUtilisateurId(u.getId()).isEmpty()) {
                    Profil p = profilRepository.findByUtilisateurId(u.getId()).orElse(null);
                    if (p != null) {
                        parentRepository.save(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent.builder()
                                .profil(p)
                                .build());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public Utilisateur inscrirePremierAdmin(Utilisateur utilisateur, Profil profil) {
        if (utilisateurRepository.existsByRoleNom("ADMIN")) {
            throw new BadRequestException("Un administrateur existe déjà dans le système. La création d'autres administrateurs doit se faire par un administrateur connecté.");
        }
        return inscrire(utilisateur, profil, "ADMIN");
    }

    @Override
    public Utilisateur inscrireSuperAdmin(Utilisateur utilisateur, Profil profil) {
        if (utilisateurRepository.existsByRoleNom("SUPER_ADMIN")) {
            throw new BadRequestException("Un compte Super-Admin existe déjà sur la plateforme Netaa. La création de compte Super-Admin est verrouillée.");
        }
        utilisateur.setEtablissement(null); // Le SuperAdmin doit impérativement avoir etablissement_id = null
        return inscrire(utilisateur, profil, "SUPER_ADMIN");
    }

    @Override
    public Optional<Utilisateur> trouverParEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    @Override
    public List<Utilisateur> listerTous() {
        try {
            com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails current = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser();
            if (current != null && current.getUtilisateur() != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(current.getUtilisateur().getRole().getNom())) {
                    return utilisateurRepository.findAll();
                }
                if (current.getUtilisateur().getEtablissement() != null) {
                    Long etabId = current.getUtilisateur().getEtablissement().getId();
                    return utilisateurRepository.findAll().stream()
                            .filter(u -> u.getEtablissement() != null && etabId.equals(u.getEtablissement().getId()))
                            .toList();
                }
            }
        } catch (Exception ignored) {}
        return utilisateurRepository.findAll();
    }

    @Override
    public void modifierStatut(Long id, boolean estActif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        utilisateur.setEstActif(estActif);
        utilisateurRepository.save(utilisateur);
    }

    @Override
    public Utilisateur modifierUtilisateur(Long id, Utilisateur details, Profil profilDetails, String nomRole) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable ID : " + id));

        if (details.getUsername() != null && !details.getUsername().isBlank()) {
            utilisateur.setUsername(details.getUsername().trim());
        }
        if (details.getEmail() != null) {
            utilisateur.setEmail(details.getEmail().isBlank() ? null : details.getEmail().trim());
        }

        if (details.getMotDePasse() != null && !details.getMotDePasse().isBlank()) {
            utilisateur.setMotDePasse(passwordEncoder.encode(details.getMotDePasse()));
        }

        if (nomRole != null && !nomRole.isBlank()) {
            Role role = roleRepository.findByNom(nomRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + nomRole));
            utilisateur.setRole(role);
        }

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        Profil profil = profilRepository.findByUtilisateurId(id).orElseGet(() -> Profil.builder().utilisateur(savedUser).build());
        if (profilDetails != null) {
            profil.setPrenom(profilDetails.getPrenom());
            profil.setNom(profilDetails.getNom());
            if (profilDetails.getTelephone() != null) profil.setTelephone(profilDetails.getTelephone());
            if (profilDetails.getGenre() != null) profil.setGenre(profilDetails.getGenre());
            if (profilDetails.getAdresse() != null) profil.setAdresse(profilDetails.getAdresse());
            profil.setEmail(savedUser.getEmail());
            profilRepository.save(profil);
        }

        return savedUser;
    }

    @Override
    public void supprimerUtilisateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable ID : " + id));

        // Delete parent entry if role is PARENT
        try {
            parentRepository.findByProfilUtilisateurId(id).ifPresent(p -> parentRepository.delete(p));
        } catch (Exception ignored) {}

        // Delete profile entry if exists
        try {
            profilRepository.findByUtilisateurId(id).ifPresent(p -> profilRepository.delete(p));
        } catch (Exception ignored) {}

        utilisateurRepository.delete(utilisateur);
    }
}


