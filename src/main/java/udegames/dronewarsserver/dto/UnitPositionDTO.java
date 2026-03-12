package udegames.dronewarsserver.dto;

import udegames.dronewarsserver.domain.entity.Position;

public class UnitPositionDTO {
    private String unitId;
    private Position position;
    private float combustible;
    private int municionDisponible;

    public UnitPositionDTO(String unitId, Position position, float combustible, int municionDisponible) {
        this.unitId = unitId;
        this.position = position;
        this.combustible = combustible;
        this.municionDisponible = municionDisponible;
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

    public int getMunicionDisponible() {
        return municionDisponible;
    }

    public void setMunicionDisponible(int municionDisponible) {
        this.municionDisponible = municionDisponible;
    }
}
