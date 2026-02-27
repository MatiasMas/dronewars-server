package udegames.dronewarsserver.service;

import org.springframework.stereotype.Service;
import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.MissileProjectile;
import udegames.dronewarsserver.domain.model.Position;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.MisilImpactoDTO;
import udegames.dronewarsserver.dto.MisilLanzadoDTO;
import udegames.dronewarsserver.dto.UnitSelectionDTO;
import udegames.dronewarsserver.engine.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MissileService implements IMissileService {
    private static final String ID_JUGADOR_MISIL = "player_2";
    private static final float VELOCIDAD_MISIL = 20f;
    private static final float TIEMPO_MAX_MISIL = 10f;

    private final GameState estadoJuego;

    public MissileService(GameState estadoJuego) {
        this.estadoJuego = estadoJuego;
    }

    @Override
    public boolean puedeDisparar(String idUnidad, String idJugador, String idObjetivo, Float objetivoX, Float objetivoY) {
        if (!estadoJuego.doesPlayerExist(idJugador)) {
            return false;
        }
        if (!ID_JUGADOR_MISIL.equals(idJugador)) {
            return false;
        }
        Unit unidadAtacante = estadoJuego.getUnitById(idUnidad);
        if (!(unidadAtacante instanceof Drone)) {
            return false;
        }
        if (!estadoJuego.doesUnitBelongsToPlayer(idUnidad, idJugador)) {
            return false;
        }
        if (!estadoJuego.isUnitAlive(idUnidad)) {
            return false;
        }
        Drone dronAtacante = (Drone) unidadAtacante;
        if (dronAtacante.getAmmo() <= 0) {
            return false;
        }

        // Si hay un punto objetivo, permitimos el disparo.
        if (objetivoX != null && objetivoY != null) {
            return true;
        }

        // Si no hay punto, necesitamos un objetivo valido.
        if (idObjetivo == null || idObjetivo.isBlank()) {
            return false;
        }

        Unit unidadObjetivo = estadoJuego.getUnitById(idObjetivo);
        if (!(unidadObjetivo instanceof Drone)) {
            return false;
        }
        if (unidadObjetivo.getOwnerId().equals(idJugador)) {
            return false;
        }
        return estadoJuego.isUnitAlive(idObjetivo);
    }

    @Override
    public resultadoDisparoMisil lanzarMisil(String unidadId, String objetivoId, Float objetivoX, Float objetivoY) {
        Unit unidadAtacante = estadoJuego.getUnitById(unidadId);
        if (!(unidadAtacante instanceof Drone)) {
            return null;
        }

        Drone dronAtacante = (Drone) unidadAtacante;
        if (!dronAtacante.consumirMunicion(1)) {
            return null;
        }

        // Seguimos el misil solo en 2D, no usamos Z.
        float zPlano = 0f;
        float destinoX;
        float destinoY;
        String objetivoRealId = null;

        if (objetivoX != null && objetivoY != null) {
            destinoX = objetivoX;
            destinoY = objetivoY;
        } else {
            Unit unidadObjetivo = estadoJuego.getUnitById(objetivoId);
            if (unidadObjetivo == null) {
                return null;
            }
            destinoX = unidadObjetivo.getPosition().getX();
            destinoY = unidadObjetivo.getPosition().getY();
            objetivoRealId = unidadObjetivo.getId();
        }

        String idMisil = UUID.randomUUID().toString();
        Position posicionAtacante = unidadAtacante.getPosition();
        Position posicionInicial = new Position(posicionAtacante.getX(), posicionAtacante.getY(), zPlano);
        Position destinoFijo = new Position(destinoX, destinoY, zPlano);

        MissileProjectile misil = new MissileProjectile(
                idMisil,
                dronAtacante.getId(),
                dronAtacante.getOwnerId(),
                objetivoRealId,
                posicionInicial,
                destinoFijo,
                VELOCIDAD_MISIL,
                TIEMPO_MAX_MISIL
        );
        estadoJuego.addMissileProjectile(misil);

        MisilLanzadoDTO misilDisparado = new MisilLanzadoDTO(
                idMisil,
                dronAtacante.getId(),
                objetivoRealId,
                posicionAtacante.getX(),
                posicionAtacante.getY(),
                zPlano,
                destinoX,
                destinoY,
                zPlano,
                dronAtacante.getAmmo()
        );

        MisilImpactoDTO misilImpacto = new MisilImpactoDTO(
                idMisil,
                dronAtacante.getId(),
                objetivoRealId,
                new ArrayList<>()
        );
        return new resultadoDisparoMisil(misilImpacto, misilDisparado);
    }
}
