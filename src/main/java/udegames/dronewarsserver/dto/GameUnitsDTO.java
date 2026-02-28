package udegames.dronewarsserver.dto;

import java.util.List;

public class GameUnitsDTO {
    private List<UnitSelectionDTO> playerUnits;
    private List<UnitSelectionDTO> enemyUnits;

    public GameUnitsDTO() {}

    public GameUnitsDTO(List<UnitSelectionDTO> playerUnits, List<UnitSelectionDTO> enemyUnits) {
        this.playerUnits = playerUnits;
        this.enemyUnits = enemyUnits;
    }

    public List<UnitSelectionDTO> getPlayerUnits() {
        return playerUnits;
    }

    public void setPlayerUnits(List<UnitSelectionDTO> playerUnits) {
        this.playerUnits = playerUnits;
    }

    public List<UnitSelectionDTO> getEnemyUnits() {
        return enemyUnits;
    }

    public void setEnemyUnits(List<UnitSelectionDTO> enemyUnits) {
        this.enemyUnits = enemyUnits;
    }
}
