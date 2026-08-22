package py.fpune.soracare.service;

import py.fpune.soracare.dao.PanelMedicoDAO;
import py.fpune.soracare.dto.ResumenPanelMedicoDTO;

import java.sql.SQLException;

public class PanelMedicoService {

    private final PanelMedicoDAO panelMedicoDAO = new PanelMedicoDAO();

    public ResumenPanelMedicoDTO consultar(long idPersona) throws SQLException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta médica no es válida.");
        }
        return panelMedicoDAO.consultarResumen(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con un médico activo."));
    }
}
