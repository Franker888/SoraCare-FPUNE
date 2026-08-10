package py.fpune.soracare.dto;

public record CuentaPacienteCreadaDTO(
        long idPaciente,
        long idUsuario,
        String nombreUsuario
) {
}
