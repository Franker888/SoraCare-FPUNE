import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

/** Ejecuta el cargador demostrativo mediante JDBC sin exponer secretos en argumentos. */
public final class EjecutarSqlJdbc {

    private static final String MARCADOR = "$soracare_demo$";

    private EjecutarSqlJdbc() {
    }

    public static void main(String[] argumentos) throws Exception {
        if (argumentos.length != 3) {
            throw new IllegalArgumentException(
                    "Uso: EjecutarSqlJdbc <jdbc-url> <usuario> <archivo-sql>"
            );
        }

        char[] contrasenaBase = null;
        char[] hashMedicos = null;
        char[] hashAdmin = null;
        char[] hashRecepcion = null;
        char[] hashPaciente = null;
        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        )) {
            contrasenaBase = leerSecreto(entrada, "contraseña de base de datos");
            hashMedicos = leerSecreto(entrada, "hash de médicos");
            hashAdmin = leerSecreto(entrada, "hash de administrador");
            hashRecepcion = leerSecreto(entrada, "hash de recepción");
            hashPaciente = leerSecreto(entrada, "hash de paciente");

            String sql = Files.readString(
                    Path.of(argumentos[2]),
                    StandardCharsets.UTF_8
            ).replaceFirst("(?m)^\\\\set[^\\r\\n]*(?:\\r?\\n)?", "");
            sql = reemplazar(sql, "hash_medicos", hashMedicos);
            sql = reemplazar(sql, "hash_admin", hashAdmin);
            sql = reemplazar(sql, "hash_recepcion", hashRecepcion);
            sql = reemplazar(sql, "hash_paciente", hashPaciente);

            Class.forName("org.postgresql.Driver");
            try (
                    Connection conexion = DriverManager.getConnection(
                            argumentos[0],
                            argumentos[1],
                            new String(contrasenaBase)
                    );
                    Statement sentencia = conexion.createStatement()
            ) {
                sentencia.execute(sql);
            }
        } finally {
            borrar(contrasenaBase);
            borrar(hashMedicos);
            borrar(hashAdmin);
            borrar(hashRecepcion);
            borrar(hashPaciente);
        }
    }

    private static char[] leerSecreto(
            BufferedReader entrada,
            String descripcion
    ) throws Exception {
        String linea = entrada.readLine();
        if (linea == null || linea.isBlank()) {
            throw new IllegalArgumentException(
                    "No se recibió " + descripcion + "."
            );
        }
        return linea.toCharArray();
    }

    private static String reemplazar(
            String sql,
            String variable,
            char[] valor
    ) {
        String texto = new String(valor);
        if (texto.contains(MARCADOR)) {
            throw new IllegalArgumentException(
                    "El valor de " + variable + " contiene un marcador inválido."
            );
        }
        return sql.replace(
                ":'" + variable + "'",
                MARCADOR + texto + MARCADOR
        );
    }

    private static void borrar(char[] valor) {
        if (valor != null) {
            Arrays.fill(valor, '\0');
        }
    }
}
