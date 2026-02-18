# Extendiendo el Juego: Guía Completa para Desarrolladores - DroneWars

Este documento proporciona una guía exhaustiva para agregar nuevas funcionalidades al juego, incluyendo ejemplos detallados de implementación de movimiento de unidades y explicaciones profundas de cómo extender el sistema.

---

## 1. Implementación Completa: Sistema de Movimiento de Unidades

Vamos a implementar el movimiento de unidades paso a paso, tocando todas las capas del sistema.

### 1.1. **Paso 1: Agregar Eventos de Comunicación**

#### **Servidor (Java)**

**Archivo**: `src/main/java/udegames/dronewarsserver/websocket/CommunicationEvents.java`

```java
public interface CommunicationEvents {
    interface ClientToServerEvents {
        String REGISTER_PLAYER = "REGISTER_PLAYER";
        String GET_PLAYER_UNITS = "GET_PLAYER_UNITS";
        String SELECT_UNIT = "SELECT_UNIT";
        String MOVE_UNIT = "MOVE_UNIT"; // ✨ NUEVO
    }

    interface ServerToClientEvents {
        String PLAYER_REGISTERED = "PLAYER_REGISTERED";
        String UNITS_RECEIVED = "UNITS_RECEIVED";
        String UNIT_SELECTED = "UNIT_SELECTED";
        String SERVER_ERROR = "SERVER_ERROR";
        String AVAILABLE_PLAYERS = "AVAILABLE_PLAYERS";
        String MOVE_ACCEPTED = "MOVE_ACCEPTED"; // ✨ NUEVO
        String GAME_STATE_UPDATE = "GAME_STATE_UPDATE"; // ✨ NUEVO (actualizaciones periódicas)
    }
}
```

---

#### **Cliente (TypeScript)**

**Archivo**: `client/src/types/CommunicationEvents.ts`

```typescript
export enum ClientToServerEvents {
  REGISTER_PLAYER = 'REGISTER_PLAYER',
  GET_PLAYER_UNITS = 'GET_PLAYER_UNITS',
  SELECT_UNIT = 'SELECT_UNIT',
  MOVE_UNIT = 'MOVE_UNIT' // ✨ NUEVO
}

export enum ServerToClientEvents {
  PLAYER_REGISTERED = 'PLAYER_REGISTERED',
  UNITS_RECEIVED = 'UNITS_RECEIVED',
  UNIT_SELECTED = 'UNIT_SELECTED',
  SERVER_ERROR = 'SERVER_ERROR',
  AVAILABLE_PLAYERS = 'AVAILABLE_PLAYERS',
  MOVE_ACCEPTED = 'MOVE_ACCEPTED', // ✨ NUEVO
  GAME_STATE_UPDATE = 'GAME_STATE_UPDATE' // ✨ NUEVO
}
```

---

### 1.2. **Paso 2: Extender el Modelo de Dominio (Servidor)**

#### **Agregar `targetPosition` a la clase `Unit`**

**Archivo**: `src/main/java/udegames/dronewarsserver/domain/model/Unit.java`

```java
public abstract class Unit {
    protected String id;
    protected String ownerId;
    protected Position position;
    protected Position targetPosition; // ✨ NUEVO
    protected boolean destroyed;
    protected UnitType type;
    protected float speed; // ✨ NUEVO (unidades/segundo)

    // Getters y setters
    public Position getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Position targetPosition) {
        this.targetPosition = targetPosition;
    }

    public boolean hasTarget() {
        return targetPosition != null;
    }

    public void clearTarget() {
        this.targetPosition = null;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
```

**¿Por qué `targetPosition`?**
- Representa el **destino deseado** de la unidad.
- El `GameEngine` se encarga de mover `position` hacia `targetPosition` en cada tick.
- Separar el destino de la posición actual permite simulación suave.

---

### 1.3. **Paso 3: Crear el DTO para Movimiento**

**Archivo**: `src/main/java/udegames/dronewarsserver/dto/MoveUnitDTO.java` (NUEVO)

```java
public class MoveUnitDTO {
    private String unitId;
    private float targetX;
    private float targetY;

    // Constructores, getters, setters
    public MoveUnitDTO() {}

    public MoveUnitDTO(String unitId, float targetX, float targetY) {
        this.unitId = unitId;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // Getters y setters
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public float getTargetX() { return targetX; }
    public void setTargetX(float targetX) { this.targetX = targetX; }
    public float getTargetY() { return targetY; }
    public void setTargetY(float targetY) { this.targetY = targetY; }
}
```

