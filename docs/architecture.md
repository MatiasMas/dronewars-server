# Arquitectura y Flujo de Datos - DroneWars

Este documento proporciona una explicación exhaustiva de cómo interactúan las clases del cliente y del servidor, el flujo de comunicación a través de WebSockets, y cómo cada componente colabora para crear la experiencia de juego.

---

## 1. Modelo de Servidor Autoritativo

DroneWars implementa un **modelo de servidor autoritativo**, donde:

*   **Toda la lógica crítica** (movimiento, daño, colisiones, validaciones) se ejecuta exclusivamente en el servidor.
*   **El cliente actúa como un visor**: recibe el estado del juego y lo representa visualmente, pero no toma decisiones de gameplay.
*   **Las acciones del jugador son peticiones**: cuando un jugador selecciona una unidad o solicita un movimiento, el cliente envía una petición al servidor, que la valida y la procesa.

**¿Por qué este modelo?**
- **Prevención de trampas (anti-cheat)**: Los jugadores no pueden modificar su código cliente para obtener ventajas.
- **Consistencia**: Todos los jugadores ven el mismo estado del juego.
- **Autoridad central**: El servidor es la única fuente de verdad (source of truth).

---

## 2. Flujo de Comunicación (WebSockets)

La comunicación entre cliente y servidor se realiza mediante **mensajes JSON** a través de una **conexión WebSocket persistente** establecida en `ws://localhost:8080/game`.

### Diagrama de Flujo Genérico

```
┌─────────────┐                        ┌──────────────────┐
│   Cliente   │                        │     Servidor     │
│  (Phaser)   │                        │   (Spring WS)    │
└──────┬──────┘                        └────────┬─────────┘
       │                                        │
       │  1. Envía evento (JSON)               │
       │  { type: "SELECT_UNIT", unitId: "..." }
       ├───────────────────────────────────────>│
       │                                        │
       │                        2. handleTextMessage() recibe
       │                           y parsea el JSON
       │                                        │
       │                        3. Identifica tipo de evento
       │                           y llama al handler
       │                                        │
       │                        4. Servicio valida y consulta
       │                           GameState
       │                                        │
       │                        5. Construye respuesta (DTO)
       │                                        │
       │  6. Envía respuesta (JSON)            │
       │  { type: "UNIT_SELECTED", payload: {...} }
       │<───────────────────────────────────────┤
       │                                        │
       │  7. handleMessage() parsea            │
       │     y emite evento local              │
       │                                        │
       │  8. Listeners ejecutan callback       │
       │     (actualización visual)            │
```

---

## 3. Clases Principales de Comunicación

### 3.1. Lado del Servidor (Java)

#### **`GameWebSocketHandler` (src/main/java/.../websocket/GameWebSocketHandler.java)**

**Responsabilidad**: Punto de entrada de todos los mensajes WebSocket. Es el "receptor central" de eventos del cliente.

**Funcionamiento paso a paso**:

1. **Conexión establecida** (`afterConnectionEstablished`):
   ```java
   // GameWebSocketHandler.java:43-49
   @Override
   public void afterConnectionEstablished(WebSocketSession session) {
       connectedSessions.add(session);
       logger.info("Client connected: {}", session.getId());

       // Envía lista de jugadores disponibles automáticamente
       sendAvailablePlayers(session);
   }
   ```
   - Almacena la sesión en `connectedSessions`.
   - Automáticamente envía la lista de jugadores disponibles al cliente recién conectado.

2. **Recepción de mensajes** (`handleTextMessage`):
   ```java
   // GameWebSocketHandler.java:64-88
   @Override
   protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
       try {
           // 1. Parsea el JSON usando Jackson
           JsonNode root = objectMapper.readTree(message.getPayload());
           String messageType = root.get("type").asText();
           logger.info("Message received, type: {}, session: {}", messageType, session.getId());

           // 2. Switch para identificar el tipo de evento
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
               default:
                   sendErrorMessage(session, "Unknown message type: " + messageType);
           }
       } catch (Exception e) {
           logger.error("Error processing message: {}", message.getPayload());
           sendErrorMessage(session, "Error processing message: " + e.getMessage());
       }
   }
   ```

