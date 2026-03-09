package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.TipoUnidad;

import java.util.Optional;

public interface TipoUnidadRepository extends JpaRepository<TipoUnidad, Short> {

    Optional<TipoUnidad> findByNombre(String nombre);
}