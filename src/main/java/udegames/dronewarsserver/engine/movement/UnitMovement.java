package udegames.dronewarsserver.engine.movement;

import udegames.dronewarsserver.domain.entity.Position;

public class UnitMovement {
    // Destino actual de la unidad (posicion objetivo).
    private final Position target;
    // Velocidad en unidades por segundo.
    private final float speedPerSecond;

    public UnitMovement(Position target, float speedPerSecond) {
        this.target = target;
        this.speedPerSecond = speedPerSecond;
    }

    public Position getTarget() {
        return target;
    }

    public float getSpeedPerSecond() {
        return speedPerSecond;
    }
}
