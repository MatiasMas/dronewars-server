package udegames.dronewarsserver.domain.model;

import udegames.dronewarsserver.domain.enums.DroneState;
import udegames.dronewarsserver.domain.enums.UnitType;

public abstract class Drone extends Unit {
    protected String carrierId;
    protected float fuel;
    protected float maxFuel;
    protected int ammo;
    protected int maxAmmo;
    protected DroneState state;

    public Drone(String carrierId, float maxFuel, int maxAmmo, String ownerId, int health, Position position, UnitType type) {
        super(ownerId, health, position, type);

        this.carrierId = carrierId;
        this.maxFuel = maxFuel;
        this.fuel = maxFuel;
        this.maxAmmo = maxAmmo;
        // Al inicio todos los drones comienzan sin municion.
        this.ammo = 0;
        this.state = DroneState.DOCKED;
    }

    public String getCarrierId() {
        return carrierId;
    }

    public float getFuel() {
        return fuel;
    }

    public int getAmmo() {
        return ammo;
    }

    // Maximo permitido para la unidad (segun el jugador).
    public int getMaxAmmo() {
        return maxAmmo;
    }

    public DroneState getState() {
        return state;
    }

    public void setState(DroneState state) {
        this.state = state;
    }

    public void refuel() {
        this.fuel = maxFuel;
    }

    public void reload() {
        this.ammo = maxAmmo;
    }

    // Consume municion si hay disponible.
    public boolean consumirMunicion(int cantidad) {
        if (cantidad <= 0) {
            return false;
        }

        if (ammo < cantidad) {
            // No hay suficiente municion.
            return false;
        }

        ammo -= cantidad;
        return true;
    }
}
