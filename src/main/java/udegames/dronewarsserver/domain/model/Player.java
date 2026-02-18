package udegames.dronewarsserver.domain.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Player {
    private String id;
    private String name;

    private final Set<String> unitIds;

    public Player(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.unitIds = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getUnitIds() {
        return unitIds;
    }

    public void addUnit(String unitId) {
        unitIds.add(unitId);
    }

    public void removeUnit(String unitId) {
        unitIds.remove(unitId);
    }
}
