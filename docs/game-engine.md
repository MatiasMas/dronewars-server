# Motor de Juego y Gestión del Mundo - DroneWars

Este documento explica en profundidad cómo el servidor simula el mundo de DroneWars a través de su motor de juego (`GameEngine`), el concepto de ticks, y cómo el `GameState` gestiona el estado del mundo.

---

## 1. El Concepto de "Tick" en Game Development

En el desarrollo de juegos multijugador, un **Tick** es un **ciclo de actualización** que el servidor ejecuta a una frecuencia fija. Es el equivalente a los "frames" en un cliente gráfico, pero centrado exclusivamente en la **simulación lógica del juego**.

### Características del Sistema de Ticks

*   **Frecuencia**: 50ms (20 Ticks por segundo).
*   **Clase Responsable**: `GameEngine.java` (src/main/java/.../engine/GameEngine.java).
*   **Mecanismo**: Utiliza un `ScheduledExecutorService` de Java para ejecutar el método `update()` periódicamente.
*   **Hilo daemon**: El executor se configura con hilos daemon para no bloquear el cierre de la aplicación.

### Código de Inicialización del Sistema de Ticks

**Archivo**: `GameEngine.java:24-35`

```java
public GameEngine(GameState gameState) {
    this.gameState = gameState;

    // Crea un ScheduledExecutorService con un único hilo
    this.executor = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "GameEngine-Tick");
        thread.setDaemon(true); // Hilo daemon: no bloquea el cierre de la app
        return thread;
    });

    this.running = false;
    this.currentTick = 0;
}
```

### Inicio del Bucle de Ticks

**Archivo**: `GameEngine.java:54-69`

```java
public void start() {
    if (running) {
        logger.warn("[START] GameEngine already running");
        return;
    }

    running = true;
    logger.info("[START] Starting game engine ticks ({}ms)", TICK_INTERVAL_MS);

    // Programa la ejecución periódica de update()
    executor.scheduleAtFixedRate(
            this::update,        // Método a ejecutar
            0,                   // Delay inicial (0ms = inmediato)
            TICK_INTERVAL_MS,    // Período (50ms)
            TimeUnit.MILLISECONDS
    );
}
```

**¿Qué hace `scheduleAtFixedRate`?**
- Ejecuta `this.update()` cada 50ms.
- Si `update()` tarda más de 50ms, el próximo tick se ejecuta inmediatamente (no se salta).
- Garantiza una frecuencia constante de actualización.

---

## 2. Funcionamiento del `GameEngine`

El motor de juego pasa por **tres fases principales**:

### Fase 1: Creación (`create()`)

Se ejecuta al iniciar el servidor (llamado desde `GameConfig`). Inicializa el mundo del juego.

**Archivo**: `GameEngine.java:40-48`

```java
public void create() {
    logger.info("[CREATE] Starting game: {}", gameState.getGameId());

    createPlayers(); // Crea jugadores de prueba
    createUnits();   // Crea unidades iniciales

    logger.info("[CREATE] Game started");
    logger.info("Players: {}", gameState.getPlayers().size());
}
```

#### **Creación de Jugadores** (`createPlayers`)

**Archivo**: `GameEngine.java:110-123`

```java
private void createPlayers() {
    // Crea dos jugadores de prueba
    Player player1 = new Player("Player 1");
    Player player2 = new Player("Player 2");

    player1.setId("player_1");
    player2.setId("player_2");

    // Los agrega al GameState
    gameState.addPlayer(player1);
    gameState.addPlayer(player2);

    logger.debug("2 players created: {}", gameState.getPlayers());
    logger.debug("Player 1: {}", player1.getId());
    logger.debug("Player 2: {}", player2.getId());
}
```

**Flujo**:
1. Crea instancias de `Player` con nombres.
2. Asigna IDs manualmente (`player_1`, `player_2`).
3. Los agrega al `GameState` mediante `addPlayer()`.

---

#### **Creación de Unidades** (`createUnits`)

**Archivo**: `GameEngine.java:125-141`

