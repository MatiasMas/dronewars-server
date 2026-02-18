package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.engine.GameState;

@Service
public class MovementService implements IMovementService {
    private static final float MIN_X = 0f;
    private static final float MAX_X = 200f;
    private static final float MIN_Y = 0f;
    private static final float MAX_Y = 200f;
    private static final float MIN_Z = 0f;
    private static final float BASE_MAX_Z = 10f;
    private static final float PLAYER_HEIGHT_BONUS_FACTOR = 1.005f;
    private static final float DEFAULT_MOVE_SPEED_UNITS_PER_SEC = 20f;

    private final GameState gameState;

    public MovementService(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean canMoveUnit(String unitId, String playerId, Position target) {
        if (!gameState.doesPlayerExist(playerId)) {
            return false;
        }

        Unit unit = gameState.getUnitById(unitId);
        if (unit == null) {
            return false;
        }

        if (!gameState.doesUnitBelongsToPlayer(unitId, playerId)) {
            return false;
        }

        if (!gameState.isUnitAlive(unitId)) {
            return false;
        }

        if (target == null) {
            return false;
        }

        return isWithinBounds(target) && isWithinHeight(target, playerId);
    }

    @Override
    public Unit moveUnit(String unitId, Position target) {
        Unit unit = gameState.getUnitById(unitId);
        if (unit == null) {
            return null;
        }

        gameState.setUnitMovement(unitId, target, DEFAULT_MOVE_SPEED_UNITS_PER_SEC);
        return unit;
    }

    private boolean isWithinBounds(Position target) {
        return target.getX() >= MIN_X
                && target.getX() <= MAX_X
                && target.getY() >= MIN_Y
                && target.getY() <= MAX_Y;
    }

    private boolean isWithinHeight(Position target, String playerId) {
        float maxZ = getMaxHeightForPlayer(playerId);
        return target.getZ() >= MIN_Z && target.getZ() <= maxZ;
    }

    private float getMaxHeightForPlayer(String playerId) {
        if ("player_1".equals(playerId)) {
            return BASE_MAX_Z * PLAYER_HEIGHT_BONUS_FACTOR;
        }

        return BASE_MAX_Z;
    }
}
