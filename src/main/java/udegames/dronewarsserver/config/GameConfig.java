package udegames.dronewarsserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import udegames.dronewarsserver.engine.GameEngine;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.service.GameStateSyncService;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

@Configuration
public class GameConfig {
    private static final Logger logger = LoggerFactory.getLogger(GameConfig.class);

    /*
     * Crea una instancia unica del estado del juego durante toda la ejecucion.
     * Bean significa que sera gestionado por el sistema de inyeccion de dependencias de Spring.
     */
    @Bean
    public GameState gameState() {
        return new GameState("game-001");
    }

    /*
     * Crea una instancia unica del motor del juego durante toda la ejecucion.
     * Bean significa que sera gestionado por el sistema de inyeccion de dependencias de Spring.
     */
    @Bean
    public GameEngine gameEngine(GameState gameState, GameStateSyncService gameStateSyncService, GameWebSocketHandler gameWebSocketHandler) {
        GameEngine gameEngine = new GameEngine(gameState, gameStateSyncService, gameWebSocketHandler);

        // Crea jugadores y unidades en el estado del juego
        gameEngine.create();

        // Inicia el motor del juego en un hilo separado y espera a que el servidor de Spring arranque
        Thread engineStarterThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                logger.info("Iniciando motor del juego...");
                gameEngine.start();
                logger.info("Motor del juego iniciado");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error al iniciar el motor del juego: {}", e.getMessage());
            }
        }, "GameEngine-Starter");

        engineStarterThread.start();

        return gameEngine;
    }
}