```java
private void createUnits() {
    List<Player> players = gameState.getPlayers();

    if (players.size() < 2) {
        logger.error("There must be at least 2 players to start a game.");
        return;
    }

    Player player1 = players.get(0);
    Player player2 = players.get(1);

    // Crea unidades para cada jugador en posiciones hardcodeadas
    createPlayerUnits(player1, "carrier-p1", 10f, 10f);
    createPlayerUnits(player2, "carrier-p2", 100f, 100f);

    logger.debug("Units created for players: {}", gameState.getPlayers());
}
```

**Flujo**:
1. Obtiene la lista de jugadores del `GameState`.
2. Valida que haya al menos 2 jugadores.
3. Crea unidades para cada jugador en posiciones específicas (hardcodeadas por ahora).

---

#### **Creación de Unidades por Jugador** (`createPlayerUnits`)

**Archivo**: `GameEngine.java:143-162`

```java
private void createPlayerUnits(Player player, String carrierId, float startingX, float startingY) {
    // Crea Drone Aéreo 1
    Position aerialDrone1Position = new Position(startingX, startingY, 5f);
    AerialDrone aerialDrone1 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone1Position);

    gameState.addUnit(aerialDrone1);
    logger.debug("AerialDrone created: {} in ({}, {}, {})", aerialDrone1.getId(), aerialDrone1Position.getX(), aerialDrone1Position.getY(), aerialDrone1Position.getZ());

    // Crea Drone Aéreo 2
    Position aerialDrone2Position = new Position(startingX + 5f, startingY + 5f, 5f);
    AerialDrone aerialDrone2 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone2Position);

    gameState.addUnit(aerialDrone2);
    logger.debug("AerialDrone created: {} in ({}, {}, {})", aerialDrone2.getId(), aerialDrone2Position.getX(), aerialDrone2Position.getY(), aerialDrone2Position.getZ());

    // Crea Portadrones Aéreo
    Position aerialCarrierPosition = new Position(startingX - 5f, startingY - 5f, 5f);
    AerialCarrier aerialCarrier = new AerialCarrier(12, player.getId(), 6, aerialCarrierPosition);

    gameState.addUnit(aerialCarrier);
    logger.debug("AerialCarrier created: {} in ({}, {}, {})", aerialCarrier.getId(), aerialCarrierPosition.getX(), aerialCarrierPosition.getY(), aerialCarrierPosition.getZ());
}
```

**Flujo**:
1. Crea 2 `AerialDrone` en posiciones cercanas a las coordenadas de inicio.
2. Crea 1 `AerialCarrier` (portadrones) en una posición offset.
3. Agrega cada unidad al `GameState` mediante `addUnit()`.

**Resultado**: Cada jugador tiene 3 unidades (2 drones + 1 carrier).

---

### Fase 2: Inicio (`start()`)

Arranca el hilo de ejecución que llamará a `update()` cada 50ms.

**Archivo**: `GameEngine.java:54-69`

```java
public void start() {
    if (running) {
        logger.warn("[START] GameEngine already running");
        return;
    }

    running = true;
    logger.info("[START] Starting game engine ticks ({}ms)", TICK_INTERVAL_MS);

    executor.scheduleAtFixedRate(
            this::update,
            0,
            TICK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
    );
}
```

**Nota importante**: El uso de `setDaemon(true)` en el hilo del executor asegura que el motor no bloquee el cierre de la aplicación Spring.

---

### Fase 3: Actualización (`update()`)

**Este método es el corazón de la simulación**. Se ejecuta 20 veces por segundo.

**Archivo**: `GameEngine.java:75-86`

```java
private void update() {
    if (!running) {
        logger.warn("[UPDATE] GameEngine not running");
        return;
    }

    currentTick++;

    // logger.debug("[UPDATE] Tick: {}", currentTick);

    // LÓGICA FUTURA:
    // updateUnitsPositions();
    // checkCollisions();
    // broadcastGameState();
}
```

**Flujo actual**:
1. Verifica que el motor esté corriendo (`running`).
2. Incrementa el contador de ticks (`currentTick`).
3. **Actualmente está vacío** - aquí se agregará la lógica de simulación futura.

