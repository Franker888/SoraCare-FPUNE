package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ArchivoDocumentoClinicoDTO;
import py.fpune.soracare.dto.DocumentoClinicoDTO;
import py.fpune.soracare.dto.RegistroDocumentoClinicoDTO;
import py.fpune.soracare.dto.TipoDocumentoClinicoDTO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DocumentoClinicoDAO {

    private static final String SQL_LISTAR_TIPOS = """
            SELECT id_tipo_documento, codigo, nombre, descripcion
            FROM soracare.tipos_documento
            WHERE activo = TRUE
              AND categoria IN ('CLINICO', 'OTRO')
            ORDER BY CASE codigo
                         WHEN 'RESULTADO_ESTUDIO' THEN 1
                         WHEN 'INFORME_MEDICO' THEN 2
                         WHEN 'RECETA_MEDICA' THEN 3
                         WHEN 'CONSENTIMIENTO_INFORMADO' THEN 4
                         ELSE 5
                     END,
                     nombre
            """;

    private static final String SQL_BUSCAR_TIPO = """
            SELECT id_tipo_documento, codigo, nombre, descripcion
            FROM soracare.tipos_documento
            WHERE id_tipo_documento = ?
              AND activo = TRUE
              AND categoria IN ('CLINICO', 'OTRO')
            """;

    private static final String SQL_REGISTRAR = """
            INSERT INTO soracare.documentos (
                id_tipo_documento,
                id_paciente,
                id_atencion,
                id_usuario_carga,
                codigo_documento,
                nombre_archivo,
                nombre_original,
                extension,
                tipo_mime,
                tamano_bytes,
                ruta_almacenamiento,
                hash_sha256,
                descripcion,
                origen,
                visibilidad,
                estado,
                confidencial,
                fecha_documento
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                      'CARGADO', 'PACIENTE', 'DISPONIBLE', TRUE, ?)
            RETURNING id_documento
            """;

    private static final String SQL_LISTAR_POR_PACIENTE = """
            SELECT d.id_documento,
                   d.id_paciente,
                   d.id_atencion,
                   d.id_tipo_documento,
                   td.codigo AS codigo_tipo,
                   td.nombre AS nombre_tipo,
                   d.nombre_original,
                   d.tipo_mime,
                   d.tamano_bytes,
                   d.descripcion,
                   d.origen,
                   d.fecha_documento,
                   d.fecha_creacion,
                   COALESCE(
                       NULLIF(BTRIM(CONCAT_WS(' ', per.nombres, per.apellidos)), ''),
                       usu.nombre_usuario,
                       'Sistema'
                   ) AS cargado_por
            FROM soracare.documentos d
            INNER JOIN soracare.tipos_documento td
                    ON td.id_tipo_documento = d.id_tipo_documento
            LEFT JOIN soracare.usuarios usu
                   ON usu.id_usuario = d.id_usuario_carga
            LEFT JOIN soracare.personas per
                   ON per.id_persona = usu.id_persona
            WHERE d.id_paciente = ?
              AND d.estado = 'DISPONIBLE'
              AND td.categoria IN ('CLINICO', 'OTRO')
            ORDER BY COALESCE(d.fecha_documento, d.fecha_creacion::date) DESC,
                     d.fecha_creacion DESC,
                     d.id_documento DESC
            """;

    private static final String SQL_BUSCAR_ARCHIVO = """
            SELECT d.id_documento,
                   d.id_paciente,
                   d.nombre_original,
                   d.tipo_mime,
                   d.tamano_bytes,
                   d.ruta_almacenamiento,
                   d.hash_sha256
            FROM soracare.documentos d
            INNER JOIN soracare.tipos_documento td
                    ON td.id_tipo_documento = d.id_tipo_documento
            WHERE d.id_documento = ?
              AND d.estado = 'DISPONIBLE'
              AND td.categoria IN ('CLINICO', 'OTRO')
            """;

    private static final String SQL_REGISTRAR_ACCESO = """
            INSERT INTO soracare.accesos_documentos (
                id_documento,
                id_usuario,
                accion,
                direccion_ip,
                agente_usuario,
                exitoso,
                detalle_error
            ) VALUES (?, ?, ?, CAST(? AS inet), ?, ?, ?)
            """;

    public List<TipoDocumentoClinicoDTO> listarTiposActivos() throws SQLException {
        List<TipoDocumentoClinicoDTO> tipos = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_TIPOS);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                tipos.add(mapearTipo(resultado));
            }
        }
        return List.copyOf(tipos);
    }

    public Optional<TipoDocumentoClinicoDTO> buscarTipoActivo(
            Connection conexion,
            short idTipoDocumento
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_TIPO)) {
            sentencia.setShort(1, idTipoDocumento);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(mapearTipo(resultado))
                        : Optional.empty();
            }
        }
    }

    public long registrar(
            Connection conexion,
            RegistroDocumentoClinicoDTO datos
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_REGISTRAR)) {
            sentencia.setShort(1, datos.idTipoDocumento());
            sentencia.setLong(2, datos.idPaciente());
            if (datos.idAtencion() == null) {
                sentencia.setNull(3, Types.BIGINT);
            } else {
                sentencia.setLong(3, datos.idAtencion());
            }
            sentencia.setLong(4, datos.idUsuarioCarga());
            sentencia.setString(5, "DOC-" + UUID.randomUUID()
                    .toString().replace("-", "").toUpperCase());
            sentencia.setString(6, datos.nombreAlmacenado());
            sentencia.setString(7, datos.nombreOriginal());
            sentencia.setString(8, datos.extension());
            sentencia.setString(9, datos.tipoMime());
            sentencia.setLong(10, datos.tamanoBytes());
            sentencia.setString(11, datos.rutaAlmacenamiento());
            sentencia.setString(12, datos.hashSha256());
            if (datos.descripcion() == null) {
                sentencia.setNull(13, Types.VARCHAR);
            } else {
                sentencia.setString(13, datos.descripcion());
            }
            if (datos.fechaDocumento() == null) {
                sentencia.setNull(14, Types.DATE);
            } else {
                sentencia.setDate(14, Date.valueOf(datos.fechaDocumento()));
            }
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("PostgreSQL no devolvió el documento registrado.");
                }
                return resultado.getLong("id_documento");
            }
        }
    }

    public List<DocumentoClinicoDTO> listarPorPaciente(long idPaciente)
            throws SQLException {
        List<DocumentoClinicoDTO> documentos = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_POR_PACIENTE)) {
            sentencia.setLong(1, idPaciente);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    documentos.add(mapearDocumento(resultado));
                }
            }
        }
        return List.copyOf(documentos);
    }

    public Optional<ArchivoDocumentoClinicoDTO> buscarArchivo(long idDocumento)
            throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_ARCHIVO)) {
            sentencia.setLong(1, idDocumento);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(new ArchivoDocumentoClinicoDTO(
                                resultado.getLong("id_documento"),
                                resultado.getLong("id_paciente"),
                                resultado.getString("nombre_original"),
                                resultado.getString("tipo_mime"),
                                resultado.getLong("tamano_bytes"),
                                resultado.getString("ruta_almacenamiento"),
                                resultado.getString("hash_sha256")
                        ))
                        : Optional.empty();
            }
        }
    }

    public void registrarAcceso(
            long idDocumento,
            long idUsuario,
            String accion,
            String direccionIp,
            String agenteUsuario,
            boolean exitoso,
            String detalleError
    ) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_REGISTRAR_ACCESO)) {
            sentencia.setLong(1, idDocumento);
            sentencia.setLong(2, idUsuario);
            sentencia.setString(3, accion);
            if (direccionIp == null || direccionIp.isBlank()) {
                sentencia.setNull(4, Types.VARCHAR);
            } else {
                sentencia.setString(4, direccionIp.strip());
            }
            if (agenteUsuario == null || agenteUsuario.isBlank()) {
                sentencia.setNull(5, Types.VARCHAR);
            } else {
                String agente = agenteUsuario.strip();
                sentencia.setString(5, agente.substring(0, Math.min(agente.length(), 500)));
            }
            sentencia.setBoolean(6, exitoso);
            if (detalleError == null || detalleError.isBlank()) {
                sentencia.setNull(7, Types.VARCHAR);
            } else {
                String detalle = detalleError.strip();
                sentencia.setString(7, detalle.substring(0, Math.min(detalle.length(), 500)));
            }
            sentencia.executeUpdate();
        }
    }

    private static TipoDocumentoClinicoDTO mapearTipo(ResultSet resultado)
            throws SQLException {
        return new TipoDocumentoClinicoDTO(
                resultado.getShort("id_tipo_documento"),
                resultado.getString("codigo"),
                resultado.getString("nombre"),
                resultado.getString("descripcion")
        );
    }

    private static DocumentoClinicoDTO mapearDocumento(ResultSet resultado)
            throws SQLException {
        return new DocumentoClinicoDTO(
                resultado.getLong("id_documento"),
                resultado.getLong("id_paciente"),
                (Long) resultado.getObject("id_atencion"),
                resultado.getShort("id_tipo_documento"),
                resultado.getString("codigo_tipo"),
                resultado.getString("nombre_tipo"),
                resultado.getString("nombre_original"),
                resultado.getString("tipo_mime"),
                resultado.getLong("tamano_bytes"),
                resultado.getString("descripcion"),
                resultado.getString("origen"),
                resultado.getDate("fecha_documento") == null
                        ? null : resultado.getDate("fecha_documento").toLocalDate(),
                resultado.getObject("fecha_creacion", java.time.OffsetDateTime.class),
                resultado.getString("cargado_por")
        );
    }
}
