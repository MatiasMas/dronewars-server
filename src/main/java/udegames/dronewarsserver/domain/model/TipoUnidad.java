package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tipo_unidad")
public class TipoUnidad {
    @Id
    @Column(name = "id_tipo_unidad")
    private Short id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @OneToMany(mappedBy = "tipoUnidad")
    private Set<Unidad> unidades = new HashSet<>();

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<Unidad> getUnidades() {
        return unidades;
    }

    public void setUnidades(Set<Unidad> unidades) {
        this.unidades = unidades;
    }
}
