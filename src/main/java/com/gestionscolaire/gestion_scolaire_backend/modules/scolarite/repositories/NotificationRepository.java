package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireIdOrderByDateCreationDesc(Long destinataireId);
    List<Notification> findByDestinataireIdAndEstLuOrderByDateCreationDesc(Long destinataireId, Boolean estLu);
}


