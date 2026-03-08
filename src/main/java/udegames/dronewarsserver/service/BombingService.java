package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.enums.DroneState;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.DroneCarrier;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.BombExplodedDTO;
import udegames.dronewarsserver.dto.BombLaunchedDTO;
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.engine.GameState;
import udegames.dronewarsserver.mapper.UnitMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BombingService implements IBombingService {
    // Valores simples para explosiones (ajustables).
    private static final float RADIO_EXPLOSION = 85f;

    private final GameState estadoJuego;

    public BombingService(GameState estadoJuego) {
        this.estadoJuego = estadoJuego;
    }

    @Override
    public boolean canLaunchBomb(String idUnidad, String idJugador) {
        // Validaciones simples antes de permitir el ataque.
        if (!estadoJuego.doesPlayerExist(idJugador)) {
            return false;
        }

        Unit unidad = estadoJuego.getUnitById(idUnidad);
        // Solo drones pueden lanzar.
        if (!(unidad instanceof Drone)) {
            return false;
        }

        if (!estadoJuego.doesUnitBelongsToPlayer(idUnidad, idJugador)) {
            return false;
        }

        if (!estadoJuego.isUnitAlive(idUnidad)) {
            return false;
        }

        // Debe tener municion.
        Drone dron = (Drone) unidad;

        if(dron.getState() == DroneState.INHABILITADO){
            return false;
        }

        if (dron.getCombustible() <= 0f){
            return false;
        }

        return dron.getAmmo() > 0;

    }

    @Override
    public BombAttackResult launchBomb(String idUnidad) {
        Unit unidad = estadoJuego.getUnitById(idUnidad);
        if (!(unidad instanceof Drone)) {
            return null;
        }

        Drone dron = (Drone) unidad;
        // Resta una bomba; si no hay, se corta.
        if (!dron.consumirMunicion(1)) {
            return null;
        }

        Position posicion = dron.getPosition();
        String idBomba = UUID.randomUUID().toString();

        // Evento de lanzamiento (para el cliente).
        BombLaunchedDTO bombaLanzada = new BombLaunchedDTO(
                idBomba,
                dron.getId(),
                posicion.getX(),
                posicion.getY(),
                posicion.getZ(),
                dron.getAmmo()
        );

        // Evento de explosion con dano inmediato.
        List<UnitSelectionDTO> unidadesImpactadas = new ArrayList<>();
        for (Unit unidadObjetivo : estadoJuego.getUnits()) {
            if (unidadObjetivo.isDestroyed()) {
                continue;
            }

            if (unidadObjetivo.getOwnerId().equals(dron.getOwnerId())) {
                continue;
            }

            boolean esDron = unidadObjetivo instanceof Drone;
            boolean esPortadrones = unidadObjetivo instanceof DroneCarrier;
            if (!esDron && !esPortadrones) {
                continue;
            }

            if (!estaEnRango(posicion, unidadObjetivo.getPosition(), RADIO_EXPLOSION)) {
                continue;
            }

            // Si esta en rango, los drones se destruyen y los portadrones pierden 1 de vida.
            if (esDron) {
                unidadObjetivo.applyDamage(unidadObjetivo.getHealth());
            } else {
                unidadObjetivo.applyDamage(1);
            }
            unidadesImpactadas.add(UnitMapper.toSelectionDTO(unidadObjetivo));

            // Si quedo destruida, la sacamos del mapa.
            if (unidadObjetivo.isDestroyed()) {
                estadoJuego.removeUnit(unidadObjetivo.getId());
            }
        }

        BombExplodedDTO bombaExplotada = new BombExplodedDTO(
                idBomba,
                dron.getId(),
                posicion.getX(),
                posicion.getY(),
                unidadesImpactadas
        );

        return new BombAttackResult(bombaLanzada, bombaExplotada);
    }

    private boolean estaEnRango(Position origen, Position destino, float rango) {
        if (origen == null || destino == null) {
            return false;
        }

        // Distancia 2D (X/Y) para explosion.
        float dx = origen.getX() - destino.getX();
        float dy = origen.getY() - destino.getY();
        float distancia = (float) Math.sqrt(dx * dx + dy * dy);

        return distancia <= rango;
    }
}
