package udegames.dronewarsserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import udegames.dronewarsserver.engine.GameEngine;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

@Configuration
public class GameConfig {
    private static final Logger logger = LoggerFactory.getLogger(GameConfig.class);

    /*
     * This creates a unique game state instance through the entire execution
     * Bean means it will be managed by Spring's dependency injection system
     */
    @Bean
    public GameState gameState() {
        return new GameState("game-001");
    }

    /*
     * This creates a unique game engine instance through the entire execution
     * Bean means it will be managed by Spring's dependency injection system
     */
    @Bean
    public GameEngine gameEngine(GameState gameState, GameWebSocketHandler gameWebSocketHandler) {
        GameEngine gameEngine = new GameEngine(gameState, gameWebSocketHandler);

        // Creates players and units in the game state
        gameEngine.create();

        // Starts the game engine in a separate thread and waits a little bit for the Spring server to start first
        Thread engineStarterThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                logger.info("Starting game engine...");
                gameEngine.start();
                logger.info("Game engine started");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error starting game engine: {}", e.getMessage());
            }
        }, "GameEngine-Starter");

        engineStarterThread.start();

        return gameEngine;
    }
}
