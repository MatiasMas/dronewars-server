package udegames.dronewarsserver.service;

import udegames.dronewarsserver.domain.model.Unit;

public interface ISelectionService {
    // RF1, valida si el jugador puede seleccionar una unidad
    boolean canSelectUnit(String unitId, String playerId);

    // Obtiene la unidad a seleccionar
    Unit getUnit(String unitId);
}
