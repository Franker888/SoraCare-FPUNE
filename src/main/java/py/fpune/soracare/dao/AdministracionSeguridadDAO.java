package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ResumenUsuariosDTO;
import py.fpune.soracare.dto.RolAdministrativoDTO;
import py.fpune.soracare.dto.UsuarioAdministrativoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdministracionSeguridadDAO {

    private static final String SQL_LISTAR_USUARIOS = """
            SELECT
                usu.id_usuario,
                usu.id_persona,
                BTRIM(CONCAT_WS(' ', per.nombres, per.apellidos)) AS nombre_completo,
                per.cedula,
                per.correo,
                usu.nombre_usuario,
                usu.estado,
                usu.intentos_fallidos,
                usu.bloqueado_hasta,
                usu.debe_cambiar_contrasena,
                usu.ultimo_acceso,
                usu.creado_en,
                COALESCE(STRING_AGG(rol.nombre, ', ' ORDER BY rol.nombre), 'Sin rol') AS roles
            FROM soracare.usuarios usu
            INNER JOIN soracare.personas per ON per.id_persona = usu.id_persona
            LEFT JOIN soracare.usuarios_roles ur ON ur.id_usuario = usu.id_usuario
            LEFT JOIN soracare.roles rol ON rol.id_rol = ur.id_rol
            WHERE (? = '' OR usu.estado = ?)
              AND (
                    ? = ''
                    OR usu.nombre_usuario ILIKE '%' || ? || '%'
                    OR per.cedula::text ILIKE '%' || ? || '%'
                    OR per.correo ILIKE '%' || ? || '%'
                    OR per.nombres ILIKE '%' || ? || '%'
                    OR per.apellidos ILIKE '%' || ? || '%'
              )
            GROUP BY usu.id_usuario, per.id_persona
            ORDER BY per.apellidos, per.nombres, usu.id_usuario
            LIMIT 250
            """;

    private static final String SQL_RESUMEN = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE estado = 'ACTIVO') AS activos,
                   COUNT(*) FILTER (WHERE estado = 'BLOQUEADO') AS bloqueados,
                   COUNT(*) FILTER (WHERE estado = 'INACTIVO') AS inactivos
            FROM soracare.usuarios
            """;

    private static final String SQL_ACTUALIZAR_ESTADO = """
            UPDATE soracare.usuarios
            SET estado = ?,
                intentos_fallidos = CASE WHEN ? = 'ACTIVO' THEN 0 ELSE intentos_fallidos END,
                bloqueado_hasta = CASE WHEN ? = 'ACTIVO' THEN NULL ELSE bloqueado_hasta END,
                actualizado_en = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
            """;

    private static final String SQL_LISTAR_ROLES = """
            SELECT rol.id_rol, rol.nombre, rol.descripcion, rol.activo,
                   COUNT(ur.id_usuario) AS usuarios_asignados
            FROM soracare.roles rol
            LEFT JOIN soracare.usuarios_roles ur ON ur.id_rol = rol.id_rol
            GROUP BY rol.id_rol
            ORDER BY rol.id_rol
            """;

    private static final String SQL_ES_ADMINISTRADOR = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.usuarios_roles ur
                INNER JOIN soracare.roles rol ON rol.id_rol = ur.id_rol
                WHERE ur.id_usuario = ? AND rol.nombre = 'ADMINISTRADOR'
            ) AS es_administrador
            """;

    private static final String SQL_CONTAR_ADMINISTRADORES_ACTIVOS = """
            SELECT COUNT(DISTINCT usu.id_usuario) AS total
            FROM soracare.usuarios usu
            INNER JOIN soracare.usuarios_roles ur ON ur.id_usuario = usu.id_usuario
            INNER JOIN soracare.roles rol ON rol.id_rol = ur.id_rol
            WHERE rol.nombre = 'ADMINISTRADOR' AND usu.estado = 'ACTIVO'
            """;

    public ResumenUsuariosDTO listarUsuarios(String busqueda, String estado) throws SQLException {
        List<UsuarioAdministrativoDTO> usuarios = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_USUARIOS)) {
                sentencia.setString(1, estado);
                sentencia.setString(2, estado);
                for (int posicion = 3; posicion <= 8; posicion++) {
                    sentencia.setString(posicion, busqueda);
                }
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) usuarios.add(mapearUsuario(resultado));
                }
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_RESUMEN);
                 ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) throw new SQLException("No fue posible resumir los usuarios.");
                return new ResumenUsuariosDTO(
                        List.copyOf(usuarios),
                        resultado.getLong("total"),
                        resultado.getLong("activos"),
                        resultado.getLong("bloqueados"),
                        resultado.getLong("inactivos")
                );
            }
        }
    }

    public void actualizarEstado(long idUsuario, String estado) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
            sentencia.setString(1, estado);
            sentencia.setString(2, estado);
            sentencia.setString(3, estado);
            sentencia.setLong(4, idUsuario);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("La cuenta indicada no existe.");
            }
        }
    }

    public List<RolAdministrativoDTO> listarRoles() throws SQLException {
        List<RolAdministrativoDTO> roles = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_ROLES);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                roles.add(new RolAdministrativoDTO(
                        resultado.getShort("id_rol"),
                        resultado.getString("nombre"),
                        resultado.getString("descripcion"),
                        resultado.getBoolean("activo"),
                        resultado.getLong("usuarios_asignados")
                ));
            }
        }
        return List.copyOf(roles);
    }

    public boolean esAdministrador(long idUsuario) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ES_ADMINISTRADOR)) {
            sentencia.setLong(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) throw new SQLException("No fue posible verificar el rol.");
                return resultado.getBoolean("es_administrador");
            }
        }
    }

    public long contarAdministradoresActivos() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_CONTAR_ADMINISTRADORES_ACTIVOS);
             ResultSet resultado = sentencia.executeQuery()) {
            if (!resultado.next()) throw new SQLException("No fue posible contar administradores.");
            return resultado.getLong("total");
        }
    }

    private static UsuarioAdministrativoDTO mapearUsuario(ResultSet resultado) throws SQLException {
        return new UsuarioAdministrativoDTO(
                resultado.getLong("id_usuario"),
                resultado.getLong("id_persona"),
                resultado.getString("nombre_completo"),
                resultado.getString("cedula"),
                resultado.getString("correo"),
                resultado.getString("nombre_usuario"),
                resultado.getString("estado"),
                resultado.getShort("intentos_fallidos"),
                resultado.getObject("bloqueado_hasta", OffsetDateTime.class),
                resultado.getBoolean("debe_cambiar_contrasena"),
                resultado.getObject("ultimo_acceso", OffsetDateTime.class),
                resultado.getObject("creado_en", OffsetDateTime.class),
                resultado.getString("roles")
        );
    }
}
