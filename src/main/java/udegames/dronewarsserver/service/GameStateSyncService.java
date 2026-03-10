package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.DroneCarrier;
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
                    int municionDisponible = 0;
                    if (unit instanceof Drone dron) {
                        combustible = dron.getCombustible();
                    }
                    if (unit instanceof DroneCarrier carrier) {
                        municionDisponible = carrier.getAvailableAmmoSupply();
                    }
                    return new UnitPositionDTO(unit.getId(), unit.getPosition(), combustible, municionDisponible);
                })
                .toList();

        webSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_STATE_UPDATE, unitPositions);
    }
}
