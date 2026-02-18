package udegames.dronewarsserver.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import udegames.dronewarsserver.domain.model.AerialCarrier;
import udegames.dronewarsserver.domain.model.AerialDrone;
import udegames.dronewarsserver.domain.model.Player;
import udegames.dronewarsserver.domain.model.Position;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameEngine {
    private final GameState gameState;
    private final ScheduledExecutorService executor;
    private volatile boolean running;
    private long currentTick;

    private static final long TICK_INTERVAL_MS = 50;
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);

    public GameEngine(GameState gameState) {
        this.gameState = gameState;

        this.executor = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "GameEngine-Tick");
            thread.setDaemon(true);
            return thread;
        });

        this.running = false;
        this.currentTick = 0;
    }

    /*
     * Starts game, creates players and units
     */
    public void create() {
        logger.info("[CREATE] Starting game: {}", gameState.getGameId());

        createPlayers();
        createUnits();

        logger.info("[CREATE] Game started");
        logger.info("Players: {}", gameState.getPlayers().size());
    }

    /*
     * Starts game ticks, this simulates the frames, set at TICK_INTERVAL_MS
     * It calls the update method which is in charge of controlling the realtime events
     */
    public void start() {
        if (running) {
            logger.warn("[START] GameEngine already running");
            return;
        }

        running = true;
        logger.info("[START] Starting game engine ticks ({}ms)", TICK_INTERVAL_MS);

        executor.scheduleAtFixedRate(
                this::update,
                0,
                TICK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /*
     * It gets executed multiple times per second
     * In charge of checking for collisions, updating units positions, etc.
     */
    private void update() {
        if (!running) {
            logger.warn("[UPDATE] GameEngine not running");
            return;
        }

        currentTick++;

//        logger.debug("[UPDATE] Tick: {}", currentTick);

        // Put here anything related to checking for collisions or updating units positions, fuel, etc.
    }

    /*
     * Stops the game engine, stopping the ticks and shutting down the executor
     */
    private void stop() {
        running = false;
        executor.shutdown();
        logger.info("[STOP] GameEngine stopped");
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

    // --------------- Creating entities for the game ---------------
    private void createPlayers() {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");

        player1.setId("player_1");
        player2.setId("player_2");

        gameState.addPlayer(player1);
        gameState.addPlayer(player2);

        logger.debug("2 players created: {}", gameState.getPlayers());
        logger.debug("Player 1: {}", player1.getId());
        logger.debug("Player 2: {}", player2.getId());
    }

    private void createUnits() {
        List<Player> players = gameState.getPlayers();

        if (players.size() < 2) {
            logger.error("There must be at least 2 players to start a game.");
            return;
        }

        Player player1 = players.get(0);
        Player player2 = players.get(1);

        // Hardcoded for now to have 2 drones + 1 carrier
        createPlayerUnits(player1, "carrier-p1", 10f, 10f);
        createPlayerUnits(player2, "carrier-p2", 100f, 100f);

        logger.debug("Units created for players: {}", gameState.getPlayers());
    }

    private void createPlayerUnits(Player player, String carrierId, float startingX, float startingY) {
        // Aerial Drones
        Position aerialDrone1Position = new Position(startingX, startingY, 5f);
        AerialDrone aerialDrone1 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone1Position);

        gameState.addUnit(aerialDrone1);
        logger.debug("AerialDrone created: {} in ({}, {}, {})", aerialDrone1.getId(), aerialDrone1Position.getX(), aerialDrone1Position.getY(), aerialDrone1Position.getZ());

        Position aerialDrone2Position = new Position(startingX + 5f, startingY + 5f, 5f);
        AerialDrone aerialDrone2 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone2Position);

        gameState.addUnit(aerialDrone2);
        logger.debug("AerialDrone created: {} in ({}, {}, {})", aerialDrone2.getId(), aerialDrone2Position.getX(), aerialDrone2Position.getY(), aerialDrone2Position.getZ());

        Position aerialCarrierPosition = new Position(startingX - 5f, startingY - 5f, 5f);
        AerialCarrier aerialCarrier = new AerialCarrier(12, player.getId(), 6, aerialCarrierPosition);

        gameState.addUnit(aerialCarrier);
        logger.debug("AerialCarrier created: {} in ({}, {}, {})", aerialCarrier.getId(), aerialCarrierPosition.getX(), aerialCarrierPosition.getY(), aerialCarrierPosition.getZ());
    }
}
