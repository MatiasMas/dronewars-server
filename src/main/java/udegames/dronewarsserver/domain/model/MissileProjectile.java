package udegames.dronewarsserver.domain.model;

// Misil simple controlado por el servidor.
public class MissileProjectile {
    private final String id;
    private final String attackerUnitId;
    private final String ownerId;
    private String targetUnitId;
    private final Position position;
    private final Position destinoFijo;
    private final float speed;
    private final float tiempoMaximo;
    private float tiempoActual;
    private final float dirX;
    private final float dirY;
    private final float distanciaMaxima;
    private float distanciaRecorrida;

    public MissileProjectile(String id, String attackerUnitId, String ownerId, String targetUnitId, Position position, Position destinoFijo, float speed, float tiempoMaximo, float distanciaMaxima) {
        this.id = id;
        this.attackerUnitId = attackerUnitId;
        this.ownerId = ownerId;
        this.targetUnitId = targetUnitId;
        this.position = position;
        this.destinoFijo = destinoFijo;
        this.speed = speed;
        this.tiempoMaximo = tiempoMaximo;
        this.tiempoActual = 0f;
        this.distanciaMaxima = distanciaMaxima;
        this.distanciaRecorrida = 0f;
        float dx = destinoFijo.getX() - position.getX();
        float dy = destinoFijo.getY() - position.getY();
        float distancia = (float) Math.sqrt((dx * dx) + (dy * dy));
        if (distancia <= 0.01f) {
            // Si el punto es igual al inicio, usamos una direccion simple.
            this.dirX = 1f;
            this.dirY = 0f;
        } else {
            this.dirX = dx / distancia;
            this.dirY = dy / distancia;
        }
    }

    public String getId() {
        return id;
    }

    public String getAttackerUnitId() {
        return attackerUnitId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTargetUnitId() {
        return targetUnitId;
    }

    public Position getPosition() {
        return position;
    }

    public Position getDestinoFijo() {
        return destinoFijo;
    }

    public void setTargetUnitId(String targetUnitId) {
        this.targetUnitId = targetUnitId;
    }

    // Actualiza la posicion del misil.
    public void update(Position objetivo, float deltaSeconds) {
        // Sumamos tiempo de vida.
        tiempoActual += deltaSeconds;

        // Siempre avanzamos en la direccion elegida.
        float paso = speed * deltaSeconds;
        position.setX(position.getX() + (dirX * paso));
        position.setY(position.getY() + (dirY * paso));
        distanciaRecorrida += paso;
    }

    public boolean estaExpirado() {
        return tiempoActual >= tiempoMaximo;
    }

    public boolean excedioDistancia() {
        return distanciaRecorrida >= distanciaMaxima;
    }
}
