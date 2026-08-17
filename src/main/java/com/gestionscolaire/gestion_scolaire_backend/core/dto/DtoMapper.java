package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.UtilisateurResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.ClasseResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EnseignantResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public ProfilDto toProfilDto(Profil profil) {
        if (profil == null) {
            return null;
        }
        return ProfilDto.builder()
                .prenom(profil.getPrenom())
                .nom(profil.getNom())
                .telephone(profil.getTelephone())
                .email(profil.getEmail())
                .photoUrl(profil.getPhotoUrl())
                .genre(profil.getGenre())
                .dateNaissance(profil.getDateNaissance())
                .adresse(profil.getAdresse())
                .build();
    }

    public Profil toProfil(ProfilDto dto) {
        if (dto == null) {
            return new Profil();
        }
        return Profil.builder()
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .telephone(dto.getTelephone())
                .email(dto.getEmail())
                .photoUrl(dto.getPhotoUrl())
                .genre(dto.getGenre())
                .dateNaissance(dto.getDateNaissance())
                .adresse(dto.getAdresse())
                .build();
    }

    public UtilisateurResponse toUtilisateurResponse(Utilisateur utilisateur, Profil profil) {
        String etabNom = (utilisateur.getEtablissement() != null) 
                ? utilisateur.getEtablissement().getNom() 
                : "Établissement Scolaire";
        return UtilisateurResponse.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .username(utilisateur.getUsername())
                .role(utilisateur.getRole().getNom())
                .estActif(utilisateur.getEstActif())
                .dateCreation(utilisateur.getDateCreation())
                .profil(toProfilDto(profil))
                .etablissementNom(etabNom)
                .build();
    }

    public EleveResponse toEleveResponse(Eleve eleve) {
        String etabNom = null;
        if (eleve.getClasse() != null && eleve.getClasse().getEtablissement() != null) {
            etabNom = eleve.getClasse().getEtablissement().getNom();
        } else if (eleve.getProfil() != null && eleve.getProfil().getUtilisateur() != null && eleve.getProfil().getUtilisateur().getEtablissement() != null) {
            etabNom = eleve.getProfil().getUtilisateur().getEtablissement().getNom();
        }
        if (etabNom == null || etabNom.trim().isEmpty()) {
            etabNom = "LYCÉE MASSA MAKAN DIABATÉ";
        }
        Long pId = null;
        if (eleve.getParent() != null) {
            if (eleve.getParent().getProfil() != null && eleve.getParent().getProfil().getUtilisateur() != null) {
                pId = eleve.getParent().getProfil().getUtilisateur().getId();
            } else {
                pId = eleve.getParent().getId();
            }
        }
        return EleveResponse.builder()
                .id(eleve.getId())
                .matricule(eleve.getMatricule())
                .statut(eleve.getStatut())
                .classeId(eleve.getClasse() != null ? eleve.getClasse().getId() : null)
                .classeNom(eleve.getClasse() != null ? eleve.getClasse().getNom() : null)
                .parentId(pId)
                .profil(toProfilDto(eleve.getProfil()))
                .etablissementNom(etabNom)
                .build();
    }

    public EnseignantResponse toEnseignantResponse(Enseignant enseignant) {
        return EnseignantResponse.builder()
                .id(enseignant.getId())
                .matricule(enseignant.getMatricule())
                .biographie(enseignant.getBiographie())
                .profil(toProfilDto(enseignant.getProfil()))
                .build();
    }

    public ClasseResponse toClasseResponse(Classe classe) {
        return ClasseResponse.builder()
                .id(classe.getId())
                .nom(classe.getNom())
                .niveauId(classe.getNiveau() != null ? classe.getNiveau().getId() : null)
                .niveauNom(classe.getNiveau() != null ? classe.getNiveau().getNom() : null)
                .enseignantPrincipalId(classe.getEnseignantPrincipal() != null ? classe.getEnseignantPrincipal().getId() : null)
                .anneeScolaire(classe.getAnneeScolaire())
                .capaciteMax(classe.getCapaciteMax())
                .build();
    }
}


