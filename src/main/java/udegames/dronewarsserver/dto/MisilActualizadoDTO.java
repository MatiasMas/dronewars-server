package udegames.dronewarsserver.dto;

public class MisilActualizadoDTO {
    private String misilId;
    private float x;
    private float y;
    private float z;

    public MisilActualizadoDTO(String misilId, float x, float y, float z) {
        this.misilId = misilId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getMisilId() {return misilId;}
    public float getX() {return x;}
    public float getY() {return y;}
    public float getZ() {return z;}

    public void setMisilId(String misilId) {this.misilId = misilId;}
    public void setX(float x) {this.x = x;}
    public void setY(float y) {this.y = y;}
    public void setZ(float z) {this.z = z;}
}
