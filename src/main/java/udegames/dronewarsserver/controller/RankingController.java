package udegames.dronewarsserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import udegames.dronewarsserver.dto.RankingEntryDTO;
import udegames.dronewarsserver.service.RankingService;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
@CrossOrigin(origins = "*")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * Obtiene el ranking de puntajes ordenado por valor descendente y fecha ascendente
     * @param limit Cantidad máxima de registros a retornar (default: 10)
     * @return Lista de entradas del ranking
     */
    @GetMapping
    public ResponseEntity<List<RankingEntryDTO>> getRanking(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RankingEntryDTO> ranking = rankingService.getRanking(limit);
        return ResponseEntity.ok(ranking);
    }
}
