package py.fpune.soracare.dto;

public record RolAdministrativoDTO(
        short idRol,
        String nombre,
        String descripcion,
        boolean activo,
        long usuariosAsignados
) {
}
