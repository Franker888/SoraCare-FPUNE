package py.fpune.soracare.sifen;

public record RespuestaRecepcionSifen(
        Estado estado,
        int codigoHttp,
        String protocolo,
        String codigo,
        String mensaje,
        byte[] contenido
) {
    public RespuestaRecepcionSifen {
        contenido = contenido == null ? new byte[0] : contenido.clone();
    }

    @Override
    public byte[] contenido() {
        return contenido.clone();
    }

    public enum Estado {
        APROBADO,
        RECHAZADO,
        ERROR
    }
}
