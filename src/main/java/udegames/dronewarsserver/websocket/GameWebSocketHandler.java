package udegames.dronewarsserver.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.service.ISelectionService;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ISelectionService selectionService;

    public GameWebSocketHandler(ISelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connectedSessions.add(session);
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectedSessions.remove(session);
        sessionToPlayerId.remove(session.getId());
        System.out.println("Client disconnected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String messageType = root.get("type").asText();

            switch (messageType) {
                case "SELECT_UNIT":
                    handleSelectUnit(session, root);
                    break;
                case "REGISTER_PLAYER":
                    handleRegisterPlayer(session, root);
                    break;
                default:
                    sendErrorMessage(session, "Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }

    private void handleRegisterPlayer(WebSocketSession session, JsonNode root) {
        String playerId = root.get("playerId").asText();
        sessionToPlayerId.put(session.getId(), playerId);
        System.out.println("Player correctly registered in session: " + playerId);
    }

    private void handleSelectUnit(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        String unitId;

        // Check if player is registered on the session
        if (playerId == null) {
            sendErrorMessage(session, "Player not registered on session: " + session.getId());
            return;
        }

        unitId = root.get("unitId").asText();

        if (!selectionService.canSelectUnit(unitId, playerId)) {
            sendErrorMessage(session, "Unit cannot be selected: " + unitId);
            return;
        }

        // Get unit and map it to DTO
        Unit unit = selectionService.getUnit(unitId);
        UnitSelectionDTO unitSelectionDTO = UnitMapper.toSelectionDTO(unit);

        // Send unit selection confirmation to a client
        String response = objectMapper.writeValueAsString(unitSelectionDTO);
        session.sendMessage(new TextMessage(response));

        System.out.println("Unit: " + unitId + ", selected by player: " + playerId);
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        String response = "{\"error\": \"" + errorMessage + "\"}";
        session.sendMessage(new TextMessage(response));
    }
}