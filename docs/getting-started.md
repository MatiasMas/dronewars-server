# Guía de Inicio y Flujos de Ejecución - DroneWars

Este documento detalla el flujo de ejecución completo desde que se inicia el servidor y el cliente hasta que se establece la comunicación y se muestran las unidades en pantalla.

---

## 1. Inicialización del Servidor (Fase por Fase)

### 1.1. **Spring Boot Inicia la Aplicación**

**Punto de entrada**: `DroneWarsServerApplication.java`

```java
@SpringBootApplication
public class DroneWarsServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DroneWarsServerApplication.class, args);
    }
}
```

**Flujo**:
1. Spring Boot escanea todos los componentes anotados con `@Configuration`, `@Bean`, `@Service`, `@Component`.
2. Construye el contenedor de inyección de dependencias (IoC Container).
3. Inicializa los beans en el siguiente orden:
   - `GameState` → `GameEngine` → `SelectionService` → `GameWebSocketHandler`

---

### 1.2. **Creación del `GameState`**

**Archivo**: `GameConfig.java:18-21`

```java
@Bean
public GameState gameState() {
    return new GameState("game-001");
}
```

**Flujo**:
1. Crea una instancia única de `GameState` con ID `"game-001"`.
2. Inicializa `ConcurrentHashMap` para jugadores y unidades.
3. Guarda el timestamp de creación: `System.currentTimeMillis()`.

---

### 1.3. **Creación del `GameEngine`**

**Archivo**: `GameConfig.java:28-50`

```java
@Bean
public GameEngine gameEngine(GameState gameState) {
    GameEngine gameEngine = new GameEngine(gameState);

    // Fase 1: create() - Inicializa jugadores y unidades
    gameEngine.create();

    // Fase 2: start() - Arranca el motor en un hilo separado
    Thread engineStarterThread = new Thread(() -> {
        try {
            Thread.sleep(1000); // Espera a que Spring termine
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

**Flujo detallado**:

1. **Inyección de dependencias**: Spring inyecta el `GameState` creado anteriormente.
2. **Llamada a `create()`**:
   - `createPlayers()`: Crea 2 jugadores (`player_1`, `player_2`).
   - `createUnits()`: Crea 3 unidades por jugador (2 drones aéreos + 1 carrier).
3. **Hilo separado**:
   - Espera 1 segundo (1000ms) para asegurar que todos los beans estén listos.
   - Llama a `gameEngine.start()`.
4. **Inicio del bucle de ticks**: `start()` programa `update()` para ejecutarse cada 50ms.

**Resultado**: El motor del juego está corriendo, ejecutando `update()` 20 veces por segundo.

---

### 1.4. **Fase `create()`: Inicialización del Mundo**

#### **Creación de Jugadores**

**Archivo**: `GameEngine.java:110-123`

```java
private void createPlayers() {
    Player player1 = new Player("Player 1");
    Player player2 = new Player("Player 2");

    player1.setId("player_1");
    player2.setId("player_2");

    gameState.addPlayer(player1);
    gameState.addPlayer(player2);

    logger.debug("2 players created: {}", gameState.getPlayers());
}
```

**Resultado**: `GameState` ahora tiene 2 jugadores:
- `player_1` (nombre: "Player 1")
- `player_2` (nombre: "Player 2")

---

#### **Creación de Unidades**

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

    createPlayerUnits(player1, "carrier-p1", 10f, 10f);
    createPlayerUnits(player2, "carrier-p2", 100f, 100f);

    logger.debug("Units created for players: {}", gameState.getPlayers());
}
```

**Flujo**:
1. Obtiene la lista de jugadores del `GameState`.
2. Valida que haya al menos 2 jugadores.
3. Llama a `createPlayerUnits()` para cada jugador con posiciones de inicio diferentes:
   - `player_1`: Posición (10, 10)
   - `player_2`: Posición (100, 100)

---

#### **Creación de Unidades por Jugador**

**Archivo**: `GameEngine.java:143-162`

