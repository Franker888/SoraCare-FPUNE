package py.fpune.soracare.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.SQLException;

/** Aplica el esquema versionado antes de habilitar la aplicación. */
public final class MigradorEsquemaBD {

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
    ) {
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
}
