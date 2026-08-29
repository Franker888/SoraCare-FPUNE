package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.SentryMonitoreoService;

import java.io.IOException;

@WebServlet("/admin/integraciones/sentry")
public class IntegracionSentryServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        configurarVista(request);
        request.getRequestDispatcher(
                "/WEB-INF/views/admin/integracion-sentry.jsp"
        ).forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        try {
            String identificador =
                    SentryMonitoreoService.enviarPruebaAdministrativa();
            request.setAttribute(
                    "mensajeExito",
                    "Sentry recibió el evento de prueba. Identificador: "
                            + identificador
            );
        } catch (IllegalStateException error) {
            request.setAttribute("error", error.getMessage());
        } catch (RuntimeException error) {
            log("No fue posible enviar el evento de prueba a Sentry.", error);
            request.setAttribute(
                    "error",
                    "Sentry no pudo recibir el evento. Revisá el DSN y "
                            + "la conectividad del servidor."
            );
        }
        doGet(request, response);
    }

    private static void configurarVista(HttpServletRequest request) {
        request.setAttribute(
                "sentryConfigurado",
                SentryMonitoreoService.estaConfigurado()
        );
        request.setAttribute(
                "sentryActivo",
                SentryMonitoreoService.estaActivo()
        );
        request.setAttribute(
                "sentryAmbiente",
                SentryMonitoreoService.ambienteConfigurado()
        );
    }
}
