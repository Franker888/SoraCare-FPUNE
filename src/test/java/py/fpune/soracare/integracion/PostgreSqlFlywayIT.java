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

    private static final int CANTIDAD_MIGRACIONES_ESPERADA = 36;
    private static final String ULTIMA_VERSION_ESPERADA = "036";
    private static final String BASE_TEMPORAL = "soracare_pruebas";
    private static final String USUARIO_TEMPORAL = "soracare_app";

    @Container
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:18.6-bookworm")
            )
                    .withDatabaseName(BASE_TEMPORAL)
                    .withUsername(USUARIO_TEMPORAL)
                    .withPassword(UUID.randomUUID().toString());

    private static Flyway flyway;
    private static MigrateResult resultadoMigracion;

    @BeforeAll
    static void migrarBaseTemporal() {
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

        resultadoMigracion = flyway.migrate();
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
}
