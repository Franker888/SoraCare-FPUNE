package py.fpune.soracare.dto;

public class SolicitudCambioContrasenaDTO {

    private final String contrasenaActual;
    private final String nuevaContrasena;
    private final String confirmacionNuevaContrasena;

    public SolicitudCambioContrasenaDTO(
            String contrasenaActual,
            String nuevaContrasena,
            String confirmacionNuevaContrasena
    ) {
        this.contrasenaActual =
                contrasenaActual;

        this.nuevaContrasena =
                nuevaContrasena;

        this.confirmacionNuevaContrasena =
                confirmacionNuevaContrasena;
    }

    public String getContrasenaActual() {
        return contrasenaActual;
    }

    public String getNuevaContrasena() {
        return nuevaContrasena;
    }

    public String getConfirmacionNuevaContrasena() {
        return confirmacionNuevaContrasena;
    }
}