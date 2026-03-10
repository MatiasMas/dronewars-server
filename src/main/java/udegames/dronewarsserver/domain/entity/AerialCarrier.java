package udegames.dronewarsserver.domain.entity;

import udegames.dronewarsserver.domain.enums.UnitType;

public class AerialCarrier extends DroneCarrier {
    private static final int INITIAL_BOMB_SUPPLY = 30;

    public AerialCarrier(int maxDronesCapacity, String ownerId, int health, Position position) {
        super(maxDronesCapacity, ownerId, health, position, UnitType.AERIAL_CARRIER, INITIAL_BOMB_SUPPLY);
    }
}