**Lógica futura planeada**:
- `updateUnitsPositions()`: Actualizar posiciones de unidades que se están moviendo.
- `checkCollisions()`: Detectar colisiones entre unidades.
- `broadcastGameState()`: Enviar actualizaciones a los clientes cada X ticks.

---

## 3. Gestión del Mundo: El `GameState`

El `GameState` es la **fuente de verdad** del juego. Es un contenedor que:
- Almacena todos los jugadores y unidades.
- Utiliza `ConcurrentHashMap` para evitar problemas de concurrencia entre el hilo del `GameEngine` y los hilos de WebSocket.
- Proporciona métodos de validación y consulta.

### Estructura del `GameState`

**Archivo**: `GameState.java:9-20`

```java
public class GameState {
    private final String gameId;
    private final Map<String, Player> players;      // Mapa de jugadores
    private final Map<String, Unit> units;          // Mapa de unidades
    private final long createdAt;

    public GameState(String gameId) {
        this.gameId = gameId;
        this.players = new ConcurrentHashMap<>();  // Thread-safe
        this.units = new ConcurrentHashMap<>();    // Thread-safe
        this.createdAt = System.currentTimeMillis();
    }
    // ...
}
```

**¿Por qué `ConcurrentHashMap`?**
- El `GameEngine` actualiza el estado en su propio hilo (cada 50ms).
- Los `WebSocketHandler` leen/escriben el estado cuando llegan mensajes de clientes (hilos de Netty).
- Sin sincronización, habría condiciones de carrera (race conditions).
- `ConcurrentHashMap` permite lecturas/escrituras concurrentes seguras.

---

### Gestión de Jugadores

**Archivo**: `GameState.java:26-37`

```java
// ------------ Player Management ------------
public void addPlayer(Player player) {
    players.put(player.getId(), player);
}

public Player getPlayer(String playerId) {
    return players.get(playerId);
}

public List<Player> getPlayers() {
    return new ArrayList<>(players.values());
}
```

**Flujo**: Simple gestión de un mapa donde la clave es el `playerId`.

---

### Gestión de Unidades

**Archivo**: `GameState.java:40-48`

```java
public void addUnit(Unit unit) {
    units.put(unit.getId(), unit); // 1. Agrega al mapa global

    Player player = players.get(unit.getOwnerId());

    if (player != null) {
        player.addUnit(unit.getId()); // 2. Referencia en el inventario del jugador
    }
}
```

**Flujo**:
1. Agrega la unidad al mapa global `units` (clave = `unitId`).
2. Obtiene al jugador propietario mediante `getOwnerId()`.
3. Agrega el `unitId` a la lista de unidades del jugador (`player.addUnit()`).

**Resultado**: Cada unidad está indexada globalmente Y referenciada en su jugador propietario.

---

**Archivo**: `GameState.java:50-60`

```java
public void removeUnit(String unitId) {
    Unit unit = units.remove(unitId); // 1. Remueve del mapa global

    if (unit != null) {
        Player owner = players.get(unit.getOwnerId());

        if (owner != null) {
            owner.removeUnit(unit.getId()); // 2. Remueve referencia del jugador
        }
    }
}
```

**Flujo inverso**:
1. Remueve del mapa global.
2. Remueve la referencia del inventario del jugador.

---

### Consultas de Unidades

**Archivo**: `GameState.java:62-78`

```java
public Unit getUnitById(String unitId) {
    return units.get(unitId);
}

public List<Unit> getPlayerUnits(String playerId) {
    Player player = players.get(playerId);

    if (player == null) {
        return Collections.emptyList();
    }

    // Obtiene los IDs de unidades del jugador y los mapea a objetos Unit
    return player.getUnitIds().stream().map(units::get).toList();
}

public List<Unit> getEnemyUnits(String playerId) {
    // Filtra todas las unidades que NO pertenecen al jugador
    return units.values().stream().filter(unit -> !unit.getOwnerId().equals(playerId)).toList();
}
```

**`getPlayerUnits`**:
1. Obtiene al jugador del mapa.
2. Obtiene la lista de `unitIds` del jugador.
3. Mapea cada `unitId` a su objeto `Unit` correspondiente.
4. Retorna la lista de unidades.