---

**Archivo**: `src/main/java/udegames/dronewarsserver/dto/UnitPositionDTO.java` (NUEVO)

```java
public class UnitPositionDTO {
    private String unitId;
    private Position position;

    public UnitPositionDTO(String unitId, Position position) {
        this.unitId = unitId;
        this.position = position;
    }

    // Getters y setters
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
}
```

---

### 1.4. **Paso 4: Crear el Servicio de Movimiento**

**Archivo**: `src/main/java/udegames/dronewarsserver/service/IMovementService.java` (NUEVO)

```java
public interface IMovementService {
    boolean canMoveUnit(String unitId, String playerId, Position target);
    void setUnitTarget(String unitId, Position target);
}
```

---

**Archivo**: `src/main/java/udegames/dronewarsserver/service/MovementService.java` (NUEVO)

```java
@Service
public class MovementService implements IMovementService {
    private final GameState gameState;

    public MovementService(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean canMoveUnit(String unitId, String playerId, Position target) {
        // 1. Verifica que la unidad exista
        Unit unit = gameState.getUnitById(unitId);
        if (unit == null) {
            return false;
        }

        // 2. Verifica que la unidad pertenezca al jugador
        if (!unit.getOwnerId().equals(playerId)) {
            return false;
        }

        // 3. Verifica que la unidad esté viva
        if (unit.isDestroyed()) {
            return false;
        }

        // 4. Verifica que el destino esté dentro de los límites del mapa (ejemplo: 0-200)
        if (target.getX() < 0 || target.getX() > 200 || target.getY() < 0 || target.getY() > 200) {
            return false;
        }

        // 5. Validaciones adicionales (colisiones con terreno, rango máximo, etc.)
        // ... (futuro)

        return true;
    }

    @Override
    public void setUnitTarget(String unitId, Position target) {
        Unit unit = gameState.getUnitById(unitId);
        if (unit != null) {
            unit.setTargetPosition(target);
        }
    }
}
```

**Flujo**:
1. Valida que la unidad exista, pertenezca al jugador y esté viva.
2. Valida que el destino esté dentro de los límites del mapa.
3. Marca el `targetPosition` de la unidad.

---

### 1.5. **Paso 5: Agregar Handler en WebSocket**

**Archivo**: `src/main/java/udegames/dronewarsserver/websocket/GameWebSocketHandler.java`

```java
public class GameWebSocketHandler extends TextWebSocketHandler {
    // ... campos existentes ...
    private final IMovementService movementService; // ✨ NUEVO

    public GameWebSocketHandler(ISelectionService selectionService, GameState gameState, IMovementService movementService) {
        this.selectionService = selectionService;
        this.gameState = gameState;
        this.movementService = movementService; // ✨ NUEVO
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String messageType = root.get("type").asText();
            logger.info("Message received, type: {}, session: {}", messageType, session.getId());

            switch (messageType) {
                case CommunicationEvents.ClientToServerEvents.REGISTER_PLAYER:
                    handleRegisterPlayer(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.SELECT_UNIT:
                    handleSelectUnit(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.GET_PLAYER_UNITS:
                    handleGetPlayerUnits(session, root);
                    break;
                case CommunicationEvents.ClientToServerEvents.MOVE_UNIT: // ✨ NUEVO
                    handleMoveUnit(session, root);
                    break;
                default:
                    sendErrorMessage(session, "Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message.getPayload());
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }

    // ✨ NUEVO
    private void handleMoveUnit(WebSocketSession session, JsonNode root) throws IOException {
        String playerId = sessionToPlayerId.get(session.getId());

        if (playerId == null) {
            logger.warn("Player not registered on session: {}", session.getId());
            sendErrorMessage(session, "Player not registered");
            return;
        }

        String unitId = root.get("unitId").asText();
        float targetX = root.get("targetX").floatValue();
        float targetY = root.get("targetY").floatValue();

        // Obtiene la posición Z actual de la unidad (mantiene la altura)
        Unit unit = gameState.getUnitById(unitId);
        if (unit == null) {
            sendErrorMessage(session, "Unit not found");
            return;
        }

        Position target = new Position(targetX, targetY, unit.getPosition().getZ());

        // Valida que el movimiento sea legal
        if (!movementService.canMoveUnit(unitId, playerId, target)) {
            logger.warn("Invalid move: unitId={}, target=({}, {})", unitId, targetX, targetY);
            sendErrorMessage(session, "Invalid move");
            return;
        }

        // Marca el destino en la unidad
        movementService.setUnitTarget(unitId, target);

        // Envía confirmación al cliente
        sendResponse(session, CommunicationEvents.ServerToClientEvents.MOVE_ACCEPTED, unitId);

        logger.info("Move accepted: unitId={}, target=({}, {})", unitId, targetX, targetY);
    }
}
```