```java
private void createPlayerUnits(Player player, String carrierId, float startingX, float startingY) {
    // Drone 1
    Position aerialDrone1Position = new Position(startingX, startingY, 5f);
    AerialDrone aerialDrone1 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone1Position);
    gameState.addUnit(aerialDrone1);

    // Drone 2
    Position aerialDrone2Position = new Position(startingX + 5f, startingY + 5f, 5f);
    AerialDrone aerialDrone2 = new AerialDrone(carrierId, 100f, 1, player.getId(), 1, aerialDrone2Position);
    gameState.addUnit(aerialDrone2);

    // Carrier
    Position aerialCarrierPosition = new Position(startingX - 5f, startingY - 5f, 5f);
    AerialCarrier aerialCarrier = new AerialCarrier(12, player.getId(), 6, aerialCarrierPosition);
    gameState.addUnit(aerialCarrier);
}
```

**Resultado para `player_1`** (inicio en 10, 10):
- `AerialDrone 1`: Posición (10, 10, 5)
- `AerialDrone 2`: Posición (15, 15, 5)
- `AerialCarrier`: Posición (5, 5, 5)

**Resultado para `player_2`** (inicio en 100, 100):
- `AerialDrone 1`: Posición (100, 100, 5)
- `AerialDrone 2`: Posición (105, 105, 5)
- `AerialCarrier`: Posición (95, 95, 5)

**Total**: 6 unidades en el `GameState` (3 por jugador).

---

### 1.5. **Fase `start()`: Arranque del Bucle de Ticks**

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

**Flujo**:
1. Marca `running = true`.
2. Programa la ejecución periódica de `update()` cada 50ms.
3. El método `update()` comienza a ejecutarse 20 veces por segundo.

**Estado final del servidor**:
- ✅ GameState con 2 jugadores y 6 unidades.
- ✅ Motor corriendo (ticks activos).
- ✅ WebSocket endpoint listo en `ws://localhost:8081/game`.

---

## 2. Inicialización del Cliente (Paso a Paso)

### 2.1. **Phaser Inicia `GameScene`**

**Archivo**: `GameScene.ts:17-50`

```typescript
async create() {
    console.log("[GameScene] Creating scene...");

    // 1. Crea el cliente WebSocket
    this.websocketClient = new WebSocketClient();

    try {
        // 2. Conecta al servidor
        await this.websocketClient.connect();
    } catch (err) {
        console.error("[GameScene] Error connecting to server:", err);
        this.showError("Error connecting to server");
        return;
    }

    // 3. Crea el manager de selección
    this.selectionManager = new SelectionManager();

    // 4. Espera lista de jugadores disponibles
    await this.waitForAvailablePlayers();

    // 5. Selecciona el primer jugador disponible
    const selectedPlayerId = this.selectFirstAvailablePlayer();
    if (!selectedPlayerId) {
        this.showError('No players available');
        return;
    }

    // 6. Registra el jugador en el servidor
    this.websocketClient.registerPlayer(selectedPlayerId);
    await this.waitForPlayerRegistration();

    // 7. Solicita las unidades del jugador
    this.websocketClient.requestPlayerUnits();

    // 8. Configura los listeners de eventos
    this.setupEventListeners();

    // 9. Dibuja la UI
    this.drawUI();
}
```

---

### 2.2. **Conexión WebSocket**

**Archivo**: `WebSocketClient.ts:19-51`

```typescript
public async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
        try {
            this.socket = new WebSocket(this.url); // ws://localhost:8081/game

            this.socket.onopen = () => {
                console.log("[WebSocket] Connected to server");
                this.isConnected = true;
                this.emit(ClientInternalEvents.CONNECTED);
                resolve();
            };

            this.socket.onclose = () => {
                console.log("[WebSocket] Disconnected from server");
                this.isConnected = false;
                this.emit(ClientInternalEvents.DISCONNECTED);
            };

            this.socket.onerror = (error: Event) => {
                console.error("[WebSocket] Error:", error);
                this.isConnected = false;
                this.emit(ClientInternalEvents.CONNECTION_ERROR, error);
            };

            this.socket.onmessage = (event: MessageEvent) => {
                this.handleMessage(event.data);
            };
        } catch (err) {
            reject(err);
        }
    });
}
```

**Flujo**:
1. Crea una conexión WebSocket a `ws://localhost:8081/game`.
2. Configura listeners para `onopen`, `onclose`, `onerror`, `onmessage`.
3. Cuando la conexión se establece, resuelve la promesa.

---

### 2.3. **Servidor Envía Lista de Jugadores Disponibles**

**¿Cuándo?**: Automáticamente cuando el cliente se conecta.

