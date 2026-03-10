package udegames.dronewarsserver.repository;

public interface IRankingPuntaje {
    int getValor();

    String getJugadorNickname();

    java.time.Instant getFechaRegistro();
}
