package udegames.dronewarsserver.engine;

import udegames.dronewarsserver.domain.entity.Player;
import udegames.dronewarsserver.domain.entity.BombProjectile;
import udegames.dronewarsserver.domain.entity.MissileProjectile;
import udegames.dronewarsserver.domain.entity.Position;
import udegames.dronewarsserver.domain.entity.Unit;
import udegames.dronewarsserver.engine.movement.UnitMovement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private final String gameId;
    private final Map<String, Player> players;
    private final Map<String, Unit> units;
    private final Map<String, BombProjectile> bombProjectiles;
    private final Map<String, MissileProjectile> missileProjectiles;
    private final Map<String, UnitMovement> unitMovements;
    private volatile boolean partidaPausada = false;
    private final long createdAt;

    public GameState(String gameId) {
        this.gameId = gameId;
        this.players = new ConcurrentHashMap<>();
        this.units = new ConcurrentHashMap<>();
        this.bombProjectiles = new ConcurrentHashMap<>();
        this.missileProjectiles = new ConcurrentHashMap<>();
        this.unitMovements = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getGameId() {
        return gameId;
    }

    //Gestion de jugadores
    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players.values());
    }

    // Gestion de unidades
    public void addUnit(Unit unit) {
        units.put(unit.getId(), unit);

        Player player = players.get(unit.getOwnerId());

        if (player != null) {
            player.addUnit(unit.getId());
        }
    }

    public void removeUnit(String unitId) {
        Unit unit = units.remove(unitId);
        unitMovements.remove(unitId);

        if (unit != null) {
            Player owner = players.get(unit.getOwnerId());

            if (owner != null) {
                owner.removeUnit(unit.getId());
            }
        }
    }

    public Unit getUnitById(String unitId) {
        return units.get(unitId);
    }

    public List<Unit> getPlayerUnits(String playerId) {
        Player player = players.get(playerId);

        if (player == null) {
            return Collections.emptyList();
        }

        return player.getUnitIds().stream().map(units::get).toList();
    }

    public List<Unit> getEnemyUnits(String playerId) {
        return units.values().stream().filter(unit -> !unit.getOwnerId().equals(playerId)).toList();
    }

    public List<Unit> getUnits() {
        return new ArrayList<>(units.values());
    }

    public void addBombProjectile(BombProjectile bombProjectile) {
        bombProjectiles.put(bombProjectile.getId(), bombProjectile);
    }

    public void removeBombProjectile(String bombId) {
        bombProjectiles.remove(bombId);
    }

    public List<BombProjectile> getBombProjectiles() {
        return new ArrayList<>(bombProjectiles.values());
    }

    public void addMissileProjectile(MissileProjectile missileProjectile) {
        missileProjectiles.put(missileProjectile.getId(), missileProjectile);
    }

    public void removeMissileProjectile(String misilId) {
        missileProjectiles.remove(misilId);
    }

    public List<MissileProjectile> getMissileProjectiles() {
        return new ArrayList<>(missileProjectiles.values());
    }

    // Gestion de movimiento
    public void setUnitMovement(String unitId, Position target, float speedPerSecond) {
        unitMovements.put(unitId, new UnitMovement(target, speedPerSecond));
    }

    public void clearUnitMovement(String unitId) {
        unitMovements.remove(unitId);
    }

    public Map<String, UnitMovement> getUnitMovements() {
        return unitMovements;
    }

    // Validaciones
    public boolean doesUnitBelongsToPlayer(String unitId, String playerId) {
        Unit unit = getUnitById(unitId);

        return unit != null && unit.getOwnerId().equals(playerId);
    }

    public boolean isUnitAlive(String unitId) {
        Unit unit = getUnitById(unitId);

        return unit != null && !unit.isDestroyed();
    }

    public boolean doesPlayerExist(String playerId) {
        return players.containsKey(playerId);
    }

    // Gestion estado servidor
    public boolean isPartidaPausada() {
        return partidaPausada;
    }

    public void setPartidaPausada(boolean partidaPausada) {
        this.partidaPausada = partidaPausada;
    }

    /**
     * Resetea el estado en memoria para cargar una partida desde la base de datos.
     * Mantiene el mismo gameId del servidor.
     */
    public void resetState() {
        players.clear();
        units.clear();
        bombProjectiles.clear();
        missileProjectiles.clear();
        unitMovements.clear();
        partidaPausada = false;
    }
}
