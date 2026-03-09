package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "portadron_naval")
public class PortadronNaval extends Unidad {

    @Column(name = "integridad", nullable = false)
    private int integridad;

    @OneToMany(
            mappedBy = "portadronNaval",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<DronNaval> drones = new HashSet<>();

    public int getIntegridad() {
        return integridad;
    }

    public void setIntegridad(int integridad) {
        this.integridad = integridad;
    }

    public Set<DronNaval> getDrones() {
        return drones;
    }

    public void setDrones(Set<DronNaval> drones) {
        this.drones = drones;
    }
}
