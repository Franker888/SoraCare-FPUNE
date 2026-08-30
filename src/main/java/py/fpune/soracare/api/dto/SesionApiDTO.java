package py.fpune.soracare.api.dto;

import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.util.List;

public record SesionApiDTO(
        boolean autenticado,
        String nombreUsuario,
        List<String> roles,
        String tokenCsrf,
        String cabeceraCsrf
) {

    public static SesionApiDTO desde(
            UsuarioAutenticadoDTO usuario,
            String tokenCsrf
    ) {
        return new SesionApiDTO(
                true,
                usuario.getNombreUsuario(),
                usuario.getRoles().stream().sorted().toList(),
                tokenCsrf,
                SeguridadApiRest.CABECERA_CSRF
        );
    }
}