**Archivo (Servidor)**: `GameWebSocketHandler.java:43-49`

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    connectedSessions.add(session);
    logger.info("Client connected: {}", session.getId());

    // Envía lista de jugadores automáticamente
    sendAvailablePlayers(session);
}
```

**Archivo (Servidor)**: `GameWebSocketHandler.java:93-109`

```java
private void sendAvailablePlayers(WebSocketSession session) {
    try {
        List<AvailablePlayerDTO> availablePlayers = gameState.getPlayers().stream()
                .map(player -> new AvailablePlayerDTO(
                        player.getId(),
                        player.getName(),
                        !registeredPlayers.contains(player.getId()) // true si está disponible
                ))
                .toList();

        sendResponse(session, CommunicationEvents.ServerToClientEvents.AVAILABLE_PLAYERS, availablePlayers);

        logger.info("Available players sent to the client: {}", availablePlayers.size());
    } catch (IOException e) {
        logger.error("Error sending available players to the client", e);
    }
}
```

**Mensaje enviado**:
```json
{
  "type": "AVAILABLE_PLAYERS",
  "payload": [
    {
      "playerId": "player_1",
      "playerName": "Player 1",
      "available": true
    },
    {
      "playerId": "player_2",
      "playerName": "Player 2",
      "available": true
    }
  ]
}
```

---

### 2.4. **Cliente Recibe y Procesa Lista de Jugadores**

**Archivo**: `GameScene.ts:52-71`

```typescript
private async waitForAvailablePlayers(): Promise<void> {
    return new Promise(resolve => {
        const timeout = setTimeout(() => {
            console.warn("[GameScene] Available players timed out");
            resolve();
        }, 3000);

        this.websocketClient?.on(ServerToClientEvents.AVAILABLE_PLAYERS, (players: IAvailablePlayer[]) => {
            clearTimeout(timeout);
            this.availablePlayers = players;

            console.log(`[GameScene] ${players.length} players available`);
            players.forEach(player => {
                console.log(`- ${player.playerName} (${player.playerId}) - ${player.available ? 'Available' : 'Taken'}`);
            });

            resolve();
        })
    })
}
```

**Flujo**:
1. Espera hasta que llegue el evento `AVAILABLE_PLAYERS`.
2. Almacena la lista en `this.availablePlayers`.
3. Imprime en consola los jugadores disponibles.

**Output en consola**:
```
[GameScene] 2 players available
- Player 1 (player_1) - Available
- Player 2 (player_2) - Available
```

---

### 2.5. **Cliente Selecciona un Jugador**

**Archivo**: `GameScene.ts:73-82`

```typescript
private selectFirstAvailablePlayer(): string | null {
    const availablePlayer = this.availablePlayers.find(player => player.available);

    if (availablePlayer) {
        console.log(`[GameScene] Selecting player: ${availablePlayer.playerName}]`);
        return availablePlayer.playerId;
    }

    return null;
}
```

**Flujo**:
1. Busca el primer jugador con `available: true`.
2. Retorna su `playerId` (ej: `"player_1"`).

---

### 2.6. **Cliente Registra el Jugador en el Servidor**

**Archivo**: `WebSocketClient.ts:57-64`

```typescript
public registerPlayer(playerId: string): void {
    this.playerId = playerId;

    this.send({
        type: ClientToServerEvents.REGISTER_PLAYER,
        playerId: playerId
    });
}
```

**Mensaje enviado**:
```json
{
  "type": "REGISTER_PLAYER",
  "playerId": "player_1"
}
```

---

**Archivo (Servidor)**: `GameWebSocketHandler.java:111-132`

```java
private void handleRegisterPlayer(WebSocketSession session, JsonNode root) throws IOException {
    String playerId = root.get("playerId").asText();

    if (!gameState.doesPlayerExist(playerId)) {
        logger.error("Player does not exist in the game: {}", playerId);
        sendErrorMessage(session, "Player does not exist in the game: " + playerId);
        return;
    }

    if (registeredPlayers.contains(playerId)) {
        logger.warn("Player already registered in the game: {}", playerId);
        sendErrorMessage(session, "Player already registered in the game: " + playerId);
        return;
    }

    sessionToPlayerId.put(session.getId(), playerId); // Asocia sesión con jugador
    registeredPlayers.add(playerId); // Marca como registrado

    sendResponse(session, CommunicationEvents.ServerToClientEvents.PLAYER_REGISTERED, playerId);

    logger.info("Player correctly registered in session: {}", playerId);
}
```

**Flujo**:
1. Valida que el jugador exista en el `GameState`.
2. Valida que no esté ya registrado.
3. Asocia la sesión WebSocket con el `playerId` en el mapa `sessionToPlayerId`.
4. Agrega el `playerId` al conjunto `registeredPlayers`.
5. Envía confirmación al cliente.

**Mensaje enviado**:
```json
{
  "type": "PLAYER_REGISTERED",
  "payload": "player_1"
}
```

---

### 2.7. **Cliente Espera Confirmación de Registro**

**Archivo**: `GameScene.ts:84-97`

```typescript
private async waitForPlayerRegistration(): Promise<void> {
    return new Promise(resolve => {
        const timeout = setTimeout(() => {
            console.warn("[GameScene] Player registration timed out");
            resolve();
        }, 3000);

        this.websocketClient?.on(ServerToClientEvents.PLAYER_REGISTERED, () => {
            clearTimeout(timeout);
            console.log("[GameScene] Player registered successfully");
            resolve();
        });
    });
}
```

**Output en consola**:
```
[GameScene] Player registered successfully
```

---

### 2.8. **Cliente Solicita sus Unidades**

**Archivo**: `WebSocketClient.ts:70-74`

```typescript
public requestPlayerUnits(): void {
    this.send({
        type: ClientToServerEvents.GET_PLAYER_UNITS
    });
}
```

**Mensaje enviado**:
```json
{
  "type": "GET_PLAYER_UNITS"
}
```

---

**Archivo (Servidor)**: `GameWebSocketHandler.java:165-190`

```java
private void handleGetPlayerUnits(WebSocketSession session, JsonNode root) throws IOException {
    String playerId = sessionToPlayerId.get(session.getId());
    List<Unit> playerUnits;
    List<Unit> enemyUnits;

    if (playerId == null) {
        logger.warn("Player not registered on session: {}", session.getId());
        sendErrorMessage(session, "Player not registered on session: " + session.getId());
        return;
    }

    // Obtiene unidades del jugador y del enemigo
    playerUnits = gameState.getPlayerUnits(playerId);
    enemyUnits = gameState.getEnemyUnits(playerId);
    logger.debug("Player: {}, units: {}, enemy units: {}", playerId, playerUnits.size(), enemyUnits.size());

    // Convierte a DTOs
    List<UnitSelectionDTO> playerUnitDTOs = playerUnits.stream().map(UnitMapper::toSelectionDTO).toList();
    List<UnitSelectionDTO> enemyUnitDTOs = enemyUnits.stream().map(UnitMapper::toSelectionDTO).toList();

    GameUnitsDTO gameUnitsDTO = new GameUnitsDTO(playerUnitDTOs, enemyUnitDTOs);
    sendResponse(session, CommunicationEvents.ServerToClientEvents.UNITS_RECEIVED, gameUnitsDTO);

    logger.info("Units sent to {} client: player owns {}, enemy owns {}", playerId, playerUnitDTOs.size(), enemyUnitDTOs.size());
}
```

**Flujo**:
1. Obtiene el `playerId` asociado a la sesión desde `sessionToPlayerId`.
2. Consulta el `GameState` para obtener:
   - `playerUnits`: Unidades del jugador.
   - `enemyUnits`: Unidades de otros jugadores.
3. Convierte las unidades a DTOs mediante `UnitMapper.toSelectionDTO()`.
4. Envía ambas listas al cliente.

**Mensaje enviado**:
```json
{
  "type": "UNITS_RECEIVED",
  "payload": {
    "playerUnits": [
      {
        "unitId": "aerial-drone-001",
        "type": "AERIAL_DRONE",
        "ownerId": "player_1",
        "position": { "x": 10.0, "y": 10.0, "z": 5.0 }
      },
      {
        "unitId": "aerial-drone-002",
        "type": "AERIAL_DRONE",
        "ownerId": "player_1",
        "position": { "x": 15.0, "y": 15.0, "z": 5.0 }
      },
      {
        "unitId": "aerial-carrier-001",
        "type": "AERIAL_CARRIER",
        "ownerId": "player_1",
        "position": { "x": 5.0, "y": 5.0, "z": 5.0 }
      }
    ],
    "enemyUnits": [
      // 3 unidades de player_2
    ]
  }
}
```

---

### 2.9. **Cliente Renderiza las Unidades**

**Archivo**: `GameScene.ts:103-114`

```typescript
private setupEventListeners(): void {
    if (!this.websocketClient || !this.selectionManager) return;

    // Listener: Recepción de unidades
    this.websocketClient.on(ServerToClientEvents.UNITS_RECEIVED, (data: any) => {
        const playerUnits: IUnit[] = data.playerUnits;
        const enemyUnits: IUnit[] = data.enemyUnits;

        console.log(`[GameScene] Received ${playerUnits.length} units for player and ${enemyUnits.length} units for enemy from server`);
        this.selectionManager?.setPlayerUnits(playerUnits);
        this.renderUnits(playerUnits, enemyUnits);
    });

    // ... otros listeners ...
}
```

**Archivo**: `GameScene.ts:136-154`

```typescript
private renderUnits(playerUnits: IUnit[], enemyUnits: IUnit[]): void {
    const centerX = this.cameras.main.centerX;
    const centerY = this.cameras.main.centerY;

    // Renderiza unidades del jugador (izquierda)
    playerUnits.forEach((unit, index) => {
        const x = centerX - 350;
        const y = centerY - 150 + index * 120;
        this.createUnitSprite(unit, x, y, true); // true = unidad propia
    });

    // Renderiza unidades enemigas (derecha)
    enemyUnits.forEach((unit, index) => {
        const x = centerX + 250;
        const y = centerY - 150 + index * 120;
        this.createUnitSprite(unit, x, y, false); // false = unidad enemiga
    });

    console.log(`[GameScene] ${playerUnits.length} player units and ${enemyUnits.length} enemy units rendered`);
}
```

**Archivo**: `GameScene.ts:156-178`

```typescript
private createUnitSprite(unit: IUnit, x: number, y: number, isPlayerUnit: boolean): void {
    const sprite = this.add.rectangle(x, y, 60, 60, this.getUnitColor(unit.type));

    sprite.setStrokeStyle(2, isPlayerUnit ? 0x00ff00 : 0xff0000); // Verde = propio, Rojo = enemigo
    sprite.setInteractive({ useHandCursor: true });

    sprite.on('pointerdown', () => {
        if (isPlayerUnit) {
            this.selectionManager?.selectUnit(unit.unitId);
        }
    });

    sprite.on('pointerover', () => sprite.setScale(1.1)); // Hover
    sprite.on('pointerout', () => sprite.setScale(1));

    this.unitSprites.set(unit.unitId, sprite);

    // Agrega etiqueta de texto
    this.add.text(x, y, this.getUnitLabel(unit.type), {
        fontSize: '12px',
        color: '#ffffff',
        align: 'center'
    }).setOrigin(0.5);
}
```

**Resultado visual**:
- 3 sprites en el lado izquierdo (unidades del jugador, borde verde).
- 3 sprites en el lado derecho (unidades enemigas, borde rojo).
- Etiquetas: "A.D" (Aerial Drone), "A.C" (Aerial Carrier).

---

## 3. Diagrama de Flujo Completo: Inicialización

```
[SERVIDOR]
Spring Boot arranca
    ↓
