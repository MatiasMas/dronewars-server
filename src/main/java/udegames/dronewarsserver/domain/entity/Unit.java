package udegames.dronewarsserver.domain.entity;

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

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, health);
        if (this.health == 0) {
            this.destroyed = true;
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
        if (destroyed) {
            this.health = 0;
        }
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

    // Aplica dano simple y marca destruida si llega a 0.
    public void applyDamage(int dano) {
        // Si no hay dano o ya esta destruida, no hacemos nada.
        if (dano <= 0 || destroyed) {
            return;
        }

        health -= dano;
        if (health <= 0) {
            health = 0;
            destroyed = true;
        }
    }
}
