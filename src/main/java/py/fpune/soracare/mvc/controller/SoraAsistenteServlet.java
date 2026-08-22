package py.fpune.soracare.mvc.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.service.SoraOpenAIService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Puerta de enlace pública entre el asistente visual Sora y OpenAI.
 * La clave permanece exclusivamente en el servidor.
 */
@WebServlet("/api/sora/chat")
public class SoraAsistenteServlet extends HttpServlet {

    private static final String ATRIBUTO_LIMITE =
            "soraLimiteConsultas";
    private static final long VENTANA_MILISEGUNDOS = 60_000L;
    private static final int MAXIMO_CONSULTAS_POR_VENTANA = 8;
    private static final int ESTADO_DEMASIADAS_SOLICITUDES = 429;

    private final Gson gson = new Gson();
    private SoraOpenAIService soraService;

    @Override
    public void init() {
        soraService = new SoraOpenAIService();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        configurarRespuesta(response);

        if (!admitirConsulta(request.getSession(true))) {
            responderError(
                    response,
                    ESTADO_DEMASIADAS_SOLICITUDES,
                    "Esperá un momento antes de volver a consultar a Sora."
            );
            return;
        }

        try {
            SolicitudChat solicitud = gson.fromJson(
                    request.getReader(),
                    SolicitudChat.class
            );
            if (solicitud == null) {
                throw new IllegalArgumentException(
                        "Escribí una consulta para Sora."
                );
            }
            String respuestaSora = soraService.responder(
                    solicitud.mensaje,
                    solicitud.historial
            );
            escribirJson(
                    response,
                    HttpServletResponse.SC_OK,
                    new RespuestaChat(respuestaSora, "ia")
            );
        } catch (JsonSyntaxException | IllegalArgumentException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    error.getMessage()
            );
        } catch (IllegalStateException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Sora está funcionando en modo de orientación local."
            );
        } catch (IOException error) {
            log("No fue posible obtener una respuesta de OpenAI.", error);
            responderError(
                    response,
                    HttpServletResponse.SC_BAD_GATEWAY,
                    "Sora no pudo conectarse con la IA en este momento."
            );
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        configurarRespuesta(response);
        responderError(
                response,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Este recurso sólo admite consultas enviadas desde Sora."
        );
    }

    private static boolean admitirConsulta(HttpSession sesion) {
        synchronized (sesion) {
            long ahora = System.currentTimeMillis();
            LimiteConsultas limite = (LimiteConsultas) sesion.getAttribute(
                    ATRIBUTO_LIMITE
            );
            if (limite == null
                    || ahora - limite.inicioVentana >= VENTANA_MILISEGUNDOS) {
                sesion.setAttribute(
                        ATRIBUTO_LIMITE,
                        new LimiteConsultas(ahora, 1)
                );
                return true;
            }
            if (limite.cantidad >= MAXIMO_CONSULTAS_POR_VENTANA) {
                return false;
            }
            sesion.setAttribute(
                    ATRIBUTO_LIMITE,
                    new LimiteConsultas(limite.inicioVentana, limite.cantidad + 1)
            );
            return true;
        }
    }

    private void responderError(
            HttpServletResponse response,
            int estado,
            String mensaje
    ) throws IOException {
        escribirJson(response, estado, new RespuestaError(mensaje));
    }

    private void escribirJson(
            HttpServletResponse response,
            int estado,
            Object contenido
    ) throws IOException {
        response.setStatus(estado);
        response.getWriter().write(gson.toJson(contenido));
    }

    private static void configurarRespuesta(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private static final class SolicitudChat {
        private String mensaje;
        private List<SoraOpenAIService.MensajeConversacion> historial;
    }

    private record RespuestaChat(String respuesta, String modo) {
    }

    private record RespuestaError(String error) {
    }

    private record LimiteConsultas(long inicioVentana, int cantidad) {
    }
}
