package udegames.dronewarsserver.domain.entity;

import udegames.dronewarsserver.domain.enums.UnitType;

public class NavalCarrier extends DroneCarrier {
    private static final int INITIAL_MISSILE_SUPPLY = 60;

    public NavalCarrier(int maxDronesCapacity, String ownerId, int health, Position position) {
        super(maxDronesCapacity, ownerId, health, position, UnitType.NAVAL_CARRIER, INITIAL_MISSILE_SUPPLY);
    }
}
