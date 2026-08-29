package py.fpune.soracare.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SuscripcionPushServletSeguridadTest {

    @Test
    void rechazaUnPostSinTokenCsrf() throws Exception {
        HttpSession sesion = crearSesionAutenticada();
        ResultadoHttp resultado = ejecutarPost(sesion, Map.of(
                "accion", "registrar",
                "token", "token-fcm-prueba"
        ));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, resultado.estado());
        assertFalse(resultado.cuerpo().contains("token-fcm-prueba"));
    }

    @Test
    void rechazaUnPostConTokenCsrfIncorrecto() throws Exception {
        HttpSession sesion = crearSesionAutenticada();
        ProteccionCsrf.obtenerOCrear(sesion);
        ResultadoHttp resultado = ejecutarPost(sesion, Map.of(
                "accion", "registrar",
                "token", "token-fcm-prueba",
                "tokenCsrf", "token-csrf-incorrecto"
        ));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, resultado.estado());
    }

    private static ResultadoHttp ejecutarPost(
            HttpSession sesion,
            Map<String, String> parametros
    ) throws Exception {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                SuscripcionPushServletSeguridadTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getSession" -> sesion;
                    case "getParameter" -> parametros.get((String) argumentos[0]);
                    case "getHeader" -> null;
                    default -> valorPredeterminado(metodo.getReturnType());
                }
        );

        AtomicInteger estado = new AtomicInteger(HttpServletResponse.SC_OK);
        StringWriter cuerpo = new StringWriter();
        PrintWriter escritor = new PrintWriter(cuerpo);
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                SuscripcionPushServletSeguridadTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "setStatus" -> {
                        estado.set((Integer) argumentos[0]);
                        yield null;
                    }
                    case "getWriter" -> escritor;
                    default -> valorPredeterminado(metodo.getReturnType());
                }
        );

        SuscripcionPushServlet servlet = new SuscripcionPushServlet();
        servlet.init();
        servlet.doPost(request, response);
        escritor.flush();
        return new ResultadoHttp(estado.get(), cuerpo.toString());
    }

    private static HttpSession crearSesionAutenticada() {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("usuarioAutenticado", new UsuarioAutenticadoDTO(
                1L, 1L, "paciente.prueba", false, Set.of("PACIENTE")
        ));
        return (HttpSession) Proxy.newProxyInstance(
                SuscripcionPushServletSeguridadTest.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getAttribute" -> atributos.get((String) argumentos[0]);
                    case "setAttribute" -> {
                        atributos.put((String) argumentos[0], argumentos[1]);
                        yield null;
                    }
                    case "removeAttribute" -> {
                        atributos.remove((String) argumentos[0]);
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

    private record ResultadoHttp(int estado, String cuerpo) {
    }
}