3. **Manejo de evento SELECT_UNIT** (`handleSelectUnit`):
   ```java
   // GameWebSocketHandler.java:134-163
   private void handleSelectUnit(WebSocketSession session, JsonNode root) throws IOException {
       // 1. Obtiene el ID del jugador asociado a esta sesión
       String playerId = sessionToPlayerId.get(session.getId());
       String unitId;

       // 2. Validación: ¿El jugador está registrado?
       if (playerId == null) {
           logger.warn("Player not registered on session: {}", session.getId());
           sendErrorMessage(session, "Player not registered on session: " + session.getId());
           return;
       }

       // 3. Extrae el unitId del JSON
       unitId = root.get("unitId").asText();
       logger.debug("Unit: {}, selected by player: {}", unitId, playerId);

       // 4. Delega la validación al servicio
       if (!selectionService.canSelectUnit(unitId, playerId)) {
           logger.warn("Unit cannot be selected: {}", unitId);
           sendErrorMessage(session, "Unit cannot be selected: " + unitId);
           return;
       }

       // 5. Obtiene la unidad del GameState
       Unit unit = selectionService.getUnit(unitId);

       // 6. Convierte la unidad a DTO (Data Transfer Object)
       UnitSelectionDTO unitSelectionDTO = UnitMapper.toSelectionDTO(unit);

       // 7. Envía respuesta al cliente
       sendResponse(session, CommunicationEvents.ServerToClientEvents.UNIT_SELECTED, unitSelectionDTO);

       logger.info("Unit: {}, selected by player: {}", unitId, playerId);
   }
   ```

4. **Envío de respuestas** (`sendResponse`):
   ```java
   // GameWebSocketHandler.java:197-209
   /**
    * Formato estándar de respuesta: { type: "...", payload: {...} }
    */
   private void sendResponse(WebSocketSession session, String eventType, Object payload) throws IOException {
       ServerResponseDTO response = new ServerResponseDTO(eventType, payload);
       String jsonResponse = objectMapper.writeValueAsString(response);
       session.sendMessage(new TextMessage(jsonResponse));
   }
   ```

**Mapas internos importantes**:
- `sessionToPlayerId`: Relaciona cada sesión WebSocket con el ID del jugador.
- `registeredPlayers`: Conjunto de jugadores que ya están conectados (evita duplicados).
- `connectedSessions`: Todas las sesiones WebSocket activas.

---

#### **`SelectionService` (src/main/java/.../service/SelectionService.java)**

**Responsabilidad**: Contiene la lógica de negocio para validar si un jugador puede seleccionar una unidad.

```java
// SelectionService.java:15-33
@Override
public boolean canSelectUnit(String unitId, String playerId) {
    // 1. Verifica que el jugador exista
    if (!gameState.doesPlayerExist(playerId)) {
        return false;
    }

    // 2. Verifica que la unidad exista
    if (gameState.getUnitById(unitId) == null) {
        return false;
    }

    // 3. Verifica que la unidad pertenezca al jugador
    if (!gameState.doesUnitBelongsToPlayer(unitId, playerId)) {
        return false;
    }

    // 4. Verifica que la unidad esté viva
    return gameState.isUnitAlive(unitId);
}

@Override
public Unit getUnit(String unitId) {
    return gameState.getUnitById(unitId);
}
```

**Flujo de validación**: El servicio consulta el `GameState` para verificar la existencia del jugador, la unidad, la propiedad y el estado de vida.

---

#### **`CommunicationEvents` (src/main/java/.../websocket/CommunicationEvents.java)**

**Responsabilidad**: Define las constantes de los nombres de eventos para mantener consistencia entre cliente y servidor.

