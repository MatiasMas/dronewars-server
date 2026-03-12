package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.PortadronAereo;

public interface PortadronAereoRepository extends JpaRepository<PortadronAereo, Long> {
}