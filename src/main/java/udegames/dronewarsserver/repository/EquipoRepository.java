package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.Equipo;

import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Short> {

    Optional<Equipo> findByNombre(String nombre);
}