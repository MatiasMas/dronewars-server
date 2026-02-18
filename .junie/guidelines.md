# Junie Guidelines - DroneWars

Este documento contiene los estándares de codificación y convenciones para el proyecto DroneWars, tanto para el servidor como para el cliente.

## Estandar de Servidor (Java + Spring + WebSockets)

### Convenciones Generales
- **Lenguaje:** Java 21 (o Java 17, ya que Spring es compatible con ambas).
- **Convención de nombres:**
    - `camelCase` para métodos y variables.
    - `PascalCase` para clases.
    - `MAYUSCULA_CON_GUIONES` para constantes.
    - Las interfaces deben comenzar con `I` (ej. `IGameService`).
- **Responsabilidades:** Limitar las responsabilidades a una sola clase (S de SOLID).
- **Lógica de negocio:** No poner lógica de negocio en los controladores.

### Estructura de Carpetas
Dentro del package `udegames.droneswarserver` (o el paquete raíz correspondiente):

- `config`: Configuración de Spring y WebSockets.
- `controller`: Endpoints y rutas.
- `service`: Lógica de la aplicación.
- `domain`: Todo lo referido al dominio, como entidades y modelos.
- `repository`: Clases que acceden a la base de datos con patrón repository.
- `websocket`: Implementación de la clase que comunica con websockets.
- `dto`: Data Transfer Objects (DTO).
- `mapper`: Métodos que transforman entre entidades ↔ DTOs.
- `engine`: Implementación de los métodos `create`/`update` que manejan los ticks del juego (servidor autoritativo).
- `exception`: Todas las excepciones personalizadas que puedan surgir.

### Patrones de Diseño
- **Arquitectura en Capas:**
    - `Controller` → Endpoints y rutas de acceso.
    - `Service` → Lógica del juego.
    - `Repository` → Acceso a la base de datos.
- **DTO (Data Transfer Objects):** Para la comunicación entre cliente y servidor, evitando exponer entidades directamente.
- **Singleton:** El "Game Engine" debe ser una instancia única por partida.
- **Factory:** Evaluar su uso para la creación de unidades sin modificar la lógica existente.
- **Repository:** Para la comunicación con la base de datos (por defecto en Spring).

---

## Estandar de Cliente (Phaser.io)

### Convenciones Generales
- **Lenguaje:** TypeScript estricto.
- **Convención de nombres:**
    - `camelCase` para métodos y variables.
    - `PascalCase` para clases.
    - Las interfaces deben comenzar con `I`.
- **Lógica de comunicación:** No poner lógica de comunicación en las escenas (`Scenes`).

### Estructura de Carpetas
Dentro de la carpeta `src`:

- `scenes`: Pantallas del juego.
- `entities`: Entidades como drones, portadrones, proyectiles, etc.
- `network`: `WebSocketClient` y `MessageHandler`, manejo de la comunicación con el servidor.
- `ui`: HUD, barras, paneles (trabajan en conjunto con las escenas).
- `managers`: Clases que gestionan los movimientos y selecciones de las unidades.
- `types`: Tipos de TypeScript e interfaces a nivel de proyecto.
- `utils`: Utilidades del proyecto, funciones o integraciones con herramientas específicas.
- `config`: Configuraciones de Phaser y del juego en sí.
