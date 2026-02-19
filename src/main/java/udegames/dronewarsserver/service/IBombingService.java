package udegames.dronewarsserver.service;

import udegames.dronewarsserver.domain.model.BombProjectile;

public interface IBombingService {
    boolean canLaunchBomb(String attackerUnitId, String playerId);

    BombProjectile launchBomb(String attackerUnitId);
}
