package udegames.dronewarsserver.dto;

/**
 * ServerResponseDTO
 * Standard format for all server responses to the client
 * <p>
 * Example Structure:
 * {
 * "type": "EVENT_NAME",
 * "payload": { ...data... }
 * }
 */
public class ServerResponseDTO {
    private String type;
    private Object payload;

    public ServerResponseDTO(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}