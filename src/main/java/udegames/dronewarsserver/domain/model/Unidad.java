package udegames.dronewarsserver.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "unidad")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Unidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jugador", nullable = false)
    private Jugador jugador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_unidad", nullable = false)
    private TipoUnidad tipoUnidad;

    @Column(name = "coordenada_x", nullable = false)
    private int coordenadaX;

    @Column(name = "coordenada_y", nullable = false)
    private int coordenadaY;

    @Column(name = "coordenada_z")
    private Integer coordenadaZ;

    @Column(name = "combustible", nullable = false)
    private int combustible;

    @Column(name = "municion", nullable = false)
    private int municion;

    @Column(name = "destruida", nullable = false)
    private boolean destruida = false;

    @Column(name = "inhabilitada", nullable = false)
    private boolean inhabilitada = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public TipoUnidad getTipoUnidad() {
        return tipoUnidad;
    }

    public void setTipoUnidad(TipoUnidad tipoUnidad) {
        this.tipoUnidad = tipoUnidad;
    }

    public int getCoordenadaX() {
        return coordenadaX;
    }

    public void setCoordenadaX(int coordenadaX) {
        this.coordenadaX = coordenadaX;
    }

    public int getCoordenadaY() {
        return coordenadaY;
    }

    public void setCoordenadaY(int coordenadaY) {
        this.coordenadaY = coordenadaY;
    }

    public Integer getCoordenadaZ() {
        return coordenadaZ;
    }

    public void setCoordenadaZ(Integer coordenadaZ) {
        this.coordenadaZ = coordenadaZ;
    }

    public int getCombustible() {
        return combustible;
    }

    public void setCombustible(int combustible) {
        this.combustible = combustible;
    }

    public int getMunicion() {
        return municion;
    }

    public void setMunicion(int municion) {
        this.municion = municion;
    }

    public boolean isDestruida() {
        return destruida;
    }

    public void setDestruida(boolean destruida) {
        this.destruida = destruida;
    }

    public boolean isInhabilitada() {
        return inhabilitada;
    }

    public void setInhabilitada(boolean inhabilitada) {
        this.inhabilitada = inhabilitada;
    }
}