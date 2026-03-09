package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.entity.Drone;
import udegames.dronewarsserver.dto.UnitPositionDTO;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.websocket.CommunicationEvents;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

import java.util.List;

@Service
public class GameStateSyncService {
    private final GameState gameState;
    private final GameWebSocketHandler webSocketHandler;

    public GameStateSyncService(GameState gameState, GameWebSocketHandler webSocketHandler) {
        this.gameState = gameState;
        this.webSocketHandler = webSocketHandler;
    }

    public void broadcastGameState() {
        List<UnitPositionDTO> unitPositions = gameState.getUnits().stream()
                .map(unit -> {
                    float combustible = 0f;
                    if (unit instanceof Drone dron) {
                        combustible = dron.getCombustible();
                    }
                    return new UnitPositionDTO(unit.getId(), unit.getPosition(), combustible);
                })
                .toList();

        webSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_STATE_UPDATE, unitPositions);
    }
}