**Flujo**:
1. Extrae `unitId`, `targetX`, `targetY` del JSON.
2. Valida con `movementService.canMoveUnit()`.
3. Si es válido, marca el destino con `movementService.setUnitTarget()`.
4. Envía confirmación `MOVE_ACCEPTED` al cliente.
5. **Nota importante**: El servidor NO mueve la unidad inmediatamente. El `GameEngine` lo hará en el próximo tick.

---

### 1.6. **Paso 6: Modificar `GameEngine.update()` para Procesar Movimiento**

**Archivo**: `src/main/java/udegames/dronewarsserver/engine/GameEngine.java`

```java
public class GameEngine {
    private final GameState gameState;
    private final ScheduledExecutorService executor;
    private final GameWebSocketHandler webSocketHandler; // ✨ NUEVO
    private volatile boolean running;
    private long currentTick;

    private static final long TICK_INTERVAL_MS = 50;
    private static final float DELTA_TIME = TICK_INTERVAL_MS / 1000f; // 0.05 segundos
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);

    public GameEngine(GameState gameState, GameWebSocketHandler webSocketHandler) {
        this.gameState = gameState;
        this.webSocketHandler = webSocketHandler; // ✨ NUEVO
        // ... resto del constructor ...
    }

    private void update() {
        if (!running) return;

        currentTick++;

        // ✨ NUEVO: Actualiza posiciones de unidades
        updateUnitsPositions();

        // Cada 3 ticks (150ms), sincroniza con clientes
        if (currentTick % 3 == 0) {
            broadcastGameState();
        }
    }

    // ✨ NUEVO
    private void updateUnitsPositions() {
        gameState.getUnits().values().forEach(unit -> {
            if (unit.hasTarget()) {
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
                    logger.debug("Unit {} arrived at target ({}, {})", unit.getId(), target.getX(), target.getY());
                } else {
                    // Mueve un paso hacia el destino
                    float speed = unit.getSpeed(); // ej: 10 unidades/segundo
                    float step = Math.min(speed * DELTA_TIME, distance); // Máximo: distancia restante
                    float ratio = step / distance;

                    current.setX(current.getX() + dx * ratio);
                    current.setY(current.getY() + dy * ratio);
                }
            }
        });
    }

    // ✨ NUEVO
    private void broadcastGameState() {
        List<UnitPositionDTO> unitPositions = gameState.getUnits().values().stream()
                .map(unit -> new UnitPositionDTO(unit.getId(), unit.getPosition()))
                .toList();

        webSocketHandler.broadcastToAll(
                CommunicationEvents.ServerToClientEvents.GAME_STATE_UPDATE,
                unitPositions
        );

        logger.debug("Broadcasted game state to all clients: {} units", unitPositions.size());
    }
}
```

**Flujo**:
1. **`updateUnitsPositions()`**: Itera sobre todas las unidades que tienen `targetPosition`.
2. Calcula el vector de dirección `(dx, dy)` y la distancia.
3. Si la distancia es menor a 0.1, llegó al destino → actualiza posición y limpia `targetPosition`.
4. Si no, mueve un paso hacia el destino: `speed * DELTA_TIME`.
5. **`broadcastGameState()`**: Cada 3 ticks (150ms), envía las posiciones de todas las unidades a todos los clientes conectados.

---

#### **Agregar método `broadcastToAll` en `GameWebSocketHandler`**

**Archivo**: `src/main/java/udegames/dronewarsserver/websocket/GameWebSocketHandler.java`

