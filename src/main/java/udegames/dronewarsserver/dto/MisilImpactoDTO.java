package udegames.dronewarsserver.dto;

import java.util.List;

public class MisilImpactoDTO {
    private String misilId;
    private String unidadAtaqueId;
    private String unidadDefensaId;
    private List<UnitSelectionDTO> unidadesImpactadas;

    public MisilImpactoDTO(String misilId, String unidadAtaqueId, String unidadDefensaId, List<UnitSelectionDTO> unidadesImpactadas) {
        this.misilId = misilId;
        this.unidadAtaqueId = unidadAtaqueId;
        this.unidadDefensaId = unidadDefensaId;
        this.unidadesImpactadas = unidadesImpactadas;
    }

    public String getMisilId() {return this.misilId;}
    public String getUnidadAtaqueId() {return this.unidadAtaqueId;}
    public String getUnidadDefensaId() {return this.unidadDefensaId;}
    public List<UnitSelectionDTO> getUnidadesImpactadas() {return this.unidadesImpactadas;}

    public void setMisilId(String misilId) {this.misilId = misilId;}
    public void setUnidadAtaqueId(String unidadAtaqueId) {this.unidadAtaqueId = unidadAtaqueId;}
    public void setUnidadDefensaId(String unidadDefensaId) {this.unidadDefensaId = unidadDefensaId;}

    public void setUnidadesImpactadas(List<UnitSelectionDTO> unidadesImpactadas) {this.unidadesImpactadas = unidadesImpactadas;}

}
