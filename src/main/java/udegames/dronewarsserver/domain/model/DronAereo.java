package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dron_aereo")
public class DronAereo extends Unidad {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_portadron_aereo", nullable = false)
    private PortadronAereo portadronAereo;

    public PortadronAereo getPortadronAereo() {
        return portadronAereo;
    }

    public void setPortadronAereo(PortadronAereo portadronAereo) {
        this.portadronAereo = portadronAereo;
    }
}
