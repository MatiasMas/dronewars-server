package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.DroneCarrier;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.engine.GameState;

@Service
public class AmmoService implements IAmmoService {
    // Rango maximo permitido para recargar (en unidades del mundo).
    private static final float RANGO_RECARGA_UNIDADES = 15f;

    private final GameState estadoJuego;

    public AmmoService(GameState estadoJuego) {
        this.estadoJuego = estadoJuego;
    }

    @Override
    public boolean canReloadAmmo(String idUnidad, String idJugador, String idPortadrones) {
        // Validaciones basicas del jugador y la unidad.
        if (!estadoJuego.doesPlayerExist(idJugador)) {
            return false;
        }

        Unit unidad = estadoJuego.getUnitById(idUnidad);
        if (!(unidad instanceof Drone)) {
            return false;
        }

        if (!estadoJuego.doesUnitBelongsToPlayer(idUnidad, idJugador)) {
            return false;
        }

        if (!estadoJuego.isUnitAlive(idUnidad)) {
            return false;
        }

        Drone dron = (Drone) unidad;

        // Usamos carrierId del payload si viene, si no el del dron.
        String idPortadronesEfectivo = (idPortadrones != null && !idPortadrones.isBlank())
                ? idPortadrones
                : dron.getCarrierId();
        if (idPortadronesEfectivo == null || idPortadronesEfectivo.isBlank()) {
            return false;
        }

        Unit unidadPortadrones = estadoJuego.getUnitById(idPortadronesEfectivo);
        if (!(unidadPortadrones instanceof DroneCarrier)) {
            return false;
        }

        if (!idJugador.equals(unidadPortadrones.getOwnerId())) {
            return false;
        }

        if (!estaEnRango(dron.getPosition(), unidadPortadrones.getPosition())) {
            return false;
        }

        // No exceder el maximo permitido.
        return dron.getAmmo() < dron.getMaxAmmo();
    }

    @Override
    public int reloadAmmo(String idUnidad) {
        Unit unidad = estadoJuego.getUnitById(idUnidad);
        if (!(unidad instanceof Drone)) {
            return -1;
        }

        Drone dron = (Drone) unidad;
        // Recarga completa.
        dron.reload();
        return dron.getAmmo();
    }

    private boolean estaEnRango(Position posicionDron, Position posicionPortadrones) {
        if (posicionDron == null || posicionPortadrones == null) {
            return false;
        }

        float dx = posicionDron.getX() - posicionPortadrones.getX();
        float dy = posicionDron.getY() - posicionPortadrones.getY();
        float dz = posicionDron.getZ() - posicionPortadrones.getZ();
        float distancia = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        return distancia <= RANGO_RECARGA_UNIDADES;
    }
}
