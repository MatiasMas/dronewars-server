package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "portadron_aereo")
public class PortadronAereo extends Unidad {

    @Column(name = "integridad", nullable = false)
    private int integridad;

    @OneToMany(
            mappedBy = "portadronAereo",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<DronAereo> drones = new HashSet<>();

    // getters y setters
}
