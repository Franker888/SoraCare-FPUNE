package py.fpune.soracare.sifen;

/** Resultado de consulta, separado del estado fiscal persistido del documento. */
public record ResultadoConsultaSifen(Estado estado, String cdc, String codigo,
                                    String mensaje, String protocolo, int eventos, String observacionEventos) {
    public ResultadoConsultaSifen(Estado estado, String cdc, String codigo, String mensaje, String protocolo, int eventos) {
        this(estado, cdc, codigo, mensaje, protocolo, eventos, "NO_EVALUADO");
    }
    public enum Estado { ENCONTRADO, NO_ENCONTRADO, SIN_PERMISO, ERROR }
}