```java
public interface CommunicationEvents {
    interface ClientToServerEvents {
        String REGISTER_PLAYER = "REGISTER_PLAYER";
        String GET_PLAYER_UNITS = "GET_PLAYER_UNITS";
        String SELECT_UNIT = "SELECT_UNIT";
    }

    interface ServerToClientEvents {
        String PLAYER_REGISTERED = "PLAYER_REGISTERED";
        String UNITS_RECEIVED = "UNITS_RECEIVED";
        String UNIT_SELECTED = "UNIT_SELECTED";
        String SERVER_ERROR = "SERVER_ERROR";
        String AVAILABLE_PLAYERS = "AVAILABLE_PLAYERS";
    }
}
```

---

### 3.2. Lado del Cliente (TypeScript/Phaser)

#### **`WebSocketClient` (client/src/network/WebSocketClient.ts)**

**Responsabilidad**: Gestiona la conexión WebSocket, el envío de mensajes y la distribución de eventos entrantes.

**Arquitectura interna**: Implementa un patrón **Pub/Sub (Publicador/Suscriptor)** donde:
- Los componentes se **suscriben** (`on`) a eventos específicos.
- Cuando llega un mensaje del servidor, `handleMessage` lo parsea y **emite** (`emit`) el evento localmente.

**Métodos clave**:

1. **`connect()`**: Establece la conexión WebSocket.
   ```typescript
   // WebSocketClient.ts:19-51
   public async connect(): Promise<void> {
       return new Promise((resolve, reject) => {
           try {
               this.socket = new WebSocket(this.url); // ws://localhost:8080/game

               // Evento: Conexión exitosa
               this.socket.onopen = () => {
                   console.log("[WebSocket] Connected to server");
                   this.isConnected = true;
                   this.emit(ClientInternalEvents.CONNECTED); // Emite evento interno
                   resolve();
               };

               // Evento: Conexión cerrada
               this.socket.onclose = () => {
                   console.log("[WebSocket] Disconnected from server");
                   this.isConnected = false;
                   this.emit(ClientInternalEvents.DISCONNECTED);
               };

               // Evento: Error de conexión
               this.socket.onerror = (error: Event) => {
                   console.error("[WebSocket] Error:", error);
                   this.isConnected = false;
                   this.emit(ClientInternalEvents.CONNECTION_ERROR, error);
               };

               // Evento: Mensaje recibido
               this.socket.onmessage = (event: MessageEvent) => {
                   this.handleMessage(event.data); // Procesa el mensaje
               };
           } catch (err) {
               reject(err);
           }
       });
   }
   ```

2. **`handleMessage()`**: Procesa mensajes entrantes del servidor.
   ```typescript
   // WebSocketClient.ts:133-166
   private handleMessage(eventData: string): void {
       try {
           const data = JSON.parse(eventData); // Parsea el JSON

           // Identifica el tipo de mensaje y emite el evento correspondiente
           if (data.type === ServerToClientEvents.AVAILABLE_PLAYERS) {
               this.emit(ServerToClientEvents.AVAILABLE_PLAYERS, data.payload);
               return;
           }

           if (data.type === ServerToClientEvents.UNITS_RECEIVED) {
               this.emit(ServerToClientEvents.UNITS_RECEIVED, data.payload);
               return;
           }

           if (data.type === ServerToClientEvents.UNIT_SELECTED) {
               this.emit(ServerToClientEvents.UNIT_SELECTED, data.payload);
               return;
           }

           if (data.type === ServerToClientEvents.SERVER_ERROR) {
               this.emit(ServerToClientEvents.SERVER_ERROR, data.payload);
               return;
           }

           // Fallback: emite cualquier otro tipo
           if (data.type) {
               this.emit(data.type, data);
           }
       } catch (err) {
           console.log("[WebSocket] Error parsing message:", err);
       }
   }
   ```

