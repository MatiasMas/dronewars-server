package udegames.dronewarsserver.dto;

import java.util.List;

public class BombExplodedDTO {
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

    public String getAttackerUnitId() {
        return attackerUnitId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public List<UnitSelectionDTO> getImpactedUnits() {
        return impactedUnits;
    }
}
