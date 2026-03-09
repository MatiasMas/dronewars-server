package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.Unidad;

import java.util.List;

public interface UnidadRepository extends JpaRepository<Unidad, Long> {

    List<Unidad> findByJugadorId(Long jugadorId);
}