```java
// ✨ NUEVO
public void broadcastToAll(String eventType, Object payload) {
    ServerResponseDTO response = new ServerResponseDTO(eventType, payload);
    String jsonResponse;

    try {
        jsonResponse = objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
        logger.error("Error serializing broadcast message", e);
        return;
    }

    TextMessage message = new TextMessage(jsonResponse);

    connectedSessions.forEach(session -> {
        if (session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                logger.error("Error broadcasting to session: {}", session.getId(), e);
            }
        }
    });

    logger.debug("Broadcasted {} to {} sessions", eventType, connectedSessions.size());
}
```

---

### 1.7. **Paso 7: Implementar Lado del Cliente**

#### **Agregar Método en `WebSocketClient`**

**Archivo**: `client/src/network/WebSocketClient.ts`

```typescript
// ✨ NUEVO
public requestMove(unitId: string, targetX: number, targetY: number): void {
    this.send({
        type: ClientToServerEvents.MOVE_UNIT,
        unitId: unitId,
        targetX: targetX,
        targetY: targetY
    });
}
```

---

#### **Configurar Input de Mapa en `GameScene`**

**Archivo**: `client/src/scenes/GameScene.ts`

```typescript
async create() {
    // ... código existente ...

    this.setupEventListeners();
    this.setupMapInput(); // ✨ NUEVO
    this.drawUI();
}

// ✨ NUEVO
private setupMapInput(): void {
    // Detecta clics en el mapa (fondo)
    this.input.on('pointerdown', (pointer: Phaser.Input.Pointer) => {
        // Si hay una unidad seleccionada, envía comando de movimiento
        const selectedUnit = this.selectionManager?.getSelectedUnit();

        if (selectedUnit) {
            const worldX = pointer.worldX;
            const worldY = pointer.worldY;

            console.log(`[GameScene] Requesting move for ${selectedUnit.unitId} to (${worldX}, ${worldY})`);

            this.websocketClient?.requestMove(selectedUnit.unitId, worldX, worldY);
        }
    });
}
```

**Flujo**:
1. Escucha clics en el mapa (cualquier lugar).
2. Si hay una unidad seleccionada, envía un comando `MOVE_UNIT` al servidor con las coordenadas del clic.

---

#### **Configurar Listeners de Respuesta**

**Archivo**: `client/src/scenes/GameScene.ts`

```typescript
private setupEventListeners(): void {
    if (!this.websocketClient || !this.selectionManager) return;

    // ... listeners existentes ...

    // ✨ NUEVO: Confirmación de movimiento aceptado
    this.websocketClient.on(ServerToClientEvents.MOVE_ACCEPTED, (unitId: string) => {
        console.log(`[GameScene] Move accepted for unit: ${unitId}`);
        // Opcional: Mostrar indicador visual (ej: partícula, línea)
    });

    // ✨ NUEVO: Actualización de posiciones del servidor
    this.websocketClient.on(ServerToClientEvents.GAME_STATE_UPDATE, (unitPositions) => {
        this.updateUnitPositions(unitPositions);
    });
}

// ✨ NUEVO
private updateUnitPositions(unitPositions: any[]): void {
    unitPositions.forEach(data => {
        const sprite = this.unitSprites.get(data.unitId);

        if (sprite) {
            // Interpolación suave usando Phaser Tweens
            this.tweens.add({
                targets: sprite,
                x: data.position.x,
                y: data.position.y,
                duration: 150, // Duración de la animación (150ms)
                ease: 'Linear'
            });
        }
    });
}
```

**Flujo**:
1. Escucha `MOVE_ACCEPTED` (confirmación del servidor).
2. Escucha `GAME_STATE_UPDATE` (actualizaciones periódicas cada 150ms).
3. Actualiza los sprites con animación suave usando Phaser Tweens.

**¿Por qué interpolación?**
- El servidor envía actualizaciones cada 150ms.
- Sin interpolación, el sprite "saltaría" de una posición a otra (efecto de parpadeo).
- Con interpolación, el sprite se mueve suavemente entre las posiciones.

---

## 2. Diagrama de Flujo Completo: Movimiento de Unidad

