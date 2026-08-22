package py.fpune.soracare.service;

import py.fpune.soracare.dao.AdministracionSeguridadDAO;
import py.fpune.soracare.dto.ResumenUsuariosDTO;
import py.fpune.soracare.dto.RolAdministrativoDTO;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class AdministracionSeguridadService {

    private static final Set<String> ESTADOS = Set.of("", "ACTIVO", "BLOQUEADO", "INACTIVO");
    private final AdministracionSeguridadDAO seguridadDAO = new AdministracionSeguridadDAO();

    public ResumenUsuariosDTO listarUsuarios(String busqueda, String estado) throws SQLException {
        String q = normalizar(busqueda, 100);
        String estadoNormalizado = normalizar(estado, 20).toUpperCase();
        if (!ESTADOS.contains(estadoNormalizado)) {
            throw new IllegalArgumentException("El estado de usuario no es válido.");
        }
        return seguridadDAO.listarUsuarios(q, estadoNormalizado);
    }

    public List<RolAdministrativoDTO> listarRoles() throws SQLException {
        return seguridadDAO.listarRoles();
    }

    public void actualizarEstado(
            String idUsuario,
            String nuevoEstado,
            long idAdministrador
    ) throws SQLException {
        long id = convertirId(idUsuario);
        String estado = normalizar(nuevoEstado, 20).toUpperCase();
        if (!Set.of("ACTIVO", "INACTIVO").contains(estado)) {
            throw new IllegalArgumentException("La acción solicitada no es válida.");
        }
        if (id == idAdministrador && "INACTIVO".equals(estado)) {
            throw new IllegalArgumentException("No podés inactivar tu propia cuenta mientras la utilizás.");
        }
        if ("INACTIVO".equals(estado)
                && seguridadDAO.esAdministrador(id)
                && seguridadDAO.contarAdministradoresActivos() <= 1) {
            throw new IllegalArgumentException("No se puede inactivar el último administrador activo.");
        }
        seguridadDAO.actualizarEstado(id, estado);
    }

    public void desbloquear(String idUsuario) throws SQLException {
        seguridadDAO.actualizarEstado(convertirId(idUsuario), "ACTIVO");
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("La cuenta seleccionada no es válida.");
        }
    }

    private static String normalizar(String valor, int maximo) {
        if (valor == null) return "";
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("El filtro ingresado es demasiado extenso.");
        }
        return limpio;
    }
}
