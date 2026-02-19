package udegames.dronewarsserver.domain.model;

import udegames.dronewarsserver.domain.enums.UnitType;

import java.util.UUID;

public abstract class Unit {
    protected String id;
    protected String ownerId;
    protected int health;
    protected boolean destroyed;
    protected Position position;
    protected UnitType type;

    public Unit(String ownerId, int health, Position position, UnitType type) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.health = health;
        this.position = position;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getHealth() {
        return health;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public Position getPosition() {
        return position;
    }

    public UnitType getType() {
        return type;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    // Applies validated damage once and marks the unit as destroyed when health reaches zero.
    public void applyDamage(int damage) {
        if (destroyed || damage <= 0) {
            return;
        }

        health = Math.max(0, health - damage);
        if (health == 0) {
            destroyed = true;
        }
    }
}
