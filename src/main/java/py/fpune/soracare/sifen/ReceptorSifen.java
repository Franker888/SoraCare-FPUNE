package py.fpune.soracare.sifen;

public record ReceptorSifen(
        boolean contribuyente,
        Integer tipoContribuyente,
        String ruc,
        Integer digitoVerificador,
        int tipoDocumentoIdentidad,
        String descripcionDocumentoIdentidad,
        String numeroDocumentoIdentidad,
        String nombreRazonSocial,
        String correo,
        String codigoCliente
) {
}
