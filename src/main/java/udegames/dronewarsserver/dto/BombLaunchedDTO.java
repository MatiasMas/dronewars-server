package udegames.dronewarsserver.dto;

public class BombLaunchedDTO {
    private String bombId;
    private String attackerUnitId;
    private float x;
    private float y;
    private float z;

    public BombLaunchedDTO(String bombId, String attackerUnitId, float x, float y, float z) {
        this.bombId = bombId;
        this.attackerUnitId = attackerUnitId;
        this.x = x;
        this.y = y;
        this.z = z;
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

    public float getZ() {
        return z;
    }
}
