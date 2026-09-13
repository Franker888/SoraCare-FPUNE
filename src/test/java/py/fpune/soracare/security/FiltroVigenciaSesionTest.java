package py.fpune.soracare.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import py.fpune.soracare.dao.SesionVigenteDAO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import java.sql.SQLException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FiltroVigenciaSesionTest {
    @Test
    void administradorRevocadoNoPuedeLlegarAlPostDeReactivacion() throws Exception {
        var request = solicitud("POST", "/admin/usuarios", "ADMINISTRADOR", false);
        request.setParameter("accion", "estado");
        request.setParameter("idUsuario", "7");
        request.setParameter("nuevoEstado", "ACTIVO");
        var sesion = (MockHttpSession) request.getSession();
        ProteccionCsrf.obtenerOCrear(sesion);
        var response = new MockHttpServletResponse();
        var destino = new MockFilterChain();
        filtro(false).doFilter(request, response, (req, res) ->
                new FiltroAutorizacionPorRol().doFilter(req, res, destino));
        assertTrue(sesion.isInvalid());
        assertNull(destino.getRequest());
        assertEquals("/SoraCare-FPUNE/login", response.getRedirectedUrl());
    }

    @Test
    void pacienteRevocadoNoEjecutaApiAunqueTengaCsrf() throws Exception {
        var request = solicitud("POST", "/api/v1/citas", "PACIENTE", false);
        request.setPathInfo("/citas");
        request.addHeader("X-CSRF-Token", ProteccionCsrf.obtenerOCrear(request.getSession()));
        var response = new MockHttpServletResponse();
        var destino = new MockFilterChain();
        filtro(false).doFilter(request, response, (req, res) ->
                new FiltroSeguridadApiRest().doFilter(req, res, destino));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("NO_AUTENTICADO"));
        assertNull(destino.getRequest());
    }

    @Test
    void permiteCuentaActivaYFlujoDeCambioObligatorio() throws Exception {
        for (String ruta : new String[]{"/admin/usuarios", "/medico/inicio", "/cambiar-contrasena"}) {
            var request = solicitud("GET", ruta, "MEDICO", true);
            var destino = new MockFilterChain();
            filtro(true).doFilter(request, new MockHttpServletResponse(), destino);
            assertNotNull(destino.getRequest());
            assertFalse(((MockHttpSession) request.getSession()).isInvalid());
        }
    }

    @Test
    void permiteApiActivaConRolYCsrfValidos() throws Exception {
        var request = solicitud("POST", "/api/v1/citas", "PACIENTE", false);
        request.setPathInfo("/citas");
        request.addHeader("X-CSRF-Token", ProteccionCsrf.obtenerOCrear(request.getSession()));
        var destino = new MockFilterChain();
        filtro(true).doFilter(request, new MockHttpServletResponse(), (req, res) ->
                new FiltroSeguridadApiRest().doFilter(req, res, destino));
        assertNotNull(destino.getRequest());
    }

    @Test
    void anonimoNoConsultaBaseNiCreaSesion() throws Exception {
        var request = new MockHttpServletRequest("GET", "/especialidades");
        var destino = new MockFilterChain();
        new FiltroVigenciaSesion(new SesionVigenteDAO() {
            @Override public boolean esVigente(long id, long version) { fail("Consulta innecesaria"); return false; }
        }).doFilter(request, new MockHttpServletResponse(), destino);
        assertNotNull(destino.getRequest());
        assertNull(request.getSession(false));
    }

    @Test
    void fallaCerradoSiNoPuedeConsultarBaseSinDestruirSesionValida() throws Exception {
        var request = solicitud("GET", "/api/v1/sesion", "PACIENTE", false);
        var response = new MockHttpServletResponse();
        var destino = new MockFilterChain();
        new FiltroVigenciaSesion(new SesionVigenteDAO() {
            @Override public boolean esVigente(long id, long version) throws SQLException {
                throw new SQLException("fallo simulado");
            }
        }).doFilter(request, response, destino);
        assertEquals(503, response.getStatus());
        assertNull(destino.getRequest());
        assertFalse(((MockHttpSession) request.getSession()).isInvalid());
    }

    @Test
    void rechazaIdentidadMalformada() throws Exception {
        var request = solicitud("GET", "/perfil", "PACIENTE", false);
        request.getSession().setAttribute("usuarioAutenticado", "invalido");
        var destino = new MockFilterChain();
        filtro(true).doFilter(request, new MockHttpServletResponse(), destino);
        assertNull(destino.getRequest());
    }

    private static FiltroVigenciaSesion filtro(boolean vigente) {
        return new FiltroVigenciaSesion(new SesionVigenteDAO() {
            @Override public boolean esVigente(long id, long version) {
                assertEquals(7, id);
                assertEquals(3, version);
                return vigente;
            }
        });
    }

    private static MockHttpServletRequest solicitud(String metodo, String ruta, String rol, boolean cambio) {
        var request = new MockHttpServletRequest(metodo, "/SoraCare-FPUNE" + ruta);
        request.setContextPath("/SoraCare-FPUNE");
        request.getSession().setAttribute("usuarioAutenticado",
                new UsuarioAutenticadoDTO(7, 8, "test", cambio, Set.of(rol), 3));
        return request;
    }
}
