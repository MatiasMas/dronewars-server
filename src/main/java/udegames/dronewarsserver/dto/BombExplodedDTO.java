package udegames.dronewarsserver.dto;

import java.util.List;

public class BombExplodedDTO {
    // Mantener nombres para el payload del cliente.
    private String bombId;
    private String attackerUnitId;
    private float x;
    private float y;
    private List<UnitSelectionDTO> impactedUnits;

    public BombExplodedDTO(String bombId, String attackerUnitId, float x, float y, List<UnitSelectionDTO> impactedUnits) {
        this.bombId = bombId;
        this.attackerUnitId = attackerUnitId;
        this.x = x;
        this.y = y;
        this.impactedUnits = impactedUnits;
    }

    public String getBombId() {
        return bombId;
    }

    public void setBombId(String bombId) {
        this.bombId = bombId;
    }

    public String getAttackerUnitId() {
        return attackerUnitId;
    }

    public void setAttackerUnitId(String attackerUnitId) {
        this.attackerUnitId = attackerUnitId;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public List<UnitSelectionDTO> getImpactedUnits() {
        return impactedUnits;
    }

    public void setImpactedUnits(List<UnitSelectionDTO> impactedUnits) {
        this.impactedUnits = impactedUnits;
    }
}
