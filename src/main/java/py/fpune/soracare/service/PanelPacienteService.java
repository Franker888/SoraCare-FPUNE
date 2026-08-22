package py.fpune.soracare.service;

import py.fpune.soracare.dao.PanelPacienteDAO;
import py.fpune.soracare.dto.ResumenPanelPacienteDTO;

import java.sql.SQLException;

public class PanelPacienteService {

    private final PanelPacienteDAO panelPacienteDAO = new PanelPacienteDAO();

    public ResumenPanelPacienteDTO consultar(long idPersona) throws SQLException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta del paciente no es válida.");
        }
        return panelPacienteDAO.consultarResumen(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con una ficha de paciente."));
    }
}
