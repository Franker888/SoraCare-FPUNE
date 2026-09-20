package py.fpune.soracare.mvc.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlPublicaRequestTest {

    @Test
    void priorizaElOrigenConfiguradoSobreEncabezadosDelCliente() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/SoraCare-FPUNE");
        request.setServerName("host-no-confiable.example");
        request.addHeader("X-Forwarded-Proto", "http");

        assertEquals(
                "https://soracare.example/SoraCare-FPUNE/auth/google",
                UrlPublicaRequest.construir(
                        request,
                        "/auth/google",
                        () -> URI.create("https://soracare.example")
                )
        );
    }

    @Test
    void conservaElComportamientoLocalSinConfiguracion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/SoraCare-FPUNE");
        request.setServerName("localhost");
        request.setServerPort(8081);

        assertEquals(
                "http://localhost:8081/SoraCare-FPUNE/auth/google",
                UrlPublicaRequest.construir(request, "/auth/google", () -> null)
        );
    }
}
