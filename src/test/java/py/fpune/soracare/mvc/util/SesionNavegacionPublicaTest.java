package py.fpune.soracare.mvc.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SesionNavegacionPublicaTest {

    @Test
    void muestraElLoginCuandoNoExisteSesion() {
        SesionNavegacionPublica navegacion =
                SesionNavegacionPublica.desde(null);

        assertFalse(navegacion.iniciada());
        assertEquals("/login", navegacion.rutaAcceso());
        assertEquals("/login", navegacion.rutaAgenda());
        assertEquals("Iniciar sesión", navegacion.textoAcceso());
    }

    @Test
    void mantieneAlPacienteDentroDeSuFlujoAutenticado() {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(
                "usuarioAutenticado",
                usuarioConRol("PACIENTE")
        );

        SesionNavegacionPublica navegacion =
                SesionNavegacionPublica.desde(sesion);

        assertTrue(navegacion.iniciada());
        assertTrue(navegacion.paciente());
        assertEquals("/dashboard", navegacion.rutaAcceso());
        assertEquals("/mis-citas?nueva=1", navegacion.rutaAgenda());
        assertEquals("Mi panel", navegacion.textoAcceso());
    }

    @Test
    void devuelveAlPersonalAutenticadoASuPanel() {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(
                "usuarioAutenticado",
                usuarioConRol("MEDICO")
        );

        SesionNavegacionPublica navegacion =
                SesionNavegacionPublica.desde(sesion);

        assertTrue(navegacion.iniciada());
        assertFalse(navegacion.paciente());
        assertEquals("/dashboard", navegacion.rutaAgenda());
        assertEquals("Ir a mi panel", navegacion.textoAgenda());
    }

    private static UsuarioAutenticadoDTO usuarioConRol(
            String rol
    ) {
        return new UsuarioAutenticadoDTO(
                10,
                20,
                "usuario.prueba",
                false,
                Set.of(rol)
        );
    }
}
