package udegames.dronewarsserver.engine;

import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.engine.movement.UnitMovement;

public class UnitMovementSystem {
    private final GameState gameState;
    private final float positionEpsilon;
    private final float tickIntervalSeconds;
    private static final float CONSUMO_COMBUSTIBLE_POR_UNIDAD = 0.01f;
    private static final float ALTURA_FORZADA_COMBUSTIBLE = 0f;

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

            if(unit instanceof Drone drone){
                float movX = newPosition.getX() - current.getX();
                float movY = newPosition.getY() - current.getY();
                float movZ = newPosition.getZ() - current.getZ();
                float distancia = (float) Math.sqrt(movX * movX + movY * movY + movZ * movZ);
                // A mayor altura, menor consumo extra por altura.
                float consumoPorAltura = CONSUMO_COMBUSTIBLE_POR_UNIDAD + (1 * newPosition.getZ());
                boolean quedaCombustible = drone.consumirCombustible((int) (distancia + consumoPorAltura));

                if(!quedaCombustible){
                    drone.inhabilitarPorCombustible();
                    gameState.clearUnitMovement(unitId);

                    // Forzamos el descenso cuando se queda sin combustible.
                    Position posicionActual = unit.getPosition();
                    Position destinoForzado = new Position(posicionActual.getX(), posicionActual.getY(), ALTURA_FORZADA_COMBUSTIBLE);

                    if (Math.abs(posicionActual.getZ() - ALTURA_FORZADA_COMBUSTIBLE) > positionEpsilon) {
                        gameState.setUnitMovement(unitId, destinoForzado, 20f);
                    } else {
                        unit.setPosition(destinoForzado);
                    }
                }
            }
        }

        return moved;
    }
}
