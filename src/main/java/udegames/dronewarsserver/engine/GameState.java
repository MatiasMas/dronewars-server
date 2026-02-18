package udegames.dronewarsserver.engine;

import udegames.dronewarsserver.domain.model.Player;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private final String gameId;
    private final Map<String, Player> players;
    private final Map<String, Unit> units;
    private final Map<String, UnitMovement> unitMovements;
    private final long createdAt;

    public GameState(String gameId) {
        this.gameId = gameId;
        this.players = new ConcurrentHashMap<>();
        this.units = new ConcurrentHashMap<>();
        this.unitMovements = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getGameId() {
        return gameId;
    }

    // ------------ Gestión de jugadores ------------
    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players.values());
    }

    // ------------ Gestión de unidades ------------
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

    // ------------ Gestión de movimiento ------------
    public void setUnitMovement(String unitId, Position target, float speedPerSecond) {
        unitMovements.put(unitId, new UnitMovement(target, speedPerSecond));
    }

    public void clearUnitMovement(String unitId) {
        unitMovements.remove(unitId);
    }

    public Map<String, UnitMovement> getUnitMovements() {
        return unitMovements;
    }

    // ------------ Validaciones ------------
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
}