**`getEnemyUnits`**:
1. Itera sobre todas las unidades del juego.
2. Filtra aquellas cuyo `ownerId` es diferente al `playerId`.
3. Retorna la lista de unidades enemigas.

---

### Validaciones

**Archivo**: `GameState.java:80-96`

```java
public boolean doesUnitBelongsToPlayer(String unitId, String playerId) {
    Unit unit = getUnitById(unitId);
    return unit != null && unit.getOwnerId().equals(playerId);
}

public boolean isUnitAlive(String unitId) {
    Unit unit = getUnitById(unitId);
    return unit != null && !unit.isDestroyed();
}

public boolean doesPlayerExist(String playerId) {
    return players.containsKey(playerId);
}
```

**Uso**: Estos métodos son utilizados por `SelectionService` y otros servicios para validar acciones.

---

## 4. Flujo de un Evento en el Motor (Ejemplo Hipotético: Movimiento)

Imaginemos que implementamos el movimiento de unidades. Así sería el flujo:

### 4.1. **Cliente solicita movimiento**

```typescript
// Cliente (GameScene.ts)
this.input.on('pointerdown', (pointer: Phaser.Input.Pointer) => {
    const selectedUnitId = this.selectionManager.getSelectedUnitId();
    if (selectedUnitId) {
        this.websocketClient.send({
            type: ClientToServerEvents.MOVE_UNIT,
            unitId: selectedUnitId,
            targetX: pointer.worldX,
            targetY: pointer.worldY
        });
    }
});
```

---

### 4.2. **Servidor valida y marca el destino**

```java
// GameWebSocketHandler.java
private void handleMoveUnit(WebSocketSession session, JsonNode root) {
    String unitId = root.get("unitId").asText();
    float tx = root.get("targetX").asFloat();
    float ty = root.get("targetY").asFloat();

    // Validaciones (pertenece al jugador, está viva, etc.)
    // ...

    // Obtiene la unidad del GameState
    Unit unit = gameState.getUnitById(unitId);

    // Marca el destino deseado (NO mueve todavía)
    unit.setTargetPosition(new Position(tx, ty, unit.getPosition().getZ()));

    logger.info("Unit {} target set to ({}, {})", unitId, tx, ty);
}
```

**Nota importante**: El servidor **NO mueve la unidad inmediatamente**. Solo marca el destino. El movimiento ocurre en el `update()`.

---

### 4.3. **Motor procesa movimiento en cada Tick**

```java
// GameEngine.java - método update() (LÓGICA FUTURA)
private void update() {
    if (!running) return;

    currentTick++;

    // Itera sobre todas las unidades
    gameState.getUnits().values().forEach(unit -> {
        // Si la unidad tiene un destino marcado
        if (unit.hasTarget()) {
            // Calcula el desplazamiento según la velocidad
            Position current = unit.getPosition();
            Position target = unit.getTargetPosition();

            // Calcula vector de dirección
            float dx = target.getX() - current.getX();
            float dy = target.getY() - current.getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // Si la distancia es muy pequeña, llegó al destino
            if (distance < 0.1f) {
                current.setX(target.getX());
                current.setY(target.getY());
                unit.clearTarget();
                logger.debug("Unit {} arrived at target", unit.getId());
            } else {
                // Mueve un paso hacia el destino
                float speed = unit.getSpeed(); // ej: 2.0 unidades/tick
                float step = Math.min(speed * (TICK_INTERVAL_MS / 1000f), distance);
                float ratio = step / distance;

                current.setX(current.getX() + dx * ratio);
                current.setY(current.getY() + dy * ratio);
            }
        }
    });

    // Cada 3 ticks (150ms), sincroniza con clientes
    if (currentTick % 3 == 0) {
        broadcastGameState();
    }
}
```

**Flujo**:
1. Itera sobre todas las unidades.
2. Si una unidad tiene `targetPosition`, calcula el vector de movimiento.
3. Mueve la posición actual un paso hacia el destino (según velocidad).
4. Si llegó al destino, limpia el `targetPosition`.
5. Cada 3 ticks, envía actualizaciones a todos los clientes.

---