3. **`on()`**: Registra un callback para un evento.
   ```typescript
   // WebSocketClient.ts:90-96
   public on(event: string, callback: EventCallback): void {
       if (!this.eventListeners.has(event)) {
           this.eventListeners.set(event, []); // Crea array si no existe
       }
       this.eventListeners.get(event)?.push(callback); // Agrega callback
   }
   ```
   **Uso**: `websocketClient.on(ServerToClientEvents.UNIT_SELECTED, (unit) => { ... })`

4. **`emit()`**: Ejecuta todos los callbacks registrados para un evento.
   ```typescript
   // WebSocketClient.ts:171-177
   private emit(event: string, data?: any): void {
       const callbacks = this.eventListeners.get(event);
       if (callbacks) {
           callbacks.forEach(callback => callback(data)); // Ejecuta cada callback
       }
   }
   ```

5. **`send()`**: Envía un mensaje al servidor.
   ```typescript
   // WebSocketClient.ts:116-127
   private send(message: any): void {
       if (!this.isConnected) {
           console.log("[WebSocket] Cannot send message, not connected to server");
           return;
       }

       try {
           this.socket?.send(JSON.stringify(message)); // Serializa a JSON
       } catch (err) {
           console.log("[WebSocket] Error sending message:", err);
       }
   }
   ```

6. **`requestUnitSelection()`**: Solicita al servidor seleccionar una unidad.
   ```typescript
   // WebSocketClient.ts:80-85
   public requestUnitSelection(unitId: string): void {
       this.send({
           type: ClientToServerEvents.SELECT_UNIT,
           unitId: unitId
       });
   }
   ```

---

#### **`GameScene` (client/src/scenes/GameScene.ts)**

**Responsabilidad**: Escena principal de Phaser que orquesta la UI, los listeners de eventos y la interacción del usuario.

**Flujo de inicialización** (`create`):
```typescript
// GameScene.ts:17-50
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

    // 3. Crea el manager de selección local
    this.selectionManager = new SelectionManager();

    // 4. Espera la lista de jugadores disponibles del servidor
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

**Configuración de listeners** (`setupEventListeners`):
```typescript
// GameScene.ts:103-133
private setupEventListeners(): void {
    if (!this.websocketClient || !this.selectionManager) return;

    // 1. Listener: Recepción de unidades del servidor
    this.websocketClient.on(ServerToClientEvents.UNITS_RECEIVED, (data: any) => {
        const playerUnits: IUnit[] = data.playerUnits;
        const enemyUnits: IUnit[] = data.enemyUnits;

        console.log(`[GameScene] Received ${playerUnits.length} units for player and ${enemyUnits.length} units for enemy from server`);
        this.selectionManager?.setPlayerUnits(playerUnits);
        this.renderUnits(playerUnits, enemyUnits); // Dibuja sprites
    });

    // 2. Listener: Confirmación de selección del servidor
    this.websocketClient.on(ServerToClientEvents.UNIT_SELECTED, (unit: IUnit) => {
        console.log(`[GameScene] Selection confirmed: ${unit.unitId}`);
        this.selectionManager?.confirmSelection(unit);
        this.highlightUnit(unit.unitId); // Resalta visualmente
    });

    // 3. Listener: Error del servidor
    this.websocketClient.on(ServerToClientEvents.SERVER_ERROR, (errorMessage: string) => {
        console.error(`[GameScene] Server error: ${errorMessage}`);
        this.showError(errorMessage);
    });

    // 4. Listener interno: Cambio de selección local
    this.selectionManager.on(ClientInternalEvents.SELECTION_CHANGED, (unit: IUnit) => {
        console.log(`[GameScene] Selection changed: ${unit.unitId}, sending selection to server...`);
        this.websocketClient?.requestUnitSelection(unit.unitId); // Envía al servidor
    })
}
```

**Interacción del usuario** (clic en unidad):
```typescript
// GameScene.ts:156-166
private createUnitSprite(unit: IUnit, x: number, y: number, isPlayerUnit: boolean): void {
    const sprite = this.add.rectangle(x, y, 60, 60, this.getUnitColor(unit.type));

    sprite.setStrokeStyle(2, isPlayerUnit ? 0x00ff00 : 0xff0000);
    sprite.setInteractive({ useHandCursor: true });

    // Evento: Clic en el sprite
    sprite.on('pointerdown', () => {
        if (isPlayerUnit) {  // Solo unidades propias son seleccionables
            this.selectionManager?.selectUnit(unit.unitId); // Llama al manager
        }
    });

    sprite.on('pointerover', () => sprite.setScale(1.1)); // Hover
    sprite.on('pointerout', () => sprite.setScale(1));

    this.unitSprites.set(unit.unitId, sprite); // Almacena referencia

    // ... etiqueta de texto ...
}
```

---

#### **`SelectionManager` (client/src/managers/SelectionManager.ts)**

**Responsabilidad**: Gestiona el estado local de selección de unidades y emite eventos internos.

```typescript
// SelectionManager.ts:24-39
public selectUnit(unitId: string): IUnit | null {
    // 1. Busca la unidad en la lista del jugador
    const unit = this.playerUnits.find(unit => unit.unitId === unitId);

    if (!unit) {
        console.warn(`[SelectionManager] Unit not found: ${unitId}`);
        return null;
    }

    // 2. Si ya está seleccionada, no hace nada
    if (this.selectedUnit?.unitId === unitId) {
        return unit;
    }

    // 3. Actualiza la selección local
    this.selectedUnit = unit;

    // 4. Emite evento interno (GameScene lo escucha)
    this.emit(ClientInternalEvents.SELECTION_CHANGED, unit);
    return unit;
}

