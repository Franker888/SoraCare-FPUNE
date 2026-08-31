package py.fpune.soracare.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import py.fpune.soracare.service.SoraContextoPublicoService;
import py.fpune.soracare.service.SoraOpenAIService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoraAsistenteServletTest {

    @Test
    void rechazaContenidoQueNoSeaJsonSinConsumirOpenAI() throws Exception {
        SoraFalsa servicio = new SoraFalsa();
        ResultadoHttp resultado = ejecutarPost(
                crearServlet(servicio),
                crearSesion("sesion-prueba"),
                "text/plain",
                "{\"mensaje\":\"Hola\"}",
                -1
        );

        assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                resultado.estado());
        assertEquals(0, servicio.llamadas.get());
    }

    @Test
    void rechazaUnCuerpoDemasiadoGrandeSinConsumirOpenAI() throws Exception {
        SoraFalsa servicio = new SoraFalsa();
        ResultadoHttp resultado = ejecutarPost(
                crearServlet(servicio),
                crearSesion("sesion-prueba"),
                "application/json",
                "{}",
                9_000
        );

        assertEquals(413, resultado.estado());
        assertEquals(0, servicio.llamadas.get());
    }

    @Test
    void enviaAOpenAIUnIdentificadorAnonimoDeLaSesion() throws Exception {
        SoraFalsa servicio = new SoraFalsa();
        ResultadoHttp resultado = ejecutarPost(
                crearServlet(servicio),
                crearSesion("sesion-secreta-prueba"),
                "application/json;charset=UTF-8",
                "{\"mensaje\":\"¿Cómo reprogramo mi cita?\",\"historial\":[]}",
                -1
        );

        assertEquals(HttpServletResponse.SC_OK, resultado.estado());
        assertTrue(resultado.cuerpo().contains("respuesta de prueba"));
        assertEquals(64, servicio.identificador.get().length());
        assertFalse(servicio.identificador.get().contains("sesion-secreta"));
        assertTrue(servicio.contexto.get().contains("Contexto de prueba"));
    }

    @Test
    void limitaLaNovenaConsultaYComunicaCuandoReintentar() throws Exception {
        SoraFalsa servicio = new SoraFalsa();
        SoraAsistenteServlet servlet = crearServlet(servicio);
        HttpSession sesion = crearSesion("sesion-limite");
        ResultadoHttp resultado = null;

        for (int indice = 0; indice < 9; indice++) {
            resultado = ejecutarPost(
                    servlet,
                    sesion,
                    "application/json",
                    "{\"mensaje\":\"Consulta válida\"}",
                    -1
            );
        }

        assertEquals(429, resultado.estado());
        assertEquals("60", resultado.cabeceras().get("Retry-After"));
        assertEquals(8, servicio.llamadas.get());
    }

    private static SoraAsistenteServlet crearServlet(SoraFalsa servicio) {
        return new SoraAsistenteServlet(servicio, new ContextoFalso());
    }

    private static ResultadoHttp ejecutarPost(
            SoraAsistenteServlet servlet,
            HttpSession sesion,
            String tipoContenido,
            String cuerpoSolicitud,
            long longitudDeclarada
    ) throws Exception {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                SoraAsistenteServletTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getSession" -> sesion;
                    case "getContentType" -> tipoContenido;
                    case "getContentLengthLong" -> longitudDeclarada;
                    case "getReader" -> new BufferedReader(
                            new StringReader(cuerpoSolicitud)
                    );
                    default -> valorPredeterminado(metodo.getReturnType());
                }
        );

        AtomicInteger estado = new AtomicInteger(HttpServletResponse.SC_OK);
        Map<String, String> cabeceras = new HashMap<>();
        StringWriter cuerpo = new StringWriter();
        PrintWriter escritor = new PrintWriter(cuerpo);
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                SoraAsistenteServletTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "setStatus" -> {
                        estado.set((Integer) argumentos[0]);
                        yield null;
                    }
                    case "setHeader" -> {
                        cabeceras.put(
                                (String) argumentos[0],
                                (String) argumentos[1]
                        );
                        yield null;
                    }
                    case "getWriter" -> escritor;
                    default -> valorPredeterminado(metodo.getReturnType());
                }
        );

        servlet.doPost(request, response);
        escritor.flush();
        return new ResultadoHttp(estado.get(), cuerpo.toString(), cabeceras);
    }

    private static HttpSession crearSesion(String id) {
        Map<String, Object> atributos = new HashMap<>();
        return (HttpSession) Proxy.newProxyInstance(
                SoraAsistenteServletTest.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getId" -> id;
                    case "getAttribute" -> atributos.get((String) argumentos[0]);
                    case "setAttribute" -> {
                        atributos.put((String) argumentos[0], argumentos[1]);
                        yield null;
                    }
                    default -> valorPredeterminado(metodo.getReturnType());
                }
        );
    }

    private static Object valorPredeterminado(Class<?> tipo) {
        if (!tipo.isPrimitive()) return null;
        if (tipo == boolean.class) return false;
        if (tipo == byte.class) return (byte) 0;
        if (tipo == short.class) return (short) 0;
        if (tipo == int.class) return 0;
        if (tipo == long.class) return 0L;
        if (tipo == float.class) return 0F;
        if (tipo == double.class) return 0D;
        if (tipo == char.class) return '\0';
        return null;
    }

    private static final class SoraFalsa extends SoraOpenAIService {
        private final AtomicInteger llamadas = new AtomicInteger();
        private final AtomicReference<String> identificador =
                new AtomicReference<>();
        private final AtomicReference<String> contexto =
                new AtomicReference<>();

        @Override
        public String responder(
                String mensaje,
                List<MensajeConversacion> historial,
                String identificadorSeguridad,
                String contextoAplicacion
        ) throws IOException {
            llamadas.incrementAndGet();
            identificador.set(identificadorSeguridad);
            contexto.set(contextoAplicacion);
            return "respuesta de prueba";
        }
    }

    private static final class ContextoFalso
            extends SoraContextoPublicoService {

        @Override
        public ContextoPublico consultar() {
            return new ContextoPublico("Contexto de prueba");
        }
    }

    private record ResultadoHttp(
            int estado,
            String cuerpo,
            Map<String, String> cabeceras
    ) {
    }
}