```
[CLIENTE]
Usuario hace clic en el mapa (x: 50, y: 50)
    ↓
GameScene.input.on('pointerdown') detecta clic
    ↓
Verifica si hay unidad seleccionada (selectedUnit)
    ↓
WebSocketClient.requestMove(unitId, 50, 50)
    ↓
Envía JSON: { type: "MOVE_UNIT", unitId: "...", targetX: 50, targetY: 50 }
    ↓
━━━━━━━━━━━━━━━━━━ [VIAJE POR LA RED] ━━━━━━━━━━━━━━━━━━
    ↓
[SERVIDOR]
GameWebSocketHandler.handleTextMessage()
    ↓
Identifica tipo: MOVE_UNIT → handleMoveUnit()
    ↓
Extrae: unitId, targetX, targetY
    ↓
MovementService.canMoveUnit(unitId, playerId, target)
    ↓
Validaciones:
  ├─ ¿Unidad existe?
  ├─ ¿Pertenece al jugador?
  ├─ ¿Está viva?
  └─ ¿Destino dentro de límites del mapa?
    ↓
MovementService.setUnitTarget(unitId, target)
    ↓
unit.setTargetPosition(new Position(50, 50, z))
    ↓
Envía MOVE_ACCEPTED al cliente
    ↓
━━━━━━━━━━━━━━━━━ [VIAJE POR LA RED] ━━━━━━━━━━━━━━━━━━
    ↓
[CLIENTE]
GameScene recibe MOVE_ACCEPTED
    ↓
console.log("Move accepted for unit: ...")
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[SERVIDOR - GameEngine.update() cada 50ms]
GameEngine.update() se ejecuta
    ↓
updateUnitsPositions()
    ↓
Itera sobre todas las unidades
    ↓
¿unit.hasTarget() == true?
    ↓ (Sí)
Calcula distancia actual → destino
    ↓
Mueve position hacia targetPosition (speed * 0.05)
    ↓
[Cada 3 ticks (150ms)]
    ↓
broadcastGameState()
    ↓
Envía GAME_STATE_UPDATE con todas las posiciones
    ↓
━━━━━━━━━━━━━━━━━ [VIAJE POR LA RED] ━━━━━━━━━━━━━━━━━━
    ↓
[CLIENTE]
GameScene recibe GAME_STATE_UPDATE
    ↓
updateUnitPositions(unitPositions)
    ↓
Para cada unidad:
  ├─ Obtiene sprite
  └─ Inicia Tween (animación suave 150ms)
    ↓
Sprite se mueve visualmente hacia la nueva posición
```

---

## 3. Guía para Agregar Nuevos Tipos de Unidades

### 3.1. **Paso 1: Crear la Clase de Dominio**

**Archivo**: `src/main/java/udegames/dronewarsserver/domain/model/NavalDrone.java` (NUEVO)

```java
public class NavalDrone extends Drone {
    private float torpedoRange;
    private int torpedoCount;

    public NavalDrone(String carrierId, float fuel, int armor, String ownerId, int level, Position position) {
        super(carrierId, fuel, armor, ownerId, level, position);
        this.type = UnitType.NAVAL_DRONE;
        this.speed = 5f; // Más lento que drones aéreos
        this.torpedoRange = 15f;
        this.torpedoCount = 3;
    }

    // Getters y setters específicos
    public float getTorpedoRange() { return torpedoRange; }
    public void setTorpedoRange(float torpedoRange) { this.torpedoRange = torpedoRange; }
    public int getTorpedoCount() { return torpedoCount; }
    public void setTorpedoCount(int torpedoCount) { this.torpedoCount = torpedoCount; }

    // Método específico
    public boolean canLaunchTorpedo() {
        return torpedoCount > 0 && !isDestroyed();
    }
}
```

---

### 3.2. **Paso 2: Agregar el ENUM**

**Archivo**: `src/main/java/udegames/dronewarsserver/domain/enums/UnitType.java`

```java
public enum UnitType {
    AERIAL_DRONE,
    AERIAL_CARRIER,
    NAVAL_DRONE, // ✨ NUEVO
    NAVAL_CARRIER
}
```

---

### 3.3. **Paso 3: Crear Unidades en `GameEngine`**

**Archivo**: `src/main/java/udegames/dronewarsserver/engine/GameEngine.java`

```java
private void createPlayerUnits(Player player, String carrierId, float startingX, float startingY) {
    // ... unidades existentes ...

    // ✨ NUEVO: Drone Naval
    Position navalDronePosition = new Position(startingX + 10f, startingY, 0f); // z=0 (agua)
    NavalDrone navalDrone = new NavalDrone(carrierId, 100f, 2, player.getId(), 1, navalDronePosition);
    gameState.addUnit(navalDrone);

    logger.debug("NavalDrone created: {} in ({}, {}, {})", navalDrone.getId(), navalDronePosition.getX(), navalDronePosition.getY(), navalDronePosition.getZ());
}
```

