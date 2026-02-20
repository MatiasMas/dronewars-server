package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.enums.UnitType;
import udegames.dronewarsserver.domain.model.BombProjectile;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.engine.GameState;

@Service
public class BombingService implements IBombingService {
    private static final float BOMB_FALL_SPEED = 25f;
    private static final float BOMB_BLAST_RADIUS = 8f;
    private static final int BOMB_DAMAGE = 2;

    private final GameState gameState;

    public BombingService(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean canLaunchBomb(String attackerUnitId, String playerId) {
        Unit attacker = gameState.getUnitById(attackerUnitId);

        if (attacker == null || attacker.isDestroyed()) {
            return false;
        }

        if (!attacker.getOwnerId().equals(playerId)) {
            return false;
        }

        // RF8: only the selected red drone can launch bombs.
        if (attacker.getType() != UnitType.AERIAL_DRONE) {
            return false;
        }

        if (!(attacker instanceof Drone drone)) {
            return false;
        }

        return drone.hasAmmo();
    }

    @Override
    public BombProjectile launchBomb(String attackerUnitId) {
        Unit attacker = gameState.getUnitById(attackerUnitId);

        if (!(attacker instanceof Drone drone)) {
            return null;
        }

        if (!drone.consumeAmmo()) {
            return null;
        }

        Position attackerPosition = attacker.getPosition();
        Position bombSpawnPosition = new Position(attackerPosition.getX(), attackerPosition.getY(), attackerPosition.getZ());

        BombProjectile projectile = new BombProjectile(
                attacker.getId(),
                attacker.getOwnerId(),
                bombSpawnPosition,
                BOMB_BLAST_RADIUS,
                BOMB_DAMAGE,
                BOMB_FALL_SPEED
        );

        gameState.addBombProjectile(projectile);
        return projectile;
    }
}
