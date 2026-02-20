package udegames.dronewarsserver.domain.model;

import java.util.UUID;

public class BombProjectile {
    private final String id;
    private final String attackerUnitId;
    private final String ownerId;
    private final Position position;
    private final float blastRadius;
    private final int damage;
    private final float fallSpeed;

    public BombProjectile(String attackerUnitId, String ownerId, Position position, float blastRadius, int damage, float fallSpeed) {
        this.id = UUID.randomUUID().toString();
        this.attackerUnitId = attackerUnitId;
        this.ownerId = ownerId;
        this.position = position;
        this.blastRadius = blastRadius;
        this.damage = damage;
        this.fallSpeed = fallSpeed;
    }

    public String getId() {
        return id;
    }

    public String getAttackerUnitId() {
        return attackerUnitId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Position getPosition() {
        return position;
    }

    public float getBlastRadius() {
        return blastRadius;
    }

    public int getDamage() {
        return damage;
    }

    public void update(float deltaTimeSeconds) {
        float nextZ = position.getZ() - (fallSpeed * deltaTimeSeconds);
        position.setZ(Math.max(0f, nextZ));
    }

    public boolean hasReachedGround() {
        return position.getZ() <= 0f;
    }
}
