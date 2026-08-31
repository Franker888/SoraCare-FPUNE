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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    private static final int ESTADO_CUERPO_DEMASIADO_GRANDE = 413;
    private static final int MAXIMO_CUERPO_CARACTERES = 8_192;

    private final Gson gson = new Gson();
    private final SoraOpenAIService soraService;

    public SoraAsistenteServlet() {
        this(new SoraOpenAIService());
    }

    SoraAsistenteServlet(SoraOpenAIService soraService) {
        this.soraService = Objects.requireNonNull(soraService);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        configurarRespuesta(response);

        if (!esContenidoJson(request.getContentType())) {
            responderError(
                    response,
                    HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "La consulta debe enviarse en formato JSON."
            );
            return;
        }

        if (request.getContentLengthLong() > MAXIMO_CUERPO_CARACTERES) {
            responderError(
                    response,
                    ESTADO_CUERPO_DEMASIADO_GRANDE,
                    "La consulta enviada es demasiado grande."
            );
            return;
        }

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        SolicitudChat solicitud;
        try {
            solicitud = gson.fromJson(
                    leerCuerpoLimitado(request),
                    SolicitudChat.class
            );
            if (solicitud == null) {
                throw new IllegalArgumentException(
                        "Escribí una consulta para Sora."
                );
            }
            solicitud.mensaje = SoraOpenAIService.validarMensaje(
                    solicitud.mensaje
            );
        } catch (CuerpoDemasiadoGrandeException error) {
            responderError(
                    response,
                    ESTADO_CUERPO_DEMASIADO_GRANDE,
                    "La consulta enviada es demasiado grande."
            );
            return;
        } catch (JsonSyntaxException | IllegalArgumentException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    error.getMessage()
            );
            return;
        }

        HttpSession sesion = request.getSession(true);
        if (!admitirConsulta(sesion)) {
            response.setHeader("Retry-After", "60");
            responderError(
                    response,
                    ESTADO_DEMASIADAS_SOLICITUDES,
                    "Esperá un momento antes de volver a consultar a Sora."
            );
            return;
        }

        try {
            String respuestaSora = soraService.responder(
                    solicitud.mensaje,
                    solicitud.historial,
                    crearIdentificadorSeguridad(sesion)
            );
            escribirJson(
                    response,
                    HttpServletResponse.SC_OK,
                    new RespuestaChat(respuestaSora, "ia")
            );
        } catch (IllegalArgumentException error) {
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

    private static boolean esContenidoJson(String tipoContenido) {
        if (tipoContenido == null) return false;
        String tipo = tipoContenido.toLowerCase(Locale.ROOT);
        return tipo.equals("application/json")
                || tipo.startsWith("application/json;");
    }

    private static String leerCuerpoLimitado(HttpServletRequest request)
            throws IOException {
        StringBuilder cuerpo = new StringBuilder();
        char[] bloque = new char[1_024];
        var lector = request.getReader();
        int leidos;
        while ((leidos = lector.read(bloque)) != -1) {
            if (cuerpo.length() + leidos > MAXIMO_CUERPO_CARACTERES) {
                throw new CuerpoDemasiadoGrandeException();
            }
            cuerpo.append(bloque, 0, leidos);
        }
        return cuerpo.toString();
    }

    static String crearIdentificadorSeguridad(HttpSession sesion) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(sesion.getId().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(
                    "SHA-256 no está disponible en esta ejecución.",
                    error
            );
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

    private static final class CuerpoDemasiadoGrandeException
            extends IOException {
    }
}
