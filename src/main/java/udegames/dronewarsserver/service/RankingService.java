package udegames.dronewarsserver.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import udegames.dronewarsserver.domain.model.Equipo;
import udegames.dronewarsserver.domain.model.Jugador;
import udegames.dronewarsserver.domain.model.Partida;
import udegames.dronewarsserver.domain.model.Puntaje;
import udegames.dronewarsserver.dto.RankingEntryDTO;
import udegames.dronewarsserver.repository.EquipoRepository;
import udegames.dronewarsserver.repository.IRankingPuntaje;
import udegames.dronewarsserver.repository.JugadorRepository;
import udegames.dronewarsserver.repository.PartidaRepository;
import udegames.dronewarsserver.repository.PuntajeRepository;

import java.time.Instant;
import java.util.List;

@Service
public class RankingService {

    private final PuntajeRepository puntajeRepository;
    private final PartidaRepository partidaRepository;
    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;

    public RankingService(
            PuntajeRepository puntajeRepository,
            PartidaRepository partidaRepository,
            JugadorRepository jugadorRepository,
            EquipoRepository equipoRepository
    ) {
        this.puntajeRepository = puntajeRepository;
        this.partidaRepository = partidaRepository;
        this.jugadorRepository = jugadorRepository;
        this.equipoRepository = equipoRepository;
    }


    // Obtiene el ranking de puntajes ordenado por valor descendente y fecha ascendente

    public List<RankingEntryDTO> getRanking(int limit) {
        List<IRankingPuntaje> rankingRaw = puntajeRepository.findRanking(PageRequest.of(0, limit));
        
        return rankingRaw.stream()
                .map(r -> new RankingEntryDTO(
                        r.getJugadorNickname(),
                        r.getValor(),
                        r.getFechaRegistro()
                ))
                .toList();
    }


    // Guarda el puntaje del ganador cuando termina una partida

    @Transactional
    public void saveWinnerScore(String nickname, int score, String playerId, String gameId) {
        // Obtener equipos de la base de datos (son requeridos por la restricción NOT NULL)
        Equipo equipoNaval = equipoRepository.findByNombre("NAVAL")
                .orElseThrow(() -> new IllegalStateException("No existe equipo NAVAL"));
        Equipo equipoAereo = equipoRepository.findByNombre("AEREO")
                .orElseThrow(() -> new IllegalStateException("No existe equipo AEREO"));
        
        // Crear una partida temporal para el puntaje
        Partida partida = new Partida();
        partida.setEstado("FINALIZADA");
        partida.setGuardada(false);
        partida.setTerminada(true);
        // Generar código único para cumplir con la validación
        partida.setCodigoUnico("RANK-" + System.currentTimeMillis());

        // Crear jugador ganador
        Jugador ganador = new Jugador();
        ganador.setNickname(nickname);
        ganador.setPartida(partida);
        ganador.setEquipo(equipoNaval); // Asignar equipo para cumplir NOT NULL

        // Crear jugador perdedor ficticio para cumplir con validación de 2 jugadores
        Jugador perdedor = new Jugador();
        perdedor.setNickname("---");
        perdedor.setPartida(partida);
        perdedor.setEquipo(equipoAereo); // Asignar equipo para cumplir NOT NULL

        // Agregar jugadores a la partida ANTES de guardar
        // para que pase la validación @Size(min = 2, max = 2)
        partida.getJugadores().add(ganador);
        partida.getJugadores().add(perdedor);

        // Ahora guardar la partida con sus jugadores
        partida = partidaRepository.save(partida);

        // Crear puntaje del ganador
        Puntaje puntaje = new Puntaje();
        puntaje.setJugador(ganador);
        puntaje.setPartida(partida);
        puntaje.setValor(score);
        puntaje.setFechaRegistro(Instant.now());
        ganador.setPuntaje(puntaje);

        // Crear puntaje del perdedor con 0 puntos
        Puntaje puntajePerdedor = new Puntaje();
        puntajePerdedor.setJugador(perdedor);
        puntajePerdedor.setPartida(partida);
        puntajePerdedor.setValor(0);
        puntajePerdedor.setFechaRegistro(Instant.now());
        perdedor.setPuntaje(puntajePerdedor);

        // Agregar puntajes a la partida
        partida.getPuntajes().add(puntaje);
        partida.getPuntajes().add(puntajePerdedor);

        // Guardar todo en cascada
        partidaRepository.save(partida);
    }
}
