package py.fpune.soracare.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.SesionApiDTO;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

@RestController
@RequestMapping(
        path = "/sesion",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class SesionRestController {

    @GetMapping
    public SesionApiDTO consultar(HttpServletRequest request) {
        UsuarioAutenticadoDTO usuario =
                SeguridadApiRest.requerirUsuario(request);

        return SesionApiDTO.desde(
                usuario,
                SeguridadApiRest.obtenerTokenCsrf(request)
        );
    }
}