GameConfig.gameState() → Crea GameState
    ↓
GameConfig.gameEngine(gameState)
    ↓
gameEngine.create()
    ↓
  ├─ createPlayers() → player_1, player_2
  └─ createUnits()   → 3 unidades/jugador
    ↓
gameEngine.start() → Bucle de ticks (cada 50ms)
    ↓
Servidor listo en ws://localhost:8081/game
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLIENTE]
Phaser inicia GameScene.create()
    ↓
websocketClient.connect()
    ↓
[CONEXIÓN ESTABLECIDA]
    ↓
Servidor automáticamente envía AVAILABLE_PLAYERS
    ↓
Cliente recibe lista de jugadores
    ↓
selectFirstAvailablePlayer() → "player_1"
    ↓
websocketClient.registerPlayer("player_1")
    ↓
Servidor valida y envía PLAYER_REGISTERED
    ↓
Cliente recibe confirmación
    ↓
websocketClient.requestPlayerUnits()
    ↓
Servidor consulta GameState y envía UNITS_RECEIVED
    ↓
Cliente renderiza sprites (3 propios + 3 enemigos)
    ↓
setupEventListeners() → Configura listeners de eventos
    ↓
drawUI() → Dibuja UI (título, instrucciones, etc.)
    ↓
[JUEGO LISTO PARA INTERACCIÓN]
```

---

## 4. Estado Final del Sistema

### **Servidor**:
- ✅ `GameState` con 2 jugadores y 6 unidades.
- ✅ Motor corriendo (20 ticks/seg).
- ✅ WebSocket Handler escuchando conexiones.
- ✅ 1 sesión conectada asociada a `player_1`.

### **Cliente**:
- ✅ Conexión WebSocket activa.
- ✅ Jugador registrado como `player_1`.
- ✅ 6 sprites renderizados (3 propios + 3 enemigos).
- ✅ Listeners configurados para eventos de servidor.
- ✅ `SelectionManager` listo para gestionar selecciones.

---

## 5. Ejemplo Completo: Flujo de Selección de Unidad (Revisado)

Ya cubierto en detalle en `architecture.md`, pero aquí un resumen:

```
Usuario hace clic en sprite
    ↓
