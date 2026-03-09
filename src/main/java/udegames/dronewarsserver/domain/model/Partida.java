package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "partida")
public class Partida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partida")
    private Long id;

    @NotBlank
    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "guardada", nullable = false)
    private boolean guardada = false;

    @Column(name = "terminada", nullable = false)
    private boolean terminada = false;

    @NotBlank
    @Column(name = "codigo_unico", nullable = false, unique = true, length = 64)
    private String codigoUnico;

    @OneToMany(
            mappedBy = "partida",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Size(min = 2, max = 2) // Validación a nivel de dominio
    private Set<Jugador> jugadores = new HashSet<>();

    @OneToMany(
            mappedBy = "partida",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Puntaje> puntajes = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isGuardada() {
        return guardada;
    }

    public void setGuardada(boolean guardada) {
        this.guardada = guardada;
    }

    public boolean isTerminada() {
        return terminada;
    }

    public void setTerminada(boolean terminada) {
        this.terminada = terminada;
    }

    public String getCodigoUnico() {
        return codigoUnico;
    }

    public void setCodigoUnico(String codigoUnico) {
        this.codigoUnico = codigoUnico;
    }

    public Set<Jugador> getJugadores() {
        return jugadores;
    }

    public void setJugadores(Set<Jugador> jugadores) {
        this.jugadores = jugadores;
    }

    public Set<Puntaje> getPuntajes() {
        return puntajes;
    }

    public void setPuntajes(Set<Puntaje> puntajes) {
        this.puntajes = puntajes;
    }
}
