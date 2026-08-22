package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record UsuarioAdministrativoDTO(
        long idUsuario,
        long idPersona,
        String nombreCompleto,
        String cedula,
        String correo,
        String nombreUsuario,
        String estado,
        short intentosFallidos,
        OffsetDateTime bloqueadoHasta,
        boolean debeCambiarContrasena,
        OffsetDateTime ultimoAcceso,
        OffsetDateTime creadoEn,
        String roles
) {
}
