package py.fpune.soracare.api.security;

public class ApiAccesoDenegadoException extends RuntimeException {

    public ApiAccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
