package py.fpune.soracare.mvc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultaSifenCsrfTest {
    @Test void noConsultaSifenSinTokenCsrf() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("accion", "consultarSifenPrueba");
        request.setParameter("idSolicitud", "1");
        var response = new MockHttpServletResponse();
        new FacturaInternaMedicoServlet().doPost(request, response);
        assertEquals(403, response.getStatus());
    }
}
