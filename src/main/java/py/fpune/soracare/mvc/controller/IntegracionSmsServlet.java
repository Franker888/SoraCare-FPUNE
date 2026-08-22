package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.SmsTwilioService;

import java.io.IOException;

@WebServlet("/admin/integraciones/sms")
public class IntegracionSmsServlet extends HttpServlet {

    private SmsTwilioService smsService;

    @Override
    public void init() {
        smsService = new SmsTwilioService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        configurarVista(request);
        request.getRequestDispatcher(
                "/WEB-INF/views/admin/integracion-sms.jsp"
        ).forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String destinatario = request.getParameter("destinatario");
        String mensaje = request.getParameter("mensaje");
        request.setAttribute("destinatario", destinatario);
        request.setAttribute("mensaje", mensaje);

        try {
            SmsTwilioService.ResultadoEnvio resultado = smsService.enviar(
                    destinatario, mensaje);
            request.setAttribute(
                    "mensajeExito",
                    "Twilio aceptó el SMS. Referencia: "
                            + resultado.referenciaProveedor()
                            + " · Estado inicial: " + resultado.estado()
            );
        } catch (IllegalArgumentException | IllegalStateException error) {
            request.setAttribute("error", error.getMessage());
        } catch (IOException error) {
            log("No fue posible enviar el SMS de prueba con Twilio.", error);
            request.setAttribute(
                    "error",
                    "Twilio no pudo completar el envío. Revisá las credenciales, "
                            + "el número remitente y que el destinatario esté verificado."
            );
        }

        doGet(request, response);
    }

    private void configurarVista(HttpServletRequest request) {
        request.setAttribute("twilioConfigurado", smsService.estaConfigurado());
        request.setAttribute(
                "remitenteTwilio", smsService.remitenteConfigurado());
    }
}
