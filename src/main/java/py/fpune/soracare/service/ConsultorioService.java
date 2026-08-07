package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.ConsultorioDAO;
import py.fpune.soracare.dao.SedeDAO;
import py.fpune.soracare.dto.RegistroConsultorioDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Sede;
import py.fpune.soracare.mvc.model.TipoConsultorio;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ConsultorioService {

    private static final String SQLSTATE_VIOLACION_UNICIDAD =
            "23505";

    private final ConsultorioDAO consultorioDAO;
    private final SedeDAO sedeDAO;

    public ConsultorioService() {
        this.consultorioDAO =
                new ConsultorioDAO();

        this.sedeDAO =
                new SedeDAO();
    }

    public Consultorio registrar(
            RegistroConsultorioDTO datosRegistro
    ) throws SQLException {

        if (datosRegistro == null) {
            throw new IllegalArgumentException(
                    "Los datos del consultorio son obligatorios."
            );
        }

        int idSede =
                convertirIdSede(
                        datosRegistro.getIdSede()
                );

        String codigo =
                normalizarCodigo(
                        datosRegistro.getCodigo()
                );

        String nombre =
                normalizarTextoObligatorio(
                        datosRegistro.getNombre(),
                        "El nombre del consultorio es obligatorio."
                );

        String piso =
                normalizarTextoOpcional(
                        datosRegistro.getPiso()
                );

        String ubicacionReferencia =
                normalizarTextoOpcional(
                        datosRegistro.getUbicacionReferencia()
                );

        TipoConsultorio tipo =
                convertirTipo(
                        datosRegistro.getTipo()
                );

        short capacidad =
                convertirCapacidad(
                        datosRegistro.getCapacidad()
                );

        boolean accesible =
                convertirAccesible(
                        datosRegistro.getAccesible()
                );

        Consultorio consultorio =
                new Consultorio(
                        idSede,
                        codigo,
                        nombre,
                        piso,
                        ubicacionReferencia,
                        tipo,
                        capacidad,
                        accesible
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                verificarSedeDisponible(
                        conexion,
                        idSede
                );

                verificarCodigoDisponible(
                        conexion,
                        idSede,
                        codigo
                );

                Consultorio consultorioCreado =
                        consultorioDAO.crear(
                                conexion,
                                consultorio
                        );

                conexion.commit();

                return consultorioCreado;

            } catch (SQLException error) {

                ejecutarRollback(
                        conexion,
                        error
                );

                if (esViolacionDeUnicidad(
                        error
                )) {
                    throw new IllegalArgumentException(
                            "Ya existe un consultorio "
                                    + "con ese código "
                                    + "dentro de la sede seleccionada.",
                            error
                    );
                }

                throw error;

            } catch (RuntimeException error) {

                ejecutarRollback(
                        conexion,
                        error
                );

                throw error;
            }
        }
    }

    public Optional<Consultorio> buscarPorId(
            long idConsultorio
    ) throws SQLException {

        if (idConsultorio <= 0) {
            throw new IllegalArgumentException(
                    "El ID del consultorio debe ser "
                            + "mayor que cero."
            );
        }

        return consultorioDAO.buscarPorId(
                idConsultorio
        );
    }

    public List<Consultorio> listarTodos()
            throws SQLException {

        return consultorioDAO.listarTodos();
    }

    public List<Consultorio> listarPorSede(
            int idSede
    ) throws SQLException {

        if (idSede <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la sede debe ser mayor que cero."
            );
        }

        return consultorioDAO.listarPorSede(
                idSede
        );
    }

    public List<Consultorio> listarDisponiblesPorSede(
            int idSede
    ) throws SQLException {

        if (idSede <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la sede debe ser mayor que cero."
            );
        }

        return consultorioDAO.listarDisponiblesPorSede(
                idSede
        );
    }

    private void verificarSedeDisponible(
            Connection conexion,
            int idSede
    ) throws SQLException {

        Optional<Sede> sede =
                sedeDAO.buscarPorId(
                        conexion,
                        idSede
                );

        if (sede.isEmpty()) {
            throw new IllegalArgumentException(
                    "La sede seleccionada no existe."
            );
        }

        if (!sede.get().isActivo()) {
            throw new IllegalArgumentException(
                    "La sede seleccionada se encuentra inactiva."
            );
        }
    }

    private void verificarCodigoDisponible(
            Connection conexion,
            int idSede,
            String codigo
    ) throws SQLException {

        Optional<Consultorio> consultorioExistente =
                consultorioDAO.buscarPorSedeYCodigo(
                        conexion,
                        idSede,
                        codigo
                );

        if (consultorioExistente.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un consultorio "
                            + "con ese código "
                            + "dentro de la sede seleccionada."
            );
        }
    }

    private static int convertirIdSede(
            String idSede
    ) {
        if (idSede == null
                || idSede.isBlank()) {

            throw new IllegalArgumentException(
                    "La sede del consultorio es obligatoria."
            );
        }

        try {
            int id =
                    Integer.parseInt(
                            idSede.strip()
                    );

            if (id <= 0) {
                throw new IllegalArgumentException(
                        "El ID de la sede debe ser mayor que cero."
                );
            }

            return id;

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La sede seleccionada no es válida.",
                    error
            );
        }
    }

    private static String normalizarCodigo(
            String codigo
    ) {
        String codigoNormalizado =
                normalizarTextoObligatorio(
                        codigo,
                        "El código del consultorio es obligatorio."
                );

        return codigoNormalizado.toUpperCase(
                Locale.ROOT
        );
    }

    private static TipoConsultorio convertirTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {

            throw new IllegalArgumentException(
                    "El tipo de consultorio es obligatorio."
            );
        }

        try {
            return TipoConsultorio.valueOf(
                    tipo.strip()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "El tipo de consultorio no es válido.",
                    error
            );
        }
    }

    private static short convertirCapacidad(
            String capacidad
    ) {
        if (capacidad == null
                || capacidad.isBlank()) {

            throw new IllegalArgumentException(
                    "La capacidad del consultorio es obligatoria."
            );
        }

        String capacidadNormalizada =
                capacidad.strip();

        if (!capacidadNormalizada.matches(
                "\\d+"
        )) {
            throw new IllegalArgumentException(
                    "La capacidad debe contener solamente números."
            );
        }

        try {
            short capacidadNumerica =
                    Short.parseShort(
                            capacidadNormalizada
                    );

            if (capacidadNumerica < 1
                    || capacidadNumerica > 50) {

                throw new IllegalArgumentException(
                        "La capacidad del consultorio debe estar "
                                + "entre 1 y 50 personas."
                );
            }

            return capacidadNumerica;

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La capacidad del consultorio "
                            + "no es un número válido.",
                    error
            );
        }
    }

    private static boolean convertirAccesible(
            String accesible
    ) {
        if (accesible == null
                || accesible.isBlank()) {

            return false;
        }

        String valor =
                accesible.strip()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return switch (valor) {
            case "true",
                 "on",
                 "1",
                 "si",
                 "sí" -> true;

            case "false",
                 "off",
                 "0",
                 "no" -> false;

            default ->
                    throw new IllegalArgumentException(
                            "El valor de accesibilidad "
                                    + "no es válido."
                    );
        };
    }

    private static String normalizarTextoObligatorio(
            String valor,
            String mensajeError
    ) {
        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    mensajeError
            );
        }

        return valor.strip();
    }

    private static String normalizarTextoOpcional(
            String valor
    ) {
        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        return valor.strip();
    }

    private static boolean esViolacionDeUnicidad(
            SQLException error
    ) {
        SQLException errorActual =
                error;

        while (errorActual != null) {

            if (SQLSTATE_VIOLACION_UNICIDAD.equals(
                    errorActual.getSQLState()
            )) {
                return true;
            }

            errorActual =
                    errorActual.getNextException();
        }

        return false;
    }

    private static void ejecutarRollback(
            Connection conexion,
            Exception errorOriginal
    ) {
        try {
            conexion.rollback();

        } catch (SQLException errorRollback) {
            errorOriginal.addSuppressed(
                    errorRollback
            );
        }
    }
}