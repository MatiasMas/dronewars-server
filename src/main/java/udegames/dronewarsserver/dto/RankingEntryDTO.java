package udegames.dronewarsserver.dto;

import java.time.Instant;

public class RankingEntryDTO {
    private String nickname;
    private int score;
    private Instant timestamp;

    public RankingEntryDTO() {
    }

    public RankingEntryDTO(String nickname, int score, Instant timestamp) {
        this.nickname = nickname;
        this.score = score;
        this.timestamp = timestamp;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
