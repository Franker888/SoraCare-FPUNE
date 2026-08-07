package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.EspecialidadDAO;
import py.fpune.soracare.dto.RegistroEspecialidadDTO;
import py.fpune.soracare.mvc.model.Especialidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EspecialidadService {

    private static final int DURACION_MINIMA_MINUTOS =
            10;

    private static final int DURACION_MAXIMA_MINUTOS =
            240;

    private static final String SQLSTATE_VIOLACION_UNICIDAD =
            "23505";

    private final EspecialidadDAO especialidadDAO;

    public EspecialidadService() {
        this.especialidadDAO =
                new EspecialidadDAO();
    }

    public Especialidad registrar(
            RegistroEspecialidadDTO datosRegistro
    ) throws SQLException {

        validarDatosRegistro(
                datosRegistro
        );

        String nombre =
                normalizarTextoObligatorio(
                        datosRegistro.getNombre(),
                        "El nombre de la especialidad "
                                + "es obligatorio."
                );

        String descripcion =
                normalizarTextoOpcional(
                        datosRegistro.getDescripcion()
                );

        int duracionConsultaMinutos =
                convertirDuracionConsulta(
                        datosRegistro
                                .getDuracionConsultaMinutos()
                );

        Especialidad especialidad =
                new Especialidad(
                        nombre,
                        descripcion,
                        duracionConsultaMinutos
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                verificarNombreDisponible(
                        conexion,
                        especialidad.getNombre()
                );

                Especialidad especialidadCreada =
                        especialidadDAO.crear(
                                conexion,
                                especialidad
                        );

                conexion.commit();

                return especialidadCreada;

            } catch (SQLException error) {

                ejecutarRollback(
                        conexion,
                        error
                );

                if (esViolacionDeUnicidad(
                        error
                )) {
                    throw new IllegalArgumentException(
                            "Ya existe una especialidad "
                                    + "registrada con ese nombre.",
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

    public Optional<Especialidad> buscarPorId(
            int idEspecialidad
    ) throws SQLException {

        if (idEspecialidad <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la especialidad "
                            + "debe ser mayor que cero."
            );
        }

        return especialidadDAO.buscarPorId(
                idEspecialidad
        );
    }

    public List<Especialidad> listarTodas()
            throws SQLException {

        return especialidadDAO.listarTodas();
    }

    public List<Especialidad> listarActivas()
            throws SQLException {

        return especialidadDAO.listarActivas();
    }

    private void verificarNombreDisponible(
            Connection conexion,
            String nombre
    ) throws SQLException {

        Optional<Especialidad> especialidadExistente =
                especialidadDAO.buscarPorNombre(
                        conexion,
                        nombre
                );

        if (especialidadExistente.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una especialidad "
                            + "registrada con ese nombre."
            );
        }
    }

    private static void validarDatosRegistro(
            RegistroEspecialidadDTO datosRegistro
    ) {
        if (datosRegistro == null) {
            throw new IllegalArgumentException(
                    "Los datos de la especialidad "
                            + "son obligatorios."
            );
        }
    }

    private static int convertirDuracionConsulta(
            String duracion
    ) {
        if (duracion == null
                || duracion.isBlank()) {

            throw new IllegalArgumentException(
                    "La duración estimada de la consulta "
                            + "es obligatoria."
            );
        }

        String duracionNormalizada =
                duracion.strip();

        if (!duracionNormalizada.matches(
                "\\d+"
        )) {
            throw new IllegalArgumentException(
                    "La duración de la consulta debe "
                            + "contener solamente números."
            );
        }

        try {
            int duracionNumerica =
                    Integer.parseInt(
                            duracionNormalizada
                    );

            if (duracionNumerica
                    < DURACION_MINIMA_MINUTOS
                    || duracionNumerica
                    > DURACION_MAXIMA_MINUTOS) {

                throw new IllegalArgumentException(
                        "La duración de la consulta debe estar "
                                + "entre "
                                + DURACION_MINIMA_MINUTOS
                                + " y "
                                + DURACION_MAXIMA_MINUTOS
                                + " minutos."
                );
            }

            return duracionNumerica;

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La duración de la consulta "
                            + "no es un número válido.",
                    error
            );
        }
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