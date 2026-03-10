package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dron_naval")
public class DronNaval extends Unidad {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_portadron_naval", nullable = false)
    private PortadronNaval portadronNaval;

    public PortadronNaval getPortadronNaval() {
        return portadronNaval;
    }

    public void setPortadronNaval(PortadronNaval portadronNaval) {
        this.portadronNaval = portadronNaval;
    }
}
