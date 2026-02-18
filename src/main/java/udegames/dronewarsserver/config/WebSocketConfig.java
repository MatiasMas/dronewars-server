package udegames.dronewarsserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.service.IMovementService;
import udegames.dronewarsserver.service.ISelectionService;
import udegames.dronewarsserver.websocket.GameWebSocketBroadcaster;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ISelectionService selectionService;
    private final GameState gameState;
    private final IMovementService movementService;
    private final GameWebSocketBroadcaster broadcaster;

    @Autowired
    public WebSocketConfig(ISelectionService selectionService, GameState gameState, IMovementService movementService, GameWebSocketBroadcaster broadcaster) {
        this.selectionService = selectionService;
        this.gameState = gameState;
        this.movementService = movementService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(new GameWebSocketHandler(selectionService, gameState, movementService, broadcaster), "/game")
                .setAllowedOrigins("*");
    }
}
