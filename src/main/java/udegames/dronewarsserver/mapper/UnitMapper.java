package udegames.dronewarsserver.mapper;

import udegames.dronewarsserver.domain.model.Unit;
import udegames.dronewarsserver.dto.UnitSelectionDTO;

public class UnitMapper {

    public static UnitSelectionDTO toSelectionDTO(Unit unit) {
        return new UnitSelectionDTO(
                unit.getId(),
                unit.getType().toString(),
                unit.getPosition().getX(),
                unit.getPosition().getY(),
                unit.getPosition().getZ(),
                unit.getHealth()
        );
    }
}
