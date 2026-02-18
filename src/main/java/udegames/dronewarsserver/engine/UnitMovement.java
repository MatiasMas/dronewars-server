package udegames.dronewarsserver.engine;

import udegames.dronewarsserver.domain.model.Position;

public class UnitMovement {
    private final Position target;
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
