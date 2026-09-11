package py.fpune.soracare.mvc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.ComprobantePdfService;
import py.fpune.soracare.service.KudeService;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class KudePdfServletTest {
    @Test void descargaPdfAutorizadoSinCache() throws Exception {
        var service = new KudeService() {
            @Override public ComprobantePdfService.DocumentoPdf obtener(long id, UsuarioAutenticadoDTO usuario) {
                assertEquals(5, id); assertEquals(20, usuario.getIdPersona());
                return new ComprobantePdfService.DocumentoPdf("kude-prueba.pdf", new byte[]{1, 2}, "hash");
            }
        };
        var request = solicitud(); request.setParameter("id", "5");
        var response = new MockHttpServletResponse();
        new KudePdfServlet(service).doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/pdf", response.getContentType());
        assertEquals("private, no-store, max-age=0", response.getHeader("Cache-Control"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("inline; filename=\"kude-prueba.pdf\"", response.getHeader("Content-Disposition"));
        assertArrayEquals(new byte[]{1, 2}, response.getContentAsByteArray());
    }

    @Test void rechazaSinSesionEIdentificadorInvalido() throws Exception {
        var servlet = new KudePdfServlet(new KudeService());
        var response = new MockHttpServletResponse();
        servlet.doGet(new MockHttpServletRequest(), response);
        assertEquals(401, response.getStatus());
        for (String id : new String[]{"abc", "0", "-1", "999999999999999999999"}) {
            var request = solicitud(); request.setParameter("id", id);
            response = new MockHttpServletResponse();
            servlet.doGet(request, response);
            assertEquals(403, response.getStatus());
            assertEquals(0, response.getContentAsByteArray().length);
        }
    }

    private static MockHttpServletRequest solicitud() {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute("usuarioAutenticado", new UsuarioAutenticadoDTO(10, 20, "medico", false, Set.of("MEDICO")));
        return request;
    }
}
