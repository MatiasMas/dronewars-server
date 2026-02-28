package udegames.dronewarsserver.dto;

public class AvailablePlayerDTO {
    private String playerId;
    private String playerName;
    private boolean available;

    public AvailablePlayerDTO() {}

    public AvailablePlayerDTO(String playerId, String playerName, boolean available) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.available = available;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public boolean getAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
