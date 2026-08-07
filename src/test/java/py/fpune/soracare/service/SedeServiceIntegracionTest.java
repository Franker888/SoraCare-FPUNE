package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.RegistroSedeDTO;
import py.fpune.soracare.mvc.model.Sede;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class SedeServiceIntegracionTest {

    private static final String SQL_ELIMINAR_SEDE_TEMPORAL = """
            DELETE FROM soracare.sedes
            WHERE LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
               OR LOWER(BTRIM(nombre)) =
                  LOWER(BTRIM(?))
            """;

    public static void main(
            String[] args
    ) throws Exception {

        SedeService servicio =
                new SedeService();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String codigoTemporal =
                "ST-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String nombreTemporal =
                "Sede Servicio "
                        + identificadorTemporal;

        try {
            probarSolicitudNula(
                    servicio
            );

            probarCodigoVacio(
                    servicio
            );

            probarNombreVacio(
                    servicio
            );

            probarDireccionVacia(
                    servicio
            );

            probarCiudadVacia(
                    servicio
            );

            Sede sedeCreada =
                    probarRegistroCorrecto(
                            servicio,
                            codigoTemporal,
                            nombreTemporal,
                            identificadorTemporal
                    );

            probarBusquedaPorId(
                    servicio,
                    sedeCreada
            );

            probarListadoGeneral(
                    servicio,
                    sedeCreada
            );

            probarListadoActivo(
                    servicio,
                    sedeCreada
            );

            probarCodigoDuplicado(
                    servicio,
                    codigoTemporal,
                    identificadorTemporal
            );

            probarNombreDuplicado(
                    servicio,
                    nombreTemporal,
                    identificadorTemporal
            );

            System.out.println();
            System.out.println(
                    "SedeService fue probado correctamente."
            );

            System.out.println(
                    "Los campos obligatorios vacíos "
                            + "fueron rechazados."
            );

            System.out.println(
                    "La sede fue registrada correctamente."
            );

            System.out.println(
                    "El código y el correo fueron normalizados."
            );

            System.out.println(
                    "La búsqueda y los listados "
                            + "fueron verificados."
            );

            System.out.println(
                    "El código duplicado fue rechazado."
            );

            System.out.println(
                    "El nombre duplicado fue rechazado."
            );

        } finally {
            eliminarSedeTemporal(
                    codigoTemporal,
                    nombreTemporal
            );

            verificarEliminacion(
                    servicio,
                    codigoTemporal,
                    nombreTemporal
            );

            System.out.println(
                    "La sede temporal fue eliminada. "
                            + "No quedaron datos de prueba."
            );
        }
    }

    private static void probarSolicitudNula(
            SedeService servicio
    ) throws SQLException {

        try {
            servicio.registrar(
                    null
            );

            throw new AssertionError(
                    "Una solicitud nula debería ser rechazada."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    "Solicitud nula: RECHAZADA."
            );
        }
    }

    private static void probarCodigoVacio(
            SedeService servicio
    ) throws SQLException {

        RegistroSedeDTO datos =
                crearDatosBase(
                        "   ",
                        "Sede Código Vacío",
                        "codigo.vacio@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Código vacío"
        );
    }

    private static void probarNombreVacio(
            SedeService servicio
    ) throws SQLException {

        RegistroSedeDTO datos =
                crearDatosBase(
                        "SEDE-NOMBRE-VACIO",
                        "   ",
                        "nombre.vacio@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Nombre vacío"
        );
    }

    private static void probarDireccionVacia(
            SedeService servicio
    ) throws SQLException {

        RegistroSedeDTO datos =
                new RegistroSedeDTO(
                        "SEDE-DIRECCION-VACIA",
                        "Sede Dirección Vacía",
                        "   ",
                        "Ciudad del Este",
                        "Alto Paraná",
                        null,
                        null
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Dirección vacía"
        );
    }

    private static void probarCiudadVacia(
            SedeService servicio
    ) throws SQLException {

        RegistroSedeDTO datos =
                new RegistroSedeDTO(
                        "SEDE-CIUDAD-VACIA",
                        "Sede Ciudad Vacía",
                        "Avenida Temporal 100",
                        "   ",
                        "Alto Paraná",
                        null,
                        null
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Ciudad vacía"
        );
    }

    private static void verificarRegistroRechazado(
            SedeService servicio,
            RegistroSedeDTO datos,
            String descripcion
    ) throws SQLException {

        try {
            servicio.registrar(
                    datos
            );

            throw new AssertionError(
                    descripcion
                            + " debería ser rechazado."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    descripcion
                            + ": RECHAZADO."
            );
        }
    }

    private static Sede probarRegistroCorrecto(
            SedeService servicio,
            String codigoTemporal,
            String nombreTemporal,
            String identificadorTemporal
    ) throws SQLException {

        RegistroSedeDTO datos =
                new RegistroSedeDTO(
                        "  "
                                + codigoTemporal
                                .toLowerCase(Locale.ROOT)
                                + "  ",
                        "  "
                                + nombreTemporal
                                + "  ",
                        "  Avenida Temporal 1234  ",
                        "  Ciudad del Este  ",
                        "  Alto Paraná  ",
                        "  061 555 123  ",
                        "  SEDE."
                                + identificadorTemporal
                                .toUpperCase(Locale.ROOT)
                                + "@SORACARE.TEST  "
                );

        Sede sede =
                servicio.registrar(
                        datos
                );

        if (sede == null) {
            throw new AssertionError(
                    "El servicio no devolvió la sede registrada."
            );
        }

        if (sede.getIdSede() == null
                || sede.getIdSede() <= 0) {

            throw new AssertionError(
                    "La sede registrada no posee un ID válido."
            );
        }

        if (!codigoTemporal.equals(
                sede.getCodigo()
        )) {
            throw new AssertionError(
                    "El código no fue normalizado "
                            + "correctamente."
            );
        }

        String correoEsperado =
                "sede."
                        + identificadorTemporal
                        .toLowerCase(Locale.ROOT)
                        + "@soracare.test";

        if (!correoEsperado.equals(
                sede.getCorreo()
        )) {
            throw new AssertionError(
                    "El correo no fue normalizado "
                            + "correctamente."
            );
        }

        if (!nombreTemporal.equals(
                sede.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre no fue normalizado "
                            + "correctamente."
            );
        }

        if (!sede.isActivo()) {
            throw new AssertionError(
                    "La sede nueva debería estar activa."
            );
        }

        System.out.println(
                "Registro correcto: SEDE CREADA."
        );

        return sede;
    }

    private static void probarBusquedaPorId(
            SedeService servicio,
            Sede sedeCreada
    ) throws SQLException {

        Optional<Sede> resultado =
                servicio.buscarPorId(
                        sedeCreada.getIdSede()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La sede no fue encontrada por ID."
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

    private static void probarListadoGeneral(
            SedeService servicio,
            Sede sedeCreada
    ) throws SQLException {

        List<Sede> sedes =
                servicio.listarTodas();

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
                    "La sede no aparece "
                            + "en el listado general."
            );
        }
    }

    private static void probarListadoActivo(
            SedeService servicio,
            Sede sedeCreada
    ) throws SQLException {

        List<Sede> sedes =
                servicio.listarActivas();

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
                    "La sede no aparece "
                            + "en el listado de activas."
            );
        }
    }

    private static void probarCodigoDuplicado(
            SedeService servicio,
            String codigoTemporal,
            String identificadorTemporal
    ) throws SQLException {

        RegistroSedeDTO datos =
                crearDatosBase(
                        "   "
                                + codigoTemporal
                                .toLowerCase(Locale.ROOT)
                                + "   ",
                        "Otra Sede "
                                + identificadorTemporal,
                        "codigo.duplicado@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Código duplicado"
        );
    }

    private static void probarNombreDuplicado(
            SedeService servicio,
            String nombreTemporal,
            String identificadorTemporal
    ) throws SQLException {

        RegistroSedeDTO datos =
                crearDatosBase(
                        "OTRA-"
                                + identificadorTemporal
                                .toUpperCase(Locale.ROOT),
                        "   "
                                + nombreTemporal
                                .toUpperCase(Locale.ROOT)
                                + "   ",
                        "nombre.duplicado@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Nombre duplicado"
        );
    }

    private static RegistroSedeDTO crearDatosBase(
            String codigo,
            String nombre,
            String correo
    ) {
        return new RegistroSedeDTO(
                codigo,
                nombre,
                "Avenida Temporal 100",
                "Ciudad del Este",
                "Alto Paraná",
                "061 555 100",
                correo
        );
    }

    private static void eliminarSedeTemporal(
            String codigoTemporal,
            String nombreTemporal
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_ELIMINAR_SEDE_TEMPORAL
                        )
        ) {
            sentencia.setString(
                    1,
                    codigoTemporal
            );

            sentencia.setString(
                    2,
                    nombreTemporal
            );

            sentencia.executeUpdate();
        }
    }

    private static void verificarEliminacion(
            SedeService servicio,
            String codigoTemporal,
            String nombreTemporal
    ) throws SQLException {

        boolean permaneceRegistrada =
                servicio.listarTodas()
                        .stream()
                        .anyMatch(
                                sede ->
                                        codigoTemporal.equalsIgnoreCase(
                                                sede.getCodigo()
                                        )
                                                || nombreTemporal
                                                .equalsIgnoreCase(
                                                        sede.getNombre()
                                                )
                        );

        if (permaneceRegistrada) {
            throw new AssertionError(
                    "La sede temporal no fue eliminada."
            );
        }
    }
}