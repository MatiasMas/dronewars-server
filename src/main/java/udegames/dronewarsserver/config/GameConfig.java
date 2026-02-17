package udegames.dronewarsserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import udegames.dronewarsserver.engine.GameState;

@Configuration
public class GameConfig {

    /*
     * This creates a unique game state instance through the entire execution
     * Bean means it will be managed by Spring's dependency injection system
     */
    @Bean
    public GameState gameState() {
        return new GameState("game-001");
    }
}
