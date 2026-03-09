package udegames.dronewarsserver.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import udegames.dronewarsserver.domain.model.Puntaje;

import java.util.List;

public interface PuntajeRepository extends JpaRepository<Puntaje, Long> {

    @Query("""
            SELECT p.valor AS valor,
                   j.nickname AS jugadorNickname,
                   p.fechaRegistro AS fechaRegistro
            FROM Puntaje p
            JOIN p.jugador j
            ORDER BY p.valor DESC, p.fechaRegistro ASC
            """)
    List<IRankingPuntaje> findRanking(Pageable pageable);
}