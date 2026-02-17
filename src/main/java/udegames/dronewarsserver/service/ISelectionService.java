package udegames.dronewarsserver.service;

import udegames.dronewarsserver.domain.model.Unit;

public interface ISelectionService {
    // RF1, Validates if Player can select a unit or not
    boolean canSelectUnit(String unitId, String playerId);

    // Gets unit to be selected
    Unit getUnit(String unitId);
}