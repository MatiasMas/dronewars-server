package udegames.dronewarsserver.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import udegames.dronewarsserver.domain.model.AerialCarrier;
import udegames.dronewarsserver.domain.model.AerialDrone;
import udegames.dronewarsserver.domain.model.Player;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.service.GameStateSyncService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameEngine {
    private final GameState gameState;
    private final GameStateSyncService gameStateSyncService;
    private final UnitMovementSystem movementSystem;
    private final ScheduledExecutorService executor;
    private volatile boolean running;
    private long currentTick;

    private static final long TICK_INTERVAL_MS = 50;
    // Umbral para considerar que la unidad ya llego al destino.
    private static final float POSITION_EPSILON = 0.01f;
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);

    public GameEngine(GameState gameState, GameStateSyncService gameStateSyncService) {
        this.gameState = gameState;
        this.gameStateSyncService = gameStateSyncService;
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

        // Colocar aqui todo lo relacionado con colisiones, posiciones, combustible, etc.
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
        createPlayerUnits(player1, "carrier-p1", 10f, 10f);
        createPlayerUnits(player2, "carrier-p2", 100f, 100f);

        logger.debug("Unidades creadas para jugadores: {}", gameState.getPlayers());
    }

    private void createPlayerUnits(Player player, String carrierId, float startingX, float startingY) {
        // Drones aereos
        Position aerialDrone1Position = new Position(startingX, startingY, 5f);
        AerialDrone aerialDrone1 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone1Position);

        gameState.addUnit(aerialDrone1);
        logger.debug("AerialDrone creado: {} en ({}, {}, {})", aerialDrone1.getId(), aerialDrone1Position.getX(), aerialDrone1Position.getY(), aerialDrone1Position.getZ());

        Position aerialDrone2Position = new Position(startingX + 5f, startingY + 5f, 5f);
        AerialDrone aerialDrone2 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone2Position);

        gameState.addUnit(aerialDrone2);
        logger.debug("AerialDrone creado: {} en ({}, {}, {})", aerialDrone2.getId(), aerialDrone2Position.getX(), aerialDrone2Position.getY(), aerialDrone2Position.getZ());

        Position aerialCarrierPosition = new Position(startingX - 5f, startingY - 5f, 5f);
        AerialCarrier aerialCarrier = new AerialCarrier(12, player.getId(), 6, aerialCarrierPosition);

        gameState.addUnit(aerialCarrier);
        logger.debug("AerialCarrier creado: {} en ({}, {}, {})", aerialCarrier.getId(), aerialCarrierPosition.getX(), aerialCarrierPosition.getY(), aerialCarrierPosition.getZ());
    }
}

