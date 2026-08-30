package py.fpune.soracare.api.security;

public class ApiNoAutenticadoException extends RuntimeException {

    public ApiNoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
