package udegames.dronewarsserver.service;

public interface IAmmoService {
    // Valida si la unidad puede recargar municion.
    boolean canReloadAmmo(String idUnidad, String idJugador, String idPortadrones);

    // Recarga la municion al maximo y devuelve la municion actual.
    int reloadAmmo(String idUnidad);
}
