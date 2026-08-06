package py.fpune.soracare.dto;

public class SolicitudCreacionUsuarioDTO {

    private final String idPersona;
    private final String nombreUsuario;
    private final String contrasena;
    private final String confirmacionContrasena;
    private final String nombreRol;
    private final boolean debeCambiarContrasena;

    public SolicitudCreacionUsuarioDTO(
            String idPersona,
            String nombreUsuario,
            String contrasena,
            String confirmacionContrasena,
            String nombreRol,
            boolean debeCambiarContrasena
    ) {
        this.idPersona = idPersona;
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
        this.confirmacionContrasena =
                confirmacionContrasena;
        this.nombreRol = nombreRol;
        this.debeCambiarContrasena =
                debeCambiarContrasena;
    }

    public String getIdPersona() {
        return idPersona;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getConfirmacionContrasena() {
        return confirmacionContrasena;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public boolean isDebeCambiarContrasena() {
        return debeCambiarContrasena;
    }
}