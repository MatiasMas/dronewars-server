package udegames.dronewarsserver.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import udegames.dronewarsserver.domain.entity.*;
import udegames.dronewarsserver.dto.*;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.engine.GameEngine;
import udegames.dronewarsserver.service.IAmmoService;
import udegames.dronewarsserver.service.IBombingService;
import udegames.dronewarsserver.service.IMissileService;
import udegames.dronewarsserver.service.IMovementService;
import udegames.dronewarsserver.service.ISelectionService;
import udegames.dronewarsserver.service.MainMenuService;
import udegames.dronewarsserver.service.PersistenciaPartidaService;
import udegames.dronewarsserver.service.RankingService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private static final Set<String> registeredPlayers = ConcurrentHashMap.newKeySet();
    private static final long RECONNECT_GRACE_SECONDS = 1;
    private static final String DISCONNECT_WIN_REASON = "PLAYER_DISCONNECTED_TIMEOUT";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameState gameState;
    private final ISelectionService selectionService;
    private final IMovementService movementService;
    private final IAmmoService servicioMunicion;
    private final IBombingService bombingService;
    private final IMissileService servicioMisil;
    private final MainMenuService mainMenuService;
    private final PersistenciaPartidaService persistenciaPartidaService;
    private final GameEngine gameEngine;
    private final RankingService rankingService;
    private final ScheduledExecutorService disconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> pendingDisconnectTasks = new ConcurrentHashMap<>();


    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private volatile boolean gameEnded = false;

    public GameWebSocketHandler(
            ISelectionService selectionService,
            GameState gameState,
            IMovementService movementService,
            IAmmoService servicioMunicion,
            IBombingService bombingService,
            IMissileService servicioMisil,
            MainMenuService mainMenuService,
            PersistenciaPartidaService persistenciaPartidaService,
            @Lazy GameEngine gameEngine,
            RankingService rankingService
    ) {
        this.selectionService = selectionService;
        this.bombingService = bombingService;
        this.gameState = gameState;
        this.movementService = movementService;
        this.servicioMunicion = servicioMunicion;
        this.servicioMisil = servicioMisil;
        this.mainMenuService = mainMenuService;
        this.persistenciaPartidaService = persistenciaPartidaService;
        this.gameEngine = gameEngine;
        this.rankingService = rankingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connectedSessions.add(session);
        logger.info("Cliente conectado: {}", session.getId());

        // Enviar lista de jugadores disponibles al cliente (temporal, se elimina con lobby)
        sendAvailablePlayers(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectedSessions.remove(session);
        boolean wasMatchInProgress = !gameEnded && registeredPlayers.size() >= 2;
        String playerId = sessionToPlayerId.remove(session.getId());

        if (playerId != null) {
            registeredPlayers.remove(playerId);
            logger.info("Jugador: {} desregistrado del juego, disponible de nuevo", playerId);

            if (wasMatchInProgress) {
                scheduleDisconnectForfeit(playerId);
            }
        }

        logger.info("Cliente desconectado: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            logger.info("Payload crudo: {}", payload);

            JsonNode root = objectMapper.readTree(payload);
            JsonNode typeNode = root.get("type");
            String messageType = typeNode == null ? "" : typeNode.asText();
            if (messageType != null && !messageType.isBlank()) {
                // Normalizamos espacios para evitar errores por tipeo en el evento.
                messageType = messageType.trim().replaceAll("\\s+", "_");
            }
            logger.info("Mensaje recibido, tipo: {}, sesion: {}", messageType, session.getId());

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
                case CommunicationEvents.ClientToServerEvents.LAUNCH_BOMB:
                    handleLaunchBomb(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.MOVE_UNIT:
                    handleMoveUnit(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.RELOAD_AMMO:
                    handleRecargarMunicion(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.LAUNCH_MISSILE:
                    hanldeDispararMisil(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.CREATE_NEW_GAME:
                    handleCreateNewGame(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.JOIN_GAME:
                    handleJoinGame(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.LOAD_SAVED_GAME:
                    handleLoadSavedGame(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.SET_GAME_PAUSED:
                    handleSetGamePaused(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.GET_RANKING:
                    handleGetRanking(session);
                    break;
                case CommunicationEvents.ClientToServerEvents.EXIT_GAME:
                    handleExitGame(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.REQUEST_SAVE_GAME:
                    handleSaveGame(session);
                    break;
                case CommunicationEvents.ClientToServerEvents.RESET_GAME:
                    handleResetGame(session);
                    break;
                case CommunicationEvents.ClientToServerEvents.REQUEST_AVAILABLE_PLAYERS:
                    sendAvailablePlayers(session);
                    break;
                case CommunicationEvents.ClientToServerEvents.SAVE_WINNER_SCORE:
                    handleSaveWinnerScore(session, root);
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
     * Envia lista de jugadores disponibles al cliente (temporal, se elimina con lobby)
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

    /*
     * Broadcast lista de jugadores disponibles a todos los clientes conectados
     */
    private void broadcastAvailablePlayers() {
        List<AvailablePlayerDTO> availablePlayers = gameState.getPlayers().stream()
                .map(player -> new AvailablePlayerDTO(
                        player.getId(),
                        player.getName(),
                        !registeredPlayers.contains(player.getId())
                ))
                .toList();

        for (WebSocketSession s : connectedSessions) {
            if (s.isOpen()) {
                try {
                    sendResponse(s, CommunicationEvents.ServerToClientEvents.AVAILABLE_PLAYERS, availablePlayers);
                } catch (IOException e) {
                    logger.error("Error al hacer broadcast de jugadores disponibles", e);
                }
            }
        }

        logger.debug("Broadcast de jugadores disponibles completado: {} jugadores", availablePlayers.size());
    }

    private void handleRegisterPlayer(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode datos = obtenerDatos(root);
        JsonNode playerIdNode = datos.get("playerId");
        String playerId = playerIdNode == null ? null : playerIdNode.asText();
        if (playerId == null || playerId.isBlank()) {
            sendErrorMessage(session, "PlayerId invalido");
            return;
        }

        Map<String, Object> payloadPausa = Map.of(
                "paused", gameState.isPartidaPausada(),
                "updatedBy", "server",
                "timestamp", System.currentTimeMillis()
        );
        sendResponse(session, CommunicationEvents.ServerToClientEvents.GAME_PAUSE_UPDATED, payloadPausa);

        if (!gameState.doesPlayerExist(playerId)) {
            logger.error("El jugador no existe en el juego: {}", playerId);
            sendErrorMessage(session, "El jugador no existe en el juego: " + playerId);
            return;
        }

        if (registeredPlayers.contains(playerId)) {
            logger.warn("El jugador ya esta registrado en el juego: {}", playerId);
            sendErrorMessage(session, "El jugador ya esta registrado en el juego: " + playerId);
            return;
        }

        sessionToPlayerId.put(session.getId(), playerId);
        registeredPlayers.add(playerId);
        cancelDisconnectTimeout(playerId);
        if (registeredPlayers.size() == 1) {
            gameEnded = false;
        }

        sendResponse(session, CommunicationEvents.ServerToClientEvents.PLAYER_REGISTERED, playerId);

        // Broadcast updated available players to all connected clients
        broadcastAvailablePlayers();

        logger.info("Jugador registrado correctamente en la sesion: {}", playerId);
    }

    private void handleSelectUnit(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        String unitId;

        // Verificar si hay un jugador registrado en la sesion
        if (playerId == null) {
            logger.warn("Jugador no registrado en la sesion: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesion: " + session.getId());
            return;
        }

        JsonNode datos = obtenerDatos(root);
        JsonNode unitIdNode = datos.get("unitId");
        unitId = unitIdNode == null ? null : unitIdNode.asText();
        if (unitId == null || unitId.isBlank()) {
            logger.warn("Payload de seleccion invalido");
            sendErrorMessage(session, "Payload de seleccion invalido");
            return;
        }
        logger.debug("Unidad: {}, seleccionada por jugador: {}", unitId, playerId);

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

        // Verificar si hay un jugador registrado en la sesion
        if (playerId == null) {
            logger.warn("Jugador no registrado en la sesion: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesion: " + session.getId());
            return;
        }

        // Obtener unidades del juego
        playerUnits = gameState.getPlayerUnits(playerId);
        enemyUnits = gameState.getEnemyUnits(playerId);
        logger.debug("Jugador: {}, unidades: {}, unidades enemigas: {}", playerId, playerUnits.size(), enemyUnits.size());

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
            logger.warn("Jugador no registrado en la sesion: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesion: " + session.getId());
            return;
        }

        if (gameState.isPartidaPausada()) {
            sendErrorMessage(session, "La partida esta pausada");
            return;
        }

        JsonNode datos = obtenerDatos(root);
        JsonNode unitIdNode = datos.get("unitId");
        JsonNode targetXNode = datos.get("targetX");
        JsonNode targetYNode = datos.get("targetY");
        if (unitIdNode == null || targetXNode == null || targetYNode == null) {
            logger.warn("Payload de movimiento invalido");
            sendErrorMessage(session, "Payload de movimiento invalido");
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
        JsonNode targetZNode = datos.get("targetZ");
        if (targetZNode != null && !targetZNode.isNull()) {
            targetZ = (float) targetZNode.asDouble();
        }

        Position target = new Position(targetX, targetY, targetZ);

        if (!movementService.canMoveUnit(unitId, playerId, target)) {
            logger.warn("Movimiento invalido: unitId={}, target=({}, {}, {})", unitId, targetX, targetY, targetZ);
            sendErrorMessage(session, "Movimiento invalido");
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

    private void handleRecargarMunicion(WebSocketSession session, JsonNode root) throws IOException {
        String idJugador = sessionToPlayerId.get(session.getId());

        if (idJugador == null) {
            logger.warn("Jugador no registrado en la sesion: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesion: " + session.getId());
            return;
        }

        if (gameState.isPartidaPausada()) {
            sendErrorMessage(session, "La partida esta pausada");
            return;
        }

        // Payload esperado:
        // payload: { unitId: "<id_dron>", carrierId: "<id_portadrones_opcional>" }
        JsonNode datos = obtenerDatos(root);
        JsonNode nodoIdUnidad = datos.get("unitId");
        if (nodoIdUnidad == null || nodoIdUnidad.isNull()) {
            logger.warn("Payload de recarga invalido");
            sendErrorMessage(session, "Payload de recarga invalido");
            return;
        }

        String idUnidad = nodoIdUnidad.asText();
        JsonNode nodoIdPortadrones = datos.get("carrierId");
        String idPortadrones = null;
        if (nodoIdPortadrones != null && !nodoIdPortadrones.isNull()) {
            idPortadrones = nodoIdPortadrones.asText();
        }

        if (!servicioMunicion.canReloadAmmo(idUnidad, idJugador, idPortadrones)) {
            logger.warn("Recarga invalida: unitId={}, carrierId={}", idUnidad, idPortadrones);
            sendErrorMessage(session, "Recarga invalida");
            return;
        }

        int municion = servicioMunicion.reloadAmmo(idUnidad);
        if (municion < 0) {
            logger.warn("No se pudo recargar municion: {}", idUnidad);
            sendErrorMessage(session, "No se pudo recargar municion: " + idUnidad);
            return;
        }

        Unit unidad = gameState.getUnitById(idUnidad);
        float combustible = 0f;
        if (unidad instanceof Drone dron) {
            combustible = dron.getCombustible();
        }

        AmmoReloadedDTO response = new AmmoReloadedDTO(idUnidad, municion, combustible);
        sendResponse(session, CommunicationEvents.ServerToClientEvents.MUNICION_RECARGADA, response);

        logger.info("Municion recargada: unitId={}, ammo={}", idUnidad, municion);
    }

    private void handleLaunchBomb(WebSocketSession session, JsonNode root) throws IOException {
        String idJugador = sessionToPlayerId.get(session.getId());
        if (idJugador == null) {
            logger.warn("Jugador no registrado en la sesion: {}", session.getId());
            sendErrorMessage(session, "Jugador no registrado en la sesion: " + session.getId());
            return;
        }

        if (gameState.isPartidaPausada()) {
            sendErrorMessage(session, "La partida esta pausada");
            return;
        }

        JsonNode datos = obtenerDatos(root);
        JsonNode nodoIdUnidad = datos.get("unitId");
        if (nodoIdUnidad == null || nodoIdUnidad.isNull()) {
            logger.warn("Payload de ataque invalido");
            sendErrorMessage(session, "Payload de ataque invalido");
            return;
        }

        String idUnidad = nodoIdUnidad.asText();
        if (!bombingService.canLaunchBomb(idUnidad, idJugador)) {
            logger.warn("Ataque invalido: unitId={}", idUnidad);
            sendErrorMessage(session, "Ataque invalido");
            return;
        }

        IBombingService.BombAttackResult resultado = bombingService.launchBomb(idUnidad);
        if (resultado == null) {
            logger.warn("No se pudo lanzar bomba: {}", idUnidad);
            sendErrorMessage(session, "No se pudo lanzar bomba");
            return;
        }

        BombLaunchedDTO bombaLanzada = resultado.getBombLaunched();
        BombExplodedDTO bombaExplotada = resultado.getBombExploded();

        // Notificamos a todos para actualizar UI en tiempo real.
        broadcastToAll(CommunicationEvents.ServerToClientEvents.BOMB_LAUNCHED, bombaLanzada);
        broadcastToAll(CommunicationEvents.ServerToClientEvents.BOMB_EXPLODED, bombaExplotada);

        logger.info("Bomba lanzada: unitId={}, bombId={}", idUnidad, bombaLanzada.getBombId());
    }

    private void hanldeDispararMisil(WebSocketSession session, JsonNode root) throws IOException {
        String idJugador = sessionToPlayerId.get(session.getId());
        if(idJugador == null) {
            logger.warn("Jugador no registrado en la sesion: {}", session.getId());
            return;
        }

        if (gameState.isPartidaPausada()) {
            sendErrorMessage(session, "La partida esta pausada");
            return;
        }

        JsonNode datos = obtenerDatos(root);
        JsonNode nodoIdUnidad = datos.get("unitId");
        JsonNode nodoIdObjetivo = datos.get("objetivoId");
        if (nodoIdObjetivo == null || nodoIdObjetivo.isNull()) {
            nodoIdObjetivo = datos.get("targetUnitId");
        }
        JsonNode nodoX = datos.get("targetX");
        JsonNode nodoY = datos.get("targetY");

        if(nodoIdUnidad == null || nodoIdUnidad.isNull()) {
            logger.warn("Payload de misil invalido");
            sendErrorMessage(session, "Payload de misil invalido");
            return;
        }

        String idUnidad = nodoIdUnidad.asText();
        String objetivoId = nodoIdObjetivo == null ? null : nodoIdObjetivo.asText();
        Float objetivoX = nodoX == null || nodoX.isNull() ? null : (float) nodoX.asDouble();
        Float objetivoY = nodoY == null || nodoY.isNull() ? null : (float) nodoY.asDouble();

        if ((objetivoId == null || objetivoId.isBlank()) && (objetivoX == null || objetivoY == null)) {
            logger.warn("Payload de misil invalido");
            sendErrorMessage(session, "Payload de misil invalido");
            return;
        }

        if(!servicioMisil.puedeDisparar(idUnidad, idJugador, objetivoId, objetivoX, objetivoY)) {
            logger.warn("Disparo invalido: UnitId={}, objetivoId={}", idUnidad, objetivoId);
            sendErrorMessage(session, "Disparo invalido");
            return;
        }

        IMissileService.resultadoDisparoMisil resultado = servicioMisil.lanzarMisil(idUnidad, objetivoId, objetivoX, objetivoY);
        if (resultado == null){
            logger.warn("No se pudo lanzar misil: {}", idUnidad);
            sendErrorMessage(session, "No se pudo lanzar misil: " + idUnidad);
            return;
        }

        MisilLanzadoDTO misilLanzado = resultado.getMisilLanzado();

        //Actualizamos UI en tiempo real
        broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_DISPARADO, misilLanzado);

        logger.info("Misil disparado: unitId={}, misilId={}", idUnidad, misilLanzado.getMisilId());
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            sendResponse(session, CommunicationEvents.ServerToClientEvents.SERVER_ERROR, errorMessage);
            logger.debug("Error enviado: {}", errorMessage);
        } catch (IOException e) {
            logger.error("Error al enviar mensaje de error: {}", errorMessage, e);
        }
    }

    /**
     * Envia respuesta estandar al cliente
     * Formato: { type: "...", payload: {...} }
     *
     * @param session   sesion WebSocket
     * @param eventType Tipo de evento (usar CommunicationEvents)
     * @param payload   Datos a enviar
     */
    private void sendResponse(WebSocketSession session, String eventType, Object payload) throws IOException {
        sendToSession(session, eventType, payload);
    }

    public void broadcastToAll(String eventType, Object payload) {
        if (CommunicationEvents.ServerToClientEvents.GAME_ENDED.equals(eventType)) {
            gameEnded = true;
        }

        String jsonResponse = serializeResponse(eventType, payload);
        if (jsonResponse == null) {
            return;
        }

        connectedSessions.forEach(session -> {
            if (!session.isOpen()) {
                return;
            }

            sendTextSafely(session, jsonResponse);
        });
    }

    private void sendToSession(WebSocketSession session, String eventType, Object payload) {
        if (session == null || !session.isOpen()) {
            return;
        }

        String jsonResponse = serializeResponse(eventType, payload);
        if (jsonResponse == null) {
            return;
        }

        sendTextSafely(session, jsonResponse);
    }

    private String serializeResponse(String eventType, Object payload) {
        ServerResponseDTO response = new ServerResponseDTO(eventType, payload);

        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error al serializar el mensaje", e);
            return null;
        }
    }

    private void sendTextSafely(WebSocketSession session, String jsonResponse) {
        TextMessage message = new TextMessage(jsonResponse);

        synchronized (session) {
            try {
                session.sendMessage(message);
            } catch (IllegalStateException e) {
                logger.warn("Sesion en estado invalido al enviar mensaje: {}", session.getId(), e);
            } catch (IOException e) {
                logger.error("Error al enviar mensaje a la sesion: {}", session.getId(), e);
            }
        }
    }

    private String obtenerTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    // Si viene payload, usamos ese nodo. Si no, usamos el root.
    private JsonNode obtenerDatos(JsonNode root) {
        if (root == null) {
            return null;
        }

        JsonNode payload = root.get("payload");
        if (payload != null && !payload.isNull()) {
            return payload;
        }

        return root;
    }

    private void handleCreateNewGame(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode datos = obtenerDatos(root);
        String playerName = readString(datos, "playerName");
        String playerId = readString(datos, "playerId");

        MenuActionDTO result = mainMenuService.createNewGame(playerId, playerName);
        sendResponse(session, CommunicationEvents.ServerToClientEvents.GAME_CREATED, result);
    }

    private void handleJoinGame(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode datos = obtenerDatos(root);
        String gameId = readString(datos, "gameId");
        String playerId = readString(datos, "playerId");

        MenuActionDTO result = mainMenuService.joinGame(gameId, playerId);
        sendResponse(session, CommunicationEvents.ServerToClientEvents.GAME_JOINED, result);
    }

    private void handleLoadSavedGame(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode datos = obtenerDatos(root);
        String saveId = readString(datos, "saveId");
        try {
            logger.info("Recibida solicitud de LOAD_SAVED_GAME con codigo: {}", saveId);

            // Limpiamos registro de jugadores / sesiones actuales antes de cargar
            registeredPlayers.clear();
            sessionToPlayerId.clear();
            gameEnded = false;

            // Cancelamos cualquier tarea de desconexión pendiente
            pendingDisconnectTasks.values().forEach(task -> task.cancel(false));
            pendingDisconnectTasks.clear();

            persistenciaPartidaService.cargarPartidaPorCodigo(gameState, saveId);
            
            // Reiniciar el motor del juego si estaba detenido
            gameEngine.restartAfterLoad();
            
            Map<String, Object> data = Map.of(
                    "saveId", saveId,
                    "gameId", gameState.getGameId()
            );
            MenuActionDTO result = new MenuActionDTO(true, "Partida cargada", "GAME", data);
            
            // Primero respondemos al cliente que solicitó la carga
            sendResponse(session, CommunicationEvents.ServerToClientEvents.SAVED_GAME_LOADED, result);

            // Cerramos todas las OTRAS conexiones (excepto la que solicitó la carga)
            // para forzar una reconexión limpia
            List<WebSocketSession> sessionsToClose = new ArrayList<>(connectedSessions);
            for (WebSocketSession s : sessionsToClose) {
                if (!s.getId().equals(session.getId()) && s.isOpen()) {
                    try {
                        s.close(CloseStatus.NORMAL.withReason("Partida guardada cargada - reconectar"));
                        logger.info("Cerrando sesion {} debido a carga de partida guardada", s.getId());
                    } catch (IOException e) {
                        logger.warn("Error al cerrar sesion {}", s.getId(), e);
                    }
                }
            }

            // Broadcast de jugadores disponibles después de limpiar registros
            broadcastAvailablePlayers();

            // NO hacemos broadcast del snapshot aquí porque los jugadores aún no están registrados
            // El snapshot se enviará cuando los jugadores se registren y soliciten sus unidades
            logger.info("Partida cargada exitosamente. Esperando que los jugadores se registren.");
        } catch (Exception e) {
            logger.error("Error al cargar partida guardada: {}", e.getMessage(), e);
            MenuActionDTO result = new MenuActionDTO(false, e.getMessage(), "LOAD_GAME", null);
            sendResponse(session, CommunicationEvents.ServerToClientEvents.SAVED_GAME_LOADED, result);
        }
    }

    private void broadcastGameStateSnapshot() {
        List<UnitPositionDTO> unitPositions = gameState.getUnits().stream()
                .map(unit -> {
                    float combustible = 0f;
                    int municionDisponible = 0;
                    if (unit instanceof Drone dron) {
                        combustible = dron.getCombustible();
                        municionDisponible = dron.getAmmo();
                    }
                    if (unit instanceof DroneCarrier carrier) {
                        municionDisponible = carrier.getAvailableAmmoSupply();
                    }
                    return new UnitPositionDTO(unit.getId(), unit.getPosition(), combustible, municionDisponible);
                })
                .toList();

        broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_STATE_UPDATE, unitPositions);
    }

    private void handleGetRanking(WebSocketSession session) throws IOException {
        RankingResponseDTO ranking = mainMenuService.getRanking();
        sendResponse(session, CommunicationEvents.ServerToClientEvents.RANKING_RECEIVED, ranking);
    }

    private void handleExitGame(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode datos = obtenerDatos(root);
        String gameId = readString(datos, "gameId");
        String playerId = readString(datos, "playerId");

        MenuActionDTO result = mainMenuService.exitGame(gameId, playerId);

        // Limpieza de sesión para flujo de juego actual
        String registeredPlayerId = sessionToPlayerId.remove(session.getId());
        if (registeredPlayerId != null) {
            registeredPlayers.remove(registeredPlayerId);
        }

        sendResponse(session, CommunicationEvents.ServerToClientEvents.GAME_EXITED, result);
    }

    private String readString(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        String value = valueNode.asText();
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void handleSetGamePaused(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());
        if(playerId == null){
            sendErrorMessage(session, "PlayerId invalido");
            return;
        }

        JsonNode datos = obtenerDatos(root);
        JsonNode pausedNode = datos.get("paused");
        if (pausedNode == null){
            sendErrorMessage(session, "Pausa invalida");
            return;
        }
        boolean pausada = pausedNode.asBoolean();
        gameState.setPartidaPausada(pausada);
        Map<String, Object> payload = Map.of(
                "pausada", pausada,
                "updatedBy", playerId,
                "timestamp", System.currentTimeMillis()
        );
        broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_PAUSE_UPDATED, payload);
    }

    private void handleSaveGame(WebSocketSession session) throws IOException {
        try {
            String codigo = persistenciaPartidaService.guardarPartidaEnCurso(gameState);
            Map<String, Object> payload = Map.of(
                    "ok", true,
                    "codigoUnico", codigo,
                    "message", "Partida guardada"
            );
            sendResponse(session, CommunicationEvents.ServerToClientEvents.SAVE_GAME_RESULT, payload);
        } catch (Exception e) {
            Map<String, Object> payload = Map.of(
                    "ok", false,
                    "message", e.getMessage()
            );
            sendResponse(session, CommunicationEvents.ServerToClientEvents.SAVE_GAME_RESULT, payload);
        }
    }

    /**
     * Maneja la solicitud de reiniciar el juego a un estado limpio.
     * Limpia el GameState, recrea jugadores y unidades, y limpia registros de sesiones.
     */
    private void handleResetGame(WebSocketSession session) throws IOException {
        try {
            logger.info("Recibida solicitud de RESET_GAME desde la sesion {}", session.getId());

            // Limpiamos registro de jugadores / sesiones actuales
            registeredPlayers.clear();
            sessionToPlayerId.clear();

            // Reiniciamos el motor de juego a un estado nuevo
            gameEngine.resetAndCreate();

            // Enviamos a TODOS un snapshot del nuevo estado limpio
            List<UnitPositionDTO> unitPositions = gameState.getUnits().stream()
                    .map(unit -> {
                        float combustible = 0f;
                        int municionDisponible = 0;
                        if (unit instanceof Drone dron) {
                            combustible = dron.getCombustible();
                            municionDisponible = dron.getAmmo();
                        }
                        if (unit instanceof DroneCarrier carrier) {
                            municionDisponible = carrier.getAvailableAmmoSupply();
                        }
                        return new UnitPositionDTO(unit.getId(), unit.getPosition(), combustible, municionDisponible);
                    })
                    .toList();

            broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_STATE_UPDATE, unitPositions);
            logger.info("RESET_GAME completado y GAME_STATE_UPDATE enviado a {} sesiones", connectedSessions.size());
        } catch (Exception e) {
            logger.error("Error al procesar RESET_GAME", e);
            sendErrorMessage(session, "No se pudo reiniciar la partida: " + e.getMessage());
        }
    }

    private void handleSaveWinnerScore(WebSocketSession session, JsonNode root) throws IOException {
        try {
            JsonNode datos = obtenerDatos(root);
            String nickname = readString(datos, "nickname");
            JsonNode scoreNode = datos.get("score");
            JsonNode playerIdNode = datos.get("playerId");
            
            if (nickname == null || nickname.isBlank() || scoreNode == null) {
                sendErrorMessage(session, "Datos incompletos para guardar puntaje");
                return;
            }
            
            int score = scoreNode.asInt();
            String playerId = playerIdNode != null ? playerIdNode.asText() : "";
            
            // Guardar puntaje en la base de datos
            rankingService.saveWinnerScore(nickname, score, playerId, gameState.getGameId());
            
            logger.info("Puntaje guardado: nickname={}, score={}, playerId={}", nickname, score, playerId);
            
            // No enviar respuesta al cliente - el guardado es silencioso
        } catch (Exception e) {
            logger.error("Error al guardar puntaje del ganador", e);
            sendErrorMessage(session, "Error al guardar puntaje: " + e.getMessage());
        }
    }

    private void scheduleDisconnectForfeit(String disconnectedPlayerId) {
        cancelDisconnectTimeout(disconnectedPlayerId);

        ScheduledFuture<?> future = disconnectScheduler.schedule(() -> {
            try {
                if (gameEnded) {
                    return;
                }

                if (registeredPlayers.contains(disconnectedPlayerId)) {
                    logger.info("Jugador {} se reconecto dentro de la ventana de gracia", disconnectedPlayerId);
                    return;
                }

                String winnerId = resolveRemainingPlayer(disconnectedPlayerId);
                if (winnerId == null || winnerId.isBlank()) {
                    logger.warn("No hay jugador restante para declarar ganador tras desconexion de {}", disconnectedPlayerId);
                    return;
                }

                GameEndedDTO payload = new GameEndedDTO(winnerId, false, DISCONNECT_WIN_REASON);
                broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_ENDED, payload);
                logger.info("Victoria por desconexion: desconectado={}, ganador={}", disconnectedPlayerId, winnerId);
            } catch (Exception e) {
                logger.error("Error evaluando desconexion de jugador {}", disconnectedPlayerId, e);
            } finally {
                pendingDisconnectTasks.remove(disconnectedPlayerId);
            }
        }, RECONNECT_GRACE_SECONDS, TimeUnit.SECONDS);

        pendingDisconnectTasks.put(disconnectedPlayerId, future);
        logger.info("Timeout de reconexion iniciado ({}s) para jugador {}", RECONNECT_GRACE_SECONDS, disconnectedPlayerId);
    }

    private void cancelDisconnectTimeout(String playerId) {
        ScheduledFuture<?> pending = pendingDisconnectTasks.remove(playerId);
        if (pending != null) {
            pending.cancel(false);
            logger.info("Timeout de reconexion cancelado para jugador {}", playerId);
        }
    }

    private String resolveRemainingPlayer(String disconnectedPlayerId) {
        String connectedOpponent = registeredPlayers.stream()
                .filter(id -> !id.equals(disconnectedPlayerId))
                .findFirst()
                .orElse(null);

        if (connectedOpponent != null) {
            return connectedOpponent;
        }

        return gameState.getPlayers().stream()
                .map(Player::getId)
                .filter(id -> !id.equals(disconnectedPlayerId))
                .findFirst()
                .orElse(null);
    }
}