package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.RegistroMedicoDTO;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MedicoServiceIntegracionTest {

    private static final String SQL_ELIMINAR_MEDICOS_TEMPORALES = """
            DELETE FROM soracare.medicos
            WHERE LOWER(BTRIM(matricula_profesional)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_ELIMINAR_PERSONAS_TEMPORALES = """
            DELETE FROM soracare.personas
            WHERE cedula IN (?, ?, ?, ?)
            """;

    private static final String SQL_EXISTE_PERSONA_POR_CEDULA = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.personas
                WHERE cedula = ?
            ) AS existe
            """;

    public static void main(
            String[] args
    ) throws Exception {

        MedicoService servicio =
                new MedicoService();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String matriculaTemporal =
                "MS-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        long cedulaPrincipal =
                generarCedulaTemporal();

        long cedulaDuplicadoMatricula =
                generarCedulaTemporal();

        long cedulaDuplicadoCorreo =
                generarCedulaTemporal();

        long cedulaValidacion =
                generarCedulaTemporal();

        String correoPrincipal =
                "medico.servicio."
                        + identificadorTemporal
                        .toLowerCase(Locale.ROOT)
                        + "@soracare.test";

        try {
            probarSolicitudNula(
                    servicio
            );

            probarNombresVacios(
                    servicio,
                    cedulaValidacion
            );

            probarCedulaInvalida(
                    servicio
            );

            probarFechaNacimientoFutura(
                    servicio,
                    cedulaValidacion
            );

            probarFechaIngresoFutura(
                    servicio,
                    cedulaValidacion
            );

            probarMatriculaVacia(
                    servicio,
                    cedulaValidacion
            );

            probarBiografiaDemasiadoLarga(
                    servicio,
                    cedulaValidacion
            );

            Medico medicoCreado =
                    probarRegistroCorrecto(
                            servicio,
                            cedulaPrincipal,
                            matriculaTemporal,
                            correoPrincipal
                    );

            probarBusquedaPorId(
                    servicio,
                    medicoCreado
            );

            probarBusquedaPorMatricula(
                    servicio,
                    medicoCreado
            );

            probarListadoGeneral(
                    servicio,
                    medicoCreado
            );

            probarListadoActivos(
                    servicio,
                    medicoCreado
            );

            probarMatriculaDuplicada(
                    servicio,
                    cedulaDuplicadoMatricula,
                    matriculaTemporal,
                    identificadorTemporal
            );

            verificarPersonaNoCreada(
                    cedulaDuplicadoMatricula,
                    "duplicado de matrícula"
            );

            probarCedulaDuplicada(
                    servicio,
                    cedulaPrincipal,
                    identificadorTemporal
            );

            probarCorreoDuplicado(
                    servicio,
                    cedulaDuplicadoCorreo,
                    correoPrincipal,
                    identificadorTemporal
            );

            verificarPersonaNoCreada(
                    cedulaDuplicadoCorreo,
                    "duplicado de correo"
            );

            System.out.println();
            System.out.println(
                    "MedicoService fue probado correctamente."
            );

            System.out.println(
                    "Las validaciones básicas fueron verificadas."
            );

            System.out.println(
                    "El médico fue registrado correctamente."
            );

            System.out.println(
                    "La relación Persona → Médico "
                            + "fue creada correctamente."
            );

            System.out.println(
                    "La búsqueda y los listados "
                            + "fueron verificados."
            );

            System.out.println(
                    "La matrícula duplicada fue rechazada."
            );

            System.out.println(
                    "La cédula duplicada fue rechazada."
            );

            System.out.println(
                    "El correo duplicado fue rechazado."
            );

            System.out.println(
                    "No se crearon personas huérfanas "
                            + "en los registros rechazados."
            );

        } finally {
            eliminarDatosTemporales(
                    matriculaTemporal,
                    cedulaPrincipal,
                    cedulaDuplicadoMatricula,
                    cedulaDuplicadoCorreo,
                    cedulaValidacion
            );

            verificarEliminacion(
                    matriculaTemporal,
                    cedulaPrincipal,
                    cedulaDuplicadoMatricula,
                    cedulaDuplicadoCorreo,
                    cedulaValidacion
            );

            System.out.println(
                    "Los datos temporales fueron eliminados. "
                            + "No quedaron datos de prueba."
            );
        }
    }

    private static void probarSolicitudNula(
            MedicoService servicio
    ) {

        try {
            servicio.registrar(
                    null
            );

            throw new AssertionError(
                    "Una solicitud nula debería ser rechazada."
            );

        } catch (SQLException esperado) {
            System.out.println(
                    "Solicitud nula: RECHAZADA."
            );
        }
    }

    private static void probarNombresVacios(
            MedicoService servicio,
            long cedula
    ) {

        RegistroMedicoDTO datos =
                crearDatosBase(
                        "   ",
                        "Médico",
                        Long.toString(cedula),
                        "MAT-NOMBRE-VACIO",
                        "nombre.vacio@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Nombres vacíos"
        );
    }

    private static void probarCedulaInvalida(
            MedicoService servicio
    ) {

        RegistroMedicoDTO datos =
                crearDatosBase(
                        "Médico",
                        "Cédula Inválida",
                        "ABC123",
                        "MAT-CEDULA-INVALIDA",
                        "cedula.invalida@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Cédula inválida"
        );
    }

    private static void probarFechaNacimientoFutura(
            MedicoService servicio,
            long cedula
    ) {

        RegistroMedicoDTO datos =
                new RegistroMedicoDTO(
                        "Médico",
                        "Nacimiento Futuro",
                        Long.toString(cedula),
                        LocalDate.now()
                                .plusDays(1)
                                .toString(),
                        null,
                        "nacimiento.futuro@soracare.test",
                        null,
                        "MAT-NACIMIENTO-FUTURO",
                        LocalDate.now().toString(),
                        null
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Fecha de nacimiento futura"
        );
    }

    private static void probarFechaIngresoFutura(
            MedicoService servicio,
            long cedula
    ) {

        RegistroMedicoDTO datos =
                new RegistroMedicoDTO(
                        "Médico",
                        "Ingreso Futuro",
                        Long.toString(cedula),
                        "1985-05-15",
                        null,
                        "ingreso.futuro@soracare.test",
                        null,
                        "MAT-INGRESO-FUTURO",
                        LocalDate.now()
                                .plusDays(1)
                                .toString(),
                        null
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Fecha de ingreso futura"
        );
    }

    private static void probarMatriculaVacia(
            MedicoService servicio,
            long cedula
    ) {

        RegistroMedicoDTO datos =
                crearDatosBase(
                        "Médico",
                        "Matrícula Vacía",
                        Long.toString(cedula),
                        "   ",
                        "matricula.vacia@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Matrícula vacía"
        );
    }

    private static void probarBiografiaDemasiadoLarga(
            MedicoService servicio,
            long cedula
    ) {

        RegistroMedicoDTO datos =
                new RegistroMedicoDTO(
                        "Médico",
                        "Biografía Larga",
                        Long.toString(cedula),
                        "1985-05-15",
                        null,
                        "biografia.larga@soracare.test",
                        null,
                        "MAT-BIOGRAFIA-LARGA",
                        LocalDate.now().toString(),
                        "A".repeat(1001)
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Biografía mayor a 1000 caracteres"
        );
    }

    private static Medico probarRegistroCorrecto(
            MedicoService servicio,
            long cedula,
            String matricula,
            String correo
    ) throws SQLException {

        RegistroMedicoDTO datos =
                new RegistroMedicoDTO(
                        "  Ana María  ",
                        "  González Temporal  ",
                        Long.toString(cedula),
                        "1985-05-15",
                        "  0981 555 000  ",
                        "  "
                                + correo
                                .toUpperCase(Locale.ROOT)
                                + "  ",
                        "  Dirección temporal  ",
                        "  "
                                + matricula
                                .toLowerCase(Locale.ROOT)
                                + "  ",
                        LocalDate.now()
                                .minusYears(2)
                                .toString(),
                        "  Médica utilizada para "
                                + "la prueba de integración.  "
                );

        Medico medico =
                servicio.registrar(
                        datos
                );

        if (medico == null) {
            throw new AssertionError(
                    "El servicio no devolvió "
                            + "el médico registrado."
            );
        }

        if (medico.getId() == null
                || medico.getId() <= 0) {

            throw new AssertionError(
                    "El médico no posee "
                            + "un id_persona válido."
            );
        }

        if (medico.getIdMedico() == null
                || medico.getIdMedico() <= 0) {

            throw new AssertionError(
                    "El médico no posee "
                            + "un id_medico válido."
            );
        }

        if (!"Ana María".equals(
                medico.getNombres()
        )) {
            throw new AssertionError(
                    "Los nombres no fueron normalizados."
            );
        }

        if (!"González Temporal".equals(
                medico.getApellidos()
        )) {
            throw new AssertionError(
                    "Los apellidos no fueron normalizados."
            );
        }

        if (!matricula
                .toLowerCase(Locale.ROOT)
                .equals(
                        medico.getMatriculaProfesional()
                )) {

            throw new AssertionError(
                    "La matrícula no fue normalizada "
                            + "como se esperaba."
            );
        }

        if (medico.getEstado()
                != EstadoMedico.ACTIVO) {

            throw new AssertionError(
                    "Un médico nuevo debería "
                            + "estar ACTIVO."
            );
        }

        if (!"Médica utilizada para la prueba de integración."
                .equals(
                        medico.getBiografia()
                )) {

            throw new AssertionError(
                    "La biografía no fue normalizada."
            );
        }

        System.out.println(
                "Registro correcto: MÉDICO CREADO."
        );

        return medico;
    }

    private static void probarBusquedaPorId(
            MedicoService servicio,
            Medico medicoCreado
    ) throws SQLException {

        Optional<Medico> resultado =
                servicio.buscarPorId(
                        medicoCreado.getIdMedico()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "El médico no fue encontrado por ID."
            );
        }

        if (!medicoCreado
                .getIdMedico()
                .equals(
                        resultado.get()
                                .getIdMedico()
                )) {

            throw new AssertionError(
                    "El médico recuperado por ID "
                            + "no coincide."
            );
        }
    }

    private static void probarBusquedaPorMatricula(
            MedicoService servicio,
            Medico medicoCreado
    ) throws SQLException {

        Optional<Medico> resultado =
                servicio.buscarPorMatricula(
                        "  "
                                + medicoCreado
                                .getMatriculaProfesional()
                                .toUpperCase(Locale.ROOT)
                                + "  "
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "El médico no fue encontrado "
                            + "por matrícula."
            );
        }

        if (!medicoCreado
                .getIdMedico()
                .equals(
                        resultado.get()
                                .getIdMedico()
                )) {

            throw new AssertionError(
                    "El médico recuperado por matrícula "
                            + "no coincide."
            );
        }
    }

    private static void probarListadoGeneral(
            MedicoService servicio,
            Medico medicoCreado
    ) throws SQLException {

        List<Medico> medicos =
                servicio.listarTodos();

        boolean encontrado =
                medicos.stream()
                        .anyMatch(
                                medico ->
                                        medicoCreado
                                                .getIdMedico()
                                                .equals(
                                                        medico
                                                                .getIdMedico()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El médico no aparece "
                            + "en el listado general."
            );
        }
    }

    private static void probarListadoActivos(
            MedicoService servicio,
            Medico medicoCreado
    ) throws SQLException {

        List<Medico> medicos =
                servicio.listarActivos();

        boolean encontrado =
                medicos.stream()
                        .anyMatch(
                                medico ->
                                        medicoCreado
                                                .getIdMedico()
                                                .equals(
                                                        medico
                                                                .getIdMedico()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El médico no aparece "
                            + "en el listado de activos."
            );
        }
    }

    private static void probarMatriculaDuplicada(
            MedicoService servicio,
            long cedula,
            String matricula,
            String identificador
    ) {

        RegistroMedicoDTO datos =
                crearDatosBase(
                        "Otro",
                        "Médico",
                        Long.toString(cedula),
                        "   "
                                + matricula
                                .toUpperCase(Locale.ROOT)
                                + "   ",
                        "matricula.duplicada."
                                + identificador
                                + "@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Matrícula duplicada"
        );
    }

    private static void probarCedulaDuplicada(
            MedicoService servicio,
            long cedulaExistente,
            String identificador
    ) {

        RegistroMedicoDTO datos =
                crearDatosBase(
                        "Otro",
                        "Médico",
                        Long.toString(
                                cedulaExistente
                        ),
                        "MAT-CEDULA-"
                                + identificador,
                        "cedula.duplicada."
                                + identificador
                                + "@soracare.test"
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Cédula duplicada"
        );
    }

    private static void probarCorreoDuplicado(
            MedicoService servicio,
            long cedula,
            String correoExistente,
            String identificador
    ) {

        RegistroMedicoDTO datos =
                crearDatosBase(
                        "Otro",
                        "Médico",
                        Long.toString(cedula),
                        "MAT-CORREO-"
                                + identificador,
                        "   "
                                + correoExistente
                                .toLowerCase(Locale.ROOT)
                                + "   "
                );

        verificarRegistroRechazado(
                servicio,
                datos,
                "Correo duplicado"
        );
    }

    private static RegistroMedicoDTO crearDatosBase(
            String nombres,
            String apellidos,
            String cedula,
            String matricula,
            String correo
    ) {
        return new RegistroMedicoDTO(
                nombres,
                apellidos,
                cedula,
                "1985-05-15",
                "0981 000 000",
                correo,
                "Dirección temporal",
                matricula,
                LocalDate.now()
                        .minusYears(1)
                        .toString(),
                "Biografía temporal"
        );
    }

    private static void verificarRegistroRechazado(
            MedicoService servicio,
            RegistroMedicoDTO datos,
            String descripcion
    ) {

        try {
            servicio.registrar(
                    datos
            );

            throw new AssertionError(
                    descripcion
                            + " debería ser rechazado."
            );

        } catch (SQLException esperado) {
            System.out.println(
                    descripcion
                            + ": RECHAZADO."
            );
        }
    }

    private static void verificarPersonaNoCreada(
            long cedula,
            String descripcion
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_PERSONA_POR_CEDULA
                        )
        ) {
            sentencia.setLong(
                    1,
                    cedula
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new AssertionError(
                            "No se pudo verificar "
                                    + "la persona temporal."
                    );
                }

                if (resultado.getBoolean(
                        "existe"
                )) {
                    throw new AssertionError(
                            "Se creó una persona huérfana "
                                    + "después del "
                                    + descripcion
                                    + "."
                    );
                }
            }
        }
    }

    private static long generarCedulaTemporal() {

        return 7_000_000_000L
                + Math.abs(
                UUID.randomUUID()
                        .getMostSignificantBits()
                        % 900_000_000L
        );
    }

    private static void eliminarDatosTemporales(
            String matricula,
            long cedulaPrincipal,
            long cedulaDuplicadoMatricula,
            long cedulaDuplicadoCorreo,
            long cedulaValidacion
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try (
                    PreparedStatement eliminarMedicos =
                            conexion.prepareStatement(
                                    SQL_ELIMINAR_MEDICOS_TEMPORALES
                            );

                    PreparedStatement eliminarPersonas =
                            conexion.prepareStatement(
                                    SQL_ELIMINAR_PERSONAS_TEMPORALES
                            )
            ) {
                eliminarMedicos.setString(
                        1,
                        matricula
                );

                eliminarMedicos.executeUpdate();

                eliminarPersonas.setLong(
                        1,
                        cedulaPrincipal
                );

                eliminarPersonas.setLong(
                        2,
                        cedulaDuplicadoMatricula
                );

                eliminarPersonas.setLong(
                        3,
                        cedulaDuplicadoCorreo
                );

                eliminarPersonas.setLong(
                        4,
                        cedulaValidacion
                );

                eliminarPersonas.executeUpdate();

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
            String matricula,
            long cedulaPrincipal,
            long cedulaDuplicadoMatricula,
            long cedulaDuplicadoCorreo,
            long cedulaValidacion
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            for (long cedula : List.of(
                    cedulaPrincipal,
                    cedulaDuplicadoMatricula,
                    cedulaDuplicadoCorreo,
                    cedulaValidacion
            )) {
                try (
                        PreparedStatement sentencia =
                                conexion.prepareStatement(
                                        SQL_EXISTE_PERSONA_POR_CEDULA
                                )
                ) {
                    sentencia.setLong(
                            1,
                            cedula
                    );

                    try (
                            ResultSet resultado =
                                    sentencia.executeQuery()
                    ) {
                        if (resultado.next()
                                && resultado.getBoolean(
                                "existe"
                        )) {

                            throw new AssertionError(
                                    "Quedó una persona temporal "
                                            + "registrada."
                            );
                        }
                    }
                }
            }
        }

        MedicoService servicio =
                new MedicoService();

        Optional<Medico> medico =
                servicio.buscarPorMatricula(
                        matricula
                );

        if (medico.isPresent()) {
            throw new AssertionError(
                    "El médico temporal no fue eliminado."
            );
        }
    }
}