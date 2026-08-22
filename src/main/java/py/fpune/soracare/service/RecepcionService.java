package py.fpune.soracare.service;

import py.fpune.soracare.dao.RecepcionDAO;
import py.fpune.soracare.dto.ResumenPanelRecepcionDTO;

import java.sql.SQLException;

public class RecepcionService {

    private final RecepcionDAO recepcionDAO = new RecepcionDAO();

    public ResumenPanelRecepcionDTO consultarResumen() throws SQLException {
        return recepcionDAO.consultarResumen();
    }
}