// SelectionManager.ts:41-44
public confirmSelection(unit: IUnit): void {
    this.selectedUnit = unit;
    this.emit(ClientInternalEvents.SELECTION_CONFIRMED, unit);
}
```

**Patrón Pub/Sub interno**:
```typescript
// SelectionManager.ts:53-59
public on(event: string, callback: ManagerCallback): void {
    if (!this.eventListeners.has(event)) {
        this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)?.push(callback);
}

// SelectionManager.ts:61-67
private emit(event: string, data?: any): void {
    const callbacks = this.eventListeners.get(event);
    if (callbacks) {
        callbacks.forEach(callback => callback(data));
    }
}
```

---

## 4. Flujo Completo: Selección de Unidad (Paso a Paso con Código)

Este es el flujo **completo** que ocurre cuando un jugador hace clic en un drone:

### 4.1. **Usuario hace clic en un sprite**

**Archivo**: `GameScene.ts:162-166`
```typescript
sprite.on('pointerdown', () => {
    if (isPlayerUnit) {
        this.selectionManager?.selectUnit(unit.unitId); // -> Paso 4.2
    }
});
```

---

### 4.2. **`SelectionManager` procesa la selección local**

**Archivo**: `SelectionManager.ts:24-39`
```typescript
public selectUnit(unitId: string): IUnit | null {
    const unit = this.playerUnits.find(unit => unit.unitId === unitId);

    if (!unit) return null;
    if (this.selectedUnit?.unitId === unitId) return unit;

    this.selectedUnit = unit;
    this.emit(ClientInternalEvents.SELECTION_CHANGED, unit); // -> Paso 4.3
    return unit;
}
```

---

### 4.3. **`GameScene` escucha el evento interno y envía al servidor**

**Archivo**: `GameScene.ts:129-132`
```typescript
this.selectionManager.on(ClientInternalEvents.SELECTION_CHANGED, (unit: IUnit) => {
    console.log(`[GameScene] Selection changed: ${unit.unitId}, sending selection to server...`);
    this.websocketClient?.requestUnitSelection(unit.unitId); // -> Paso 4.4
})
```

---

### 4.4. **`WebSocketClient` envía el mensaje JSON al servidor**

**Archivo**: `WebSocketClient.ts:80-85`
```typescript
public requestUnitSelection(unitId: string): void {
    this.send({
        type: ClientToServerEvents.SELECT_UNIT, // "SELECT_UNIT"
        unitId: unitId
    }); // -> Paso 4.5
}
```

**Mensaje enviado**:
```json
{
  "type": "SELECT_UNIT",
  "unitId": "aerial-drone-001"
}
```

---

### 4.5. **Servidor recibe el mensaje en `GameWebSocketHandler`**

**Archivo**: `GameWebSocketHandler.java:64-88`
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonNode root = objectMapper.readTree(message.getPayload()); // Parsea JSON
    String messageType = root.get("type").asText(); // "SELECT_UNIT"

    switch (messageType) {
        case CommunicationEvents.ClientToServerEvents.SELECT_UNIT:
            handleSelectUnit(session, root); // -> Paso 4.6
            break;
        // ...
    }
}
```

