package py.fpune.soracare.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionBD {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/soracare_db";

    private static final String USUARIO =
            "soracare_app";

    private ConexionBD() {
        // Evita crear objetos de esta clase.
    }

    public static Connection obtenerConexion() throws SQLException {

        String contrasena =
                System.getenv("SORACARE_DB_PASSWORD");

        if (contrasena == null || contrasena.isBlank()) {
            throw new SQLException(
                    "No se configuró la variable SORACARE_DB_PASSWORD"
            );
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException error) {
            throw new SQLException(
                    "No se pudo cargar el controlador JDBC de PostgreSQL",
                    error
            );
        }

        return DriverManager.getConnection(
                URL,
                USUARIO,
                contrasena
        );
    }
}