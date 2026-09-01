package py.fpune.soracare.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.io.IOException;

@WebFilter(
        filterName = "FiltroAutenticacion",
        urlPatterns = {
                "/dashboard",
                "/comprobantes/*",
                "/facturas/*",
                "/cuenta/*",
                "/perfil",
                "/perfil/*"
        }
)
public class FiltroAutenticacion implements Filter {

    private static final String ATRIBUTO_USUARIO_AUTENTICADO =
            "usuarioAutenticado";

    private static final String RUTA_LOGIN =
            "/login";

    private static final String RUTA_CAMBIO_CONTRASENA =
            "/cambiar-contrasena";

    @Override
    public void doFilter(
            ServletRequest solicitud,
            ServletResponse respuesta,
            FilterChain cadena
    ) throws IOException, ServletException {

        if (!(solicitud
                instanceof HttpServletRequest request)
                || !(respuesta
                instanceof HttpServletResponse response)) {

            cadena.doFilter(
                    solicitud,
                    respuesta
            );

            return;
        }

        configurarRespuestaSinCache(
                response
        );

        HttpSession sesion =
                request.getSession(false);

        if (sesion == null) {
            redirigirAlLogin(
                    request,
                    response
            );

            return;
        }

        Object atributoUsuario =
                sesion.getAttribute(
                        ATRIBUTO_USUARIO_AUTENTICADO
                );

        if (!(atributoUsuario
                instanceof UsuarioAutenticadoDTO usuarioAutenticado)) {

            sesion.invalidate();

            redirigirAlLogin(
                    request,
                    response
            );

            return;
        }

        if (usuarioAutenticado
                .isDebeCambiarContrasena()) {

            redirigirAlCambioContrasena(
                    request,
                    response
            );

            return;
        }

        cadena.doFilter(
                request,
                response
        );
    }

    private static void redirigirAlLogin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.sendRedirect(
                request.getContextPath()
                        + RUTA_LOGIN
        );
    }

    private static void redirigirAlCambioContrasena(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.sendRedirect(
                request.getContextPath()
                        + RUTA_CAMBIO_CONTRASENA
        );
    }

    private static void configurarRespuestaSinCache(
            HttpServletResponse response
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, "
                        + "must-revalidate, max-age=0"
        );

        response.setHeader(
                "Pragma",
                "no-cache"
        );

        response.setDateHeader(
                "Expires",
                0
        );
    }
}
