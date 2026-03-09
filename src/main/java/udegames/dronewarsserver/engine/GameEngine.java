package udegames.dronewarsserver.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import udegames.dronewarsserver.domain.model.AerialCarrier;
import udegames.dronewarsserver.domain.model.AerialDrone;
import udegames.dronewarsserver.domain.model.BombProjectile;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.DroneCarrier;
import udegames.dronewarsserver.domain.model.MissileProjectile;
import udegames.dronewarsserver.domain.model.Player;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.dto.*;
import udegames.dronewarsserver.service.GameStateSyncService;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.mapper.UnitMapper;
import udegames.dronewarsserver.websocket.CommunicationEvents;
import udegames.dronewarsserver.websocket.GameWebSocketHandler;
import udegames.dronewarsserver.domain.model.DroneCarrier;
import udegames.dronewarsserver.domain.model.NavalCarrier;
import udegames.dronewarsserver.domain.model.NavalDrone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameEngine {
    private final GameState gameState;
    private final GameStateSyncService gameStateSyncService;
    private final UnitMovementSystem movementSystem;
    private final GameWebSocketHandler gameWebSocketHandler;
    private final ScheduledExecutorService executor;
    private volatile boolean running;
    private long currentTick;

    private static final long TICK_INTERVAL_MS = 50;
    // Umbral para considerar que la unidad ya llego al destino.
    private static final float POSITION_EPSILON = 0.01f;
    private static final float DELTA_TIME_SECONDS = TICK_INTERVAL_MS / 1000f;
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);
    // Reglas de municion:
    // Jugador 1 usa bombas (max 1), jugador 2 usa misiles (max 2).
    private static final String ID_JUGADOR_1 = "player_1";
    private static final String ID_JUGADOR_2 = "player_2";
    private static final int MUNICION_MAX_JUGADOR_1 = 1;
    private static final int MUNICION_MAX_JUGADOR_2 = 2;
    private static final int DANO_MISIL = 1;
    private static final float RANGO_EXPLOSION_MISIL = 32f;
    private static final float MIN_X = 0f;
    private static final float MAX_X = 6700f;
    private static final float MIN_Y = 0f;
    private static final float MAX_Y = 2500f;
    // Reglas de fin de juego
    private static final long TIEMPO_ESPERA_EMPATE_MS = 120_000L;
    private static final String FIN_A = "ALL_UNITS_DESTROYED";
    private static final String FIN_B = "CARRIER_DESTROYED_AND_NO_RESOURCES";
    private static final String FIN_C = "CARRIER_DESTROYED_TIMEOUT_DRAW";

    //Recarga Automatica
    private static final float RANGO_RECARGA_AUTOMATICA = 120f;
    private static final int INTERVALO_DE_RECARGAS_TICKS = 10;

    private volatile boolean gameFinished = false;
    private Long tsCarrierDestroyedP1 = null;
    private Long tsCarrierDestroyedP2 = null;

    public GameEngine(GameState gameState, GameStateSyncService gameStateSyncService, GameWebSocketHandler gameWebSocketHandler) {
        this.gameState = gameState;
        this.gameStateSyncService = gameStateSyncService;
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.movementSystem = new UnitMovementSystem(gameState, TICK_INTERVAL_MS, POSITION_EPSILON);

        this.executor = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "GameEngine-Tick");
            thread.setDaemon(true);
            return thread;
        });

        this.running = false;
        this.currentTick = 0;
    }

    /*
     * Inicia el juego, crea jugadores y unidades
     */
    public void create() {
        logger.info("[CREATE] Iniciando juego: {}", gameState.getGameId());

        createPlayers();
        createUnits();

        logger.info("[CREATE] Juego iniciado");
        logger.info("Jugadores: {}", gameState.getPlayers().size());
    }

    /*
     * Inicia los ticks del juego, simula los frames, definidos en TICK_INTERVAL_MS.
     * Llama al metodo update que se encarga de los eventos en tiempo real.
     */
    public void start() {
        if (running) {
            logger.warn("[START] GameEngine ya esta en ejecucion");
            return;
        }

        running = true;
        logger.info("[START] Iniciando ticks del motor del juego ({}ms)", TICK_INTERVAL_MS);

        executor.scheduleAtFixedRate(
                this::update,
                0,
                TICK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /*
     * Se ejecuta multiples veces por segundo.
     * Se encarga de verificar colisiones, actualizar posiciones, etc.
     */
    private void update() {
        if (!running) {
            logger.warn("[UPDATE] GameEngine no esta en ejecucion");
            return;
        }

        currentTick++;

        if (gameState.isPartidaPausada()){ return; }

//        logger.debug("[UPDATE] Tic: {}", currentTick);

        // Colocar aqui todo lo que sea relacionado con colisiones, posiciones, combustible, etc.
        actualizarRecargaAutomatica();
        updateBombProjectiles();
        updateMissileProjectiles();

        boolean moved = movementSystem.applyMovements();
        if (moved) {
            gameStateSyncService.broadcastGameState();
        }

        evaluarFinDePartida();
    }

    /*
     * Detiene el motor del juego, detiene los ticks y apaga el executor
     */
    private void stop() {
        running = false;
        executor.shutdown();
        logger.info("[STOP] GameEngine detenido");
    }

    public GameState getGameState() {
        return gameState;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public boolean isRunning() {
        return running;
    }

    private void updateBombProjectiles() {
        List<BombProjectile> activeBombs = gameState.getBombProjectiles();
        if (activeBombs.isEmpty()) {
            return;
        }

        for (BombProjectile bomb : activeBombs) {
            bomb.update(DELTA_TIME_SECONDS);

            if (bomb.hasReachedGround()) {
                resolveBombExplosion(bomb);
                gameState.removeBombProjectile(bomb.getId());
            }
        }
    }

    private void resolveBombExplosion(BombProjectile bomb) {
        List<UnitSelectionDTO> impactedUnits = new ArrayList<>();

        for (Unit enemyUnit : gameState.getUnits()) {
            if (enemyUnit.isDestroyed() || enemyUnit.getOwnerId().equals(bomb.getOwnerId())) {
                continue;
            }

            float dx = enemyUnit.getPosition().getX() - bomb.getPosition().getX();
            float dy = enemyUnit.getPosition().getY() - bomb.getPosition().getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= bomb.getBlastRadius()) {
                enemyUnit.applyDamage(bomb.getDamage());
                impactedUnits.add(UnitMapper.toSelectionDTO(enemyUnit));
            }
        }

        BombExplodedDTO payload = new BombExplodedDTO(
                bomb.getId(),
                bomb.getAttackerUnitId(),
                bomb.getPosition().getX(),
                bomb.getPosition().getY(),
                impactedUnits
        );

        gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.BOMB_EXPLODED, payload);
        logger.info("Bomb {} exploded at ({}, {}), impacted {} enemy units",
                bomb.getId(),
                bomb.getPosition().getX(),
                bomb.getPosition().getY(),
                impactedUnits.size());
    }

    private void updateMissileProjectiles() {
        List<MissileProjectile> misiles = gameState.getMissileProjectiles();
        if (misiles.isEmpty()) {
            return;
        }

        for (MissileProjectile misil : misiles) {
            // Mover el misil
            misil.update(misil.getDestinoFijo(), DELTA_TIME_SECONDS);

            // Si sale del mapa, lo eliminamos
            if (estaFueraDeMapa(misil.getPosition())) {
                MisilImpactoDTO impactoVacio = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        new ArrayList<>()
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impactoVacio);
                gameState.removeMissileProjectile(misil.getId());
                continue;
            }

            // Enviar posicion del misil al cliente
            MisilActualizadoDTO actualizado = new MisilActualizadoDTO(
                    misil.getId(),
                    misil.getPosition().getX(),
                    misil.getPosition().getY(),
                    misil.getPosition().getZ()
            );
            gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_ACTUALIZADO, actualizado);

            // Vemos si paso cerca de un dron enemigo
            float rangoCuadrado = RANGO_EXPLOSION_MISIL * RANGO_EXPLOSION_MISIL;
            List<UnitSelectionDTO> impactadas = new ArrayList<>();

            for (Unit unidad : gameState.getUnits()) {
                if (unidad.isDestroyed()) {
                    continue;
                }

                if (unidad.getOwnerId().equals(misil.getOwnerId())) {
                    continue;
                }

                boolean esDron = unidad instanceof Drone;
                boolean esPortadrones = unidad instanceof DroneCarrier;
                if (!esDron && !esPortadrones) {
                    continue;
                }

                float dx = unidad.getPosition().getX() - misil.getPosition().getX();
                float dy = unidad.getPosition().getY() - misil.getPosition().getY();
                float distancia = (dx * dx) + (dy * dy);

                if (distancia <= rangoCuadrado) {
                    if (esDron) {
                        unidad.applyDamage(unidad.getHealth());
                    } else {
                        unidad.applyDamage(1);
                    }
                    impactadas.add(UnitMapper.toSelectionDTO(unidad));

                    if (unidad.isDestroyed()) {
                        // Removemos drones destruidos, pero mantenemos carriers destruidos en GameState
                        // para que RF25.b / RF25.c puedan detectarlos.
                        if (esDron) {
                            gameState.removeUnit(unidad.getId());
                        }
                        gameState.removeUnit(unidad.getId());
                    }
                }
            }

            if (!impactadas.isEmpty()) {
                MisilImpactoDTO impacto = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        impactadas
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impacto);
                gameState.removeMissileProjectile(misil.getId());
                continue;
            }

            // Controlamos distancia maxima del misil
            if (misil.excedioDistancia()) {
                MisilImpactoDTO impactoVacio = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        new ArrayList<>()
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impactoVacio);
                gameState.removeMissileProjectile(misil.getId());
                continue;
            }

            // Controlamos tiempo de misil
            if (misil.estaExpirado()) {
                MisilImpactoDTO impactoVacio = new MisilImpactoDTO(
                        misil.getId(),
                        misil.getAttackerUnitId(),
                        null,
                        new ArrayList<>()
                );
                gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MISIL_IMPACTADO, impactoVacio);
                gameState.removeMissileProjectile(misil.getId());
            }
        }
    }

    private boolean estaFueraDeMapa(Position posicion) {
        if (posicion == null) {
            return true;
        }

        return posicion.getX() < MIN_X
                || posicion.getX() > MAX_X
                || posicion.getY() < MIN_Y
                || posicion.getY() > MAX_Y;
    }

    // --------------- Creacion de entidades para el juego ---------------
    private void createPlayers() {
        Player player1 = new Player("Fuerzas Aereas");
        Player player2 = new Player("Fuerzas Navales");

        player1.setId("player_1");
        player2.setId("player_2");

        gameState.addPlayer(player1);
        gameState.addPlayer(player2);

        logger.debug("2 jugadores creados: {}", gameState.getPlayers());
        logger.debug("Jugador 1: {}", player1.getId());
        logger.debug("Jugador 2: {}", player2.getId());
    }

    private void createUnits() {
        List<Player> players = gameState.getPlayers();

        if (players.size() < 2) {
            logger.error("Debe haber al menos 2 jugadores para iniciar un juego.");
            return;
        }

        Player player1 = players.get(0);
        Player player2 = players.get(1);

        // Player 1: drones aéreos, lado izquierdo del mapa. 12 drones + 1 portadrones aéreo.
        createPlayer1AerialUnits(player1);
        // Player 2: drones navales, lado derecho del mapa. 6 drones + 1 portadrones naval.
        createPlayer2NavalUnits(player2);

        logger.debug("Unidades creadas para jugadores: {}", gameState.getPlayers());
    }

    /** Lado izquierdo del mapa: X bajos. 1 AerialCarrier + 12 AerialDrone, agrupados pero visibles por separado. */
    private void createPlayer1AerialUnits(Player player1) {
        int municionMaxima = MUNICION_MAX_JUGADOR_1;
        float baseX = 1200f;
        float baseY = (MIN_Y + MAX_Y) * 0.5f;
        float z = 5f;
        float separacion = 80f;

        Position posCarrier = new Position(baseX - separacion, baseY - separacion, z);
        AerialCarrier carrier = new AerialCarrier(12, player1.getId(), 6, posCarrier);
        gameState.addUnit(carrier);
        logger.debug("AerialCarrier creado: {} en ({}, {}, {})", carrier.getId(), posCarrier.getX(), posCarrier.getY(), posCarrier.getZ());

        String carrierId = carrier.getId();
        // 12 drones en formación 4 filas x 3 columnas, separados para verse por separado
        int cols = 3;
        int rows = 4;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float dx = col * separacion;
                float dy = row * separacion;
                Position pos = new Position(baseX + dx, baseY + dy, z);
                AerialDrone drone = new AerialDrone(carrierId, 6000f, municionMaxima, player1.getId(), 1, pos);
                gameState.addUnit(drone);
                logger.debug("AerialDrone creado: {} en ({}, {}, {})", drone.getId(), pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    /** Lado derecho del mapa: X altos. 1 NavalCarrier + 6 NavalDrone, agrupados pero visibles por separado. */
    private void createPlayer2NavalUnits(Player player2) {
        int municionMaxima = MUNICION_MAX_JUGADOR_2;
        float baseX = MAX_X - 1200f;
        float baseY = (MIN_Y + MAX_Y) * 0.5f;
        float z = 5f;
        float separacion = 80f;

        Position posCarrier = new Position(baseX + separacion, baseY - separacion, z);
        NavalCarrier carrier = new NavalCarrier(6, player2.getId(), 3, posCarrier);
        gameState.addUnit(carrier);
        logger.debug("NavalCarrier creado: {} en ({}, {}, {})", carrier.getId(), posCarrier.getX(), posCarrier.getY(), posCarrier.getZ());

        String carrierId = carrier.getId();
        // 6 drones en formación 2 filas x 3 columnas
        int cols = 3;
        int rows = 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float dx = -col * separacion;
                float dy = row * separacion;
                Position pos = new Position(baseX + dx, baseY + dy, z);
                NavalDrone drone = new NavalDrone(carrierId, 6000f, municionMaxima, player2.getId(), 1, pos);
                gameState.addUnit(drone);
                logger.debug("NavalDrone creado: {} en ({}, {}, {})", drone.getId(), pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    private int obtenerMaxMunicionParaJugador(Player jugador) {
        if (jugador == null) {
            return MUNICION_MAX_JUGADOR_1;
        }

        if (ID_JUGADOR_1.equals(jugador.getId())) {
            return MUNICION_MAX_JUGADOR_1;
        }

        if (ID_JUGADOR_2.equals(jugador.getId())) {
            return MUNICION_MAX_JUGADOR_2;
        }

        return MUNICION_MAX_JUGADOR_1;
    }

    private void evaluarFinDePartida() {
        if (gameFinished) {
            return;
        }

        EstadoEquipo e1 = calcularEstadoEquipo(ID_JUGADOR_1);
        EstadoEquipo e2 = calcularEstadoEquipo(ID_JUGADOR_2);

        // RF25.a: la victoria se define por destruccion total de drones enemigos,
        // sin requerir destruccion del carrier.
        if (e1.dronesVivos == 0 && e2.dronesVivos == 0) {
            emitirFinDePartida(null, true, FIN_A);
            return;
        }
        if (e1.dronesVivos == 0) {
            emitirFinDePartida(ID_JUGADOR_2, false, FIN_A);
            return;
        }
        if (e2.dronesVivos == 0) {
            emitirFinDePartida(ID_JUGADOR_1, false, FIN_A);
            return;
        }
        // RF25.b
        if (e1.carrierDestruido && e1.todasLasUnidadesRestantesSinRecursos()) {
            emitirFinDePartida(ID_JUGADOR_2, false, FIN_B);
            return;
        }
        if (e2.carrierDestruido && e2.todasLasUnidadesRestantesSinRecursos()) {
            emitirFinDePartida(ID_JUGADOR_1, false, FIN_B);
            return;
        }

        // RF25.c (ajustada):
// - Si ambos carriers destruidos => empate inmediato
// - Si solo uno destruido y pasan 120s sin destruir el otro => gana el que sigue con carrier vivo
        long ahora = System.currentTimeMillis();

        if (e1.carrierDestruido && tsCarrierDestroyedP1 == null) {
            tsCarrierDestroyedP1 = ahora;
        }

        if (e2.carrierDestruido && tsCarrierDestroyedP2 == null) {
            tsCarrierDestroyedP2 = ahora;
        }
    // Ambos carriers destruidos => empate
        if (e1.carrierDestruido && e2.carrierDestruido) {
            emitirFinDePartida(null, true, FIN_C);
            return;
        }

    // Solo carrier P1 destruido; si expira espera y P2 sigue vivo => gana P2
        if (tsCarrierDestroyedP1 != null && !e2.carrierDestruido
                && (ahora - tsCarrierDestroyedP1) >= TIEMPO_ESPERA_EMPATE_MS) {
            emitirFinDePartida(ID_JUGADOR_2, false, FIN_C);
            return;
        }

        // Solo carrier P2 destruido; si expira espera y P1 sigue vivo => gana P1
        if (tsCarrierDestroyedP2 != null && !e1.carrierDestruido
                && (ahora - tsCarrierDestroyedP2) >= TIEMPO_ESPERA_EMPATE_MS) {
            emitirFinDePartida(ID_JUGADOR_1, false, FIN_C);
            emitirFinDePartida(null, true, FIN_C);
        }
    }

    private EstadoEquipo calcularEstadoEquipo(String playerId) {
        int carriersVivos = 0;
        int dronesVivos = 0;
        int dronesVivosSinRecursos = 0;
        int unidadesVivas = 0;

        for (Unit unidad : gameState.getUnits()) {
            if (!playerId.equals(unidad.getOwnerId())) {
                continue;
            }

            if (unidad instanceof DroneCarrier) {
                if (!unidad.isDestroyed()) {
                    carriersVivos++;
                }
            }

            if (unidad.isDestroyed()) {
                continue;
            }
            unidadesVivas++;

            if (unidad instanceof Drone dron) {
                dronesVivos++;
                if (dron.getCombustible() <= 0f || dron.getAmmo() <= 0) {
                    dronesVivosSinRecursos++;
                }
            }
        }

        // Cada equipo comienza con 1 portadrones. Si no hay ninguno vivo (incluso si fue removido del estado), cuenta como destruido.
        boolean carrierDestruido = carriersVivos == 0;
        return new EstadoEquipo(unidadesVivas > 0, carrierDestruido, dronesVivos, dronesVivosSinRecursos);
    }

    private void emitirFinDePartida(String ganador, boolean empate, String razon) {
        if (gameFinished) {
            return;
        }

        gameFinished = true;
        GameEndedDTO payload = new GameEndedDTO(ganador, empate, razon);
        gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.GAME_ENDED, payload);
        logger.info("Partida finalizada. draw={}, winner={}, reason={}", empate, ganador, razon);
        stop();
    }

    private void actualizarRecargaAutomatica(){
        if(currentTick % INTERVALO_DE_RECARGAS_TICKS != 0){
            return;
        }

        for (Unit unidad : gameState.getUnits()) {
            if(!(unidad instanceof Drone dron)){
                continue;
            }

            if(dron.isDestroyed()){
                continue;
            }

            boolean faltaMunicion = dron.getAmmo() < dron.getMaxAmmo();
            boolean faltaCombustible = dron.getCombustible() < dron.getMaxFuel();

            if(!faltaMunicion && !faltaCombustible){
                continue;
            }

            boolean hayPortadronesCerca = false;
            for(Unit posibleCarrier : gameState.getUnits()){
                if(!(posibleCarrier instanceof DroneCarrier)){
                    continue;
                }
                if(!(posibleCarrier.getOwnerId().equals(unidad.getOwnerId()))){
                    continue;
                }
                if(posibleCarrier.isDestroyed()){
                    continue;
                }

                float dx = dron.getPosition().getX() - posibleCarrier.getPosition().getX();
                float dy = dron.getPosition().getY() - posibleCarrier.getPosition().getY();
                float distanciaCuadrada = (dx * dx) + (dy * dy);
                float rangoCuadrado = RANGO_RECARGA_AUTOMATICA * RANGO_RECARGA_AUTOMATICA;

                if(distanciaCuadrada <= rangoCuadrado){
                    hayPortadronesCerca = true;
                    break;
                }
            }
            if(!hayPortadronesCerca){
                continue;
            }
            //Recargamos y notificamos client
            dron.reload();
            dron.refuel();
            dron.habilitarLuegoRecarga();

            AmmoReloadedDTO payload = new AmmoReloadedDTO(dron.getId(), dron.getAmmo(), dron.getCombustible());
            gameWebSocketHandler.broadcastToAll(CommunicationEvents.ServerToClientEvents.MUNICION_RECARGADA, payload);
        }
    }

    private static class EstadoEquipo {
        final boolean tieneUnidadesVivas;
        final boolean carrierDestruido;
        final int dronesVivos;
        final int dronesVivosSinRecursos;

        EstadoEquipo(boolean tieneUnidadesVivas, boolean carrierDestruido, int dronesVivos, int dronesVivosSinRecursos) {
            this.tieneUnidadesVivas = tieneUnidadesVivas;
            this.carrierDestruido = carrierDestruido;
            this.dronesVivos = dronesVivos;
            this.dronesVivosSinRecursos = dronesVivosSinRecursos;
        }

        boolean todasLasUnidadesRestantesSinRecursos() {
            return dronesVivos > 0 && dronesVivos == dronesVivosSinRecursos;
        }
    }
}