---

### 3.4. **Paso 4: Mapear en el DTO**

**Archivo**: `src/main/java/udegames/dronewarsserver/mapper/UnitMapper.java`

```java
public class UnitMapper {
    public static UnitSelectionDTO toSelectionDTO(Unit unit) {
        UnitSelectionDTO dto = new UnitSelectionDTO();
        dto.setUnitId(unit.getId());
        dto.setType(unit.getType()); // Incluye el nuevo NAVAL_DRONE
        dto.setOwnerId(unit.getOwnerId());
        dto.setPosition(unit.getPosition());

        // Agrega atributos específicos si es NavalDrone
        if (unit instanceof NavalDrone) {
            NavalDrone navaldrone = (NavalDrone) unit;
            dto.setTorpedoCount(navalDrone.getTorpedoCount()); // ✨ NUEVO campo en DTO
        }

        return dto;
    }
}
```

---

### 3.5. **Paso 5: Visualización en Cliente**

**Archivo**: `client/src/scenes/GameScene.ts`

```typescript
private getUnitColor(type: string): number {
    const colors: { [key: string]: number } = {
        'AERIAL_DRONE': 0xff0000,     // Rojo
        'NAVAL_DRONE': 0x0000ff,      // Azul ✨ NUEVO
        'AERIAL_CARRIER': 0xffff00,   // Amarillo
        'NAVAL_CARRIER': 0x00ff00     // Verde
    };
    return colors[type] || 0xffffff;
}

private getUnitLabel(type: string): string {
    const labels: { [key: string]: string } = {
        'AERIAL_DRONE': 'A.D',
        'NAVAL_DRONE': 'N.D', // ✨ NUEVO
        'AERIAL_CARRIER': 'A.C',
        'NAVAL_CARRIER': 'N.C'
    };
    return labels[type] || '?';
}
```

---

## 4. Anatomía Detallada del Cliente: `WebSocketClient`

### 4.1. **Sistema Pub/Sub Interno**

El `WebSocketClient` implementa un patrón **Publicador/Suscriptor** (Pub/Sub) para desacoplar la capa de red de la lógica del juego.

**Flujo**:
1. **Suscripción** (`on`): Los componentes se suscriben a eventos específicos.
2. **Publicación** (`emit`): Cuando llega un mensaje del servidor, el cliente lo "publica" localmente.
3. **Ejecución** (`callbacks`): Todos los suscriptores ejecutan sus callbacks.

---

### 4.2. **Método `on(event, callback)` - Suscripción**

```typescript
private eventListeners: Map<string, EventCallback[]> = new Map();

public on(event: string, callback: EventCallback): void {
    if (!this.eventListeners.has(event)) {
        this.eventListeners.set(event, []); // Crea array vacío si no existe
    }
    this.eventListeners.get(event)?.push(callback); // Agrega callback al array
}
```

**Ejemplo de uso**:
```typescript
this.websocketClient.on(ServerToClientEvents.UNIT_SELECTED, (unit: IUnit) => {
    console.log("Unit selected:", unit.unitId);
    this.highlightUnit(unit.unitId);
});
```

**Estructura interna**:
```
eventListeners = Map {
    "UNIT_SELECTED" => [callback1, callback2, callback3],
    "UNITS_RECEIVED" => [callback1],
    "MOVE_ACCEPTED" => [callback1, callback2]
}
```

---

### 4.3. **Método `emit(event, data)` - Publicación**

```typescript
private emit(event: string, data?: any): void {
    const callbacks = this.eventListeners.get(event); // Obtiene array de callbacks

    if (callbacks) {
        callbacks.forEach(callback => callback(data)); // Ejecuta cada callback
    }
}
```

**Flujo**:
1. Obtiene el array de callbacks para el evento.
2. Itera sobre el array y ejecuta cada callback con el `data`.

**Ejemplo**:
```typescript
// Cuando llega un mensaje del servidor
this.emit(ServerToClientEvents.UNIT_SELECTED, unitData);

// Esto ejecuta:
callback1(unitData);
callback2(unitData);
callback3(unitData);
```

