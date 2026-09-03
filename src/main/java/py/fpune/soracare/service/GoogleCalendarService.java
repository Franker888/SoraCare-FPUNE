package py.fpune.soracare.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.GoogleCalendarDAO;
import py.fpune.soracare.dto.AutorizacionGoogleCalendarDTO;
import py.fpune.soracare.dto.CitaGoogleCalendarDTO;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.VinculoGoogleCalendarDTO;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class GoogleCalendarService {

    private static final URI ENDPOINT_EVENTOS = URI.create(
            "https://www.googleapis.com/calendar/v3/calendars/primary/events"
    );
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final ConfiguracionGoogleService configuracion;
    private final GoogleCalendarDAO dao;
    private final GoogleOAuthService oauth;
    private final CifradoGoogleTokenService cifrado;
    private final HttpClient httpClient;
    private final URI endpointEventos;

    public GoogleCalendarService() {
        this.configuracion = new ConfiguracionGoogleService();
        this.dao = new GoogleCalendarDAO();
        this.oauth = new GoogleOAuthService();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.endpointEventos = ENDPOINT_EVENTOS;

        byte[] clave = configuracion.obtenerClaveCifradoCalendar();
        try {
            this.cifrado = clave == null
                    ? null
                    : new CifradoGoogleTokenService(clave);
        } finally {
            if (clave != null) {
                Arrays.fill(clave, (byte) 0);
            }
        }
    }

    GoogleCalendarService(
            ConfiguracionGoogleService configuracion,
            GoogleCalendarDAO dao,
            GoogleOAuthService oauth,
            CifradoGoogleTokenService cifrado,
            HttpClient httpClient,
            URI endpointEventos
    ) {
        this.configuracion = configuracion;
        this.dao = dao;
        this.oauth = oauth;
        this.cifrado = cifrado;
        this.httpClient = httpClient;
        this.endpointEventos = endpointEventos;
    }

    public boolean estaDisponible() {
        return configuracion.calendarServidorConfigurado()
                && cifrado != null;
    }

    public boolean estaVinculado(long idUsuario) throws SQLException {
        if (!estaDisponible()) {
            return false;
        }
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return dao.buscarVinculo(conexion, idUsuario).isPresent();
        }
    }

    public Set<Long> listarCitasVinculadas(long idUsuario)
            throws SQLException {
        if (!estaDisponible()) {
            return Set.of();
        }
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return dao.listarCitasVinculadas(conexion, idUsuario);
        }
    }

    public void vincular(
            long idUsuario,
            PerfilGoogleDTO perfil,
            AutorizacionGoogleCalendarDTO autorizacion
    ) throws SQLException {
        exigirDisponible();
        if (perfil == null
                || perfil.correo() == null
                || perfil.correo().isBlank()
                || !perfil.correoVerificado()) {
            throw new SecurityException(
                    "Google no confirmó la cuenta que autorizó Calendar."
            );
        }
        if (autorizacion == null
                || autorizacion.refreshToken() == null
                || autorizacion.refreshToken().isBlank()) {
            throw new SecurityException(
                    "Google Calendar no entregó una autorización reutilizable."
            );
        }

        String tokenCifrado = cifrado.cifrar(
                autorizacion.refreshToken()
        );
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            dao.guardarVinculo(
                    conexion,
                    idUsuario,
                    perfil.correo(),
                    tokenCifrado,
                    autorizacion.alcance()
            );
        }
    }

    public void guardarCita(long idUsuario, long idCita)
            throws SQLException, IOException, InterruptedException {
        exigirDisponible();

        VinculoGoogleCalendarDTO vinculo;
        CitaGoogleCalendarDTO cita;
        Optional<String> idEvento;
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            vinculo = dao.buscarVinculo(conexion, idUsuario)
                    .orElseThrow(() -> new SecurityException(
                            "Vinculá Google Calendar antes de guardar la cita."
                    ));
            cita = dao.buscarCitaDelPaciente(conexion, idUsuario, idCita)
                    .orElseThrow(() -> new SecurityException(
                            "La cita no pertenece al paciente, no está confirmada o ya ocurrió."
                    ));
            idEvento = dao.buscarIdEvento(
                    conexion,
                    idUsuario,
                    idCita
            );
        }

        String accessToken = renovarAcceso(vinculo);
        String cuerpoEvento = construirEventoJson(cita);
        String idGuardado = null;

        if (idEvento.isPresent()) {
            HttpResponse<String> actualizada = enviarEvento(
                    "PUT",
                    endpointEvento(idEvento.get()),
                    accessToken,
                    cuerpoEvento
            );
            if (actualizada.statusCode() == 404) {
                try (Connection conexion = ConexionBD.obtenerConexion()) {
                    dao.eliminarEvento(conexion, idUsuario, idCita);
                }
            } else {
                exigirRespuestaCalendar(actualizada);
                idGuardado = idEvento.get();
            }
        }

        if (idGuardado == null) {
            HttpResponse<String> creada = enviarEvento(
                    "POST",
                    endpointEventos,
                    accessToken,
                    cuerpoEvento
            );
            exigirRespuestaCalendar(creada);
            idGuardado = obtenerIdEvento(creada.body());
        }

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            dao.guardarEvento(
                    conexion,
                    idUsuario,
                    idCita,
                    idGuardado
            );
        }
    }

    public void retirarCita(long idUsuario, long idCita)
            throws SQLException, IOException, InterruptedException {
        exigirDisponible();

        VinculoGoogleCalendarDTO vinculo;
        Optional<String> idEvento;
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            vinculo = dao.buscarVinculo(conexion, idUsuario)
                    .orElseThrow(() -> new SecurityException(
                            "Google Calendar no está vinculado."
                    ));
            idEvento = dao.buscarIdEvento(
                    conexion,
                    idUsuario,
                    idCita
            );
        }

        if (idEvento.isPresent()) {
            String accessToken = renovarAcceso(vinculo);
            HttpRequest solicitud = HttpRequest.newBuilder(
                            endpointEvento(idEvento.get())
                    )
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .DELETE()
                    .build();
            HttpResponse<String> respuesta = httpClient.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (respuesta.statusCode() != 404
                    && (respuesta.statusCode() < 200
                    || respuesta.statusCode() >= 300)) {
                exigirRespuestaCalendar(respuesta);
            }
        }

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            dao.eliminarEvento(conexion, idUsuario, idCita);
        }
    }

    static String construirEventoJson(CitaGoogleCalendarDTO cita) {
        JsonObject evento = new JsonObject();
        evento.addProperty(
                "summary",
                "Cita médica · " + cita.especialidad()
        );
        evento.addProperty(
                "description",
                "Profesional: " + cita.profesional()
                        + "\nConsulta programada desde SoraCare-FPUNE."
        );
        evento.addProperty("location", cita.ubicacion());

        JsonObject inicio = new JsonObject();
        inicio.addProperty("dateTime", cita.inicio().toString());
        inicio.addProperty("timeZone", "America/Asuncion");
        evento.add("start", inicio);

        JsonObject fin = new JsonObject();
        fin.addProperty("dateTime", cita.fin().toString());
        fin.addProperty("timeZone", "America/Asuncion");
        evento.add("end", fin);

        JsonObject recordatorios = new JsonObject();
        recordatorios.addProperty("useDefault", true);
        evento.add("reminders", recordatorios);

        JsonObject privada = new JsonObject();
        privada.addProperty(
                "soracareCitaId",
                Long.toString(cita.idCita())
        );
        JsonObject propiedades = new JsonObject();
        propiedades.add("private", privada);
        evento.add("extendedProperties", propiedades);
        return evento.toString();
    }

    private String renovarAcceso(VinculoGoogleCalendarDTO vinculo)
            throws IOException, InterruptedException {
        String refreshToken = cifrado.descifrar(
                vinculo.refreshTokenCifrado()
        );
        return oauth.renovarAccessToken(refreshToken);
    }

    private HttpResponse<String> enviarEvento(
            String metodo,
            URI endpoint,
            String accessToken,
            String cuerpo
    ) throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publicador =
                HttpRequest.BodyPublishers.ofString(
                        cuerpo,
                        StandardCharsets.UTF_8
                );
        HttpRequest solicitud = HttpRequest.newBuilder(endpoint)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .method(metodo, publicador)
                .build();
        return httpClient.send(
                solicitud,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private URI endpointEvento(String idEvento) {
        return URI.create(
                endpointEventos
                        + "/"
                        + URLEncoder.encode(
                                idEvento,
                                StandardCharsets.UTF_8
                        ).replace("+", "%20")
        );
    }

    private static void exigirRespuestaCalendar(
            HttpResponse<String> respuesta
    ) {
        int estado = respuesta.statusCode();
        if (estado >= 200 && estado < 300) {
            return;
        }
        if (estado == 401) {
            throw new SecurityException(
                    "La autorización de Google Calendar venció. Volvé a vincularla."
            );
        }
        if (estado == 403) {
            throw new SecurityException(
                    "Google Calendar no concedió el permiso requerido o la API no está habilitada."
            );
        }
        throw new IllegalStateException(
                "Google Calendar rechazó la operación."
        );
    }

    private static String obtenerIdEvento(String cuerpo) {
        try {
            JsonObject json = JsonParser.parseString(cuerpo)
                    .getAsJsonObject();
            String id = json.has("id")
                    ? json.get("id").getAsString()
                    : null;
            if (id == null || id.isBlank()) {
                throw new IllegalStateException();
            }
            return id.strip();
        } catch (RuntimeException error) {
            throw new IllegalStateException(
                    "Google Calendar no devolvió el identificador del evento."
            );
        }
    }

    private void exigirDisponible() {
        if (!estaDisponible()) {
            throw new IllegalStateException(
                    "Google Calendar todavía no tiene completa su configuración segura de servidor."
            );
        }
    }
}
