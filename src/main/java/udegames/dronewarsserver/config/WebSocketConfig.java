package udegames.dronewarsserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import udegames.dronewarsserver.service.ISelectionService;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ISelectionService selectionService;

    @Autowired
    public WebSocketConfig(ISelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(new GameWebSocketHandler(selectionService), "/game")
                .setAllowedOrigins("*");
    }
}