package udegames.dronewarsserver.dto;

public class BombLaunchedDTO {
    // Mantener nombres para el payload del cliente.
    private String bombId;
    private String attackerUnitId;
    private float x;
    private float y;
    private float z;
    private int ammo;

    public BombLaunchedDTO(String bombId, String attackerUnitId, float x, float y, float z, int ammo) {
        this.bombId = bombId;
        this.attackerUnitId = attackerUnitId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ammo = ammo;
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

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = ammo;
    }
}
