package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.SedeDAO;
import py.fpune.soracare.dto.RegistroSedeDTO;
import py.fpune.soracare.mvc.model.Sede;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SedeService {

    private static final String SQLSTATE_VIOLACION_UNICIDAD =
            "23505";

    private final SedeDAO sedeDAO;

    public SedeService() {
        this.sedeDAO =
                new SedeDAO();
    }

    public Sede registrar(
            RegistroSedeDTO datosRegistro
    ) throws SQLException {

        if (datosRegistro == null) {
            throw new IllegalArgumentException(
                    "Los datos de la sede son obligatorios."
            );
        }

        String codigo =
                normalizarCodigo(
                        datosRegistro.getCodigo()
                );

        String nombre =
                normalizarTextoObligatorio(
                        datosRegistro.getNombre(),
                        "El nombre de la sede es obligatorio."
                );

        String direccion =
                normalizarTextoObligatorio(
                        datosRegistro.getDireccion(),
                        "La dirección de la sede es obligatoria."
                );

        String ciudad =
                normalizarTextoObligatorio(
                        datosRegistro.getCiudad(),
                        "La ciudad de la sede es obligatoria."
                );

        String departamento =
                normalizarTextoOpcional(
                        datosRegistro.getDepartamento()
                );

        String telefono =
                normalizarTextoOpcional(
                        datosRegistro.getTelefono()
                );

        String correo =
                normalizarCorreo(
                        datosRegistro.getCorreo()
                );

        Sede sede =
                new Sede(
                        codigo,
                        nombre,
                        direccion,
                        ciudad,
                        departamento,
                        telefono,
                        correo
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                verificarCodigoDisponible(
                        conexion,
                        sede.getCodigo()
                );

                verificarNombreDisponible(
                        conexion,
                        sede.getNombre()
                );

                Sede sedeCreada =
                        sedeDAO.crear(
                                conexion,
                                sede
                        );

                conexion.commit();

                return sedeCreada;

            } catch (SQLException error) {

                ejecutarRollback(
                        conexion,
                        error
                );

                if (esViolacionDeUnicidad(
                        error
                )) {
                    throw new IllegalArgumentException(
                            "Ya existe una sede registrada "
                                    + "con ese código o nombre.",
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

    public Optional<Sede> buscarPorId(
            int idSede
    ) throws SQLException {

        if (idSede <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la sede debe ser mayor que cero."
            );
        }

        return sedeDAO.buscarPorId(
                idSede
        );
    }

    public List<Sede> listarTodas()
            throws SQLException {

        return sedeDAO.listarTodas();
    }

    public List<Sede> listarActivas()
            throws SQLException {

        return sedeDAO.listarActivas();
    }

    private void verificarCodigoDisponible(
            Connection conexion,
            String codigo
    ) throws SQLException {

        Optional<Sede> sedeExistente =
                sedeDAO.buscarPorCodigo(
                        conexion,
                        codigo
                );

        if (sedeExistente.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una sede registrada "
                            + "con ese código."
            );
        }
    }

    private void verificarNombreDisponible(
            Connection conexion,
            String nombre
    ) throws SQLException {

        Optional<Sede> sedeExistente =
                sedeDAO.buscarPorNombre(
                        conexion,
                        nombre
                );

        if (sedeExistente.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una sede registrada "
                            + "con ese nombre."
            );
        }
    }

    private static String normalizarCodigo(
            String codigo
    ) {
        String codigoNormalizado =
                normalizarTextoObligatorio(
                        codigo,
                        "El código de la sede es obligatorio."
                );

        return codigoNormalizado.toUpperCase(
                Locale.ROOT
        );
    }

    private static String normalizarCorreo(
            String correo
    ) {
        String correoNormalizado =
                normalizarTextoOpcional(
                        correo
                );

        if (correoNormalizado == null) {
            return null;
        }

        return correoNormalizado.toLowerCase(
                Locale.ROOT
        );
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