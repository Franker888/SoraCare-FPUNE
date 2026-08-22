package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.EventoAuditoriaDTO;
import py.fpune.soracare.dto.FiltroAuditoriaDTO;
import py.fpune.soracare.dto.ResumenAuditoriaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaDAO {

    private static final int LIMITE_EVENTOS = 150;

    private static final String CONDICIONES_FILTRO = """
            FROM soracare.auditoria_eventos aud
            LEFT JOIN soracare.usuarios usu
                ON usu.id_usuario = aud.id_usuario
            LEFT JOIN soracare.personas per
                ON per.id_persona = usu.id_persona
            WHERE aud.fecha_evento >= ?::date
              AND aud.fecha_evento < (?::date + INTERVAL '1 day')
              AND (? = '' OR aud.tipo_evento = ?)
              AND (? = '' OR aud.modulo = ?)
              AND (? = '' OR aud.resultado = ?)
              AND (
                    ? = ''
                    OR usu.nombre_usuario ILIKE '%' || ? || '%'
                    OR COALESCE(per.nombres, '') ILIKE '%' || ? || '%'
                    OR COALESCE(per.apellidos, '') ILIKE '%' || ? || '%'
                    OR COALESCE(aud.descripcion, '') ILIKE '%' || ? || '%'
                    OR aud.entidad ILIKE '%' || ? || '%'
                    OR COALESCE(aud.ruta_solicitud, '') ILIKE '%' || ? || '%'
              )
            """;

    private static final String SQL_LISTAR = """
            SELECT
                aud.id_auditoria,
                aud.id_usuario,
                COALESCE(
                    NULLIF(BTRIM(CONCAT_WS(' ', per.nombres, per.apellidos)), ''),
                    usu.nombre_usuario,
                    'Sistema'
                ) AS actor,
                usu.nombre_usuario,
                aud.tipo_evento,
                aud.modulo,
                aud.entidad,
                aud.id_registro,
                aud.descripcion,
                aud.direccion_ip::text AS direccion_ip,
                aud.metodo_http,
                aud.ruta_solicitud,
                aud.resultado,
                aud.mensaje_error,
                aud.fecha_evento
            """ + CONDICIONES_FILTRO + """
            ORDER BY aud.fecha_evento DESC, aud.id_auditoria DESC
            LIMIT """ + " " + LIMITE_EVENTOS;

    private static final String SQL_RESUMEN = """
            SELECT
                COUNT(*) AS total,
                COUNT(*) FILTER (WHERE aud.resultado = 'EXITOSO') AS exitosos,
                COUNT(*) FILTER (WHERE aud.resultado = 'FALLIDO') AS fallidos,
                COUNT(DISTINCT aud.id_usuario) FILTER (WHERE aud.id_usuario IS NOT NULL)
                    AS usuarios_distintos
            """ + CONDICIONES_FILTRO;

    private static final String SQL_MODULOS = """
            SELECT DISTINCT modulo
            FROM soracare.auditoria_eventos
            ORDER BY modulo
            """;

    private static final String SQL_REGISTRAR = """
            INSERT INTO soracare.auditoria_eventos (
                id_usuario,
                tipo_evento,
                modulo,
                entidad,
                id_registro,
                descripcion,
                direccion_ip,
                metodo_http,
                ruta_solicitud,
                agente_usuario,
                resultado,
                mensaje_error
            )
            VALUES (?, ?, ?, ?, ?, ?, ?::inet, ?, ?, ?, ?, ?)
            """;

    public ResumenAuditoriaDTO consultar(FiltroAuditoriaDTO filtro) throws SQLException {
        List<EventoAuditoriaDTO> eventos = new ArrayList<>();
        long total;
        long exitosos;
        long fallidos;
        long usuariosDistintos;

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR)) {
                establecerFiltro(sentencia, filtro);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        eventos.add(mapearEvento(resultado));
                    }
                }
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_RESUMEN)) {
                establecerFiltro(sentencia, filtro);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException("No fue posible resumir la auditoría.");
                    }
                    total = resultado.getLong("total");
                    exitosos = resultado.getLong("exitosos");
                    fallidos = resultado.getLong("fallidos");
                    usuariosDistintos = resultado.getLong("usuarios_distintos");
                }
            }

            List<String> modulos = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_MODULOS);
                 ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    modulos.add(resultado.getString("modulo"));
                }
            }

            return new ResumenAuditoriaDTO(
                    List.copyOf(eventos), total, exitosos, fallidos,
                    usuariosDistintos, List.copyOf(modulos)
            );
        }
    }

    public void registrar(
            Long idUsuario,
            String tipoEvento,
            String modulo,
            String entidad,
            String idRegistro,
            String descripcion,
            String direccionIp,
            String metodoHttp,
            String rutaSolicitud,
            String agenteUsuario,
            String resultado,
            String mensajeError
    ) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_REGISTRAR)) {
            if (idUsuario == null) sentencia.setNull(1, Types.BIGINT);
            else sentencia.setLong(1, idUsuario);
            sentencia.setString(2, tipoEvento);
            sentencia.setString(3, modulo);
            sentencia.setString(4, entidad);
            establecerTextoOpcional(sentencia, 5, idRegistro);
            establecerTextoOpcional(sentencia, 6, descripcion);
            establecerTextoOpcional(sentencia, 7, direccionIp);
            establecerTextoOpcional(sentencia, 8, metodoHttp);
            establecerTextoOpcional(sentencia, 9, rutaSolicitud);
            establecerTextoOpcional(sentencia, 10, limitar(agenteUsuario, 500));
            sentencia.setString(11, resultado);
            establecerTextoOpcional(sentencia, 12, mensajeError);
            sentencia.executeUpdate();
        }
    }

    private static void establecerFiltro(
            PreparedStatement sentencia,
            FiltroAuditoriaDTO filtro
    ) throws SQLException {
        sentencia.setObject(1, filtro.desde());
        sentencia.setObject(2, filtro.hasta());
        sentencia.setString(3, filtro.tipoEvento());
        sentencia.setString(4, filtro.tipoEvento());
        sentencia.setString(5, filtro.modulo());
        sentencia.setString(6, filtro.modulo());
        sentencia.setString(7, filtro.resultado());
        sentencia.setString(8, filtro.resultado());
        for (int posicion = 9; posicion <= 15; posicion++) {
            sentencia.setString(posicion, filtro.busqueda());
        }
    }

    private static EventoAuditoriaDTO mapearEvento(ResultSet resultado) throws SQLException {
        long idUsuarioValor = resultado.getLong("id_usuario");
        Long idUsuario = resultado.wasNull() ? null : idUsuarioValor;
        return new EventoAuditoriaDTO(
                resultado.getLong("id_auditoria"),
                idUsuario,
                resultado.getString("actor"),
                resultado.getString("nombre_usuario"),
                resultado.getString("tipo_evento"),
                resultado.getString("modulo"),
                resultado.getString("entidad"),
                resultado.getString("id_registro"),
                resultado.getString("descripcion"),
                resultado.getString("direccion_ip"),
                resultado.getString("metodo_http"),
                resultado.getString("ruta_solicitud"),
                resultado.getString("resultado"),
                resultado.getString("mensaje_error"),
                resultado.getObject("fecha_evento", OffsetDateTime.class)
        );
    }

    private static void establecerTextoOpcional(
            PreparedStatement sentencia,
            int posicion,
            String valor
    ) throws SQLException {
        if (valor == null || valor.isBlank()) sentencia.setNull(posicion, Types.VARCHAR);
        else sentencia.setString(posicion, valor.strip());
    }

    private static String limitar(String valor, int maximo) {
        if (valor == null) return null;
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
