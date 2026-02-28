package udegames.dronewarsserver.dto;

public class MisilLanzadoDTO {
    private String misilId;
    private String unidadAtaqueId;
    private String unidadDefensaId;
    private float x;
    private float y;
    private float z;
    private float targetX;
    private float targetY;
    private float targetZ;
    private int municion;

    public MisilLanzadoDTO(
            String misilId,
            String unidadAtaqueId,
            String unidadDefensaId,
            float x,
            float y,
            float z,
            float targetX,
            float targetY,
            float targetZ,
            int municion
    ) {
        this.misilId = misilId;
        this.unidadAtaqueId = unidadAtaqueId;
        this.unidadDefensaId = unidadDefensaId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.municion = municion;
    }
    public String getMisilId(){return misilId;}

    public String getUnidadAtaqueId(){return unidadAtaqueId;}

    public String getUnidadDefensaId(){return unidadDefensaId;}

    public float getX(){return x;}
    public float getY(){return y;}
    public float getZ(){return z;}

    public float getTargetX(){return targetX;}
    public float getTargetY(){return targetY;}
    public float getTargetZ(){return targetZ;}

    public int getMunicion(){return municion;}

    public void setMisilId(String misilId){this.misilId = misilId;}
    public void setUnidadAtaqueId(String unidadAtaqueId){this.unidadAtaqueId = unidadAtaqueId;}
    public void setUnidadDefensaID(String unidadDefensaId){this.unidadDefensaId = unidadDefensaId;}
    public void setX(float x){this.x = x;}
    public void setY(float y){this.y = y;}
    public void setZ(float z){this.z = z;}
    public void setTargetX(float targetX){this.targetX = targetX;}
    public void setTargetY(float targetY){this.targetY = targetY;}
    public void setTargetZ(float targetZ){this.targetZ = targetZ;}
    public void setearMunicion(int municion){this.municion = municion;}
}
