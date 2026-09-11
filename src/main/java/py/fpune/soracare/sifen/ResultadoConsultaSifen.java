package py.fpune.soracare.sifen;

/** Resultado de consulta, separado del estado fiscal persistido del documento. */
public record ResultadoConsultaSifen(Estado estado, String cdc, String codigo,
                                    String mensaje, String protocolo, int eventos) {
    public enum Estado { ENCONTRADO, NO_ENCONTRADO, SIN_PERMISO, ERROR }
}
