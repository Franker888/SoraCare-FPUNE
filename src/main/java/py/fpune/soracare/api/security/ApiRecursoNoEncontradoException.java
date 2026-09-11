package py.fpune.soracare.api.security;

public class ApiRecursoNoEncontradoException extends RuntimeException {

    public ApiRecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
