package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.EstadoConsultorio;
import py.fpune.soracare.mvc.model.Sede;
import py.fpune.soracare.mvc.model.TipoConsultorio;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ConsultorioDAOIntegracionTest {

    public static void main(
            String[] args
    ) throws Exception {

        SedeDAO sedeDAO =
                new SedeDAO();

        ConsultorioDAO consultorioDAO =
                new ConsultorioDAO();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String codigoSedeTemporal =
                "SD-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String nombreSedeTemporal =
                "Sede Consultorio "
                        + identificadorTemporal;

        String codigoConsultorioTemporal =
                "C-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String nombreConsultorioTemporal =
                "Consultorio Prueba "
                        + identificadorTemporal;

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(false);

            try {
                Sede sedeTemporal =
                        crearSedeTemporal(
                                sedeDAO,
                                conexion,
                                codigoSedeTemporal,
                                nombreSedeTemporal
                        );

                Consultorio consultorioCreado =
                        crearConsultorioTemporal(
                                consultorioDAO,
                                conexion,
                                sedeTemporal,
                                codigoConsultorioTemporal,
                                nombreConsultorioTemporal
                        );

                verificarConsultorioCreado(
                        consultorioCreado,
                        sedeTemporal,
                        codigoConsultorioTemporal,
                        nombreConsultorioTemporal
                );

                verificarBusquedaPorId(
                        consultorioDAO,
                        conexion,
                        consultorioCreado
                );

                verificarBusquedaPorSedeYCodigo(
                        consultorioDAO,
                        conexion,
                        consultorioCreado
                );

                verificarListadoGeneral(
                        consultorioDAO,
                        conexion,
                        consultorioCreado
                );

                verificarListadoPorSede(
                        consultorioDAO,
                        conexion,
                        consultorioCreado
                );

                verificarListadoDisponiblePorSede(
                        consultorioDAO,
                        conexion,
                        consultorioCreado
                );

                verificarBusquedaInexistente(
                        consultorioDAO,
                        conexion,
                        sedeTemporal
                );

                System.out.println(
                        "ConsultorioDAO fue probado correctamente."
                );

                System.out.println(
                        "La sede temporal fue creada."
                );

                System.out.println(
                        "El consultorio temporal fue creado."
                );

                System.out.println(
                        "La búsqueda por ID fue verificada."
                );

                System.out.println(
                        "La búsqueda normalizada por sede y código "
                                + "fue verificada."
                );

                System.out.println(
                        "El listado general fue verificado."
                );

                System.out.println(
                        "El listado por sede fue verificado."
                );

                System.out.println(
                        "El listado de consultorios disponibles "
                                + "fue verificado."
                );

                System.out.println(
                        "El consultorio inexistente "
                                + "no fue encontrado."
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

    private static Sede crearSedeTemporal(
            SedeDAO sedeDAO,
            Connection conexion,
            String codigo,
            String nombre
    ) throws SQLException {

        Sede sede =
                new Sede(
                        codigo,
                        nombre,
                        "Avenida Temporal 100",
                        "Ciudad del Este",
                        "Alto Paraná",
                        null,
                        null
                );

        return sedeDAO.crear(
                conexion,
                sede
        );
    }

    private static Consultorio crearConsultorioTemporal(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Sede sede,
            String codigo,
            String nombre
    ) throws SQLException {

        Consultorio consultorio =
                new Consultorio(
                        sede.getIdSede(),
                        codigo,
                        nombre,
                        "2",
                        "Segundo piso, ala norte",
                        TipoConsultorio.CONSULTA,
                        (short) 2,
                        true
                );

        return consultorioDAO.crear(
                conexion,
                consultorio
        );
    }

    private static void verificarConsultorioCreado(
            Consultorio consultorio,
            Sede sede,
            String codigoEsperado,
            String nombreEsperado
    ) {
        if (consultorio == null) {
            throw new AssertionError(
                    "El DAO no devolvió el consultorio creado."
            );
        }

        if (consultorio.getIdConsultorio() == null
                || consultorio.getIdConsultorio() <= 0) {

            throw new AssertionError(
                    "El consultorio creado no posee un ID válido."
            );
        }

        if (!sede.getIdSede().equals(
                consultorio.getIdSede()
        )) {
            throw new AssertionError(
                    "La sede del consultorio no coincide."
            );
        }

        if (!codigoEsperado.equals(
                consultorio.getCodigo()
        )) {
            throw new AssertionError(
                    "El código del consultorio no coincide."
            );
        }

        if (!nombreEsperado.equals(
                consultorio.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre del consultorio no coincide."
            );
        }

        if (!"2".equals(
                consultorio.getPiso()
        )) {
            throw new AssertionError(
                    "El piso del consultorio no coincide."
            );
        }

        if (!"Segundo piso, ala norte".equals(
                consultorio.getUbicacionReferencia()
        )) {
            throw new AssertionError(
                    "La referencia de ubicación no coincide."
            );
        }

        if (consultorio.getTipo()
                != TipoConsultorio.CONSULTA) {

            throw new AssertionError(
                    "El tipo del consultorio no coincide."
            );
        }

        if (consultorio.getCapacidad() != 2) {
            throw new AssertionError(
                    "La capacidad del consultorio no coincide."
            );
        }

        if (!consultorio.isAccesible()) {
            throw new AssertionError(
                    "El consultorio debería ser accesible."
            );
        }

        if (consultorio.getEstado()
                != EstadoConsultorio.DISPONIBLE) {

            throw new AssertionError(
                    "El consultorio debería estar disponible."
            );
        }

        if (consultorio.getFechaCreacion() == null) {
            throw new AssertionError(
                    "La fecha de creación no fue recuperada."
            );
        }

        if (consultorio.getFechaActualizacion() == null) {
            throw new AssertionError(
                    "La fecha de actualización no fue recuperada."
            );
        }
    }

    private static void verificarBusquedaPorId(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Consultorio consultorioCreado
    ) throws SQLException {

        Optional<Consultorio> resultado =
                consultorioDAO.buscarPorId(
                        conexion,
                        consultorioCreado
                                .getIdConsultorio()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "No se encontró el consultorio por ID."
            );
        }

        if (!consultorioCreado
                .getCodigo()
                .equals(
                        resultado.get()
                                .getCodigo()
                )) {

            throw new AssertionError(
                    "El consultorio recuperado por ID "
                            + "no coincide."
            );
        }
    }

    private static void verificarBusquedaPorSedeYCodigo(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Consultorio consultorioCreado
    ) throws SQLException {

        String codigoConVariaciones =
                "   "
                        + consultorioCreado
                        .getCodigo()
                        .toLowerCase(Locale.ROOT)
                        + "   ";

        Optional<Consultorio> resultado =
                consultorioDAO.buscarPorSedeYCodigo(
                        conexion,
                        consultorioCreado.getIdSede(),
                        codigoConVariaciones
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La búsqueda normalizada por sede y código "
                            + "no encontró el consultorio."
            );
        }

        if (!consultorioCreado
                .getIdConsultorio()
                .equals(
                        resultado.get()
                                .getIdConsultorio()
                )) {

            throw new AssertionError(
                    "La búsqueda por sede y código "
                            + "devolvió otro consultorio."
            );
        }
    }

    private static void verificarListadoGeneral(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Consultorio consultorioCreado
    ) throws SQLException {

        List<Consultorio> consultorios =
                consultorioDAO.listarTodos(
                        conexion
                );

        boolean encontrado =
                consultorios.stream()
                        .anyMatch(
                                consultorio ->
                                        consultorioCreado
                                                .getIdConsultorio()
                                                .equals(
                                                        consultorio
                                                                .getIdConsultorio()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El consultorio creado no aparece "
                            + "en el listado general."
            );
        }
    }

    private static void verificarListadoPorSede(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Consultorio consultorioCreado
    ) throws SQLException {

        List<Consultorio> consultorios =
                consultorioDAO.listarPorSede(
                        conexion,
                        consultorioCreado.getIdSede()
                );

        boolean encontrado =
                consultorios.stream()
                        .anyMatch(
                                consultorio ->
                                        consultorioCreado
                                                .getIdConsultorio()
                                                .equals(
                                                        consultorio
                                                                .getIdConsultorio()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El consultorio creado no aparece "
                            + "en el listado de su sede."
            );
        }
    }

    private static void verificarListadoDisponiblePorSede(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Consultorio consultorioCreado
    ) throws SQLException {

        List<Consultorio> consultorios =
                consultorioDAO.listarDisponiblesPorSede(
                        conexion,
                        consultorioCreado.getIdSede()
                );

        boolean encontrado =
                consultorios.stream()
                        .anyMatch(
                                consultorio ->
                                        consultorioCreado
                                                .getIdConsultorio()
                                                .equals(
                                                        consultorio
                                                                .getIdConsultorio()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El consultorio creado no aparece "
                            + "en el listado de disponibles."
            );
        }
    }

    private static void verificarBusquedaInexistente(
            ConsultorioDAO consultorioDAO,
            Connection conexion,
            Sede sede
    ) throws SQLException {

        Optional<Consultorio> resultado =
                consultorioDAO.buscarPorSedeYCodigo(
                        conexion,
                        sede.getIdSede(),
                        "CONSULTORIO-INEXISTENTE-"
                                + UUID.randomUUID()
                                .toString()
                                .substring(0, 6)
                );

        if (resultado.isPresent()) {
            throw new AssertionError(
                    "No debería encontrarse "
                            + "el consultorio inexistente."
            );
        }
    }
}