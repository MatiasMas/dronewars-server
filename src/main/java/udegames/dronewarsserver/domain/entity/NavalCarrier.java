package udegames.dronewarsserver.domain.entity;

import udegames.dronewarsserver.domain.enums.UnitType;

public class NavalCarrier extends DroneCarrier {

    public NavalCarrier(int maxDronesCapacity, String ownerId, int health, Position position) {
        super(maxDronesCapacity, ownerId, health, position, UnitType.NAVAL_CARRIER);
    }
}
