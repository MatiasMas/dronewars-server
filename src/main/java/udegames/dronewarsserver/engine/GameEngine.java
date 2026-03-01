package udegames.dronewarsserver.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import udegames.dronewarsserver.domain.model.AerialCarrier;
import udegames.dronewarsserver.domain.model.AerialDrone;
import udegames.dronewarsserver.domain.model.BombProjectile;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.DroneCarrier;
import udegames.dronewarsserver.domain.model.MissileProjectile;
import udegames.dronewarsserver.domain.model.Player;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.service.GameStateSyncService;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.BombExplodedDTO;
import udegames.dronewarsserver.dto.MisilActualizadoDTO;
import udegames.dronewarsserver.dto.MisilImpactoDTO;
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.websocket.CommunicationEvents;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameEngine {
    private final GameState gameState;
    private final GameStateSyncService gameStateSyncService;
    private final UnitMovementSystem movementSystem;
    private final GameWebSocketHandler gameWebSocketHandler;
    private final ScheduledExecutorService executor;
    private volatile boolean running;
    private long currentTick;

    private static final long TICK_INTERVAL_MS = 50;
    // Umbral para considerar que la unidad ya llego al destino.
    private static final float POSITION_EPSILON = 0.01f;
    private static final float DELTA_TIME_SECONDS = TICK_INTERVAL_MS / 1000f;
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);
    // Reglas de municion:
    // Jugador 1 usa bombas (max 1), jugador 2 usa misiles (max 2).
    private static final String ID_JUGADOR_1 = "player_1";
    private static final String ID_JUGADOR_2 = "player_2";
    private static final int MUNICION_MAX_JUGADOR_1 = 1;
    private static final int MUNICION_MAX_JUGADOR_2 = 2;
    private static final int DANO_MISIL = 1;
    private static final float RANGO_EXPLOSION_MISIL = 8f;
    private static final float MIN_X = 0f;
    private static final float MAX_X = 200f;
    private static final float MIN_Y = 0f;
    private static final float MAX_Y = 200f;

    public GameEngine(GameState gameState, GameStateSyncService gameStateSyncService, GameWebSocketHandler gameWebSocketHandler) {
        this.gameState = gameState;
        this.gameStateSyncService = gameStateSyncService;
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.movementSystem = new UnitMovementSystem(gameState, TICK_INTERVAL_MS, POSITION_EPSILON);

        this.executor = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "GameEngine-Tick");
            thread.setDaemon(true);
            return thread;
        });

        this.running = false;
        this.currentTick = 0;
    }

    /*
     * Inicia el juego, crea jugadores y unidades
     */
    public void create() {
        logger.info("[CREATE] Iniciando juego: {}", gameState.getGameId());

        createPlayers();
        createUnits();

        logger.info("[CREATE] Juego iniciado");
        logger.info("Jugadores: {}", gameState.getPlayers().size());
    }

    /*
     * Inicia los ticks del juego, simula los frames, definidos en TICK_INTERVAL_MS.
     * Llama al metodo update que se encarga de los eventos en tiempo real.
     */
    public void start() {
        if (running) {
            logger.warn("[START] GameEngine ya esta en ejecucion");
            return;
        }

        running = true;
        logger.info("[START] Iniciando ticks del motor del juego ({}ms)", TICK_INTERVAL_MS);

        executor.scheduleAtFixedRate(
                this::update,
                0,
                TICK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /*
     * Se ejecuta multiples veces por segundo.
     * Se encarga de verificar colisiones, actualizar posiciones, etc.
     */
    private void update() {
        if (!running) {
            logger.warn("[UPDATE] GameEngine no esta en ejecucion");
            return;
        }

        currentTick++;

//        logger.debug("[UPDATE] Tic: {}", currentTick);

        // Colocar aqui todo lo que sea relacionado con colisiones, posiciones, combustible, etc.
        updateBombProjectiles();
        updateMissileProjectiles();

        boolean moved = movementSystem.applyMovements();
        if (moved) {
            gameStateSyncService.broadcastGameState();
        }
    }

    /*
     * Detiene el motor del juego, detiene los ticks y apaga el executor
     */
    private void stop() {
        running = false;
        executor.shutdown();
        logger.info("[STOP] GameEngine detenido");
    }

    public GameState getGameState() {
        return gameState;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public boolean isRunning() {
        return running;
    }

    private void updateBombProjectiles() {
        List<BombProjectile> activeBombs = gameState.getBombProjectiles();
        if (activeBombs.isEmpty()) {
            return;
        }

        for (BombProjectile bomb : activeBombs) {
            bomb.update(DELTA_TIME_SECONDS);

            if (bomb.hasReachedGround()) {
                resolveBombExplosion(bomb);
                gameState.removeBombProjectile(bomb.getId());
            }
        }
    }

    private void resolveBombExplosion(BombProjectile bomb) {
        List<UnitSelectionDTO> impactedUnits = new ArrayList<>();

        for (Unit enemyUnit : gameState.getUnits()) {
            if (enemyUnit.isDestroyed() || enemyUnit.getOwnerId().equals(bomb.getOwnerId())) {
                continue;
            }

            float dx = enemyUnit.getPosition().getX() - bomb.getPosition().getX();
            float dy = enemyUnit.getPosition().getY() - bomb.getPosition().getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= bomb.getBlastRadius()) {
                enemyUnit.applyDamage(bomb.getDamage());
                impactedUnits.add(UnitMapper.toSelectionDTO(enemyUnit));
            }
        }

        BombExplodedDTO payload = new BombExplodedDTO(
                bomb.getId(),
                bomb.getAttackerUnitId(),
                bomb.getPosition().getX(),
                bomb.getPosition().getY(),
                impactedUnits
        );

        gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.BOMB_EXPLODED, payload);
        logger.info("Bomb {} exploded at ({}, {}), impacted {} enemy units",
                bomb.getId(),
                bomb.getPosition().getX(),
                bomb.getPosition().getY(),
                impactedUnits.size());
    }

    private void updateMissileProjectiles() {
        List<MissileProjectile> misiles = gameState.getMissileProjectiles();
        if (misiles.isEmpty()) {
            return;
        }

        for (MissileProjectile misil : misiles) {
            // Mover el misil
            misil.update(misil.getDestinoFijo(), DELTA_TIME_SECONDS);

            // Si sale del mapa, lo eliminamos
            if (estaFueraDeMapa(misil.getPosition())) {
                MisilImpactoDTO impactoVacio = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        new ArrayList<>()
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impactoVacio);
                gameState.removeMissileProjectile(misil.getId());
                continue;
            }

            // Enviar posicion del misil al cliente
            MisilActualizadoDTO actualizado = new MisilActualizadoDTO(
                    misil.getId(),
                    misil.getPosition().getX(),
                    misil.getPosition().getY(),
                    misil.getPosition().getZ()
            );
            gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_ACTUALIZADO, actualizado);

            // Vemos si paso cerca de un dron enemigo
            float rangoCuadrado = RANGO_EXPLOSION_MISIL * RANGO_EXPLOSION_MISIL;
            List<UnitSelectionDTO> impactadas = new ArrayList<>();

            for (Unit unidad : gameState.getUnits()) {
                if (unidad.isDestroyed()) {
                    continue;
                }

                if (unidad.getOwnerId().equals(misil.getOwnerId())) {
                    continue;
                }

                boolean esDron = unidad instanceof Drone;
                boolean esPortadrones = unidad instanceof DroneCarrier;
                if (!esDron && !esPortadrones) {
                    continue;
                }

                float dx = unidad.getPosition().getX() - misil.getPosition().getX();
                float dy = unidad.getPosition().getY() - misil.getPosition().getY();
                float distancia = (dx * dx) + (dy * dy);

                if (distancia <= rangoCuadrado) {
                    if (esDron) {
                        unidad.applyDamage(unidad.getHealth());
                    } else {
                        unidad.applyDamage(1);
                    }
                    impactadas.add(UnitMapper.toSelectionDTO(unidad));

                    if (unidad.isDestroyed()) {
                        gameState.removeUnit(unidad.getId());
                    }
                }
            }

            if (!impactadas.isEmpty()) {
                MisilImpactoDTO impacto = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        impactadas
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impacto);
                gameState.removeMissileProjectile(misil.getId());
                continue;
            }

            // Controlamos tiempo de misil
            if (misil.estaExpirado()) {
                MisilImpactoDTO impactoVacio = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        new ArrayList<>()
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impactoVacio);
                gameState.removeMissileProjectile(misil.getId());
            }
        }
    }

    private boolean estaFueraDeMapa(Position posicion) {
        if (posicion == null) {
            return true;
        }

        return posicion.getX() < MIN_X
                || posicion.getX() > MAX_X
                || posicion.getY() < MIN_Y
                || posicion.getY() > MAX_Y;
    }

    // --------------- Creacion de entidades para el juego ---------------
    private void createPlayers() {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");

        player1.setId("player_1");
        player2.setId("player_2");

        gameState.addPlayer(player1);
        gameState.addPlayer(player2);

        logger.debug("2 jugadores creados: {}", gameState.getPlayers());
        logger.debug("Jugador 1: {}", player1.getId());
        logger.debug("Jugador 2: {}", player2.getId());
    }

    private void createUnits() {
        List<Player> players = gameState.getPlayers();

        if (players.size() < 2) {
            logger.error("Debe haber al menos 2 jugadores para iniciar un juego.");
            return;
        }

        Player player1 = players.get(0);
        Player player2 = players.get(1);

        // Hardcodeado por ahora para tener 2 drones + 1 portadrones
        createPlayerUnits(player1, 10f, 10f);
        createPlayerUnits(player2, 100f, 100f);

        logger.debug("Unidades creadas para jugadores: {}", gameState.getPlayers());
    }

    private void createPlayerUnits(Player jugador, float inicioX, float inicioY) {
        int municionMaxima = obtenerMaxMunicionParaJugador(jugador);
        // Portadrones primero, asi los drones conocen el id real.
        Position posicionPortadronesAereo = new Position(inicioX - 5f, inicioY - 5f, 5f);
        AerialCarrier portadronesAereo = new AerialCarrier(12, jugador.getId(), 6, posicionPortadronesAereo);

        gameState.addUnit(portadronesAereo);
        logger.debug("AerialCarrier creado: {} en ({}, {}, {})", portadronesAereo.getId(), posicionPortadronesAereo.getX(), posicionPortadronesAereo.getY(), posicionPortadronesAereo.getZ());

        String idPortadronesReal = portadronesAereo.getId();
        // Drones aereos
        Position posicionDronAereo1 = new Position(inicioX, inicioY, 5f);
        AerialDrone dronAereo1 = new AerialDrone(idPortadronesReal, 100f, municionMaxima, jugador.getId(), 1, posicionDronAereo1);

        gameState.addUnit(dronAereo1);
        logger.debug("AerialDrone creado: {} en ({}, {}, {})", dronAereo1.getId(), posicionDronAereo1.getX(), posicionDronAereo1.getY(), posicionDronAereo1.getZ());

        Position posicionDronAereo2 = new Position(inicioX + 5f, inicioY + 5f, 5f);
        AerialDrone dronAereo2 = new AerialDrone(idPortadronesReal, 100f, municionMaxima, jugador.getId(), 1, posicionDronAereo2);

        gameState.addUnit(dronAereo2);
        logger.debug("AerialDrone creado: {} en ({}, {}, {})", dronAereo2.getId(), posicionDronAereo2.getX(), posicionDronAereo2.getY(), posicionDronAereo2.getZ());
    }

    private int obtenerMaxMunicionParaJugador(Player jugador) {
        if (jugador == null) {
            return MUNICION_MAX_JUGADOR_1;
        }

        if (ID_JUGADOR_1.equals(jugador.getId())) {
            return MUNICION_MAX_JUGADOR_1;
        }

        if (ID_JUGADOR_2.equals(jugador.getId())) {
            return MUNICION_MAX_JUGADOR_2;
        }

        return MUNICION_MAX_JUGADOR_1;
    }
}
