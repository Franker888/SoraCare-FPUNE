package py.fpune.soracare.mvc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AutenticacionGoogleServletTest {

    @Test
    void conservaLaCuentaGoogleComoPistaParaCalendar() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UsuarioAutenticadoDTO usuario = new UsuarioAutenticadoDTO(
                10,
                20,
                "paciente.google",
                false,
                Set.of("PACIENTE")
        );
        PerfilGoogleDTO perfil = new PerfilGoogleDTO(
                "sub-google",
                "paciente@gmail.com",
                true,
                "Paciente",
                "Google",
                "Paciente Google",
                null,
                null
        );

        AutenticacionGoogleServlet.iniciarSesion(request, usuario, perfil);

        assertSame(
                usuario,
                request.getSession(false).getAttribute("usuarioAutenticado")
        );
        assertEquals(
                "paciente@gmail.com",
                request.getSession(false).getAttribute(
                        AutenticacionGoogleServlet.ATRIBUTO_GOOGLE_LOGIN_HINT
                )
        );
    }
}
