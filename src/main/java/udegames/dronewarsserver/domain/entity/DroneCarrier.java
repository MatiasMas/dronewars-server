package udegames.dronewarsserver.domain.entity;

import udegames.dronewarsserver.domain.enums.UnitType;

import java.util.ArrayList;
import java.util.List;

public abstract class DroneCarrier extends Unit {
    protected List<String> dronesIds;
    protected int maxDronesCapacity;

    public DroneCarrier(int maxDronesCapacity, String ownerId, int health, Position position, UnitType type) {
        super(ownerId, health, position, type);

        this.maxDronesCapacity = maxDronesCapacity;
        this.dronesIds = new ArrayList<>();
    }

    public List<String> getDronesIds() {
        return dronesIds;
    }

    public int getMaxDronesCapacity() {
        return maxDronesCapacity;
    }
}
