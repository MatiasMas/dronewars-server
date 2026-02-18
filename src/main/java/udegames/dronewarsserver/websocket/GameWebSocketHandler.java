package udegames.dronewarsserver.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.AvailablePlayerDTO;
import udegames.dronewarsserver.dto.GameUnitsDTO;
import udegames.dronewarsserver.dto.ServerResponseDTO;
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.service.ISelectionService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private static final Set<String> registeredPlayers = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameState gameState;
    private final ISelectionService selectionService;

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);

    public GameWebSocketHandler(ISelectionService selectionService, GameState gameState) {
        this.selectionService = selectionService;
        this.gameState = gameState;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connectedSessions.add(session);
        logger.info("Client connected: {}", session.getId());

        // Send a list of available players to the client - to remove with the lobby creation
        sendAvailablePlayers(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectedSessions.remove(session);
        String playerId = sessionToPlayerId.remove(session.getId());

        if (playerId != null) {
            registeredPlayers.remove(playerId);
            logger.info("Player: {} unregistered from the game, available again", playerId);
        }

        logger.info("Client disconnected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String messageType = root.get("type").asText();
            logger.info("Message received, type: {}, session: {}", messageType, session.getId());

            switch (messageType) {
                case CommunicationEvents.ClientToServerEvents.REGISTER_PLAYER:
                    handleRegisterPlayer(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.SELECT_UNIT:
                    handleSelectUnit(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.GET_PLAYER_UNITS:
                    handleGetPlayerUnits(session, root);
                    break;
                default:
                    sendErrorMessage(session, "Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message.getPayload());
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }

    /*
     * Send a list of available players to the client - to remove with the lobby creation
     */
    private void sendAvailablePlayers(WebSocketSession session) {
        try {
            List<AvailablePlayerDTO> availablePlayers = gameState.getPlayers().stream()
                    .map(player -> new AvailablePlayerDTO(
                            player.getId(),
                            player.getName(),
                            !registeredPlayers.contains(player.getId())
                    ))
                    .toList();

            sendResponse(session, CommunicationEvents.ServerToClientEvents.AVAILABLE_PLAYERS, availablePlayers);

            logger.info("Available players sent to the client: {}", availablePlayers.size());
        } catch (IOException e) {
            logger.error("Error sending available players to the client", e);
        }
    }

    private void handleRegisterPlayer(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = root.get("playerId").asText();

        if (!gameState.doesPlayerExist(playerId)) {
            logger.error("Player does not exist in the game: {}", playerId);
            sendErrorMessage(session, "Player does not exist in the game: " + playerId);
            return;
        }

        if (registeredPlayers.contains(playerId)) {
            logger.warn("Player already registered in the game: {}", playerId);
            sendErrorMessage(session, "Player already registered in the game: " + playerId);
            return;
        }

        sessionToPlayerId.put(session.getId(), playerId);
        registeredPlayers.add(playerId);

        sendResponse(session, CommunicationEvents.ServerToClientEvents.PLAYER_REGISTERED, playerId);

        logger.info("Player correctly registered in session: {}", playerId);
    }

    private void handleSelectUnit(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        String unitId;

        // Check if a player is registered on the session
        if (playerId == null) {
            logger.warn("Player not registered on session: {}", session.getId());
            sendErrorMessage(session, "Player not registered on session: " + session.getId());
            return;
        }

        unitId = root.get("unitId").asText();
        logger.debug("Unit: {}, selected by player: {}", unitId, playerId);

        if (!selectionService.canSelectUnit(unitId, playerId)) {
            logger.warn("Unit cannot be selected: {}", unitId);
            sendErrorMessage(session, "Unit cannot be selected: " + unitId);
            return;
        }

        // Get a unit and map it to DTO
        Unit unit = selectionService.getUnit(unitId);

        // Convert a unit to DTO, so it can be sent to the client
        UnitSelectionDTO unitSelectionDTO = UnitMapper.toSelectionDTO(unit);

        sendResponse(session, CommunicationEvents.ServerToClientEvents.UNIT_SELECTED, unitSelectionDTO);

        logger.info("Unit: {}, selected by player: {}", unitId, playerId);
    }

    private void handleGetPlayerUnits(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        List<Unit> playerUnits;
        List<Unit> enemyUnits;

        // Check if a player is registered on the session
        if (playerId == null) {
            logger.warn("Player not registered on session: {}", session.getId());
            sendErrorMessage(session, "Player not registered on session: " + session.getId());
            return;
        }

        // Get game units
        playerUnits = gameState.getPlayerUnits(playerId);
        enemyUnits = gameState.getEnemyUnits(playerId);
        logger.debug("Player: {}, units: {}, enemy units: {}", playerId, playerUnits.size(), enemyUnits.size());

        // Convert units to DTOs, so they can be sent to the client
        List<UnitSelectionDTO> playerUnitDTOs = playerUnits.stream().map(UnitMapper::toSelectionDTO).toList();
        List<UnitSelectionDTO> enemyUnitDTOs = enemyUnits.stream().map(UnitMapper::toSelectionDTO).toList();

        GameUnitsDTO gameUnitsDTO = new GameUnitsDTO(playerUnitDTOs, enemyUnitDTOs);
        sendResponse(session, CommunicationEvents.ServerToClientEvents.UNITS_RECEIVED, gameUnitsDTO);

        logger.info("Units sent to {} client: player owns {}, enemy owns {}", playerId, playerUnitDTOs.size(), enemyUnitDTOs.size());
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        sendResponse(session, CommunicationEvents.ServerToClientEvents.SERVER_ERROR, errorMessage);
        logger.debug("Error sent: {}", errorMessage);
    }

    /**
     * Sends standard response to the client
     * Format: { type: "...", payload: {...} }
     *
     * @param session   Websocket session
     * @param eventType Event type (use CommunicationEvents)
     * @param payload   Data to be sent
     */
    private void sendResponse(WebSocketSession session, String eventType, Object payload) throws IOException {
        ServerResponseDTO response = new ServerResponseDTO(eventType, payload);
        String jsonResponse = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonResponse));
    }
}