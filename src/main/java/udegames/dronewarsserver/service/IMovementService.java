package udegames.dronewarsserver.service;

import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;

public interface IMovementService {
    boolean canMoveUnit(String unitId, String playerId, Position target);

    Unit moveUnit(String unitId, Position target);
}
