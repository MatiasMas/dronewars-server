package udegames.dronewarsserver.domain.model;

import udegames.dronewarsserver.domain.enums.UnitType;

public class AerialCarrier extends DroneCarrier {

    public AerialCarrier(int maxDronesCapacity, String ownerId, int health, Position position) {
        super(maxDronesCapacity, ownerId, health, position, UnitType.AERIAL_CARRIER);
    }
}
