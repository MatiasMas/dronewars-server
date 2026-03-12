package udegames.dronewarsserver.service;
import udegames.dronewarsserver.dto.MisilLanzadoDTO;
import udegames.dronewarsserver.dto.MisilImpactoDTO;

public interface IMissileService {
    boolean puedeDisparar(String unidadId, String jugadorId, String objetivoId, Float objetivoX, Float objetivoY);

    resultadoDisparoMisil lanzarMisil(String unidadId, String objetivoId, Float objetivoX, Float objetivoY);

    default boolean puedeDisparar(String unidadId, String jugadorId, String objetivoId) {
        return puedeDisparar(unidadId, jugadorId, objetivoId, null, null);
    }

    default resultadoDisparoMisil lanzarMisil(String unidadId, String objetivoId) {
        return lanzarMisil(unidadId, objetivoId, null, null);
    }

    class resultadoDisparoMisil {
        private final MisilLanzadoDTO misilLanzado;
        private final MisilImpactoDTO misilImpacto;

        public resultadoDisparoMisil (MisilImpactoDTO misilImpactado, MisilLanzadoDTO misilDisparado){
            this.misilImpacto = misilImpactado;
            this.misilLanzado = misilDisparado;
        }

        public MisilLanzadoDTO getMisilLanzado() {return misilLanzado;}
        public MisilImpactoDTO getMisilImpacto() {return misilImpacto;}
    }

}
