package udegames.dronewarsserver.dto;

import java.util.List;

public class RankingResponseDTO {
    private String generatedAt;
    private List<RankingEntryDTO> entries;

    public RankingResponseDTO(String generatedAt, List<RankingEntryDTO> entries) {
        this.generatedAt = generatedAt;
        this.entries = entries;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public List<RankingEntryDTO> getEntries() {
        return entries;
    }
}