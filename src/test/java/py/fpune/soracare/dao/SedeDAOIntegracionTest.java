package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Sede;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class SedeDAOIntegracionTest {

    public static void main(
            String[] args
    ) throws Exception {

        SedeDAO sedeDAO =
                new SedeDAO();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String codigoTemporal =
                "SD-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String nombreTemporal =
                "Sede Prueba "
                        + identificadorTemporal;

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(false);

            try {
                Sede sedeNueva =
                        new Sede(
                                codigoTemporal,
                                nombreTemporal,
                                "Avenida Temporal 1234",
                                "Ciudad del Este",
                                "Alto Paraná",
                                "061 555 123",
                                "sede."
                                        + identificadorTemporal
                                        + "@soracare.test"
                        );

                Sede sedeCreada =
                        sedeDAO.crear(
                                conexion,
                                sedeNueva
                        );

                verificarSedeCreada(
                        sedeCreada,
                        codigoTemporal,
                        nombreTemporal
                );

                verificarBusquedaPorId(
                        sedeDAO,
                        conexion,
                        sedeCreada
                );

                verificarBusquedaPorCodigo(
                        sedeDAO,
                        conexion,
                        sedeCreada
                );

                verificarBusquedaPorNombre(
                        sedeDAO,
                        conexion,
                        sedeCreada
                );

                verificarListadoCompleto(
                        sedeDAO,
                        conexion,
                        sedeCreada
                );

                verificarListadoActivo(
                        sedeDAO,
                        conexion,
                        sedeCreada
                );

                verificarCodigoInexistente(
                        sedeDAO,
                        conexion
                );

                verificarNombreInexistente(
                        sedeDAO,
                        conexion
                );

                System.out.println(
                        "SedeDAO fue probado correctamente."
                );

                System.out.println(
                        "La sede temporal fue creada."
                );

                System.out.println(
                        "La búsqueda por ID fue verificada."
                );

                System.out.println(
                        "La búsqueda normalizada por código "
                                + "fue verificada."
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
                        "Las búsquedas inexistentes "
                                + "no devolvieron resultados."
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

    private static void verificarSedeCreada(
            Sede sede,
            String codigoEsperado,
            String nombreEsperado
    ) {
        if (sede == null) {
            throw new AssertionError(
                    "El DAO no devolvió la sede creada."
            );
        }

        if (sede.getIdSede() == null
                || sede.getIdSede() <= 0) {

            throw new AssertionError(
                    "La sede creada no posee un ID válido."
            );
        }

        if (!codigoEsperado.equals(
                sede.getCodigo()
        )) {
            throw new AssertionError(
                    "El código de la sede creada "
                            + "no coincide."
            );
        }

        if (!nombreEsperado.equals(
                sede.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre de la sede creada "
                            + "no coincide."
            );
        }

        if (!"Avenida Temporal 1234".equals(
                sede.getDireccion()
        )) {
            throw new AssertionError(
                    "La dirección de la sede "
                            + "no coincide."
            );
        }

        if (!"Ciudad del Este".equals(
                sede.getCiudad()
        )) {
            throw new AssertionError(
                    "La ciudad de la sede "
                            + "no coincide."
            );
        }

        if (!"Alto Paraná".equals(
                sede.getDepartamento()
        )) {
            throw new AssertionError(
                    "El departamento de la sede "
                            + "no coincide."
            );
        }

        if (!sede.isActivo()) {
            throw new AssertionError(
                    "La sede nueva debería estar activa."
            );
        }

        if (sede.getFechaCreacion() == null) {
            throw new AssertionError(
                    "La fecha de creación no fue recuperada."
            );
        }

        if (sede.getFechaActualizacion() == null) {
            throw new AssertionError(
                    "La fecha de actualización no fue recuperada."
            );
        }
    }

    private static void verificarBusquedaPorId(
            SedeDAO sedeDAO,
            Connection conexion,
            Sede sedeCreada
    ) throws SQLException {

        Optional<Sede> resultado =
                sedeDAO.buscarPorId(
                        conexion,
                        sedeCreada.getIdSede()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "No se encontró la sede por ID."
            );
        }

        if (!sedeCreada.getCodigo().equals(
                resultado.get().getCodigo()
        )) {
            throw new AssertionError(
                    "La sede recuperada por ID "
                            + "no coincide."
            );
        }
    }

    private static void verificarBusquedaPorCodigo(
            SedeDAO sedeDAO,
            Connection conexion,
            Sede sedeCreada
    ) throws SQLException {

        String codigoConVariaciones =
                "   "
                        + sedeCreada
                        .getCodigo()
                        .toLowerCase(Locale.ROOT)
                        + "   ";

        Optional<Sede> resultado =
                sedeDAO.buscarPorCodigo(
                        conexion,
                        codigoConVariaciones
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La búsqueda normalizada por código "
                            + "no encontró la sede."
            );
        }

        if (!sedeCreada.getIdSede().equals(
                resultado.get().getIdSede()
        )) {
            throw new AssertionError(
                    "La búsqueda por código devolvió "
                            + "otra sede."
            );
        }
    }

    private static void verificarBusquedaPorNombre(
            SedeDAO sedeDAO,
            Connection conexion,
            Sede sedeCreada
    ) throws SQLException {

        String nombreConVariaciones =
                "   "
                        + sedeCreada
                        .getNombre()
                        .toUpperCase(Locale.ROOT)
                        + "   ";

        Optional<Sede> resultado =
                sedeDAO.buscarPorNombre(
                        conexion,
                        nombreConVariaciones
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La búsqueda normalizada por nombre "
                            + "no encontró la sede."
            );
        }

        if (!sedeCreada.getIdSede().equals(
                resultado.get().getIdSede()
        )) {
            throw new AssertionError(
                    "La búsqueda por nombre devolvió "
                            + "otra sede."
            );
        }
    }

    private static void verificarListadoCompleto(
            SedeDAO sedeDAO,
            Connection conexion,
            Sede sedeCreada
    ) throws SQLException {

        List<Sede> sedes =
                sedeDAO.listarTodas(
                        conexion
                );

        boolean encontrada =
                sedes.stream()
                        .anyMatch(
                                sede ->
                                        sedeCreada
                                                .getIdSede()
                                                .equals(
                                                        sede.getIdSede()
                                                )
                        );

        if (!encontrada) {
            throw new AssertionError(
                    "La sede creada no aparece "
                            + "en el listado completo."
            );
        }
    }

    private static void verificarListadoActivo(
            SedeDAO sedeDAO,
            Connection conexion,
            Sede sedeCreada
    ) throws SQLException {

        List<Sede> sedesActivas =
                sedeDAO.listarActivas(
                        conexion
                );

        boolean encontrada =
                sedesActivas.stream()
                        .anyMatch(
                                sede ->
                                        sedeCreada
                                                .getIdSede()
                                                .equals(
                                                        sede.getIdSede()
                                                )
                        );

        if (!encontrada) {
            throw new AssertionError(
                    "La sede creada no aparece "
                            + "en el listado de activas."
            );
        }
    }

    private static void verificarCodigoInexistente(
            SedeDAO sedeDAO,
            Connection conexion
    ) throws SQLException {

        Optional<Sede> resultado =
                sedeDAO.buscarPorCodigo(
                        conexion,
                        "INEX-"
                                + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                );

        if (resultado.isPresent()) {
            throw new AssertionError(
                    "No debería encontrarse "
                            + "el código inexistente."
            );
        }
    }

    private static void verificarNombreInexistente(
            SedeDAO sedeDAO,
            Connection conexion
    ) throws SQLException {

        Optional<Sede> resultado =
                sedeDAO.buscarPorNombre(
                        conexion,
                        "Sede Inexistente "
                                + UUID.randomUUID()
                );

        if (resultado.isPresent()) {
            throw new AssertionError(
                    "No debería encontrarse "
                            + "la sede inexistente."
            );
        }
    }
}