GameScene.sprite.on('pointerdown')
    ↓
SelectionManager.selectUnit(unitId)
    ↓ (emite ClientInternalEvents.SELECTION_CHANGED)
GameScene escucha evento
    ↓
WebSocketClient.requestUnitSelection(unitId)
    ↓
[VIAJE POR LA RED]
    ↓
GameWebSocketHandler.handleSelectUnit()
    ↓
SelectionService.canSelectUnit() → Validación
    ↓
Servidor envía UNIT_SELECTED
    ↓
[VIAJE POR LA RED]
    ↓
WebSocketClient.handleMessage()
    ↓
GameScene escucha UNIT_SELECTED
    ↓
SelectionManager.confirmSelection()
    ↓
GameScene.highlightUnit() → Borde amarillo
```

---

## 6. Logs de Consola (Ejemplo Real)

### **Servidor (Spring Boot)**:
```
[GameConfig] Creating GameState: game-001
[GameEngine] [CREATE] Starting game: game-001
[GameEngine] 2 players created: [Player{id='player_1', name='Player 1'}, Player{id='player_2', name='Player 2'}]
[GameEngine] AerialDrone created: aerial-drone-001 in (10.0, 10.0, 5.0)
[GameEngine] AerialDrone created: aerial-drone-002 in (15.0, 15.0, 5.0)
[GameEngine] AerialCarrier created: aerial-carrier-001 in (5.0, 5.0, 5.0)
[GameEngine] AerialDrone created: aerial-drone-003 in (100.0, 100.0, 5.0)
[GameEngine] AerialDrone created: aerial-drone-004 in (105.0, 105.0, 5.0)
[GameEngine] AerialCarrier created: aerial-carrier-002 in (95.0, 95.0, 5.0)
[GameEngine] [CREATE] Game started
[GameConfig] Starting game engine...
[GameEngine] [START] Starting game engine ticks (50ms)
[GameConfig] Game engine started
[GameWebSocketHandler] Client connected: session-001
[GameWebSocketHandler] Available players sent to the client: 2
[GameWebSocketHandler] Message received, type: REGISTER_PLAYER, session: session-001
[GameWebSocketHandler] Player correctly registered in session: player_1
[GameWebSocketHandler] Message received, type: GET_PLAYER_UNITS, session: session-001
[GameWebSocketHandler] Units sent to player_1 client: player owns 3, enemy owns 3
```

### **Cliente (Browser Console)**:
```
[WebSocket] Connected to server
[GameScene] Creating scene...
[GameScene] 2 players available
- Player 1 (player_1) - Available
- Player 2 (player_2) - Available
[GameScene] Selecting player: Player 1]
[GameScene] Player registered successfully
[GameScene] Received 3 units for player and 3 units for enemy from server
[GameScene] 3 player units and 3 enemy units rendered
```

---

## 7. Resumen de Eventos de Red

| Evento | Dirección | Descripción |
|--------|-----------|-------------|
| `AVAILABLE_PLAYERS` | Servidor → Cliente | Lista de jugadores disponibles (automático) |
| `REGISTER_PLAYER` | Cliente → Servidor | Solicitud de registro de jugador |
| `PLAYER_REGISTERED` | Servidor → Cliente | Confirmación de registro |
| `GET_PLAYER_UNITS` | Cliente → Servidor | Solicitud de unidades del jugador |
| `UNITS_RECEIVED` | Servidor → Cliente | Lista de unidades propias y enemigas |
| `SELECT_UNIT` | Cliente → Servidor | Solicitud de selección de unidad |
| `UNIT_SELECTED` | Servidor → Cliente | Confirmación de selección |
| `SERVER_ERROR` | Servidor → Cliente | Mensaje de error |