### 4.4. **Sincronización con clientes**

```java
// GameEngine.java - método hipotético
private void broadcastGameState() {
    // Obtiene todas las unidades
    List<Unit> units = new ArrayList<>(gameState.getUnits().values());

    // Convierte a DTOs
    List<UnitPositionDTO> unitPositions = units.stream()
        .map(unit -> new UnitPositionDTO(unit.getId(), unit.getPosition()))
        .toList();

    // Envía a todos los clientes conectados (a través de GameWebSocketHandler)
    webSocketHandler.broadcastToAll(
        ServerToClientEvents.GAME_STATE_UPDATE,
        unitPositions
    );
}
```

---

### 4.5. **Cliente actualiza visualización**

```typescript
// GameScene.ts
this.websocketClient.on(ServerToClientEvents.GAME_STATE_UPDATE, (unitPositions) => {
    unitPositions.forEach(data => {
        const sprite = this.unitSprites.get(data.unitId);
        if (sprite) {
            // Interpolación suave (opcional)
            this.tweens.add({
                targets: sprite,
                x: data.position.x,
                y: data.position.y,
                duration: 150, // Duración de la interpolación
                ease: 'Linear'
            });
        }
    });
});
```

---

## 5. Diagrama de Flujo: Evento de Movimiento

```
Usuario hace clic en el mapa
    ↓
GameScene.input.on('pointerdown') detecta clic
    ↓
WebSocketClient.send({ type: MOVE_UNIT, unitId, targetX, targetY })
    ↓
[VIAJE POR LA RED]
    ↓
GameWebSocketHandler.handleMoveUnit()
    ↓
Validaciones (pertenece al jugador, está viva, etc.)
    ↓
gameState.getUnitById(unitId).setTargetPosition(target)
    ↓
[ESPERA HASTA EL PRÓXIMO TICK]
    ↓
GameEngine.update() se ejecuta (cada 50ms)
    ↓
Itera sobre unidades con targetPosition
    ↓
Calcula nuevo position según velocidad
    ↓
Actualiza position en GameState
    ↓
[Cada 3 ticks (150ms)]
    ↓
GameEngine.broadcastGameState()
    ↓
Envía UnitPositionDTO a todos los clientes
    ↓
[VIAJE POR LA RED]
    ↓
WebSocketClient.handleMessage() parsea GAME_STATE_UPDATE
    ↓
GameScene actualiza sprites (con interpolación)
```

---

## 6. Inicialización Completa del Motor (Desde el Arranque del Servidor)

### 6.1. **Spring Boot arranca**

**Archivo**: `GameConfig.java:18-21`

```java
@Bean
public GameState gameState() {
    return new GameState("game-001"); // Crea instancia única de GameState
}
```

**Flujo**: Spring crea el bean `GameState` y lo almacena en el contenedor de inyección de dependencias.

---

### 6.2. **Spring crea el GameEngine**

**Archivo**: `GameConfig.java:28-50`

```java
@Bean
public GameEngine gameEngine(GameState gameState) {
    GameEngine gameEngine = new GameEngine(gameState); // Inyecta GameState

    // Fase 1: Creación (inicializa jugadores y unidades)
    gameEngine.create();

    // Fase 2: Inicio (arranca el bucle de ticks en un hilo separado)
    Thread engineStarterThread = new Thread(() -> {
        try {
            Thread.sleep(1000); // Espera 1 segundo a que Spring termine de inicializar
            logger.info("Starting game engine...");
            gameEngine.start();
            logger.info("Game engine started");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error starting game engine: {}", e.getMessage());
        }
    }, "GameEngine-Starter");

    engineStarterThread.start();

    return gameEngine;
}
```

**Flujo**:
1. Spring inyecta el `GameState` en el constructor de `GameEngine`.
2. Llama a `gameEngine.create()` (crea jugadores y unidades).
3. Crea un hilo separado que espera 1 segundo y luego llama a `gameEngine.start()`.
4. `start()` inicia el bucle de ticks (cada 50ms).

**¿Por qué esperar 1 segundo?**
- Asegura que Spring haya terminado de inicializar todos los beans (WebSocketHandler, servicios, etc.) antes de que el motor comience a ejecutar lógica.

