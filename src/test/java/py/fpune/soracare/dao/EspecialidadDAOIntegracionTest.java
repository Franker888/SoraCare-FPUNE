package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Especialidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EspecialidadDAOIntegracionTest {

    public static void main(
            String[] args
    ) throws Exception {

        EspecialidadDAO especialidadDAO =
                new EspecialidadDAO();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String nombreTemporal =
                "Especialidad Prueba "
                        + identificadorTemporal;

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(false);

            try {
                Especialidad especialidadNueva =
                        new Especialidad(
                                nombreTemporal,
                                "Especialidad temporal utilizada "
                                        + "para probar EspecialidadDAO.",
                                45
                        );

                Especialidad especialidadCreada =
                        especialidadDAO.crear(
                                conexion,
                                especialidadNueva
                        );

                verificarEspecialidadCreada(
                        especialidadCreada,
                        nombreTemporal
                );

                verificarBusquedaPorId(
                        especialidadDAO,
                        conexion,
                        especialidadCreada
                );

                verificarBusquedaPorNombre(
                        especialidadDAO,
                        conexion,
                        especialidadCreada
                );

                verificarListadoCompleto(
                        especialidadDAO,
                        conexion,
                        especialidadCreada
                );

                verificarListadoActivo(
                        especialidadDAO,
                        conexion,
                        especialidadCreada
                );

                verificarBusquedaInexistente(
                        especialidadDAO,
                        conexion
                );

                System.out.println(
                        "EspecialidadDAO fue probado correctamente."
                );

                System.out.println(
                        "La especialidad temporal fue creada."
                );

                System.out.println(
                        "La búsqueda por ID fue verificada."
                );

                System.out.println(
                        "La búsqueda normalizada por nombre "
                                + "fue verificada."
                );

                System.out.println(
                        "Los listados general y activo "
                                + "fueron verificados."
                );

                System.out.println(
                        "La especialidad inexistente "
                                + "no fue encontrada."
                );

            } finally {
                conexion.rollback();

                System.out.println(
                        "La transacción fue revertida. "
                                + "No quedaron datos de prueba."
                );
            }
        }
    }

    private static void verificarEspecialidadCreada(
            Especialidad especialidad,
            String nombreEsperado
    ) {
        if (especialidad == null) {
            throw new AssertionError(
                    "El DAO no devolvió la especialidad creada."
            );
        }

        if (especialidad.getIdEspecialidad() == null
                || especialidad.getIdEspecialidad() <= 0) {

            throw new AssertionError(
                    "La especialidad creada no posee un ID válido."
            );
        }

        if (!nombreEsperado.equals(
                especialidad.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre de la especialidad creada "
                            + "no coincide."
            );
        }

        if (especialidad.getDuracionConsultaMinutos()
                != 45) {

            throw new AssertionError(
                    "La duración de la consulta "
                            + "no coincide."
            );
        }

        if (!especialidad.isActivo()) {
            throw new AssertionError(
                    "La especialidad nueva debería estar activa."
            );
        }

        if (especialidad.getFechaCreacion() == null) {
            throw new AssertionError(
                    "La fecha de creación no fue recuperada."
            );
        }

        if (especialidad.getFechaActualizacion() == null) {
            throw new AssertionError(
                    "La fecha de actualización no fue recuperada."
            );
        }
    }

    private static void verificarBusquedaPorId(
            EspecialidadDAO especialidadDAO,
            Connection conexion,
            Especialidad especialidadCreada
    ) throws SQLException {

        Optional<Especialidad> resultado =
                especialidadDAO.buscarPorId(
                        conexion,
                        especialidadCreada
                                .getIdEspecialidad()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "No se encontró la especialidad por ID."
            );
        }

        if (!especialidadCreada.getNombre().equals(
                resultado.get().getNombre()
        )) {
            throw new AssertionError(
                    "La especialidad recuperada por ID "
                            + "no coincide."
            );
        }
    }

    private static void verificarBusquedaPorNombre(
            EspecialidadDAO especialidadDAO,
            Connection conexion,
            Especialidad especialidadCreada
    ) throws SQLException {

        String nombreConVariaciones =
                "   "
                        + especialidadCreada
                        .getNombre()
                        .toUpperCase()
                        + "   ";

        Optional<Especialidad> resultado =
                especialidadDAO.buscarPorNombre(
                        conexion,
                        nombreConVariaciones
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La búsqueda normalizada por nombre "
                            + "no encontró la especialidad."
            );
        }

        if (!especialidadCreada
                .getIdEspecialidad()
                .equals(
                        resultado.get()
                                .getIdEspecialidad()
                )) {

            throw new AssertionError(
                    "La búsqueda por nombre devolvió "
                            + "otra especialidad."
            );
        }
    }

    private static void verificarListadoCompleto(
            EspecialidadDAO especialidadDAO,
            Connection conexion,
            Especialidad especialidadCreada
    ) throws SQLException {

        List<Especialidad> especialidades =
                especialidadDAO.listarTodas(
                        conexion
                );

        boolean encontrada =
                especialidades.stream()
                        .anyMatch(
                                especialidad ->
                                        especialidadCreada
                                                .getIdEspecialidad()
                                                .equals(
                                                        especialidad
                                                                .getIdEspecialidad()
                                                )
                        );

        if (!encontrada) {
            throw new AssertionError(
                    "La especialidad creada no aparece "
                            + "en el listado completo."
            );
        }
    }

    private static void verificarListadoActivo(
            EspecialidadDAO especialidadDAO,
            Connection conexion,
            Especialidad especialidadCreada
    ) throws SQLException {

        List<Especialidad> especialidadesActivas =
                especialidadDAO.listarActivas(
                        conexion
                );

        boolean encontrada =
                especialidadesActivas.stream()
                        .anyMatch(
                                especialidad ->
                                        especialidadCreada
                                                .getIdEspecialidad()
                                                .equals(
                                                        especialidad
                                                                .getIdEspecialidad()
                                                )
                        );

        if (!encontrada) {
            throw new AssertionError(
                    "La especialidad creada no aparece "
                            + "en el listado de activas."
            );
        }
    }

    private static void verificarBusquedaInexistente(
            EspecialidadDAO especialidadDAO,
            Connection conexion
    ) throws SQLException {

        Optional<Especialidad> resultado =
                especialidadDAO.buscarPorNombre(
                        conexion,
                        "Especialidad Inexistente "
                                + UUID.randomUUID()
                );

        if (resultado.isPresent()) {
            throw new AssertionError(
                    "No debería encontrarse "
                            + "la especialidad inexistente."
            );
        }
    }
}