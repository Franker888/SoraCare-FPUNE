package py.fpune.soracare.dto;

public record ResultadoInicioGoogleDTO(
        Estado estado,
        UsuarioAutenticadoDTO usuarioAutenticado,
        Long idUsuarioExistente
) {

    public enum Estado {
        AUTENTICADO,
        REQUIERE_VINCULACION,
        REQUIERE_REGISTRO
    }

    public static ResultadoInicioGoogleDTO autenticado(
            UsuarioAutenticadoDTO usuario
    ) {
        return new ResultadoInicioGoogleDTO(
                Estado.AUTENTICADO,
                usuario,
                null
        );
    }

    public static ResultadoInicioGoogleDTO requiereVinculacion(
            long idUsuario
    ) {
        return new ResultadoInicioGoogleDTO(
                Estado.REQUIERE_VINCULACION,
                null,
                idUsuario
        );
    }

    public static ResultadoInicioGoogleDTO requiereRegistro() {
        return new ResultadoInicioGoogleDTO(
                Estado.REQUIERE_REGISTRO,
                null,
                null
        );
    }
}
