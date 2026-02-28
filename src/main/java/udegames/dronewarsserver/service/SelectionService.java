package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.engine.GameState;

@Service
public class SelectionService implements ISelectionService {
    private final GameState gameState;

    public SelectionService(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean canSelectUnit(String unitId, String playerId) {
        // Verificamos si el jugador existe
        if (!gameState.doesPlayerExist(playerId)) {
            return false;
        }

        // Verificamos si la unidad existe
        if (gameState.getUnitById(unitId) == null) {
            return false;
        }

        // Verificamos si la unidad pertenece al jugador
        if (!gameState.doesUnitBelongsToPlayer(unitId, playerId)) {
            return false;
        }

        return gameState.isUnitAlive(unitId);
    }

    @Override
    public Unit getUnit(String unitId) {
        return gameState.getUnitById(unitId);
    }
}
