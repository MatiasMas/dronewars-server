package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.Jugador;

import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    List<Jugador> findByPartidaId(Long partidaId);
}