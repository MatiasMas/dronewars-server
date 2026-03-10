package udegames.dronewarsserver.service;

import udegames.dronewarsserver.domain.entity.Position;
import udegames.dronewarsserver.domain.entity.Unit;

public interface IMovementService {
    boolean canMoveUnit(String unitId, String playerId, Position target);

    Unit moveUnit(String unitId, Position target);
}
