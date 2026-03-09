package udegames.dronewarsserver.dto;

import udegames.dronewarsserver.domain.entity.Position;

public class UnitPositionDTO {
    private String unitId;
    private Position position;
    private float combustible;

    public UnitPositionDTO(String unitId, Position position, float combustible) {
        this.unitId = unitId;
        this.position = position;
        this.combustible = combustible;
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

    public float getCombustible() {
        return combustible;
    }

    public void setCombustible(float combustible) {
        this.combustible = combustible;
    }
}
