package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.Partida;

import java.util.Optional;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    Optional<Partida> findByCodigoUnico(String codigoUnico);
}