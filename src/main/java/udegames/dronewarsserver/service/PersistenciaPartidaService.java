package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import udegames.dronewarsserver.domain.entity.*;
import udegames.dronewarsserver.domain.enums.DroneState;
import udegames.dronewarsserver.domain.enums.UnitType;
import udegames.dronewarsserver.domain.model.*;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.repository.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class PersistenciaPartidaService {

    private static final String PLAYER_1_ID = "player_1";
    private static final String PLAYER_2_ID = "player_2";
    private static final String EQUIPO_NAVAL = "NAVAL";
    private static final String EQUIPO_AEREO = "AEREO";

    private final PartidaRepository partidaRepository;
    private final JugadorRepository jugadorRepository;
    private final UnidadRepository unidadRepository;
    private final EquipoRepository equipoRepository;
    private final TipoUnidadRepository tipoUnidadRepository;

    private final SecureRandom random = new SecureRandom();

    public PersistenciaPartidaService(
            PartidaRepository partidaRepository,
            JugadorRepository jugadorRepository,
            UnidadRepository unidadRepository,
            EquipoRepository equipoRepository,
            TipoUnidadRepository tipoUnidadRepository
    ) {
        this.partidaRepository = partidaRepository;
        this.jugadorRepository = jugadorRepository;
        this.unidadRepository = unidadRepository;
        this.equipoRepository = equipoRepository;
        this.tipoUnidadRepository = tipoUnidadRepository;
    }

    @Transactional
    public String guardarPartidaEnCurso(GameState gameState) {
        Objects.requireNonNull(gameState, "gameState es obligatorio");

        List<Player> players = gameState.getPlayers();
        if (players.size() != 2) {
            throw new IllegalStateException("Se esperaban 2 jugadores, pero hay " + players.size());
        }

        // Generacion del codigo unico de la partida.
        String codigoUnico = generarCodigoUnico();

        while (partidaRepository.findByCodigoUnico(codigoUnico).isPresent()) {
            codigoUnico = generarCodigoUnico();
        }

        Equipo naval = equipoRepository.findByNombre(EQUIPO_NAVAL)
                .orElseThrow(() -> new IllegalStateException("No existe equipo NAVAL en tabla equipo"));
        Equipo aereo = equipoRepository.findByNombre(EQUIPO_AEREO)
                .orElseThrow(() -> new IllegalStateException("No existe equipo AEREO en tabla equipo"));

        Partida partida = new Partida();
        partida.setEstado("EN_CURSO");
        partida.setGuardada(true);
        partida.setTerminada(false);
        partida.setCodigoUnico(codigoUnico);

        // Mapeo de playerId (engine) a Jugador (JPA)
        Map<String, Jugador> jugadorPorPlayerId = new HashMap<>();

        for (Player player : players) {
            Jugador jugador = new Jugador();
            jugador.setPartida(partida);
            jugador.setNickname(player.getName());
            
            if (PLAYER_1_ID.equals(player.getId())) {
                jugador.setEquipo(naval);
            } else if (PLAYER_2_ID.equals(player.getId())) {
                jugador.setEquipo(aereo);
            } else {
                jugador.setEquipo(jugadorPorPlayerId.isEmpty() ? naval : aereo);
            }

            partida.getJugadores().add(jugador);
            jugadorPorPlayerId.put(player.getId(), jugador);
        }

        // Mapeo drones con su portadrones (por id UUID)
        Map<String, PortadronNaval> portadorNavalPorIdDominio = new HashMap<>();
        Map<String, PortadronAereo> portadorAereoPorIdDominio = new HashMap<>();
        List<DronNaval> dronesNavalesPendientes = new ArrayList<>();
        List<DronAereo> dronesAereosPendientes = new ArrayList<>();
        Map<Unidad, String> carrierIdPorDronEntidad = new IdentityHashMap<>();

        for (Unit unit : gameState.getUnits()) {
            Jugador owner = jugadorPorPlayerId.get(unit.getOwnerId());
            if (owner == null) {
                // Si por alguna razón no mapeamos el owner, no persistimos esa unidad.
                continue;
            }

            Unidad entidad = crearEntidadUnidadDesdeDominio(unit);
            entidad.setJugador(owner);

            Position pos = unit.getPosition();
            entidad.setCoordenadaX(Math.round(pos.getX()));
            entidad.setCoordenadaY(Math.round(pos.getY()));
            entidad.setCoordenadaZ(Math.round(pos.getZ()));
            entidad.setDestruida(unit.isDestroyed());
            entidad.setInhabilitada(false);

            if (unit instanceof Drone drone) {
                entidad.setCombustible(Math.round(drone.getCombustible()));
                entidad.setMunicion(drone.getAmmo());
            } else if (unit instanceof DroneCarrier carrier) {
                entidad.setCombustible(0);
                entidad.setMunicion(carrier.getAvailableAmmoSupply());
            } else {
                entidad.setCombustible(0);
                entidad.setMunicion(0);
            }

            owner.getUnidades().add(entidad);

            if (entidad instanceof PortadronNaval pn) {
                pn.setIntegridad(unit.getHealth());
                portadorNavalPorIdDominio.put(unit.getId(), pn);
            } else if (entidad instanceof PortadronAereo pa) {
                pa.setIntegridad(unit.getHealth());
                portadorAereoPorIdDominio.put(unit.getId(), pa);
            } else if (entidad instanceof DronNaval dn && unit instanceof Drone d) {
                dronesNavalesPendientes.add(dn);
                carrierIdPorDronEntidad.put(dn, d.getCarrierId());
            } else if (entidad instanceof DronAereo da && unit instanceof Drone d) {
                dronesAereosPendientes.add(da);
                carrierIdPorDronEntidad.put(da, d.getCarrierId());
            }
        }

        for (DronNaval dn : dronesNavalesPendientes) {
            String carrierId = carrierIdPorDronEntidad.get(dn);
            PortadronNaval carrier = portadorNavalPorIdDominio.get(carrierId);
            if (carrier != null) {
                dn.setPortadronNaval(carrier);
                carrier.getDrones().add(dn);
            }
        }
        for (DronAereo da : dronesAereosPendientes) {
            String carrierId = carrierIdPorDronEntidad.get(da);
            PortadronAereo carrier = portadorAereoPorIdDominio.get(carrierId);
            if (carrier != null) {
                da.setPortadronAereo(carrier);
                carrier.getDrones().add(da);
            }
        }

        for (Jugador j : partida.getJugadores()) {
            Puntaje puntaje = new Puntaje();
            puntaje.setPartida(partida);
            puntaje.setJugador(j);
            puntaje.setValor(0);
            puntaje.setFechaRegistro(Instant.now());

            j.setPuntaje(puntaje);
            partida.getPuntajes().add(puntaje);
        }

        partidaRepository.save(partida);
        return codigoUnico;
    }

    @Transactional
    public void cargarPartidaPorCodigo(GameState gameState, String codigoUnico) {
        Objects.requireNonNull(gameState, "gameState es obligatorio");
        if (codigoUnico == null || codigoUnico.isBlank()) {
            throw new IllegalArgumentException("codigoUnico es obligatorio");
        }

        Partida partida = partidaRepository.findByCodigoUnico(codigoUnico.trim())
                .orElseThrow(() -> new NoSuchElementException("No existe partida guardada con ese codigo"));

        // Pausamos la partida mientras reconstruimos el game state.
        gameState.setPartidaPausada(true);
        try {
            gameState.resetState();

            List<Jugador> jugadores = jugadorRepository.findByPartidaId(partida.getId());
            if (jugadores.size() != 2) {
                throw new IllegalStateException("La partida guardada no tiene 2 jugadores (tiene " + jugadores.size() + ")");
            }

            // Determinar player_1 / player_2 por equipo
            Jugador jugadorNaval = jugadores.stream()
                    .filter(j -> j.getEquipo() != null && EQUIPO_NAVAL.equalsIgnoreCase(j.getEquipo().getNombre()))
                    .findFirst()
                    .orElse(jugadores.get(0));
            Jugador jugadorAereo = jugadores.stream()
                    .filter(j -> j.getEquipo() != null && EQUIPO_AEREO.equalsIgnoreCase(j.getEquipo().getNombre()))
                    .findFirst()
                    .orElse(jugadores.size() > 1 ? jugadores.get(1) : jugadores.get(0));

            Player p1 = new Player(jugadorNaval.getNickname());
            p1.setId(PLAYER_1_ID);
            Player p2 = new Player(jugadorAereo.getNickname());
            p2.setId(PLAYER_2_ID);

            gameState.addPlayer(p1);
            gameState.addPlayer(p2);

            // Cargar unidades por jugador
            Map<Long, Unit> dominioPorUnidadId = new HashMap<>(); // id_unidad (BD) -> unit dominio

            List<Unidad> unidadesNaval = unidadRepository.findByJugadorId(jugadorNaval.getId());
            List<Unidad> unidadesAereo = unidadRepository.findByJugadorId(jugadorAereo.getId());

            // Primero portadrones (para resolver carrierId luego)
            cargarPortadrones(gameState, PLAYER_1_ID, unidadesNaval, dominioPorUnidadId);
            cargarPortadrones(gameState, PLAYER_2_ID, unidadesAereo, dominioPorUnidadId);

            // Luego drones (para que encuentren carrier)
            cargarDrones(gameState, PLAYER_1_ID, unidadesNaval, dominioPorUnidadId);
            cargarDrones(gameState, PLAYER_2_ID, unidadesAereo, dominioPorUnidadId);
        } finally {
            gameState.setPartidaPausada(false);
        }
    }

    private void cargarPortadrones(GameState gameState, String ownerPlayerId, List<Unidad> unidades, Map<Long, Unit> out) {
        for (Unidad u : unidades) {
            if (u instanceof PortadronNaval pn) {
                NavalCarrier carrier = new NavalCarrier(
                        6,
                        ownerPlayerId,
                        pn.getIntegridad(),
                        new Position(pn.getCoordenadaX(), pn.getCoordenadaY(), pn.getCoordenadaZ() == null ? 0f : pn.getCoordenadaZ())
                );
                carrier.setAvailableAmmoSupply(pn.getMunicion());
                carrier.setDestroyed(pn.isDestruida());
                gameState.addUnit(carrier);
                out.put(pn.getId(), carrier);
            } else if (u instanceof PortadronAereo pa) {
                AerialCarrier carrier = new AerialCarrier(
                        12,
                        ownerPlayerId,
                        pa.getIntegridad(),
                        new Position(pa.getCoordenadaX(), pa.getCoordenadaY(), pa.getCoordenadaZ() == null ? 0f : pa.getCoordenadaZ())
                );
                carrier.setAvailableAmmoSupply(pa.getMunicion());
                carrier.setDestroyed(pa.isDestruida());
                gameState.addUnit(carrier);
                out.put(pa.getId(), carrier);
            }
        }
    }

    private void cargarDrones(GameState gameState, String ownerPlayerId, List<Unidad> unidades, Map<Long, Unit> carriersPorId) {
        for (Unidad u : unidades) {
            if (u instanceof DronNaval dn) {
                Unit carrier = dn.getPortadronNaval() == null ? null : carriersPorId.get(dn.getPortadronNaval().getId());
                String carrierIdDominio = carrier == null ? "" : carrier.getId();
                NavalDrone drone = new NavalDrone(
                        carrierIdDominio,
                        8000f,
                        2,
                        ownerPlayerId,
                        1,
                        new Position(dn.getCoordenadaX(), dn.getCoordenadaY(), dn.getCoordenadaZ() == null ? 0f : dn.getCoordenadaZ())
                );
                drone.setCombustible(dn.getCombustible());
                drone.setAmmo(dn.getMunicion());
                drone.setDestroyed(dn.isDestruida());
                
                // Determinar el estado correcto del dron segun su combustible
                if (!dn.isDestruida()) {
                    if (dn.getCombustible() <= 0) {
                        drone.setState(DroneState.INHABILITADO);
                    } else {
                        drone.setState(DroneState.DEPLOYED);
                    }
                } else {
                    drone.setState(DroneState.DESTROYED);
                }
                
                gameState.addUnit(drone);
            } else if (u instanceof DronAereo da) {
                Unit carrier = da.getPortadronAereo() == null ? null : carriersPorId.get(da.getPortadronAereo().getId());
                String carrierIdDominio = carrier == null ? "" : carrier.getId();
                AerialDrone drone = new AerialDrone(
                        carrierIdDominio,
                        3500f,
                        1,
                        ownerPlayerId,
                        1,
                        new Position(da.getCoordenadaX(), da.getCoordenadaY(), da.getCoordenadaZ() == null ? 0f : da.getCoordenadaZ())
                );
                drone.setCombustible(da.getCombustible());
                drone.setAmmo(da.getMunicion());
                drone.setDestroyed(da.isDestruida());
                
                // Determinar el estado correcto del dron segun su combustible
                if (!da.isDestruida()) {
                    if (da.getCombustible() <= 0) {
                        drone.setState(DroneState.INHABILITADO);
                    } else {
                        drone.setState(DroneState.DEPLOYED);
                    }
                } else {
                    drone.setState(DroneState.DESTROYED);
                }
                
                gameState.addUnit(drone);
            }
        }
    }

    private Unidad crearEntidadUnidadDesdeDominio(Unit unit) {
        UnitType tipo = unit.getType();
        if (tipo == null) {
            throw new IllegalArgumentException("Unidad sin tipo");
        }

        return switch (tipo) {
            case NAVAL_CARRIER -> {
                PortadronNaval pn = new PortadronNaval();
                pn.setTipoUnidad(tipoUnidadPorNombre("PORTADRON_NAVAL"));
                yield pn;
            }
            case AERIAL_CARRIER -> {
                PortadronAereo pa = new PortadronAereo();
                pa.setTipoUnidad(tipoUnidadPorNombre("PORTADRON_AEREO"));
                yield pa;
            }
            case NAVAL_DRONE -> {
                DronNaval dn = new DronNaval();
                dn.setTipoUnidad(tipoUnidadPorNombre("DRON_NAVAL"));
                yield dn;
            }
            case AERIAL_DRONE -> {
                DronAereo da = new DronAereo();
                da.setTipoUnidad(tipoUnidadPorNombre("DRON_AEREO"));
                yield da;
            }
        };
    }

    private TipoUnidad tipoUnidadPorNombre(String nombre) {
        return tipoUnidadRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("No existe tipo_unidad " + nombre));
    }

    private String generarCodigoUnico() {
        // Código alfanumérico (10 chars), sin O/0/1/I
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; 
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