---

### 4.6. **`handleSelectUnit` valida y procesa la selección**

**Archivo**: `GameWebSocketHandler.java:134-163`
```java
private void handleSelectUnit(WebSocketSession session, JsonNode root) throws IOException {
    String playerId = sessionToPlayerId.get(session.getId()); // Obtiene playerId de la sesión
    String unitId = root.get("unitId").asText();

    // Validación: ¿Jugador registrado?
    if (playerId == null) {
        sendErrorMessage(session, "Player not registered on session: " + session.getId());
        return;
    }

    // Validación: ¿Puede seleccionar la unidad? -> Paso 4.7
    if (!selectionService.canSelectUnit(unitId, playerId)) {
        sendErrorMessage(session, "Unit cannot be selected: " + unitId);
        return;
    }

    // Obtiene la unidad del GameState
    Unit unit = selectionService.getUnit(unitId);

    // Convierte a DTO
    UnitSelectionDTO unitSelectionDTO = UnitMapper.toSelectionDTO(unit);

    // Envía respuesta al cliente -> Paso 4.8
    sendResponse(session, CommunicationEvents.ServerToClientEvents.UNIT_SELECTED, unitSelectionDTO);
}
```

---

### 4.7. **`SelectionService` valida la selección**

**Archivo**: `SelectionService.java:15-33`
```java
@Override
public boolean canSelectUnit(String unitId, String playerId) {
    if (!gameState.doesPlayerExist(playerId)) return false;
    if (gameState.getUnitById(unitId) == null) return false;
    if (!gameState.doesUnitBelongsToPlayer(unitId, playerId)) return false;
    return gameState.isUnitAlive(unitId);
}
```

---

### 4.8. **Servidor envía respuesta JSON al cliente**

**Archivo**: `GameWebSocketHandler.java:197-209`
```java
private void sendResponse(WebSocketSession session, String eventType, Object payload) throws IOException {
    ServerResponseDTO response = new ServerResponseDTO(eventType, payload);
    String jsonResponse = objectMapper.writeValueAsString(response);
    session.sendMessage(new TextMessage(jsonResponse)); // -> Paso 4.9
}
```

**Mensaje enviado**:
```json
{
  "type": "UNIT_SELECTED",
  "payload": {
    "unitId": "aerial-drone-001",
    "type": "AERIAL_DRONE",
    "ownerId": "player_1",
    "position": { "x": 10.0, "y": 10.0, "z": 5.0 }
  }
}
```

---

### 4.9. **Cliente recibe el mensaje en `WebSocketClient`**

**Archivo**: `WebSocketClient.ts:44-46`
```typescript
this.socket.onmessage = (event: MessageEvent) => {
    this.handleMessage(event.data); // -> Paso 4.10
};
```

---

### 4.10. **`handleMessage` parsea y emite evento local**

**Archivo**: `WebSocketClient.ts:148-151`
```typescript
if (data.type === ServerToClientEvents.UNIT_SELECTED) {
    this.emit(ServerToClientEvents.UNIT_SELECTED, data.payload); // -> Paso 4.11
    return;
}
```

---

### 4.11. **`GameScene` escucha el evento y actualiza la UI**

