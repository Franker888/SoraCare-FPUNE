package py.fpune.soracare.integracion;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.fpune.soracare.dao.EspecialidadDAO;
import py.fpune.soracare.dao.SolicitudSifenDAO;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.config.MigradorEsquemaBD;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class PostgreSqlFlywayIT {

    private static final int CANTIDAD_MIGRACIONES_ESPERADA = 39;
    private static final String ULTIMA_VERSION_ESPERADA = "039";
    private static final String BASE_TEMPORAL = "soracare_pruebas";
    private static final String USUARIO_TEMPORAL = "soracare_admin_test";

    @Container
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:18.6-bookworm")
            )
                    .withDatabaseName(BASE_TEMPORAL)
                    .withUsername(USUARIO_TEMPORAL)
                    .withPassword(UUID.randomUUID().toString());

    private static Flyway flyway;

    @Test
    void revocaSesionesInclusoTrasReactivarYConservaUnLoginNuevo() throws Exception {
        var sesiones = new py.fpune.soracare.dao.SesionVigenteDAO();
        var usuarios = new py.fpune.soracare.dao.UsuarioDAO();
        try (Connection c = abrirConexion()) {
            c.setAutoCommit(false);
            try {
                long persona = insertar(c, "INSERT INTO soracare.personas(nombres,apellidos,cedula) VALUES ('Test','Revocacion',987654329) RETURNING id_persona");
                long usuario = insertar(c, "INSERT INTO soracare.usuarios(id_persona,nombre_usuario,contrasena_hash) VALUES (" + persona + ",'test.revocacion','hash-sintetico-no-login') RETURNING id_usuario");
                assertTrue(sesiones.esVigente(c, usuario, 0));
                try (Statement s = c.createStatement()) {
                    s.executeUpdate("UPDATE soracare.usuarios SET estado='INACTIVO' WHERE id_usuario=" + usuario);
                    assertFalse(sesiones.esVigente(c, usuario, 0));
                    s.executeUpdate("UPDATE soracare.usuarios SET estado='ACTIVO' WHERE id_usuario=" + usuario);
                    assertFalse(sesiones.esVigente(c, usuario, 0));
                    assertTrue(sesiones.esVigente(c, usuario, 2));
                    // Simula HIBP/BCrypt en vuelo mientras otro administrador revoca y reactiva.
                    org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class,
                            () -> usuarios.actualizarContrasena(c, usuario,
                                    "hash-sintetico-no-login", "nuevo-hash-sintetico", 0));
                    usuarios.actualizarContrasena(c, usuario,
                            "hash-sintetico-no-login", "nuevo-hash-sintetico", 2);
                    assertTrue(sesiones.esVigente(c, usuario, 2));
                    assertEquals(2, usuarios.buscarPorId(c, usuario).orElseThrow().getVersionSesion());
                    assertEquals(2, usuarios.buscarPorIdentificador(c, "test.revocacion").orElseThrow().getVersionSesion());
                    s.executeUpdate("UPDATE soracare.usuarios SET estado='ACTIVO', ultimo_acceso=CURRENT_TIMESTAMP WHERE id_usuario=" + usuario);
                    assertTrue(sesiones.esVigente(c, usuario, 2));
                    s.executeUpdate("UPDATE soracare.usuarios SET estado='BLOQUEADO' WHERE id_usuario=" + usuario);
                    assertFalse(sesiones.esVigente(c, usuario, 2));
                    assertFalse(sesiones.esVigente(c, usuario, 3));
                    assertFalse(sesiones.esVigente(c, -1, 0));
                }
            } finally { c.rollback(); }
        }
    }
    private static MigrateResult resultadoMigracion;

    @BeforeAll
    static void migrarBaseTemporal() throws Exception {
        flyway = Flyway.configure()
                .dataSource(
                        POSTGRESQL.getJdbcUrl(),
                        POSTGRESQL.getUsername(),
                        POSTGRESQL.getPassword()
                )
                .locations("classpath:db/migration")
                .schemas("soracare")
                .defaultSchema("soracare")
                .validateMigrationNaming(true)
                .cleanDisabled(true)
                .load();

        resultadoMigracion = MigradorEsquemaBD.migrar(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()
        );
    }

    @Test
    void aplicaYValidaTodoElEsquemaEnUnaBaseAislada() throws Exception {
        assertEquals(
                CANTIDAD_MIGRACIONES_ESPERADA,
                resultadoMigracion.migrationsExecuted
        );
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(
                ULTIMA_VERSION_ESPERADA,
                flyway.info().current().getVersion().getVersion()
        );

        try (
                Connection conexion = abrirConexion();
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery("""
                        SELECT
                            current_database() AS base_actual,
                            to_regclass('soracare.especialidades') AS especialidades,
                            to_regclass('soracare.citas') AS citas,
                            to_regclass('soracare.facturas_internas') AS facturas_internas,
                            to_regclass('soracare.facturas_internas_detalles') AS detalles_factura,
                            to_regclass('soracare.solicitudes_sifen') AS solicitudes_sifen,
                            to_regclass('soracare.numeraciones_sifen') AS numeraciones_sifen,
                            to_regclass('soracare.sifen_identificador_envio_seq') AS secuencia_envio_sifen,
                            to_regclass('soracare.google_calendar_vinculos') AS google_calendar_vinculos,
                            to_regclass('soracare.google_calendar_eventos') AS google_calendar_eventos,
                            (
                                SELECT schema_owner
                                FROM information_schema.schemata
                                WHERE schema_name = 'soracare'
                            ) AS propietario_esquema,
                            (
                                SELECT rolcanlogin
                                FROM pg_roles
                                WHERE rolname = 'soracare_app'
                            ) AS propietario_puede_iniciar_sesion,
                            (
                                SELECT COUNT(*)
                                FROM soracare.flyway_schema_history
                                WHERE success = TRUE
                                  AND version IS NOT NULL
                            ) AS migraciones
                        """)
        ) {
            assertTrue(resultado.next());
            assertEquals(BASE_TEMPORAL, resultado.getString("base_actual"));
            assertNotNull(resultado.getString("especialidades"));
            assertNotNull(resultado.getString("citas"));
            assertNotNull(resultado.getString("facturas_internas"));
            assertNotNull(resultado.getString("detalles_factura"));
            assertNotNull(resultado.getString("solicitudes_sifen"));
            assertNotNull(resultado.getString("numeraciones_sifen"));
            assertNotNull(resultado.getString("secuencia_envio_sifen"));
            assertNotNull(resultado.getString("google_calendar_vinculos"));
            assertNotNull(resultado.getString("google_calendar_eventos"));
            assertEquals("soracare_app", resultado.getString("propietario_esquema"));
            assertFalse(resultado.getBoolean("propietario_puede_iniciar_sesion"));
            assertEquals(
                    CANTIDAD_MIGRACIONES_ESPERADA,
                    resultado.getInt("migraciones")
            );
        }
    }

    @Test
    void ejecutaUnDaoRealYRevierteSusDatosDePrueba() throws Exception {
        EspecialidadDAO especialidadDAO = new EspecialidadDAO();
        String nombreTemporal = "Integración "
                + UUID.randomUUID().toString().substring(0, 8);

        try (Connection conexion = abrirConexion()) {
            conexion.setAutoCommit(false);

            Especialidad creada = especialidadDAO.crear(
                    conexion,
                    new Especialidad(
                            nombreTemporal,
                            "Registro temporal para validar PostgreSQL.",
                            40
                    )
            );

            assertNotNull(creada.getIdEspecialidad());
            assertTrue(
                    especialidadDAO.buscarPorNombre(conexion, nombreTemporal)
                            .isPresent()
            );

            conexion.rollback();
        }

        try (Connection conexion = abrirConexion()) {
            assertFalse(
                    especialidadDAO.buscarPorNombre(conexion, nombreTemporal)
                            .isPresent()
            );
        }
    }

    @Test
    void reservaNumeracionSifenSecuencialDentroDeLaTransaccion() throws Exception {
        SolicitudSifenDAO solicitudDAO = new SolicitudSifenDAO();
        try (Connection conexion = abrirConexion()) {
            conexion.setAutoCommit(false);

            assertEquals(
                    "0000001",
                    solicitudDAO.reservarNumero(
                            conexion, "PRUEBA", "1", "1", ""
                    )
            );
            assertEquals(
                    "0000002",
                    solicitudDAO.reservarNumero(
                            conexion, "PRUEBA", "1", "1", ""
                    )
            );

            conexion.rollback();
        }
    }

    private static Connection abrirConexion() throws Exception {
        return DriverManager.getConnection(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()
        );
    }

    @Test
    void historialRealAislaMedicosYNoModificaElEstadoFiscal() throws Exception {
        var dao = new py.fpune.soracare.dao.ConsultaSifenDAO();
        String cdc = "1".repeat(44);
        try (var c = abrirConexion()) {
            c.setAutoCommit(false);
            long persona = insertar(c, "INSERT INTO soracare.personas(nombres,apellidos,cedula) VALUES ('Test','Historial',987654321) RETURNING id_persona");
            long paciente = insertar(c, "INSERT INTO soracare.pacientes(id_persona) VALUES (" + persona + ") RETURNING id_paciente");
            long medico = insertar(c, "INSERT INTO soracare.medicos(id_persona,matricula_profesional) VALUES (" + persona + ",'TEST-HIST') RETURNING id_medico");
            long usuario = insertar(c, "INSERT INTO soracare.usuarios(id_persona,nombre_usuario,contrasena_hash) VALUES (" + persona + ",'test.historial','hash-sintetico-no-login') RETURNING id_usuario");
            long especialidad = insertar(c, "INSERT INTO soracare.especialidades(nombre,descripcion) VALUES ('Test historial','Temporal') RETURNING id_especialidad");
            long sede = insertar(c, "INSERT INTO soracare.sedes(codigo,nombre,direccion,ciudad) VALUES ('TEST-H','Test','Test','Test') RETURNING id_sede");
            long consultorio = insertar(c, "INSERT INTO soracare.consultorios(id_sede,codigo,nombre) VALUES (" + sede + ",'TEST-H','Test') RETURNING id_consultorio");
            try (var s = c.createStatement()) {
                s.executeUpdate("INSERT INTO soracare.medicos_especialidades(id_medico,id_especialidad) VALUES (" + medico + "," + especialidad + ")");
            }
            long cita = insertar(c, "INSERT INTO soracare.citas(id_paciente,id_medico,id_especialidad,id_consultorio,id_estado_cita,fecha_hora_inicio,fecha_hora_fin,motivo_consulta) VALUES ("
                    + paciente + "," + medico + "," + especialidad + "," + consultorio + ",(SELECT MIN(id_estado_cita) FROM soracare.estados_citas),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '30 minutes','Test') RETURNING id_cita");
            long pago = insertar(c, "INSERT INTO soracare.pagos(id_cita,id_metodo_pago,id_estado_pago,monto) VALUES (" + cita
                    + ",(SELECT MIN(id_metodo_pago) FROM soracare.metodos_pago),(SELECT MIN(id_estado_pago) FROM soracare.estados_pago),100) RETURNING id_pago");
            long factura = insertar(c, "INSERT INTO soracare.facturas_internas(id_pago,id_usuario_emisor,tipo_documento,numero_documento,nombre_razon_social,direccion,correo,subtotal,total,descripcion) VALUES ("
                    + pago + "," + usuario + ",'CI','123','Test','Test','test@example.invalid',100,100,'Test') RETURNING id_factura");
            long solicitud = insertar(c, "INSERT INTO soracare.solicitudes_sifen(id_factura_interna,ambiente,estado,cdc) VALUES (" + factura + ",'PRUEBA','ERROR','" + cdc + "') RETURNING id_solicitud");
            org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> dao.iniciar(c, 900, solicitud, persona + 10000, cdc));
            dao.iniciar(c, 901, solicitud, persona, cdc);
            assertEquals("SIN_RESULTADO", dao.listarDelMedico(c, persona).getFirst().estado());
            assertTrue(dao.listarDelMedico(c, persona + 10000).isEmpty());
            var resultado = new py.fpune.soracare.sifen.ResultadoConsultaSifen(
                    py.fpune.soracare.sifen.ResultadoConsultaSifen.Estado.ENCONTRADO, cdc, "0422", "Encontrado", "123", 2, "REVISION_REQUERIDA");
            dao.finalizar(c, 901, resultado);
            var registro = dao.listarDelMedico(c, persona).getFirst();
            assertEquals("ENCONTRADO", registro.estado());
            assertEquals(2, registro.eventos());
            assertEquals("123", registro.protocolo());
            assertEquals("REVISION_REQUERIDA", registro.observacionEventos());
            org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> dao.finalizar(c, 901, resultado));
            for (long id = 902; id <= 952; id++) dao.iniciar(c, id, solicitud, persona, cdc);
            var recientes = dao.listarDelMedico(c, persona);
            assertEquals(50, recientes.size());
            assertEquals(952, recientes.getFirst().idConsulta());
            assertEquals(903, recientes.getLast().idConsulta());
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT estado FROM soracare.solicitudes_sifen WHERE id_solicitud=" + solicitud)) {
                assertTrue(r.next()); assertEquals("ERROR", r.getString(1));
            }
            c.rollback();
        }
    }

    private static long insertar(Connection c, String sql) throws Exception {
        try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
            assertTrue(r.next()); return r.getLong(1);
        }
    }
}
