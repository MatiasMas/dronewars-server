package udegames.dronewarsserver.dto;

public class AmmoReloadedDTO {
    // Mantener estos nombres para que coincidan con el payload esperado por el cliente.
    private String unitId;
    private int ammo;
    private float combustible;

    public AmmoReloadedDTO(String unitId, int ammo, float combustible) {
        this.unitId = unitId;
        this.ammo = ammo;
        this.combustible = combustible;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = ammo;
    }

    public float getCombustible() {
        return combustible;
    }

    public void setCombustible(float combustible) {
        this.combustible = combustible;
    }
}
