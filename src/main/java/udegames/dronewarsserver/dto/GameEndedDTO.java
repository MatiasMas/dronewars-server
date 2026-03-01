package udegames.dronewarsserver.dto;

public class GameEndedDTO {
    private String winnerTeamId; // "player_1", "player_2" o null (empate)
    private boolean draw;
    private String reason; // ALL_UNITS_DESTROYED | CARRIER_DESTROYED_AND_NO_RESOURCES | CARRIER_DESTROYED_TIMEOUT_DRAW

    public GameEndedDTO(String winnerTeamId, boolean draw, String reason) {
        this.winnerTeamId = winnerTeamId;
        this.draw = draw;
        this.reason = reason;
    }

    public String getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(String winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public boolean isDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