**Archivo**: `GameScene.ts:117-121`
```typescript
this.websocketClient.on(ServerToClientEvents.UNIT_SELECTED, (unit: IUnit) => {
    console.log(`[GameScene] Selection confirmed: ${unit.unitId}`);
    this.selectionManager?.confirmSelection(unit); // Actualiza manager
    this.highlightUnit(unit.unitId); // Resalta visualmente
});
```

---

### 4.12. **`highlightUnit` actualiza el sprite**

**Archivo**: `GameScene.ts:180-189`
```typescript
private highlightUnit(unitId: string): void {
    // Resetea todos los bordes
    this.unitSprites.forEach(sprite => {
        sprite.setStrokeStyle(2, 0x888888);
    });

    // Resalta la unidad seleccionada
    const selectedSprite = this.unitSprites.get(unitId);
    if (selectedSprite) {
        selectedSprite.setStrokeStyle(4, 0xffff00); // Borde amarillo grueso
    }
}
```

---

## 5. Resumen de Interacciones entre Clases

### **Flujo Cliente → Servidor**

```
Usuario (clic)
    ↓
GameScene.sprite.on('pointerdown')
    ↓
SelectionManager.selectUnit()
    ↓ (emite ClientInternalEvents.SELECTION_CHANGED)
GameScene escucha evento
    ↓
WebSocketClient.requestUnitSelection()
    ↓
WebSocketClient.send() → JSON por WebSocket
    ↓
[VIAJE POR LA RED]
    ↓
GameWebSocketHandler.handleTextMessage()
    ↓
GameWebSocketHandler.handleSelectUnit()
    ↓
SelectionService.canSelectUnit() → consulta GameState
    ↓
SelectionService.getUnit() → consulta GameState
    ↓
UnitMapper.toSelectionDTO() (convierte Unit a DTO)
    ↓
GameWebSocketHandler.sendResponse() → JSON por WebSocket
```

### **Flujo Servidor → Cliente**

```
GameWebSocketHandler.sendResponse()
    ↓
[VIAJE POR LA RED]
    ↓
WebSocketClient.onmessage
    ↓
WebSocketClient.handleMessage() (parsea JSON)
    ↓
WebSocketClient.emit(ServerToClientEvents.UNIT_SELECTED)
    ↓
GameScene escucha evento
    ↓
SelectionManager.confirmSelection() (actualiza estado local)
    ↓
GameScene.highlightUnit() (actualiza UI)
```

---

## 6. Anatomía de los Métodos del Cliente

### **`on(event, callback)`**
- **Propósito**: Suscribe un callback a un evento específico.
- **Uso**: `client.on(ServerToClientEvents.UNIT_SELECTED, (data) => { ... })`
- **Implementación**: Almacena el callback en un `Map<string, EventCallback[]>`.
- **Analogía**: Es como "subscribirse a un canal de YouTube" - cuando el canal publica algo, recibes una notificación.

### **`emit(event, data)`**
- **Propósito**: Ejecuta todos los callbacks registrados para un evento.
- **Uso interno**: `this.emit(ServerToClientEvents.UNIT_SELECTED, data.payload)`
- **Implementación**: Itera sobre todos los callbacks del evento y los ejecuta.
- **Analogía**: Es como "tocar una campana" - todos los que están escuchando reaccionan.

### **`handleMessage(eventData)`**
- **Propósito**: Recibe mensajes del servidor, los parsea y los re-emite como eventos locales.
- **Flujo**:
  1. Parsea el JSON: `JSON.parse(eventData)`
  2. Identifica el tipo: `data.type`
  3. Emite el evento local: `this.emit(data.type, data.payload)`
- **Analogía**: Es como un "traductor" que convierte mensajes de red en eventos locales.

### **`send(message)`**
- **Propósito**: Serializa un objeto a JSON y lo envía al servidor.
- **Flujo**:
  1. Verifica conexión: `if (!this.isConnected) return`
  2. Serializa: `JSON.stringify(message)`
  3. Envía: `this.socket?.send(jsonString)`
- **Analogía**: Es como "escribir una carta y ponerla en el buzón".
