package udegames.dronewarsserver.dto;

import udegames.dronewarsserver.domain.model.Position;

public class UnitPositionDTO {
    private String unitId;
    private Position position;

    public UnitPositionDTO(String unitId, Position position) {
        this.unitId = unitId;
        this.position = position;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
