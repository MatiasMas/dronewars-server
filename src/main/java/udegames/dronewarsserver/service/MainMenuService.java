package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.enums.GameStatus;
import udegames.dronewarsserver.dto.MenuActionDTO;
import udegames.dronewarsserver.dto.RankingEntryDTO;
import udegames.dronewarsserver.dto.RankingResponseDTO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MainMenuService {
    private static final int MAX_PLAYERS = 2;

    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, SavedGame> savedGames = new ConcurrentHashMap<>();
    private final List<RankingEntryDTO> ranking = new CopyOnWriteArrayList<>();

    public MainMenuService() {
        seedSavedGames();
        seedRanking();
    }

    public MenuActionDTO createNewGame(String playerId, String playerName) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        String normalizedPlayerName = normalizePlayerName(playerName);

        String gameId = "game-" + UUID.randomUUID().toString().substring(0, 8);
        Lobby lobby = new Lobby(gameId);
        lobby.players.put(normalizedPlayerId, normalizedPlayerName);
        lobbies.put(gameId, lobby);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gameId", gameId);
        data.put("players", new LinkedHashMap<>(lobby.players));
        data.put("status", lobby.status.name());

        return new MenuActionDTO(true, "Partida creada", "LOBBY", data);
    }

    public MenuActionDTO joinGame(String gameId, String playerId) {
        if (gameId == null || gameId.isBlank()) {
            return new MenuActionDTO(false, "gameId es obligatorio", "JOIN_GAME", null);
        }

        Lobby lobby = lobbies.get(gameId);
        if (lobby == null) {
            return new MenuActionDTO(false, "La partida no existe", "JOIN_GAME", null);
        }

        String normalizedPlayerId = normalizePlayerId(playerId);
        String playerName = "1";
        String normalizedPlayerName = normalizePlayerName(playerName);

        synchronized (lobby) {
            if (lobby.status != GameStatus.WAITING) {
                return new MenuActionDTO(false, "La partida ya inicio o finalizo", "JOIN_GAME", null);
            }

            boolean alreadyInside = lobby.players.containsKey(normalizedPlayerId);
            if (!alreadyInside && lobby.players.size() >= MAX_PLAYERS) {
                return new MenuActionDTO(false, "La partida esta llena", "JOIN_GAME", null);
            }

            lobby.players.put(normalizedPlayerId, normalizedPlayerName);

            if (lobby.players.size() == MAX_PLAYERS) {
                lobby.status = GameStatus.IN_PROGRESS;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("gameId", gameId);
            data.put("players", new LinkedHashMap<>(lobby.players)); // playerId -> playerName
            data.put("status", lobby.status.name());

            return new MenuActionDTO(true, "Te uniste a la partida", "LOBBY", data);
        }
    }

    public MenuActionDTO loadSavedGame(String saveId) {
        if (saveId == null || saveId.isBlank()) {
            return new MenuActionDTO(false, "saveId es obligatorio", "LOAD_GAME", null);
        }

        SavedGame save = savedGames.get(saveId);
        if (save == null) {
            return new MenuActionDTO(false, "No existe partida guardada", "LOAD_GAME", null);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("saveId", save.saveId());
        data.put("gameId", save.gameId());
        data.put("description", save.description());
        data.put("savedAt", save.savedAt());

        return new MenuActionDTO(true, "Partida cargada", "GAME", data);
    }

    public RankingResponseDTO getRanking() {
        List<RankingEntryDTO> ordered = new ArrayList<>(ranking);
        ordered.sort(
                Comparator.comparingInt(RankingEntryDTO::getPoints).reversed()
                        .thenComparingInt(RankingEntryDTO::getWins).reversed()
                        .thenComparing(RankingEntryDTO::getPlayerName)
        );

        return new RankingResponseDTO(Instant.now().toString(), ordered);
    }

    public MenuActionDTO exitGame(String gameId, String playerId) {
        if (gameId != null && !gameId.isBlank() && playerId != null && !playerId.isBlank()) {
            Lobby lobby = lobbies.get(gameId);

            if (lobby != null) {
                synchronized (lobby) {
                    lobby.players.remove(playerId.trim());

                    if (lobby.players.isEmpty()) {
                        lobbies.remove(gameId);
                    } else if (lobby.status == GameStatus.IN_PROGRESS) {
                        lobby.status = GameStatus.WAITING;
                    }
                }
            }
        }

        return new MenuActionDTO(true, "Salida de partida confirmada", "MAIN_MENU", null);
    }

    private String normalizePlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return playerId.trim();
    }

    private String normalizePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "Player";
        }
        return playerName.trim();
    }

    private void seedSavedGames() {
        savedGames.put("save-001", new SavedGame("save-001", "game-alpha", "Partida guardada - mapa norte", "2026-03-01T15:40:00Z"));
        savedGames.put("save-002", new SavedGame("save-002", "game-beta", "Partida guardada - mapa sur", "2026-03-03T11:10:00Z"));
    }

    private void seedRanking() {
        ranking.add(new RankingEntryDTO("player_1", "Player 1", 8, 2, 1, 25));
        ranking.add(new RankingEntryDTO("player_2", "Player 2", 6, 4, 1, 19));
        ranking.add(new RankingEntryDTO("player_3", "Player 3", 4, 5, 2, 14));
    }

    private static class Lobby {
        private final String gameId;
        private final Map<String, String> players = new LinkedHashMap<>(); // playerId -> playerName
        private volatile GameStatus status = GameStatus.WAITING;

        private Lobby(String gameId) {
            this.gameId = gameId;
        }
    }

    private record SavedGame(String saveId, String gameId, String description, String savedAt) {}
}