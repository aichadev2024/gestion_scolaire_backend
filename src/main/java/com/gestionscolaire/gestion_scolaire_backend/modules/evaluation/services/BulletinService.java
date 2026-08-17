package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.BulletinResponse;

public interface BulletinService {
    BulletinResponse genererBulletin(Long eleveId, String periode, String anneeScolaire);
    BulletinResponse getBulletinDetails(Long eleveId, String periode, String anneeScolaire);
    BulletinResponse verrouillerBulletin(Long bulletinId);
}


