package py.fpune.soracare.service;

import py.fpune.soracare.dao.MedicamentoDAO;
import py.fpune.soracare.dto.RegistroMedicamentoDTO;
import py.fpune.soracare.mvc.model.Medicamento;
import py.fpune.soracare.mvc.model.ViaAdministracion;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class MedicamentoService {

    private static final String SQLSTATE_UNICIDAD = "23505";
    private final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();

    public Medicamento registrar(RegistroMedicamentoDTO datos) throws SQLException {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del medicamento son obligatorios.");
        }
        Medicamento medicamento = new Medicamento();
        medicamento.setNombreComercial(datos.nombreComercial());
        medicamento.setPrincipioActivo(datos.principioActivo());
        medicamento.setPresentacion(datos.presentacion());
        medicamento.setConcentracion(datos.concentracion());
        medicamento.setViaAdministracion(convertirVia(datos.viaAdministracion()));
        medicamento.setRequiereReceta(convertirBooleano(datos.requiereReceta()));

        try {
            return medicamentoDAO.crear(medicamento);
        } catch (SQLException error) {
            if (esViolacionDeUnicidad(error)) {
                throw new IllegalArgumentException(
                        "Ya existe un medicamento con esos datos principales.",
                        error
                );
            }
            throw error;
        }
    }

    public List<Medicamento> listarTodos() throws SQLException {
        return medicamentoDAO.listarTodos();
    }

    public List<Medicamento> listarActivos() throws SQLException {
        return medicamentoDAO.listarActivos();
    }

    public void cambiarEstado(long idMedicamento, boolean activo) throws SQLException {
        if (idMedicamento <= 0) {
            throw new IllegalArgumentException("El medicamento seleccionado no es válido.");
        }
        medicamentoDAO.cambiarEstado(idMedicamento, activo);
    }

    private static ViaAdministracion convertirVia(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Seleccioná la vía de administración.");
        }
        try {
            return ViaAdministracion.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("La vía de administración no es válida.", error);
        }
    }

    private static boolean convertirBooleano(String valor) {
        return valor != null && switch (valor.strip().toLowerCase(Locale.ROOT)) {
            case "true", "on", "1", "si", "sí" -> true;
            default -> false;
        };
    }

    private static boolean esViolacionDeUnicidad(Throwable error) {
        Throwable causaActual = error;

        while (causaActual != null) {
            if (causaActual instanceof SQLException errorSql) {
                SQLException errorActual = errorSql;

                while (errorActual != null) {
                    if (SQLSTATE_UNICIDAD.equals(errorActual.getSQLState())) {
                        return true;
                    }
                    errorActual = errorActual.getNextException();
                }
            }
            causaActual = causaActual.getCause();
        }

        return false;
    }
}
