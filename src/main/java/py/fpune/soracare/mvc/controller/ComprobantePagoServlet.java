package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.ComprobantePagoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.PacienteService;
import py.fpune.soracare.service.PagoService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/comprobantes/pago")
public class ComprobantePagoServlet extends HttpServlet {

    private PagoService pagoService;
    private PacienteService pacienteService;

    @Override
    public void init() {
        pagoService = new PagoService();
        pacienteService = new PacienteService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setHeader("Cache-Control", "private, no-store, max-age=0");
        try {
            UsuarioAutenticadoDTO usuario = usuario(request);
            boolean personal = usuario.tieneRol("ADMINISTRADOR")
                    || usuario.tieneRol("RECEPCIONISTA");
            Long idPaciente = null;
            if (!personal && usuario.tieneRol("PACIENTE")) {
                Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Tu cuenta no posee una ficha de paciente asociada."));
                idPaciente = paciente.getIdPaciente();
            }
            ComprobantePagoDTO comprobante = pagoService.obtenerComprobante(
                    convertirLong(request.getParameter("id")), idPaciente, personal);
            response.setContentType("application/pdf");
            response.setContentLength(comprobante.contenidoPdf().length);
            response.setHeader("Content-Disposition", "inline; filename=\""
                    + comprobante.nombreArchivo().replace("\"", "") + "\"");
            response.getOutputStream().write(comprobante.contenidoPdf());
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, error.getMessage());
        } catch (SQLException error) {
            log("Error al descargar el comprobante.", error);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No fue posible descargar el comprobante.");
        }
    }

    private static UsuarioAutenticadoDTO usuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return (UsuarioAutenticadoDTO) sesion.getAttribute("usuarioAutenticado");
    }

    private static long convertirLong(String valor) {
        try {
            long numero = Long.parseLong(valor == null ? "" : valor.strip());
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El comprobante seleccionado no es valido.", error);
        }
    }
}
