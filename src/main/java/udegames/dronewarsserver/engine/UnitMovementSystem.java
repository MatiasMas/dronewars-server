package udegames.dronewarsserver.engine;

import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.engine.movement.UnitMovement;

public class UnitMovementSystem {
    private final GameState gameState;
    private final float positionEpsilon;
    private final float tickIntervalSeconds;

    public UnitMovementSystem(GameState gameState, long tickIntervalMs, float positionEpsilon) {
        this.gameState = gameState;
        this.positionEpsilon = positionEpsilon;
        this.tickIntervalSeconds = tickIntervalMs / 1000f;
    }

    public boolean applyMovements() {
        boolean moved = false;

        for (var entry : gameState.getUnitMovements().entrySet()) {
            String unitId = entry.getKey();
            UnitMovement movement = entry.getValue();
            Unit unit = gameState.getUnitById(unitId);

            if (unit == null) {
                gameState.clearUnitMovement(unitId);
                continue;
            }

            Position current = unit.getPosition();
            Position target = movement.getTarget();

            // Vector direccion hacia el objetivo y distancia actual al destino.
            float dx = target.getX() - current.getX();
            float dy = target.getY() - current.getY();
            float dz = target.getZ() - current.getZ();
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance <= positionEpsilon) {
                unit.setPosition(target);
                gameState.clearUnitMovement(unitId);
                moved = true;
                continue;
            }

            // Paso maximo en este tick segun la velocidad (unidades/segundo).
            float maxStep = movement.getSpeedPerSecond() * tickIntervalSeconds;
            if (maxStep <= 0f) {
                unit.setPosition(target);
                gameState.clearUnitMovement(unitId);
                moved = true;
                continue;
            }

            if (distance <= maxStep) {
                unit.setPosition(target);
                gameState.clearUnitMovement(unitId);
                moved = true;
                continue;
            }

            // Proporcion del desplazamiento sobre el vector direccion.
            float ratio = maxStep / distance;
            Position newPosition = new Position(
                    current.getX() + dx * ratio,
                    current.getY() + dy * ratio,
                    current.getZ() + dz * ratio
            );
            unit.setPosition(newPosition);
            moved = true;
        }

        return moved;
    }
}
