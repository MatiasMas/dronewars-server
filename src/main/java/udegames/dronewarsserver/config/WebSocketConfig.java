package udegames.dronewarsserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.service.ISelectionService;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ISelectionService selectionService;
    private final GameState gameState;

    @Autowired
    public WebSocketConfig(ISelectionService selectionService, GameState gameState) {
        this.selectionService = selectionService;
        this.gameState = gameState;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(new GameWebSocketHandler(selectionService, gameState), "/game")
                .setAllowedOrigins("*");
    }
}