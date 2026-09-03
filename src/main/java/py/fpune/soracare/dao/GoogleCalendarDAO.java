package py.fpune.soracare.dao;

import py.fpune.soracare.dto.CitaGoogleCalendarDTO;
import py.fpune.soracare.dto.VinculoGoogleCalendarDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class GoogleCalendarDAO {

    private static final String SQL_BUSCAR_VINCULO = """
            SELECT id_usuario,
                   correo_google,
                   refresh_token_cifrado,
                   alcance,
                   actualizado_en
            FROM soracare.google_calendar_vinculos
            WHERE id_usuario = ?
            """;

    private static final String SQL_GUARDAR_VINCULO = """
            INSERT INTO soracare.google_calendar_vinculos (
                id_usuario,
                correo_google,
                refresh_token_cifrado,
                alcance
            )
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id_usuario) DO UPDATE
            SET correo_google = EXCLUDED.correo_google,
                refresh_token_cifrado = EXCLUDED.refresh_token_cifrado,
                alcance = EXCLUDED.alcance,
                actualizado_en = CURRENT_TIMESTAMP
            """;

    private static final String SQL_BUSCAR_EVENTO = """
            SELECT id_evento_google
            FROM soracare.google_calendar_eventos
            WHERE id_usuario = ?
              AND id_cita = ?
            """;

    private static final String SQL_LISTAR_CITAS_VINCULADAS = """
            SELECT id_cita
            FROM soracare.google_calendar_eventos
            WHERE id_usuario = ?
            ORDER BY id_cita
            """;

    private static final String SQL_GUARDAR_EVENTO = """
            INSERT INTO soracare.google_calendar_eventos (
                id_usuario,
                id_cita,
                id_evento_google
            )
            VALUES (?, ?, ?)
            ON CONFLICT (id_usuario, id_cita) DO UPDATE
            SET id_evento_google = EXCLUDED.id_evento_google,
                actualizado_en = CURRENT_TIMESTAMP
            """;

    private static final String SQL_ELIMINAR_EVENTO = """
            DELETE FROM soracare.google_calendar_eventos
            WHERE id_usuario = ?
              AND id_cita = ?
            """;

    private static final String SQL_BUSCAR_CITA_PACIENTE = """
            SELECT c.id_cita,
                   esp.nombre AS especialidad,
                   BTRIM(pm.nombres || ' ' || pm.apellidos) AS profesional,
                   BTRIM(co.nombre || ' - ' || sede.nombre) AS ubicacion,
                   c.fecha_hora_inicio,
                   c.fecha_hora_fin
            FROM soracare.citas c
            JOIN soracare.estados_citas ec
              ON ec.id_estado_cita = c.id_estado_cita
            JOIN soracare.pacientes pac
              ON pac.id_paciente = c.id_paciente
            JOIN soracare.usuarios u
              ON u.id_persona = pac.id_persona
            JOIN soracare.medicos med
              ON med.id_medico = c.id_medico
            JOIN soracare.personas pm
              ON pm.id_persona = med.id_persona
            JOIN soracare.especialidades esp
              ON esp.id_especialidad = c.id_especialidad
            JOIN soracare.consultorios co
              ON co.id_consultorio = c.id_consultorio
            JOIN soracare.sedes sede
              ON sede.id_sede = co.id_sede
            WHERE c.id_cita = ?
              AND u.id_usuario = ?
              AND ec.codigo = 'CONFIRMADA'
              AND c.fecha_hora_inicio > CURRENT_TIMESTAMP
            """;

    public Optional<VinculoGoogleCalendarDTO> buscarVinculo(
            Connection conexion,
            long idUsuario
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_BUSCAR_VINCULO)) {
            sentencia.setLong(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                return Optional.of(new VinculoGoogleCalendarDTO(
                        resultado.getLong("id_usuario"),
                        resultado.getString("correo_google"),
                        resultado.getString("refresh_token_cifrado"),
                        resultado.getString("alcance"),
                        resultado.getObject(
                                "actualizado_en",
                                OffsetDateTime.class
                        )
                ));
            }
        }
    }

    public void guardarVinculo(
            Connection conexion,
            long idUsuario,
            String correo,
            String refreshTokenCifrado,
            String alcance
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_GUARDAR_VINCULO)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setString(2, textoObligatorio(
                    correo,
                    "El correo de Google es obligatorio."
            ));
            sentencia.setString(3, textoObligatorio(
                    refreshTokenCifrado,
                    "La autorización cifrada es obligatoria."
            ));
            sentencia.setString(4, textoObligatorio(
                    alcance,
                    "El alcance autorizado es obligatorio."
            ));
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No fue posible guardar la vinculación con Google Calendar."
                );
            }
        }
    }

    public Optional<String> buscarIdEvento(
            Connection conexion,
            long idUsuario,
            long idCita
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        validarId(idCita, "La cita no es válida.");
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_BUSCAR_EVENTO)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setLong(2, idCita);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(resultado.getString("id_evento_google"))
                        : Optional.empty();
            }
        }
    }

    public Set<Long> listarCitasVinculadas(
            Connection conexion,
            long idUsuario
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        Set<Long> ids = new LinkedHashSet<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_LISTAR_CITAS_VINCULADAS)) {
            sentencia.setLong(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    ids.add(resultado.getLong("id_cita"));
                }
            }
        }
        return Set.copyOf(ids);
    }

    public void guardarEvento(
            Connection conexion,
            long idUsuario,
            long idCita,
            String idEvento
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        validarId(idCita, "La cita no es válida.");
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_GUARDAR_EVENTO)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setLong(2, idCita);
            sentencia.setString(3, textoObligatorio(
                    idEvento,
                    "El identificador del evento es obligatorio."
            ));
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No fue posible relacionar la cita con Google Calendar."
                );
            }
        }
    }

    public void eliminarEvento(
            Connection conexion,
            long idUsuario,
            long idCita
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        validarId(idCita, "La cita no es válida.");
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_ELIMINAR_EVENTO)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setLong(2, idCita);
            sentencia.executeUpdate();
        }
    }

    public Optional<CitaGoogleCalendarDTO> buscarCitaDelPaciente(
            Connection conexion,
            long idUsuario,
            long idCita
    ) throws SQLException {
        validarId(idUsuario, "El usuario no es válido.");
        validarId(idCita, "La cita no es válida.");
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_BUSCAR_CITA_PACIENTE)) {
            sentencia.setLong(1, idCita);
            sentencia.setLong(2, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CitaGoogleCalendarDTO(
                        resultado.getLong("id_cita"),
                        resultado.getString("especialidad"),
                        resultado.getString("profesional"),
                        resultado.getString("ubicacion"),
                        resultado.getObject(
                                "fecha_hora_inicio",
                                OffsetDateTime.class
                        ),
                        resultado.getObject(
                                "fecha_hora_fin",
                                OffsetDateTime.class
                        )
                ));
            }
        }
    }

    private static void validarId(long id, String mensaje) {
        if (id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private static String textoObligatorio(
            String valor,
            String mensaje
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }
}
