package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.RegistroCuentaPacienteDTO;
import py.fpune.soracare.mvc.util.UrlPublicaRequest;
import py.fpune.soracare.service.RegistroCuentaPacienteService;
import py.fpune.soracare.service.ConfiguracionGoogleService;
import py.fpune.soracare.service.TurnstileService;
import py.fpune.soracare.service.VerificacionContrasenaException;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/registro")
public class RegistroCuentaPacienteServlet extends HttpServlet {

    private RegistroCuentaPacienteService registroService;
    private ConfiguracionGoogleService configuracionGoogleService;
    private TurnstileService turnstileService;

    @Override
    public void init() {
        registroService = new RegistroCuentaPacienteService();
        configuracionGoogleService = new ConfiguracionGoogleService();
        turnstileService = new TurnstileService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (estaAutenticado(request)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader(
                "Referrer-Policy",
                "no-referrer-when-downgrade"
        );
        prepararIntegracionesPublicas(request);
        request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Los errores POST vuelven a mostrar el botón de Google de la misma vista.
        response.setHeader("Referrer-Policy", "no-referrer-when-downgrade");
        request.setCharacterEncoding("UTF-8");
        if (estaAutenticado(request)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            turnstileService.validar(
                    request.getParameter("cf-turnstile-response"),
                    "registro"
            );

            RegistroCuentaPacienteDTO datos = new RegistroCuentaPacienteDTO(
                    request.getParameter("nombres"), request.getParameter("apellidos"),
                    request.getParameter("cedula"), request.getParameter("fechaNacimiento"),
                    request.getParameter("telefono"), request.getParameter("correo"),
                    request.getParameter("direccion"), request.getParameter("grupoSanguineo"),
                    request.getParameter("contactoEmergencia"), request.getParameter("telefonoEmergencia"),
                    request.getParameter("nombreUsuario"), request.getParameter("contrasena"),
                    request.getParameter("confirmacionContrasena"));
            registroService.registrar(datos);
            response.sendRedirect(request.getContextPath() + "/login?registro=exitoso");
        } catch (VerificacionContrasenaException error) {
            log("No fue posible consultar Pwned Passwords durante el registro.", error);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("error", error.getMessage());
            prepararIntegracionesPublicas(request);
            request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                    .forward(request, response);
        } catch (IllegalArgumentException | SecurityException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            prepararIntegracionesPublicas(request);
            request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                    .forward(request, response);
        } catch (IOException error) {
            log("No fue posible validar Turnstile durante el registro.", error);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute(
                    "error",
                    "No pudimos completar la verificación de seguridad. "
                            + "Intentá nuevamente en unos instantes."
            );
            prepararIntegracionesPublicas(request);
            request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                    .forward(request, response);
        } catch (SQLException error) {
            log("Error al crear una cuenta de paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible crear la cuenta. Revisá los datos e intentá nuevamente.");
            prepararIntegracionesPublicas(request);
            request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                    .forward(request, response);
        }
    }

    private static boolean estaAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return sesion != null && sesion.getAttribute("usuarioAutenticado") != null;
    }

    private void prepararAccesoGoogle(HttpServletRequest request) {
        String clientId = configuracionGoogleService.obtenerClientId();
        if (clientId == null) {
            return;
        }
        request.setAttribute("googleClientId", clientId);
        request.setAttribute(
                "googleLoginUri",
                UrlPublicaRequest.construir(request, "/auth/google")
        );
    }

    private void prepararIntegracionesPublicas(HttpServletRequest request) {
        prepararAccesoGoogle(request);
        String siteKey = turnstileService.obtenerSiteKey();
        if (siteKey != null) {
            request.setAttribute("turnstileSiteKey", siteKey);
        }
    }
}
