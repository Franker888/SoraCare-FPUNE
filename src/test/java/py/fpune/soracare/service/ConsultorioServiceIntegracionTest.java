package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.SedeDAO;
import py.fpune.soracare.dto.RegistroConsultorioDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.EstadoConsultorio;
import py.fpune.soracare.mvc.model.Sede;
import py.fpune.soracare.mvc.model.TipoConsultorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ConsultorioServiceIntegracionTest {

    private static final String SQL_ELIMINAR_CONSULTORIOS_TEMPORALES = """
            DELETE FROM soracare.consultorios
            WHERE LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_ELIMINAR_SEDES_TEMPORALES = """
            DELETE FROM soracare.sedes
            WHERE LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
               OR LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
               OR LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
            """;

    public static void main(
            String[] args
    ) throws Exception {

        ConsultorioService servicio =
                new ConsultorioService();

        SedeDAO sedeDAO =
                new SedeDAO();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String codigoSedePrincipal =
                "CS1-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String codigoSedeSecundaria =
                "CS2-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String codigoSedeInactiva =
                "CSI-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        String codigoConsultorio =
                "CT-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        Sede sedePrincipal = null;
        Sede sedeSecundaria = null;
        Sede sedeInactiva = null;

        try {
            sedePrincipal =
                    crearSedeTemporal(
                            sedeDAO,
                            codigoSedePrincipal,
                            "Sede Consultorio Principal "
                                    + identificadorTemporal,
                            true
                    );

            sedeSecundaria =
                    crearSedeTemporal(
                            sedeDAO,
                            codigoSedeSecundaria,
                            "Sede Consultorio Secundaria "
                                    + identificadorTemporal,
                            true
                    );

            sedeInactiva =
                    crearSedeTemporal(
                            sedeDAO,
                            codigoSedeInactiva,
                            "Sede Consultorio Inactiva "
                                    + identificadorTemporal,
                            false
                    );

            probarSolicitudNula(
                    servicio
            );

            probarSedeVacia(
                    servicio
            );

            probarSedeInexistente(
                    servicio
            );

            probarSedeInactiva(
                    servicio,
                    sedeInactiva
            );

            probarCodigoVacio(
                    servicio,
                    sedePrincipal
            );

            probarTipoInvalido(
                    servicio,
                    sedePrincipal
            );

            probarCapacidadNoNumerica(
                    servicio,
                    sedePrincipal
            );

            probarCapacidadFueraDeRango(
                    servicio,
                    sedePrincipal
            );

            probarAccesibilidadInvalida(
                    servicio,
                    sedePrincipal
            );

            Consultorio consultorioCreado =
                    probarRegistroCorrecto(
                            servicio,
                            sedePrincipal,
                            codigoConsultorio
                    );

            probarBusquedaPorId(
                    servicio,
                    consultorioCreado
            );

            probarListadoGeneral(
                    servicio,
                    consultorioCreado
            );

            probarListadoPorSede(
                    servicio,
                    consultorioCreado
            );

            probarListadoDisponiblePorSede(
                    servicio,
                    consultorioCreado
            );

            probarCodigoDuplicadoMismaSede(
                    servicio,
                    sedePrincipal,
                    codigoConsultorio
            );

            Consultorio consultorioOtraSede =
                    probarMismoCodigoEnOtraSede(
                            servicio,
                            sedeSecundaria,
                            codigoConsultorio
                    );

            verificarConsultorioOtraSede(
                    consultorioCreado,
                    consultorioOtraSede
            );

            System.out.println();
            System.out.println(
                    "ConsultorioService fue probado correctamente."
            );

            System.out.println(
                    "La solicitud nula fue rechazada."
            );

            System.out.println(
                    "La sede vacía e inexistente "
                            + "fueron rechazadas."
            );

            System.out.println(
                    "La sede inactiva fue rechazada."
            );

            System.out.println(
                    "El código vacío fue rechazado."
            );

            System.out.println(
                    "El tipo inválido fue rechazado."
            );

            System.out.println(
                    "Las capacidades inválidas "
                            + "fueron rechazadas."
            );

            System.out.println(
                    "La accesibilidad inválida "
                            + "fue rechazada."
            );

            System.out.println(
                    "El consultorio fue registrado correctamente."
            );

            System.out.println(
                    "El código fue normalizado correctamente."
            );

            System.out.println(
                    "La búsqueda y los listados "
                            + "fueron verificados."
            );

            System.out.println(
                    "El código duplicado dentro de la misma sede "
                            + "fue rechazado."
            );

            System.out.println(
                    "El mismo código fue permitido "
                            + "en una sede diferente."
            );

        } finally {
            eliminarDatosTemporales(
                    codigoConsultorio,
                    codigoSedePrincipal,
                    codigoSedeSecundaria,
                    codigoSedeInactiva
            );

            verificarEliminacion(
                    servicio,
                    codigoConsultorio,
                    codigoSedePrincipal,
                    codigoSedeSecundaria,
                    codigoSedeInactiva
            );

            System.out.println(
                    "Los consultorios y sedes temporales "
                            + "fueron eliminados. "
                            + "No quedaron datos de prueba."
            );
        }
    }

    private static Sede crearSedeTemporal(
            SedeDAO sedeDAO,
            String codigo,
            String nombre,
            boolean activa
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

        sede.setActivo(
                activa
        );

        return sedeDAO.crear(
                sede
        );
    }

    private static void probarSolicitudNula(
            ConsultorioService servicio
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

    private static void probarSedeVacia(
            ConsultorioService servicio
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        "   ",
                        "C-SEDE-VACIA",
                        "Consultorio Sede Vacía",
                        "1",
                        null,
                        "CONSULTA",
                        "1",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Sede vacía"
        );
    }

    private static void probarSedeInexistente(
            ConsultorioService servicio
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                Integer.MAX_VALUE
                        ),
                        "C-SEDE-INEXISTENTE",
                        "Consultorio Sede Inexistente",
                        "1",
                        null,
                        "CONSULTA",
                        "1",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Sede inexistente"
        );
    }

    private static void probarSedeInactiva(
            ConsultorioService servicio,
            Sede sedeInactiva
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sedeInactiva.getIdSede()
                        ),
                        "C-SEDE-INACTIVA",
                        "Consultorio Sede Inactiva",
                        "1",
                        null,
                        "CONSULTA",
                        "1",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Sede inactiva"
        );
    }

    private static void probarCodigoVacio(
            ConsultorioService servicio,
            Sede sede
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                crearDatosBase(
                        sede,
                        "   ",
                        "Consultorio Código Vacío"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Código vacío"
        );
    }

    private static void probarTipoInvalido(
            ConsultorioService servicio,
            Sede sede
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sede.getIdSede()
                        ),
                        "C-TIPO-INVALIDO",
                        "Consultorio Tipo Inválido",
                        "1",
                        null,
                        "QUIROFANO",
                        "1",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Tipo inválido"
        );
    }

    private static void probarCapacidadNoNumerica(
            ConsultorioService servicio,
            Sede sede
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sede.getIdSede()
                        ),
                        "C-CAPACIDAD-TEXTO",
                        "Consultorio Capacidad Texto",
                        "1",
                        null,
                        "CONSULTA",
                        "dos",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Capacidad no numérica"
        );
    }

    private static void probarCapacidadFueraDeRango(
            ConsultorioService servicio,
            Sede sede
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sede.getIdSede()
                        ),
                        "C-CAPACIDAD-RANGO",
                        "Consultorio Capacidad Rango",
                        "1",
                        null,
                        "CONSULTA",
                        "51",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Capacidad fuera de rango"
        );
    }

    private static void probarAccesibilidadInvalida(
            ConsultorioService servicio,
            Sede sede
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sede.getIdSede()
                        ),
                        "C-ACCESIBILIDAD",
                        "Consultorio Accesibilidad",
                        "1",
                        null,
                        "CONSULTA",
                        "1",
                        "quizas"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Accesibilidad inválida"
        );
    }

    private static void verificarRegistroRechazado(
            ConsultorioService servicio,
            RegistroConsultorioDTO datos,
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

    private static Consultorio probarRegistroCorrecto(
            ConsultorioService servicio,
            Sede sede,
            String codigoConsultorio
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        "  "
                                + sede.getIdSede()
                                + "  ",
                        "  "
                                + codigoConsultorio
                                .toLowerCase(Locale.ROOT)
                                + "  ",
                        "  Consultorio Cardiología Temporal  ",
                        "  2  ",
                        "  Segundo piso, ala norte  ",
                        "  consulta  ",
                        "2",
                        "sí"
                );

        Consultorio consultorio =
                servicio.registrar(
                        datos
                );

        if (consultorio == null) {
            throw new AssertionError(
                    "El servicio no devolvió "
                            + "el consultorio registrado."
            );
        }

        if (consultorio.getIdConsultorio() == null
                || consultorio.getIdConsultorio() <= 0) {

            throw new AssertionError(
                    "El consultorio registrado "
                            + "no posee un ID válido."
            );
        }

        if (!sede.getIdSede().equals(
                consultorio.getIdSede()
        )) {
            throw new AssertionError(
                    "La sede del consultorio "
                            + "no coincide."
            );
        }

        if (!codigoConsultorio.equals(
                consultorio.getCodigo()
        )) {
            throw new AssertionError(
                    "El código del consultorio "
                            + "no fue normalizado correctamente."
            );
        }

        if (!"Consultorio Cardiología Temporal".equals(
                consultorio.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre no fue normalizado correctamente."
            );
        }

        if (!"2".equals(
                consultorio.getPiso()
        )) {
            throw new AssertionError(
                    "El piso no fue normalizado correctamente."
            );
        }

        if (!"Segundo piso, ala norte".equals(
                consultorio.getUbicacionReferencia()
        )) {
            throw new AssertionError(
                    "La referencia de ubicación "
                            + "no fue normalizada correctamente."
            );
        }

        if (consultorio.getTipo()
                != TipoConsultorio.CONSULTA) {

            throw new AssertionError(
                    "El tipo del consultorio "
                            + "no fue convertido correctamente."
            );
        }

        if (consultorio.getCapacidad() != 2) {
            throw new AssertionError(
                    "La capacidad del consultorio "
                            + "no fue convertida correctamente."
            );
        }

        if (!consultorio.isAccesible()) {
            throw new AssertionError(
                    "La accesibilidad debería ser verdadera."
            );
        }

        if (consultorio.getEstado()
                != EstadoConsultorio.DISPONIBLE) {

            throw new AssertionError(
                    "El consultorio nuevo "
                            + "debería estar disponible."
            );
        }

        System.out.println(
                "Registro correcto: CONSULTORIO CREADO."
        );

        return consultorio;
    }

    private static void probarBusquedaPorId(
            ConsultorioService servicio,
            Consultorio consultorioCreado
    ) throws SQLException {

        Optional<Consultorio> resultado =
                servicio.buscarPorId(
                        consultorioCreado.getIdConsultorio()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "El consultorio no fue encontrado por ID."
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

    private static void probarListadoGeneral(
            ConsultorioService servicio,
            Consultorio consultorioCreado
    ) throws SQLException {

        List<Consultorio> consultorios =
                servicio.listarTodos();

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
                    "El consultorio no aparece "
                            + "en el listado general."
            );
        }
    }

    private static void probarListadoPorSede(
            ConsultorioService servicio,
            Consultorio consultorioCreado
    ) throws SQLException {

        List<Consultorio> consultorios =
                servicio.listarPorSede(
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
                    "El consultorio no aparece "
                            + "en el listado de su sede."
            );
        }
    }

    private static void probarListadoDisponiblePorSede(
            ConsultorioService servicio,
            Consultorio consultorioCreado
    ) throws SQLException {

        List<Consultorio> consultorios =
                servicio.listarDisponiblesPorSede(
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
                    "El consultorio no aparece "
                            + "en el listado de disponibles."
            );
        }
    }

    private static void probarCodigoDuplicadoMismaSede(
            ConsultorioService servicio,
            Sede sede,
            String codigoConsultorio
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sede.getIdSede()
                        ),
                        "   "
                                + codigoConsultorio
                                .toLowerCase(Locale.ROOT)
                                + "   ",
                        "Otro Consultorio Duplicado",
                        "3",
                        null,
                        "PROCEDIMIENTO",
                        "3",
                        "false"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Código duplicado en la misma sede"
        );
    }

    private static Consultorio probarMismoCodigoEnOtraSede(
            ConsultorioService servicio,
            Sede sede,
            String codigoConsultorio
    ) throws SQLException {

        RegistroConsultorioDTO datos =
                new RegistroConsultorioDTO(
                        String.valueOf(
                                sede.getIdSede()
                        ),
                        codigoConsultorio,
                        "Consultorio Otra Sede",
                        "1",
                        "Primer piso",
                        "DIAGNOSTICO",
                        "4",
                        "true"
                );

        Consultorio consultorio =
                servicio.registrar(
                        datos
                );

        if (consultorio == null
                || consultorio.getIdConsultorio() == null) {

            throw new AssertionError(
                    "El mismo código debería poder utilizarse "
                            + "en otra sede."
            );
        }

        System.out.println(
                "Mismo código en otra sede: PERMITIDO."
        );

        return consultorio;
    }

    private static void verificarConsultorioOtraSede(
            Consultorio primero,
            Consultorio segundo
    ) {
        if (primero.getIdSede().equals(
                segundo.getIdSede()
        )) {
            throw new AssertionError(
                    "Los consultorios deberían pertenecer "
                            + "a sedes diferentes."
            );
        }

        if (!primero.getCodigo().equals(
                segundo.getCodigo()
        )) {
            throw new AssertionError(
                    "Los consultorios deberían utilizar "
                            + "el mismo código."
            );
        }

        if (primero.getIdConsultorio().equals(
                segundo.getIdConsultorio()
        )) {
            throw new AssertionError(
                    "Los consultorios deberían tener "
                            + "IDs diferentes."
            );
        }
    }

    private static RegistroConsultorioDTO crearDatosBase(
            Sede sede,
            String codigo,
            String nombre
    ) {
        return new RegistroConsultorioDTO(
                String.valueOf(
                        sede.getIdSede()
                ),
                codigo,
                nombre,
                "1",
                "Ala temporal",
                "CONSULTA",
                "1",
                "false"
        );
    }

    private static void eliminarDatosTemporales(
            String codigoConsultorio,
            String codigoSedePrincipal,
            String codigoSedeSecundaria,
            String codigoSedeInactiva
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try (
                    PreparedStatement eliminarConsultorios =
                            conexion.prepareStatement(
                                    SQL_ELIMINAR_CONSULTORIOS_TEMPORALES
                            );

                    PreparedStatement eliminarSedes =
                            conexion.prepareStatement(
                                    SQL_ELIMINAR_SEDES_TEMPORALES
                            )
            ) {
                eliminarConsultorios.setString(
                        1,
                        codigoConsultorio
                );

                eliminarConsultorios.executeUpdate();

                eliminarSedes.setString(
                        1,
                        codigoSedePrincipal
                );

                eliminarSedes.setString(
                        2,
                        codigoSedeSecundaria
                );

                eliminarSedes.setString(
                        3,
                        codigoSedeInactiva
                );

                eliminarSedes.executeUpdate();

                conexion.commit();

            } catch (SQLException error) {

                try {
                    conexion.rollback();

                } catch (SQLException errorRollback) {
                    error.addSuppressed(
                            errorRollback
                    );
                }

                throw error;
            }
        }
    }

    private static void verificarEliminacion(
            ConsultorioService servicio,
            String codigoConsultorio,
            String codigoSedePrincipal,
            String codigoSedeSecundaria,
            String codigoSedeInactiva
    ) throws SQLException {

        boolean permaneceConsultorio =
                servicio.listarTodos()
                        .stream()
                        .anyMatch(
                                consultorio ->
                                        codigoConsultorio
                                                .equalsIgnoreCase(
                                                        consultorio.getCodigo()
                                                )
                        );

        if (permaneceConsultorio) {
            throw new AssertionError(
                    "Algún consultorio temporal "
                            + "no fue eliminado."
            );
        }

        SedeDAO sedeDAO =
                new SedeDAO();

        boolean permaneceSede =
                sedeDAO.listarTodas()
                        .stream()
                        .anyMatch(
                                sede ->
                                        codigoSedePrincipal
                                                .equalsIgnoreCase(
                                                        sede.getCodigo()
                                                )
                                                || codigoSedeSecundaria
                                                .equalsIgnoreCase(
                                                        sede.getCodigo()
                                                )
                                                || codigoSedeInactiva
                                                .equalsIgnoreCase(
                                                        sede.getCodigo()
                                                )
                        );

        if (permaneceSede) {
            throw new AssertionError(
                    "Alguna sede temporal no fue eliminada."
            );
        }
    }
}