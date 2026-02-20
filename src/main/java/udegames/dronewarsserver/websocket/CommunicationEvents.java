package udegames.dronewarsserver.websocket;

/*
 * CommunicationEvents define todos los eventos que se transmiten entre cliente y servidor.
 * El cliente envia un evento; el servidor procesa el evento con el nombre usado aqui.
 *
 * ServerToClient: Cuando el servidor envia un evento al cliente
 * ClientToServer: Cuando el cliente envia un evento al servidor
 */
public class CommunicationEvents {

    public static class ClientToServerEvents {
        public static final String REGISTER_PLAYER = "REGISTER_PLAYER";
        public static final String GET_PLAYER_UNITS = "GET_PLAYER_UNITS";
        public static final String SELECT_UNIT = "SELECT_UNIT";
        public static final String LAUNCH_BOMB = "LAUNCH_BOMB";
        public static final String MOVE_UNIT = "MOVE_UNIT";
        public static final String RELOAD_AMMO = "RELOAD_AMMO";
        // Ataque con bomba (jugador 1).
        public static final String LAUNCH_BOMB = "LAUNCH_BOMB";

        // Futuro
        // public static final String REQUEST_MOVE_UNIT = "REQUEST_MOVE_UNIT";
        // public static final String REQUEST_ATTACK = "REQUEST_ATTACK";
    }

    public static class ServerToClientEvents {
        public static final String PLAYER_REGISTERED = "PLAYER_REGISTERED";
        public static final String UNITS_RECEIVED = "UNITS_RECEIVED";
        public static final String UNIT_SELECTED = "UNIT_SELECTED";
        public static final String SERVER_ERROR = "SERVER_ERROR";
        public static final String AVAILABLE_PLAYERS = "AVAILABLE_PLAYERS";
        public static final String BOMB_LAUNCHED = "BOMB_LAUNCHED";
        public static final String BOMB_EXPLODED = "BOMB_EXPLODED";
        public static final String MOVE_ACCEPTED = "MOVE_ACCEPTED";
        public static final String GAME_STATE_UPDATE = "GAME_STATE_UPDATE";
        public static final String MUNICION_RECARGADA = "MUNICION_RECARGADA";
        // Eventos de bombas.
        public static final String BOMB_LAUNCHED = "BOMB_LAUNCHED";
        public static final String BOMB_EXPLODED = "BOMB_EXPLODED";

        // Futuro
        // public static final String GAME_STATE_UPDATED = "GAME_STATE_UPDATED";
        // public static final String UNIT_DESTROYED = "UNIT_DESTROYED";
    }
}