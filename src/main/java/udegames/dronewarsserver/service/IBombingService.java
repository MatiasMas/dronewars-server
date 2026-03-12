package udegames.dronewarsserver.service;

import udegames.dronewarsserver.dto.BombExplodedDTO;
import udegames.dronewarsserver.dto.BombLaunchedDTO;

public interface IBombingService {
    boolean canLaunchBomb(String idUnidad, String idJugador);

    BombAttackResult launchBomb(String idUnidad);

    class BombAttackResult {
        private final BombLaunchedDTO bombaLanzada;
        private final BombExplodedDTO bombaExplotada;

        public BombAttackResult(BombLaunchedDTO bombaLanzada, BombExplodedDTO bombaExplotada) {
            this.bombaLanzada = bombaLanzada;
            this.bombaExplotada = bombaExplotada;
        }

        public BombLaunchedDTO getBombLaunched() {
            return bombaLanzada;
        }

        public BombExplodedDTO getBombExploded() {
            return bombaExplotada;
        }
    }
}
