package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ConfiguracionSistemaDTO;
import py.fpune.soracare.dto.HistorialConfiguracionDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfiguracionSistemaDAO {

    private static final String SQL_LISTAR = """
            SELECT id_configuracion, clave, nombre, valor, tipo_dato, categoria,
                   descripcion, es_publica, es_editable, activo, fecha_actualizacion
            FROM soracare.configuraciones_sistema
            WHERE activo = TRUE
            ORDER BY categoria, nombre
            """;

    private static final String SQL_BUSCAR = """
            SELECT id_configuracion, clave, nombre, valor, tipo_dato, categoria,
                   descripcion, es_publica, es_editable, activo, fecha_actualizacion
            FROM soracare.configuraciones_sistema
            WHERE id_configuracion = ?
            FOR UPDATE
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE soracare.configuraciones_sistema
            SET valor = ?
            WHERE id_configuracion = ?
              AND activo = TRUE
              AND es_editable = TRUE
            """;

    private static final String SQL_HISTORIAL = """
            SELECT his.id_historial, con.nombre, con.clave,
                   his.valor_anterior, his.valor_nuevo,
                   COALESCE(usu.nombre_usuario, 'Sistema') AS usuario,
                   his.motivo, his.fecha_cambio
            FROM soracare.historial_configuraciones his
            INNER JOIN soracare.configuraciones_sistema con
                ON con.id_configuracion = his.id_configuracion
            LEFT JOIN soracare.usuarios usu
                ON usu.id_usuario = his.id_usuario
            ORDER BY his.fecha_cambio DESC, his.id_historial DESC
            LIMIT 40
            """;

    public List<ConfiguracionSistemaDTO> listar() throws SQLException {
        List<ConfiguracionSistemaDTO> configuraciones = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) configuraciones.add(mapear(resultado));
        }
        return List.copyOf(configuraciones);
    }

    public List<HistorialConfiguracionDTO> listarHistorial() throws SQLException {
        List<HistorialConfiguracionDTO> historial = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_HISTORIAL);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                historial.add(new HistorialConfiguracionDTO(
                        resultado.getLong("id_historial"),
                        resultado.getString("nombre"),
                        resultado.getString("clave"),
                        resultado.getString("valor_anterior"),
                        resultado.getString("valor_nuevo"),
                        resultado.getString("usuario"),
                        resultado.getString("motivo"),
                        resultado.getObject("fecha_cambio", OffsetDateTime.class)
                ));
            }
        }
        return List.copyOf(historial);
    }

    public ConfiguracionSistemaDTO actualizar(
            long idConfiguracion,
            String valor,
            long idUsuario,
            String motivo
    ) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                Optional<ConfiguracionSistemaDTO> encontrada = buscar(conexion, idConfiguracion);
                if (encontrada.isEmpty()) {
                    throw new SQLException("La configuración indicada no existe.");
                }
                if (!encontrada.get().editable()) {
                    throw new SQLException("La configuración indicada es de solo lectura.");
                }

                establecerContexto(conexion, idUsuario, motivo);
                try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
                    sentencia.setString(1, valor);
                    sentencia.setLong(2, idConfiguracion);
                    if (sentencia.executeUpdate() != 1) {
                        throw new SQLException("No fue posible guardar la configuración.");
                    }
                }
                ConfiguracionSistemaDTO actualizada = buscar(conexion, idConfiguracion)
                        .orElseThrow(() -> new SQLException("No fue posible recuperar la configuración."));
                conexion.commit();
                return actualizada;
            } catch (SQLException error) {
                try {
                    conexion.rollback();
                } catch (SQLException rollback) {
                    error.addSuppressed(rollback);
                }
                throw error;
            }
        }
    }

    private static Optional<ConfiguracionSistemaDTO> buscar(
            Connection conexion,
            long idConfiguracion
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR)) {
            sentencia.setLong(1, idConfiguracion);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    private static void establecerContexto(
            Connection conexion,
            long idUsuario,
            String motivo
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT set_config('soracare.id_usuario', ?, true), "
                        + "set_config('soracare.motivo_configuracion', ?, true)")) {
            sentencia.setString(1, Long.toString(idUsuario));
            sentencia.setString(2, motivo);
            sentencia.executeQuery();
        }
    }

    private static ConfiguracionSistemaDTO mapear(ResultSet resultado) throws SQLException {
        return new ConfiguracionSistemaDTO(
                resultado.getLong("id_configuracion"),
                resultado.getString("clave"),
                resultado.getString("nombre"),
                resultado.getString("valor"),
                resultado.getString("tipo_dato"),
                resultado.getString("categoria"),
                resultado.getString("descripcion"),
                resultado.getBoolean("es_publica"),
                resultado.getBoolean("es_editable"),
                resultado.getBoolean("activo"),
                resultado.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
