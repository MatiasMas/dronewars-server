package udegames.dronewarsserver.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;
import udegames.dronewarsserver.dto.ServerResponseDTO;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameWebSocketBroadcaster {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketBroadcaster.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();

    public void registerSession(WebSocketSession session) {
        connectedSessions.add(session);
    }

    public void unregisterSession(WebSocketSession session) {
        connectedSessions.remove(session);
    }

    public void send(WebSocketSession session, String eventType, Object payload) {
        if (session == null || !session.isOpen()) {
            return;
        }

        ServerResponseDTO response = new ServerResponseDTO(eventType, payload);
        String jsonResponse;

        try {
            jsonResponse = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error al serializar el mensaje", e);
            return;
        }

        sendTextSafely(session, jsonResponse);
    }

    public void broadcast(String eventType, Object payload) {
        ServerResponseDTO response = new ServerResponseDTO(eventType, payload);
        String jsonResponse;

        try {
            jsonResponse = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error al serializar el mensaje de broadcast", e);
            return;
        }

        connectedSessions.forEach(connectedSession -> {
            if (!connectedSession.isOpen()) {
                return;
            }

            sendTextSafely(connectedSession, jsonResponse);
        });
    }

    private void sendTextSafely(WebSocketSession session, String jsonResponse) {
        TextMessage message = new TextMessage(jsonResponse);

        synchronized (session) {
            try {
                session.sendMessage(message);
            } catch (IllegalStateException e) {
                logger.warn("Sesión en estado inválido al enviar mensaje: {}", session.getId(), e);
            } catch (IOException e) {
                logger.error("Error al enviar mensaje a la sesión: {}", session.getId(), e);
            }
        }
    }
}