---

### 4.4. **Método `handleMessage(eventData)` - Receptor Central**

```typescript
private handleMessage(eventData: string): void {
    try {
        const data = JSON.parse(eventData); // 1. Parsea el JSON

        // 2. Identifica el tipo de mensaje
        if (data.type === ServerToClientEvents.AVAILABLE_PLAYERS) {
            this.emit(ServerToClientEvents.AVAILABLE_PLAYERS, data.payload); // 3. Emite evento local
            return;
        }

        if (data.type === ServerToClientEvents.UNITS_RECEIVED) {
            this.emit(ServerToClientEvents.UNITS_RECEIVED, data.payload);
            return;
        }

        // ... más casos ...

        // Fallback: emite cualquier tipo desconocido
        if (data.type) {
            this.emit(data.type, data);
        }
    } catch (err) {
        console.log("[WebSocket] Error parsing message:", err);
    }
}
```

**Flujo completo**:
```
Servidor envía: { "type": "UNIT_SELECTED", "payload": { "unitId": "..." } }
    ↓
WebSocket.onmessage recibe el string JSON
    ↓
handleMessage(eventData)
    ↓
JSON.parse(eventData) → Objeto JavaScript
    ↓
Identifica data.type === "UNIT_SELECTED"
    ↓
this.emit("UNIT_SELECTED", data.payload)
    ↓
Ejecuta todos los callbacks registrados para "UNIT_SELECTED"
    ↓
GameScene recibe el evento y actualiza la UI
```

---

### 4.5. **Método `send(message)` - Envío de Mensajes**

```typescript
private send(message: any): void {
    if (!this.isConnected) {
        console.log("[WebSocket] Cannot send message, not connected to server");
        return;
    }

    try {
        this.socket?.send(JSON.stringify(message)); // Serializa a JSON y envía
    } catch (err) {
        console.log("[WebSocket] Error sending message:", err);
    }
}
```

**Ejemplo de uso**:
```typescript
this.send({
    type: ClientToServerEvents.MOVE_UNIT,
    unitId: "aerial-drone-001",
    targetX: 50,
    targetY: 50
});

// JSON enviado:
// {"type":"MOVE_UNIT","unitId":"aerial-drone-001","targetX":50,"targetY":50}
```

---

## 5. Patrones de Diseño Utilizados

### 5.1. **Patrón Pub/Sub (Publicador/Suscriptor)**

**Ubicación**: `WebSocketClient`, `SelectionManager`

**Ventajas**:
- Desacoplamiento: Los componentes no necesitan conocerse entre sí.
- Escalabilidad: Agregar nuevos listeners es trivial.

**Ejemplo**:
```typescript
// Publicador (WebSocketClient)
this.emit(ServerToClientEvents.UNIT_SELECTED, unitData);

// Suscriptores (GameScene, SelectionManager)
websocketClient.on(ServerToClientEvents.UNIT_SELECTED, (unit) => { ... });
selectionManager.on(ClientInternalEvents.SELECTION_CHANGED, (unit) => { ... });
```

---

### 5.2. **Patrón DTO (Data Transfer Object)**

**Ubicación**: `UnitSelectionDTO`, `GameUnitsDTO`, `ServerResponseDTO`

**Propósito**: Transferir datos entre capas sin exponer la lógica interna del dominio.

**Ventajas**:
- Seguridad: No expone métodos o lógica de negocio.
- Optimización: Solo envía datos necesarios (no toda la entidad).

**Ejemplo**:
```java
// Entidad de dominio (NO se envía directamente)
Unit unit = new AerialDrone(...);

// DTO (solo datos necesarios)
UnitSelectionDTO dto = UnitMapper.toSelectionDTO(unit);
// dto contiene: unitId, type, ownerId, position (sin métodos)
```

---

### 5.3. **Patrón Service Layer**

**Ubicación**: `SelectionService`, `MovementService`

**Propósito**: Encapsular la lógica de negocio fuera del controlador (WebSocketHandler).

**Ventajas**:
- Reutilización: La lógica puede ser usada por múltiples controladores.
- Testabilidad: Fácil de testear de forma aislada.
- Separación de responsabilidades: El handler solo orquesta, el servicio contiene lógica.

