package udegames.dronewarsserver.mapper;

import udegames.dronewarsserver.domain.model.Drone;
import udegames.dronewarsserver.domain.model.DroneCarrier;
import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.UnitSelectionDTO;

public class UnitMapper {

    public static UnitSelectionDTO toSelectionDTO(Unit unit) {
        float combustible = 0f;
        int municionDisponible = 0;
        if(unit instanceof Drone dron){
            combustible = dron.getCombustible();
        }
        if (unit instanceof DroneCarrier carrier) {
            municionDisponible = carrier.getAvailableAmmoSupply();
        }
        return new UnitSelectionDTO(
                unit.getId(),
                unit.getType().toString(),
                unit.getPosition().getX(),
                unit.getPosition().getY(),
                unit.getPosition().getZ(),
                unit.getHealth(),
                combustible,
                municionDisponible
        );
    }
}
