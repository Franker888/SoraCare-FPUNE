package py.fpune.soracare.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Aplica el esquema versionado antes de habilitar la aplicación. */
public final class MigradorEsquemaBD {

    private static final String PREPARAR_ROL_PROPIETARIO = """
            DO $soracare$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM pg_roles
                    WHERE rolname = 'soracare_app'
                ) THEN
                    CREATE ROLE soracare_app NOLOGIN;
                END IF;

                IF current_user <> 'soracare_app'
                        AND NOT pg_has_role(
                            current_user,
                            'soracare_app',
                            'MEMBER'
                        ) THEN
                    EXECUTE format(
                        'GRANT soracare_app TO %I',
                        current_user
                    );
                END IF;
            END
            $soracare$;
            """;

    private MigradorEsquemaBD() {
    }

    public static MigrateResult migrar() throws SQLException {
        ConexionBD.Configuracion configuracion =
                ConexionBD.resolverConfiguracion(System::getenv);
        return migrar(
                configuracion.url(),
                configuracion.usuario(),
                configuracion.contrasena()
        );
    }

    public static MigrateResult migrar(
            String url,
            String usuario,
            String contrasena
    ) throws SQLException {
        prepararInfraestructura(url, usuario, contrasena);
        return Flyway.configure()
                .dataSource(url, usuario, contrasena)
                .locations("classpath:db/migration")
                .schemas("soracare")
                .defaultSchema("soracare")
                .validateMigrationNaming(true)
                .cleanDisabled(true)
                .connectRetries(30)
                .load()
                .migrate();
    }

    private static void prepararInfraestructura(
            String url,
            String usuario,
            String contrasena
    ) throws SQLException {
        cargarControladorPostgreSql();
        try (
                Connection conexion = DriverManager.getConnection(
                        url, usuario, contrasena
                );
                Statement sentencia = conexion.createStatement()
        ) {
            sentencia.execute(PREPARAR_ROL_PROPIETARIO);
            sentencia.execute("""
                    CREATE SCHEMA IF NOT EXISTS soracare
                        AUTHORIZATION soracare_app
                    """);
            sentencia.execute("""
                    ALTER SCHEMA soracare
                        OWNER TO soracare_app
                    """);
        }
    }

    private static void cargarControladorPostgreSql() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException excepcion) {
            throw new SQLException(
                    "El controlador PostgreSQL no está disponible.",
                    excepcion
            );
        }
    }
}
