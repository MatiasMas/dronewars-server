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
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.dto.AvailablePlayerDTO;
import udegames.dronewarsserver.dto.GameUnitsDTO;
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.service.IMovementService;
import udegames.dronewarsserver.service.ISelectionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private static final Set<String> registeredPlayers = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameState gameState;
    private final ISelectionService selectionService;
    private final IMovementService movementService;
    private final GameWebSocketBroadcaster broadcaster;

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);

    public GameWebSocketHandler(ISelectionService selectionService, GameState gameState, IMovementService movementService, GameWebSocketBroadcaster broadcaster) {
        this.selectionService = selectionService;
        this.gameState = gameState;
        this.movementService = movementService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.registerSession(session);
        logger.info("Cliente conectado: {}", session.getId());

        // Enviar lista de jugadores disponibles al cliente (temporal, se elimina con lobby)
        sendAvailablePlayers(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregisterSession(session);
        String playerId = sessionToPlayerId.remove(session.getId());

        if (playerId != null) {
            registeredPlayers.remove(playerId);
            logger.info("Jugador: {} desregistrado del juego, disponible de nuevo", playerId);
        }

        logger.info("Cliente desconectado: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            logger.info("Payload crudo: {}", payload);

            JsonNode root = objectMapper.readTree(payload);
            JsonNode typeNode = root.get("type");
            String messageType = typeNode == null ? "" : typeNode.asText();
            String normalizedType = normalizeMessageType(messageType);
            logger.info("Mensaje recibido, tipo: {}, normalizado: {}, sesión: {}", messageType, normalizedType, session.getId());

            switch (normalizedType) {
                case CommunicationEvents.ClientToServerEvents.REGISTER_PLAYER:
                    handleRegisterPlayer(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.SELECT_UNIT:
                    handleSelectUnit(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.GET_PLAYER_UNITS:
                    handleGetPlayerUnits(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.MOVE_UNIT:
                    handleMoveUnit(session, root);
                    break;
                default:
                    sendErrorMessage(session, "Tipo de mensaje desconocido: " + messageType);
            }
        } catch (Exception e) {
            logger.error("Error al procesar el mensaje: {}", message.getPayload());
            sendErrorMessage(session, "Error al procesar el mensaje: " + e.getMessage());
        }
    }

    /*
     * Envía lista de jugadores disponibles al cliente (temporal, se elimina con lobby)
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

            logger.info("Jugadores disponibles enviados al cliente: {}", availablePlayers.size());
        } catch (IOException e) {
            logger.error("Error al enviar jugadores disponibles al cliente", e);
        }
    }

    private void handleRegisterPlayer(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = root.get("playerId").asText();

        if (!gameState.doesPlayerExist(playerId)) {
            logger.error("El jugador no existe en el juego: {}", playerId);
            sendErrorMessage(session, "El jugador no existe en el juego: " + playerId);
            return;
        }

        if (registeredPlayers.contains(playerId)) {
            logger.warn("El jugador ya está registrado en el juego: {}", playerId);
            sendErrorMessage(session, "El jugador ya está registrado en el juego: " + playerId);
            return;
        }

        sessionToPlayerId.put(session.getId(), playerId);
        registeredPlayers.add(playerId);

        sendResponse(session, CommunicationEvents.ServerToClientEvents.PLAYER_REGISTERED, playerId);

        logger.info("Jugador registrado correctamente en la sesión: {}", playerId);
    }

    private void handleSelectUnit(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        String unitId;

        // Verificar si hay un jugador registrado en la sesión
        if (playerId == null) {
            logger.warn("Jugador no registrado en la sesión: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesión: " + session.getId());
            return;
        }

        unitId = root.get("unitId").asText();
        logger.debug("Unit: {}, selected by player: {}", unitId, playerId);

        if (!selectionService.canSelectUnit(unitId, playerId)) {
            logger.warn("La unidad no puede ser seleccionada: {}", unitId);
            sendErrorMessage(session, "La unidad no puede ser seleccionada: " + unitId);
            return;
        }

        // Obtener una unidad y mapearla a DTO
        Unit unit = selectionService.getUnit(unitId);

        // Convertir a DTO para enviar al cliente
        UnitSelectionDTO unitSelectionDTO = UnitMapper.toSelectionDTO(unit);

        sendResponse(session, CommunicationEvents.ServerToClientEvents.UNIT_SELECTED, unitSelectionDTO);

        logger.info("Unidad: {}, seleccionada por jugador: {}", unitId, playerId);
    }

    private void handleGetPlayerUnits(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        List<Unit> playerUnits;
        List<Unit> enemyUnits;

        // Verificar si hay un jugador registrado en la sesión
        if (playerId == null) {
            logger.warn("Jugador no registrado en la sesión: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesión: " + session.getId());
            return;
        }

        // Obtener unidades del juego
        playerUnits = gameState.getPlayerUnits(playerId);
        enemyUnits = gameState.getEnemyUnits(playerId);
        logger.debug("Player: {}, units: {}, enemy units: {}", playerId, playerUnits.size(), enemyUnits.size());

        // Convertir unidades a DTOs para enviar al cliente
        List<UnitSelectionDTO> playerUnitDTOs = playerUnits.stream().map(UnitMapper::toSelectionDTO).toList();
        List<UnitSelectionDTO> enemyUnitDTOs = enemyUnits.stream().map(UnitMapper::toSelectionDTO).toList();

        GameUnitsDTO gameUnitsDTO = new GameUnitsDTO(playerUnitDTOs, enemyUnitDTOs);
        sendResponse(session, CommunicationEvents.ServerToClientEvents.UNITS_RECEIVED, gameUnitsDTO);

        logger.info("Unidades enviadas al cliente {}: jugador tiene {}, enemigo tiene {}", playerId, playerUnitDTOs.size(), enemyUnitDTOs.size());
    }

    private void handleMoveUnit(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());

        if (playerId == null) {
            logger.warn("Jugador no registrado en la sesión: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesión: " + session.getId());
            return;
        }

        JsonNode unitIdNode = root.get("unitId");
        JsonNode targetXNode = root.get("targetX");
        JsonNode targetYNode = root.get("targetY");

        if (unitIdNode == null || targetXNode == null || targetYNode == null) {
            logger.warn("Payload de movimiento inválido: {}", root);
            sendErrorMessage(session, "Payload de movimiento inválido");
            return;
        }

        String unitId = unitIdNode.asText();
        float targetX = (float) targetXNode.asDouble();
        float targetY = (float) targetYNode.asDouble();

        Unit unit = gameState.getUnitById(unitId);
        if (unit == null) {
            logger.warn("Unidad no encontrada: {}", unitId);
            sendErrorMessage(session, "Unidad no encontrada: " + unitId);
            return;
        }

        float targetZ = unit.getPosition().getZ();
        JsonNode targetZNode = root.get("targetZ");
        if (targetZNode != null && !targetZNode.isNull()) {
            targetZ = (float) targetZNode.asDouble();
        }

        Position target = new Position(targetX, targetY, targetZ);

        if (!movementService.canMoveUnit(unitId, playerId, target)) {
            logger.warn("Movimiento inválido: unitId={}, target=({}, {}, {})", unitId, targetX, targetY, targetZ);
            sendErrorMessage(session, "Movimiento inválido");
            return;
        }

        Unit movedUnit = movementService.moveUnit(unitId, target);
        if (movedUnit == null) {
            logger.warn("No se pudo mover la unidad: {}", unitId);
            sendErrorMessage(session, "No se pudo mover la unidad: " + unitId);
            return;
        }

        sendResponse(session, CommunicationEvents.ServerToClientEvents.MOVE_ACCEPTED, unitId);

        logger.info("Movimiento aceptado: unitId={}, target=({}, {}, {})", unitId, targetX, targetY, targetZ);
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        sendResponse(session, CommunicationEvents.ServerToClientEvents.SERVER_ERROR, errorMessage);
        logger.debug("Error enviado: {}", errorMessage);
    }

    /**
     * Envía respuesta estándar al cliente
     * Formato: { type: "...", payload: {...} }
     *
     * @param session   Sesión WebSocket
     * @param eventType Tipo de evento (usar CommunicationEvents)
     * @param payload   Datos a enviar
     */
    private void sendResponse(WebSocketSession session, String eventType, Object payload) throws IOException {
        broadcaster.send(session, eventType, payload);
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null) {
            return "";
        }

        return messageType.trim().replace(' ', '_').toUpperCase();
    }
}
