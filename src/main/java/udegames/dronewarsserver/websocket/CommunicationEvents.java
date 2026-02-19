package udegames.dronewarsserver.websocket;

/*
 * CommunicationEvents defines all events that are transmitted between the client and server,
 * clients send an event; server processes the event with the name used here.
 *
 * ServerToClient: When the server sends an event to the client
 * ClientToServer: When the client sends an event to the server
 */
public class CommunicationEvents {

    public static class ClientToServerEvents {
        public static final String REGISTER_PLAYER = "REGISTER_PLAYER";
        public static final String GET_PLAYER_UNITS = "GET_PLAYER_UNITS";
        public static final String SELECT_UNIT = "SELECT_UNIT";
        public static final String LAUNCH_BOMB = "LAUNCH_BOMB";

        // Future
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

        // Future
        // public static final String GAME_STATE_UPDATED = "GAME_STATE_UPDATED";
        // public static final String UNIT_DESTROYED = "UNIT_DESTROYED";
    }
}
