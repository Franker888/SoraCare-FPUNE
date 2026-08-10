package py.fpune.soracare.dto;

public record RegistroCuentaPacienteDTO(
        String nombres,
        String apellidos,
        String cedula,
        String fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        String grupoSanguineo,
        String contactoEmergencia,
        String telefonoEmergencia,
        String nombreUsuario,
        String contrasena,
        String confirmacionContrasena
) {
}
