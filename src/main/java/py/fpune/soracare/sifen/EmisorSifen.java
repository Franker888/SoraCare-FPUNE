package py.fpune.soracare.sifen;

public record EmisorSifen(
        String ruc,
        int digitoVerificador,
        int tipoContribuyente,
        Integer tipoRegimen,
        String razonSocial,
        String nombreFantasia,
        String direccion,
        String numeroCasa,
        int departamento,
        String descripcionDepartamento,
        Integer distrito,
        String descripcionDistrito,
        int ciudad,
        String descripcionCiudad,
        String telefono,
        String correo,
        String codigoActividad,
        String descripcionActividad
) {
}
