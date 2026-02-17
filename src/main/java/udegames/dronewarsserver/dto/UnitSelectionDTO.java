package udegames.dronewarsserver.dto;

public class UnitSelectionDTO {
    private String unitId;
    private String type;
    private float x;
    private float y;
    private float z;
    private int health;

    public UnitSelectionDTO(String unitId, String type, float x, float y, float z, int health) {
        this.unitId = unitId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.health = health;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }
}
