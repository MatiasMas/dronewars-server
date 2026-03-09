package udegames.dronewarsserver.domain.entity;

import udegames.dronewarsserver.domain.enums.UnitType;

public class NavalDrone extends Drone {

    public NavalDrone(String carrierId, float maxFuel, int maxAmmo, String ownerId, int health, Position position) {
        super(carrierId, maxFuel, maxAmmo, ownerId, health, position, UnitType.NAVAL_DRONE);
    }
}
