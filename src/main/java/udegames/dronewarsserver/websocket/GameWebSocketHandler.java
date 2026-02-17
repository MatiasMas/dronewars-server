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
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.service.ISelectionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
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
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectedSessions.remove(session);
        sessionToPlayerId.remove(session.getId());
        logger.info("Client disconnected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String messageType = root.get("type").asText();
            logger.info("Message received, type: {}, session: {}", messageType, session.getId());

            switch (messageType) {
                case "REGISTER_PLAYER":
                    handleRegisterPlayer(session, root);
                    break;
                case "SELECT_UNIT":
                    handleSelectUnit(session, root);
                    break;
                case "GET_PLAYER_UNITS":
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

    private void handleRegisterPlayer(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = root.get("playerId").asText();
        sessionToPlayerId.put(session.getId(), playerId);

        if(!gameState.doesPlayerExist(playerId)) {
            logger.error("Player does not exist in the game: {}", playerId);
            sendErrorMessage(session, "Player does not exist in the game: " + playerId);
        }

        String response = "{\"type\": \"PLAYER_REGISTERED\", \"playerId\": \"" + playerId + "\"}";
        session.sendMessage(new TextMessage(response));

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

        // Send unit selection confirmation to a client
        String response = objectMapper.writeValueAsString(unitSelectionDTO);
        session.sendMessage(new TextMessage(response));

        logger.info("Unit: {}, selected by player: {}", unitId, playerId);
    }

    private void handleGetPlayerUnits(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        List<Unit> playerUnits;

        // Check if a player is registered on the session
        if (playerId == null) {
            logger.warn("Player not registered on session: {}", session.getId());
            sendErrorMessage(session, "Player not registered on session: " + session.getId());
            return;
        }

        // Get player units
        playerUnits = gameState.getPlayerUnits(playerId);
        logger.debug("Player: {}, units retrieved: {}", playerId, playerUnits);

        // Convert units to DTOs, so they can be sent to the client
        List<UnitSelectionDTO> unitDTOs = playerUnits.stream().map(UnitMapper::toSelectionDTO).toList();

        // Send units to a client
        String response = objectMapper.writeValueAsString(unitDTOs);
        session.sendMessage(new TextMessage(response));

        logger.info("{} player units sent to client: {}", unitDTOs.size(), playerId);

        for (UnitSelectionDTO dto : unitDTOs) {
            logger.debug("   - {} [{}] in ({}, {}, {})",
                    dto.getUnitId(), dto.getType(), dto.getX(), dto.getY(), dto.getZ());
        }
    }


    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        String response = "{\"error\": \"" + errorMessage + "\"}";
        session.sendMessage(new TextMessage(response));
        logger.debug("Error message sent to client: {}", errorMessage);
    }
}