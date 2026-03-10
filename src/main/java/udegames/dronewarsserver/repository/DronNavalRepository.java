package udegames.dronewarsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import udegames.dronewarsserver.domain.model.DronNaval;

import java.util.List;

public interface DronNavalRepository extends JpaRepository<DronNaval, Long> {

    List<DronNaval> findByPortadronNavalId(Long portadronId);
}