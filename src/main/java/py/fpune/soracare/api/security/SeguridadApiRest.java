package py.fpune.soracare.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;

public final class SeguridadApiRest {

    public static final String CABECERA_CSRF = "X-CSRF-Token";
    private static final String ATRIBUTO_USUARIO = "usuarioAutenticado";

    private SeguridadApiRest() {
    }

    public static UsuarioAutenticadoDTO requerirUsuario(
            HttpServletRequest request
    ) {
        HttpSession sesion = request.getSession(false);
        Object atributo = sesion == null
                ? null
                : sesion.getAttribute(ATRIBUTO_USUARIO);

        if (!(atributo instanceof UsuarioAutenticadoDTO usuario)) {
            invalidarSesion(sesion);
            throw new ApiNoAutenticadoException(
                    "Iniciá sesión para acceder a este recurso."
            );
        }

        if (usuario.isDebeCambiarContrasena()) {
            throw new ApiAccesoDenegadoException(
                    "Debés cambiar tu contraseña antes de continuar."
            );
        }

        return usuario;
    }

    public static UsuarioAutenticadoDTO requerirRol(
            HttpServletRequest request,
            String rol
    ) {
        UsuarioAutenticadoDTO usuario = requerirUsuario(request);

        if (!usuario.tieneRol(rol)) {
            throw new ApiAccesoDenegadoException(
                    "Tu cuenta no tiene permiso para realizar esta operación."
            );
        }

        return usuario;
    }

    public static String obtenerTokenCsrf(HttpServletRequest request) {
        requerirUsuario(request);
        return ProteccionCsrf.obtenerOCrear(request.getSession(false));
    }

    private static void invalidarSesion(HttpSession sesion) {
        if (sesion == null) {
            return;
        }

        try {
            sesion.invalidate();
        } catch (IllegalStateException ignorado) {
            // La sesión ya estaba invalidada.
        }
    }
}
