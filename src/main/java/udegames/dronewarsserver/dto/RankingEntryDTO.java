package udegames.dronewarsserver.dto;

public class RankingEntryDTO {
    private String playerId;
    private String playerName;
    private int wins;
    private int losses;
    private int draws;
    private int points;

    public RankingEntryDTO(String playerId, String playerName, int wins, int losses, int draws, int points) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.points = points;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public int getPoints() {
        return points;
    }
}