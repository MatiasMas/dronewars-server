package udegames.dronewarsserver.domain.model;

import udegames.dronewarsserver.domain.enums.UnitType;

import java.util.ArrayList;
import java.util.List;

public abstract class DroneCarrier extends Unit {
    protected List<String> dronesIds;
    protected int maxDronesCapacity;
    protected int availableAmmoSupply;

    public DroneCarrier(int maxDronesCapacity, String ownerId, int health, Position position, UnitType type, int initialAmmoSupply) {
        super(ownerId, health, position, type);

        this.maxDronesCapacity = maxDronesCapacity;
        this.dronesIds = new ArrayList<>();
        this.availableAmmoSupply = Math.max(initialAmmoSupply, 0);
    }

    public List<String> getDronesIds() {
        return dronesIds;
    }

    public int getMaxDronesCapacity() {
        return maxDronesCapacity;
    }

    public synchronized int getAvailableAmmoSupply() {
        return availableAmmoSupply;
    }

    // Consume municion del stock del carrier para recargar drones.
    public synchronized int consumeAmmoSupply(int requestedAmount) {
        if (requestedAmount <= 0 || availableAmmoSupply <= 0) {
            return 0;
        }

        int granted = Math.min(requestedAmount, availableAmmoSupply);
        availableAmmoSupply -= granted;
        return granted;
    }
}
