# Documentación de DroneWars

Bienvenido a la documentación oficial del proyecto **DroneWars**. Este documento sirve como guía para entender la arquitectura, el flujo de datos y el funcionamiento interno tanto del servidor como del cliente.

## Índice

1.  [Arquitectura y Flujo de Datos](architecture.md)
    *   Comunicación Cliente-Servidor (WebSockets)
    *   Interacción entre Clases
    *   Responsabilidades de los componentes
2.  [Motor de Juego y Estado](game-engine.md)
    *   Funcionamiento del Game Engine
    *   El bucle de simulación (Ticks)
    *   Gestión del Mundo y el Game State
3.  [Guía de Inicio y Ejemplos](getting-started.md)
    *   Flujo de inicialización del Servidor
    *   Flujo de inicialización del Cliente
    *   Ejemplos de interacción actual
4.  [Extendiendo el Juego](extending-the-game.md)
    *   Cómo añadir nuevos eventos de comunicación
    *   Implementación de nuevas funcionalidades
    *   Clases a modificar

---

## Resumen del Proyecto

DroneWars es un juego de combate de drones multijugador con un modelo de **servidor autoritativo**. El servidor mantiene el estado real del juego y ejecuta la lógica de simulación, mientras que el cliente se encarga de la representación visual (Phaser.io) y de enviar las acciones del jugador.
