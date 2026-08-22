package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.CorreoResendService;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@WebServlet("/admin/integraciones/correo")
public class IntegracionCorreoServlet extends HttpServlet {

    private CorreoResendService correoService;

    @Override
    public void init() {
        correoService = new CorreoResendService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        configurarVista(request);
        request.getRequestDispatcher(
                "/WEB-INF/views/admin/integracion-correo.jsp"
        ).forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String destinatario = request.getParameter("destinatario");
        request.setAttribute("destinatario", destinatario);

        try {
            String fecha = OffsetDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")
            );
            CorreoResendService.ResultadoEnvio resultado = correoService.enviar(
                    destinatario,
                    "Prueba de correo de SoraCare-FPUNE",
                    contenidoPrueba(fecha),
                    List.of(),
                    "prueba-admin-" + UUID.randomUUID()
            );
            request.setAttribute(
                    "mensajeExito",
                    "Resend acepto el correo. Referencia: "
                            + resultado.referenciaProveedor()
            );
        } catch (IllegalArgumentException | IllegalStateException error) {
            request.setAttribute("error", error.getMessage());
        } catch (IOException error) {
            log("No fue posible enviar el correo de prueba con Resend.", error);
            request.setAttribute(
                    "error",
                    "Resend no pudo completar el envio. Revisa la clave, "
                            + "el remitente y la autorizacion del destinatario."
            );
        }

        doGet(request, response);
    }

    private void configurarVista(HttpServletRequest request) {
        request.setAttribute("resendConfigurado", correoService.estaConfigurado());
        request.setAttribute("remitenteResend", correoService.remitenteConfigurado());
        request.setAttribute(
                "remitentePruebaResend",
                correoService.usaRemitenteDePrueba()
        );
    }

    private static String contenidoPrueba(String fecha) {
        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#10213a">
                  <div style="max-width:620px;margin:32px auto;background:#fff;border:1px solid #dbe5f1;border-radius:18px;overflow:hidden">
                    <div style="padding:28px 32px;background:linear-gradient(135deg,#2452c7,#159fc0);color:#fff">
                      <p style="margin:0 0 8px;font-size:13px;font-weight:700;letter-spacing:.08em">SORACARE-FPUNE</p>
                      <h1 style="margin:0;font-size:28px">Correo conectado correctamente</h1>
                    </div>
                    <div style="padding:30px 32px">
                      <p>Esta es una prueba administrativa de la integracion con Resend.</p>
                      <p>SoraCare utiliza esta conexión para enviar comprobantes e indicaciones médicas en PDF.</p>
                      <div style="margin-top:24px;padding:16px;background:#eef5ff;border-radius:12px">
                        <strong>Fecha de la prueba:</strong> %s
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fecha);
    }
}
