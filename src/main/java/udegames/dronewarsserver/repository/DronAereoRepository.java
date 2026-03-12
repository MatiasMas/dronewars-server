package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.DronAereo;

import java.util.List;

public interface DronAereoRepository extends JpaRepository<DronAereo, Long> {

    List<DronAereo> findByPortadronAereoId(Long portadronId);
}