**Ejemplo**:
```java
// Handler (orquestador)
if (!movementService.canMoveUnit(unitId, playerId, target)) {
    sendErrorMessage(session, "Invalid move");
    return;
}

// Servicio (lógica)
@Override
public boolean canMoveUnit(String unitId, String playerId, Position target) {
    // Validaciones complejas aquí
    // ...
}
```

---

## 6. Checklist para Agregar Nuevas Funcionalidades

### ✅ **Lado del Servidor**

1. **Eventos de Comunicación**:
   - [ ] Agregar constante en `CommunicationEvents.ClientToServerEvents`.
   - [ ] Agregar constante en `CommunicationEvents.ServerToClientEvents` (si hay respuesta).

2. **Modelo de Dominio**:
   - [ ] Extender clases existentes o crear nuevas en `domain/model/`.
   - [ ] Agregar campos necesarios (getters/setters).

3. **DTO**:
   - [ ] Crear DTOs necesarios en `dto/`.
   - [ ] Actualizar `UnitMapper` si es necesario.

4. **Servicio**:
   - [ ] Crear interfaz `I<Nombre>Service` en `service/`.
   - [ ] Implementar `<Nombre>Service` con lógica de negocio.
   - [ ] Anotar con `@Service`.

5. **WebSocket Handler**:
   - [ ] Agregar caso en `switch` de `handleTextMessage`.
   - [ ] Crear método `handle<Nombre>(session, root)`.
   - [ ] Validar, delegar al servicio, enviar respuesta.

6. **GameEngine** (si requiere simulación):
   - [ ] Agregar lógica en `update()`.
   - [ ] Actualizar `GameState` según sea necesario.

---

### ✅ **Lado del Cliente**

1. **Eventos de Comunicación**:
   - [ ] Agregar constante en `ClientToServerEvents` (TypeScript).
   - [ ] Agregar constante en `ServerToClientEvents` (TypeScript).

2. **WebSocket Client**:
   - [ ] Agregar método público (ej: `requestAttack()`).
   - [ ] Agregar caso en `handleMessage()` si es nuevo tipo de servidor.

3. **GameScene**:
   - [ ] Agregar listener en `setupEventListeners()`.
   - [ ] Agregar input handler (si es necesario).
   - [ ] Actualizar UI/sprites según respuesta del servidor.

4. **Manager** (si es necesario):
   - [ ] Crear/extender managers (ej: `CombatManager`).
   - [ ] Implementar lógica de estado local.

---

## 7. Ejemplo Futuro: Sistema de Combate (Boceto)

### **Cliente solicita ataque**:
```typescript
this.websocketClient.requestAttack(attackerUnitId, targetUnitId);
```

### **Servidor valida y procesa**:
```java
private void handleAttack(WebSocketSession session, JsonNode root) {
    String attackerId = root.get("attackerId").asText();
    String targetId = root.get("targetId").asText();

    if (!combatService.canAttack(attackerId, targetId, playerId)) {
        sendErrorMessage(session, "Invalid attack");
        return;
    }

    CombatResult result = combatService.executeAttack(attackerId, targetId);

    broadcastToAll(CommunicationEvents.ServerToClientEvents.COMBAT_RESULT, result);
}
```

### **Cliente visualiza resultado**:
```typescript
this.websocketClient.on(ServerToClientEvents.COMBAT_RESULT, (result) => {
    this.showCombatAnimation(result.attackerId, result.targetId, result.damage);
    this.updateHealthBars();
});
```

---

## 8. Mejores Prácticas

1. **Validar siempre en el servidor**: Nunca confíes en el cliente.
2. **Usar DTOs**: No expongas entidades de dominio directamente.
3. **Separar lógica en servicios**: Mantén los handlers delgados.
4. **Sincronización periódica**: Envía actualizaciones cada X ticks, no en cada cambio.
5. **Interpolación en cliente**: Suaviza el movimiento para ocultar latencia.
6. **Logging exhaustivo**: Usa SLF4J en servidor, `console.log` en cliente.
7. **Manejo de errores**: Envía mensajes de error claros al cliente.

---

## 9. Recursos Adicionales

- **Documentación de Phaser**: https://photonstorm.github.io/phaser3-docs/
- **Documentación de Spring WebSocket**: https://docs.spring.io/spring-framework/reference/web/websocket.html
- **Patrones de diseño en juegos multijugador**: https://developer.valvesoftware.com/wiki/Source_Multiplayer_Networking

---