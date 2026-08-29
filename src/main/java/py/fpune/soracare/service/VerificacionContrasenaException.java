package py.fpune.soracare.service;

/**
 * Indica que el servicio externo de reputacion de contrasenas no pudo
 * completar una verificacion segura.
 */
public class VerificacionContrasenaException extends RuntimeException {

    public VerificacionContrasenaException(String mensaje) {
        super(mensaje);
    }

    public VerificacionContrasenaException(
            String mensaje,
            Throwable causa
    ) {
        super(mensaje, causa);
    }
}
