package py.fpune.soracare.mvc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class ConexionServletTest {
    @Test
    void saludCorrectaNoExponeBaseNiUsuario() throws Exception {
        var servlet = new ConexionServlet() {
            @Override protected boolean baseDisponible() { return true; }
        };
        var respuesta = new MockHttpServletResponse();
        servlet.doGet(new MockHttpServletRequest(), respuesta);
        assertEquals(200, respuesta.getStatus());
        assertEquals("OK", respuesta.getContentAsString());
        assertEquals("no-store", respuesta.getHeader("Cache-Control"));
        assertEquals("nosniff", respuesta.getHeader("X-Content-Type-Options"));
    }

    @Test
    void errorSqlNoSeReflejaEnRespuesta() throws Exception {
        var servlet = new ConexionServlet() {
            @Override protected boolean baseDisponible() throws SQLException {
                throw new SQLException("usuario-secreto base-interna <script>detalle</script>");
            }
        };
        var respuesta = new MockHttpServletResponse();
        servlet.doGet(new MockHttpServletRequest(), respuesta);
        assertEquals(503, respuesta.getStatus());
        assertEquals("NO_DISPONIBLE", respuesta.getContentAsString());
    }

    @Test
    void resultadoInesperadoNoDeclaraSaludCorrecta() throws Exception {
        var servlet = new ConexionServlet() {
            @Override protected boolean baseDisponible() { return false; }
        };
        var respuesta = new MockHttpServletResponse();
        servlet.doGet(new MockHttpServletRequest(), respuesta);
        assertEquals(503, respuesta.getStatus());
    }
}