---

## 7. Marco Teórico: Arquitecturas de Sincronización

### 7.1. **Servidor Autoritativo (Implementación Actual)**

**Definición**: El servidor tiene la palabra final sobre el estado del juego.

**Características**:
- Los clientes envían "intenciones" (quiero moverme a X).
- El servidor valida y simula el movimiento.
- El servidor envía el resultado al cliente.

**Ventajas**:
- **Anti-cheat**: Los jugadores no pueden modificar su posición localmente.
- **Consistencia**: Todos los jugadores ven el mismo estado (eventualmente).

**Desventajas**:
- **Latencia**: Los jugadores experimentan un retraso entre la acción y la respuesta.

---

### 7.2. **Cliente Predictivo (Futuro)**

**Definición**: El cliente simula el movimiento localmente antes de recibir confirmación del servidor.

**Flujo**:
1. Usuario presiona "W" (mover adelante).
2. Cliente simula el movimiento inmediatamente (sensación de respuesta instantánea).
3. Cliente envía la acción al servidor.
4. Servidor valida y simula.
5. Servidor envía la posición "real".
6. Cliente compara: si hay diferencia, corrige suavemente (reconciliación).

**Ventajas**:
- **Sensación de respuesta inmediata**: No hay lag perceptible.

**Desventajas**:
- **Complejidad**: Requiere lógica de reconciliación.
- **Posibles correcciones visuales**: Si el servidor rechaza la acción, el cliente debe "retroceder".

---

## 8. Conceptos Clave de Sincronización

### 8.1. **Interpolación (Client-Side)**

Suaviza el movimiento de entidades entre actualizaciones del servidor.

**Ejemplo**:
- Servidor envía actualizaciones cada 150ms.
- Cliente recibe: `posición A → espera 150ms → posición B`.
- Sin interpolación: La unidad "salta" de A a B (parpadeo).
- Con interpolación: Cliente mueve suavemente de A a B en 150ms (fluido).

**Implementación (Phaser)**:
```typescript
this.tweens.add({
    targets: sprite,
    x: newPosition.x,
    y: newPosition.y,
    duration: 150, // Tiempo de interpolación
    ease: 'Linear'
});
```

---

### 8.2. **Dead Reckoning**

Predicción de movimiento cuando no hay actualizaciones del servidor.

**Ejemplo**:
- Cliente conoce la posición y velocidad de una unidad.
- Si no llegan actualizaciones, asume que la unidad sigue moviéndose en la misma dirección.
- Cuando llega la actualización real, corrige la diferencia.

---

## 9. Resumen del Ciclo de Vida del Motor

```
[Inicio de Spring Boot]
    ↓
GameConfig.gameState() → Crea GameState
    ↓
GameConfig.gameEngine(gameState) → Inyecta GameState en GameEngine
    ↓
gameEngine.create()
    ↓
  ├─ createPlayers() → Crea player_1 y player_2
  └─ createUnits()   → Crea 3 unidades por jugador
    ↓
[Hilo separado espera 1 segundo]
    ↓
gameEngine.start()
    ↓
executor.scheduleAtFixedRate(this::update, 0, 50ms)
    ↓
[Bucle infinito cada 50ms]
    ↓
update() → currentTick++
    ↓
  [Lógica futura:]
  ├─ updateUnitsPositions()
  ├─ checkCollisions()
  └─ broadcastGameState()
    ↓
[Continúa hasta que se detenga el servidor]
```

---

## 10. Variables Clave del Motor

| Variable | Tipo | Descripción |
|----------|------|-------------|
| `TICK_INTERVAL_MS` | `long` (50) | Intervalo entre ticks (50ms = 20 TPS) |
| `currentTick` | `long` | Contador de ticks desde el inicio |
| `running` | `boolean` | Si el motor está corriendo |
| `executor` | `ScheduledExecutorService` | Ejecutor que programa los ticks |
| `gameState` | `GameState` | Referencia al estado del juego |

**TPS (Ticks Per Second)**: 20 TPS significa que el servidor procesa 20 actualizaciones de lógica por segundo.
