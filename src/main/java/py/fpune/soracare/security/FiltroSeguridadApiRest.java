package py.fpune.soracare.security;

import com.google.gson.Gson;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.api.dto.ErrorApiDTO;
import py.fpune.soracare.api.security.ApiAccesoDenegadoException;
import py.fpune.soracare.api.security.ApiNoAutenticadoException;
import py.fpune.soracare.api.security.SeguridadApiRest;

import java.io.IOException;
import java.util.Set;

public class FiltroSeguridadApiRest implements Filter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of(
            "/especialidades",
            "/medicos",
            "/disponibilidad"
    );
    private static final String RUTA_SESION = "/sesion";
    private static final String RUTA_CITAS = "/citas";
    private static final String RUTA_DISPONIBILIDAD_MEDICO =
            "/medicos/me/disponibilidad";
    private static final Gson JSON = new Gson();

    @Override
    public void doFilter(
            ServletRequest solicitud,
            ServletResponse respuesta,
            FilterChain cadena
    ) throws IOException, ServletException {
        if (!(solicitud instanceof HttpServletRequest request)
                || !(respuesta instanceof HttpServletResponse response)) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        configurarCabeceras(response);
        String ruta = rutaApi(request);
        String metodo = request.getMethod();

        if ("OPTIONS".equals(metodo)) {
            cadena.doFilter(request, response);
            return;
        }

        if (RUTAS_PUBLICAS.contains(ruta)) {
            if (esMetodoLectura(metodo)) {
                cadena.doFilter(request, response);
            } else {
                responderError(
                        response,
                        HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "METODO_NO_PERMITIDO",
                        "El recurso solicitado es únicamente de consulta."
                );
            }
            return;
        }

        if (RUTA_SESION.equals(ruta)) {
            if (!esMetodoLectura(metodo)) {
                responderError(
                        response,
                        HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "METODO_NO_PERMITIDO",
                        "La sesión solamente puede consultarse."
                );
                return;
            }
            autenticar(request, response, null, false, cadena);
            return;
        }

        if (esRutaODescendiente(ruta, RUTA_CITAS)) {
            autenticar(
                    request,
                    response,
                    "PACIENTE",
                    !esMetodoLectura(metodo),
                    cadena
            );
            return;
        }

        if (esRutaODescendiente(ruta, RUTA_DISPONIBILIDAD_MEDICO)) {
            autenticar(
                    request,
                    response,
                    "MEDICO",
                    !esMetodoLectura(metodo),
                    cadena
            );
            return;
        }

        responderError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "RECURSO_NO_ENCONTRADO",
                "El recurso solicitado no existe."
        );
    }

    private static void autenticar(
            HttpServletRequest request,
            HttpServletResponse response,
            String rol,
            boolean validarCsrf,
        FilterChain cadena
    ) throws IOException, ServletException {
        try {
            if (rol == null) {
                SeguridadApiRest.requerirUsuario(request);
            } else {
                SeguridadApiRest.requerirRol(request, rol);
            }

            if (validarCsrf && !tokenCsrfValido(request)) {
                responderError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "CSRF_INVALIDO",
                        "La confirmación de seguridad no es válida."
                );
                return;
            }
            cadena.doFilter(request, response);
        } catch (ApiNoAutenticadoException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "NO_AUTENTICADO",
                    error.getMessage()
            );
        } catch (ApiAccesoDenegadoException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "ACCESO_DENEGADO",
                    error.getMessage()
            );
        }
    }

    private static boolean tokenCsrfValido(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return ProteccionCsrf.esValido(
                sesion,
                request.getHeader(SeguridadApiRest.CABECERA_CSRF)
        );
    }

    private static boolean esMetodoLectura(String metodo) {
        return "GET".equals(metodo) || "HEAD".equals(metodo);
    }

    private static boolean esRutaODescendiente(
            String ruta,
            String raiz
    ) {
        return raiz.equals(ruta) || ruta.startsWith(raiz + "/");
    }

    private static String rutaApi(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank()) {
            return "/";
        }

        String ruta = pathInfo.strip();
        while (ruta.length() > 1 && ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        return ruta;
    }

    private static void configurarCabeceras(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private static void responderError(
            HttpServletResponse response,
            int estado,
            String codigo,
            String mensaje
    ) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(
                JSON.toJson(new ErrorApiDTO(estado, codigo, mensaje))
        );
    }
}
