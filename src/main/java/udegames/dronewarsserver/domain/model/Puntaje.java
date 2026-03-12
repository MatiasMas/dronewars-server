package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "puntaje",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_puntaje_partida_jugador",
                        columnNames = {"id_partida", "id_jugador"}
                )
        }
)
public class Puntaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_puntaje")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_partida", nullable = false)
    private Partida partida;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jugador", nullable = false, unique = true)
    private Jugador jugador;

    @Column(name = "valor", nullable = false)
    private int valor;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Instant fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
