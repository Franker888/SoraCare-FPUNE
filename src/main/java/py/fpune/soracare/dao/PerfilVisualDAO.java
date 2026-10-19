package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.PerfilVisualDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PerfilVisualDAO {

    public PerfilVisualDTO consultar(long idPersona) throws SQLException {
        String sql = """
                SELECT avatar_codigo,
                       (ruta_foto IS NOT NULL OR contenido_foto IS NOT NULL) AS tiene_foto
                  FROM soracare.perfiles_visuales
                 WHERE id_persona = ?
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return new PerfilVisualDTO("AZUL", false);
                }
                return new PerfilVisualDTO(
                        resultado.getString("avatar_codigo"),
                        resultado.getBoolean("tiene_foto")
                );
            }
        }
    }

    public Optional<ArchivoPerfil> buscarArchivo(long idPersona) throws SQLException {
        String sql = """
                SELECT ruta_foto, contenido_foto, tipo_mime, tamano_bytes, hash_sha256
                  FROM soracare.perfiles_visuales
                 WHERE id_persona = ?
                   AND (ruta_foto IS NOT NULL OR contenido_foto IS NOT NULL)
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ArchivoPerfil(
                        resultado.getString("ruta_foto"),
                         resultado.getBytes("contenido_foto"),
                        resultado.getString("tipo_mime"),
                        resultado.getLong("tamano_bytes"),
                        resultado.getString("hash_sha256")
                ));
            }
        }
    }

    public String guardarFoto(
            long idPersona,
            String ruta,
            byte[] contenido,
            String tipoMime,
            long tamanoBytes,
            String hashSha256
    ) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                String anterior = bloquearYObtenerRuta(conexion, idPersona);
                String sql = """
                        INSERT INTO soracare.perfiles_visuales (
                            id_persona, avatar_codigo, ruta_foto, contenido_foto,
                            tipo_mime, tamano_bytes, hash_sha256, fecha_actualizacion
                        ) VALUES (?, 'AZUL', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT (id_persona) DO UPDATE SET
                            ruta_foto = EXCLUDED.ruta_foto,
                            contenido_foto = EXCLUDED.contenido_foto,
                            tipo_mime = EXCLUDED.tipo_mime,
                            tamano_bytes = EXCLUDED.tamano_bytes,
                            hash_sha256 = EXCLUDED.hash_sha256,
                            fecha_actualizacion = CURRENT_TIMESTAMP
                        """;
                try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                    sentencia.setLong(1, idPersona);
                    sentencia.setString(2, ruta);
                    sentencia.setBytes(3, contenido);
                    sentencia.setString(4, tipoMime);
                    sentencia.setLong(5, tamanoBytes);
                    sentencia.setString(6, hashSha256);
                    sentencia.executeUpdate();
                }
                conexion.commit();
                return anterior;
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                throw error;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    public String elegirAvatar(long idPersona, String avatarCodigo) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                String anterior = bloquearYObtenerRuta(conexion, idPersona);
                String sql = """
                        INSERT INTO soracare.perfiles_visuales (
                            id_persona, avatar_codigo, fecha_actualizacion
                        ) VALUES (?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT (id_persona) DO UPDATE SET
                            avatar_codigo = EXCLUDED.avatar_codigo,
                            ruta_foto = NULL,
                            contenido_foto = NULL,
                            tipo_mime = NULL,
                            tamano_bytes = NULL,
                            hash_sha256 = NULL,
                            fecha_actualizacion = CURRENT_TIMESTAMP
                        """;
                try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                    sentencia.setLong(1, idPersona);
                    sentencia.setString(2, avatarCodigo);
                    sentencia.executeUpdate();
                }
                conexion.commit();
                return anterior;
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                throw error;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    private static String bloquearYObtenerRuta(Connection conexion, long idPersona)
            throws SQLException {
        String sql = """
                SELECT ruta_foto
                  FROM soracare.perfiles_visuales
                 WHERE id_persona = ?
                 FOR UPDATE
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getString("ruta_foto") : null;
            }
        }
    }

    public record ArchivoPerfil(
            String rutaRelativa,
            byte[] contenido,
            String tipoMime,
            long tamanoBytes,
            String hashSha256
    ) {
    }
}
