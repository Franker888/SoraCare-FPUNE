package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.PacienteService;
import py.fpune.soracare.service.PagoService;
import py.fpune.soracare.service.TarjetaPruebaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/pagos/procesar")
public class ProcesarPagoServlet extends HttpServlet {

    private PagoService pagoService;
    private PacienteService pacienteService;
    private TarjetaPruebaService tarjetaPruebaService;

    @Override
    public void init() {
        pagoService = new PagoService();
        pacienteService = new PacienteService();
        tarjetaPruebaService = new TarjetaPruebaService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        try {
            PagoResumenDTO pago = pagoDelPaciente(request);
            request.setAttribute("pago", pago);
            if ("confirmado".equals(request.getParameter("resultado"))) {
                request.setAttribute("mensajeExito",
                        "Pago simulado confirmado. Tu cita y el comprobante ya están disponibles; "
                                + "el envío al correo se está procesando.");
            }
            request.getRequestDispatcher("/WEB-INF/views/pagos/procesar.jsp")
                    .forward(request, response);
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, error.getMessage());
        } catch (SQLException error) {
            log("Error al cargar el pago del paciente.", error);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No fue posible cargar el pago.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        long idPago = convertirLong(request.getParameter("idPago"));
        try {
            UsuarioAutenticadoDTO usuario = usuario(request);
            Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tu cuenta no posee una ficha de paciente asociada."));
            PagoResumenDTO actual = pagoService.buscarParaPaciente(idPago, paciente.getIdPaciente());
            String detalle = detalleSimulacion(request, actual.codigoMetodo());
            pagoService.confirmarDigital(idPago, paciente.getIdPaciente(),
                    usuario.getIdUsuario(), detalle);
            response.sendRedirect(request.getContextPath() + "/pagos/procesar?idPago="
                    + idPago + "&resultado=confirmado");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            doGet(request, response);
        } catch (SQLException error) {
            log("Error al procesar el pago simulado.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible confirmar el pago. Intenta nuevamente.");
            doGet(request, response);
        }
    }

    private PagoResumenDTO pagoDelPaciente(HttpServletRequest request) throws SQLException {
        UsuarioAutenticadoDTO usuario = usuario(request);
        Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tu cuenta no posee una ficha de paciente asociada."));
        return pagoService.buscarParaPaciente(
                convertirLong(request.getParameter("idPago")), paciente.getIdPaciente());
    }

    private String detalleSimulacion(HttpServletRequest request, String metodo) {
        return switch (metodo) {
            case "QR" -> normalizar(request.getParameter("codigoConfirmacion"),
                    "Ingresa el codigo de confirmacion mostrado en la simulacion QR.");
            case "TRANSFERENCIA" -> normalizar(request.getParameter("referenciaTransferencia"),
                    "Ingresa una referencia de transferencia de prueba.");
            case "TARJETA" -> {
                TarjetaPruebaService.Resultado resultado = tarjetaPruebaService.validar(
                        request.getParameter("numeroTarjeta"),
                        request.getParameter("vencimientoTarjeta"),
                        request.getParameter("nombreTitularTarjeta"),
                        request.getParameter("codigoSeguridadTarjeta"));
                yield "TARJETA-" + resultado.marca() + "-" + resultado.ultimos4();
            }
            default -> throw new IllegalArgumentException(
                    "El pago en efectivo debe ser confirmado por recepcion.");
        };
    }

    private static String normalizar(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
        String texto = valor.strip();
        if (texto.length() > 100) throw new IllegalArgumentException("El dato ingresado es demasiado largo.");
        return texto;
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
            throw new IllegalArgumentException("El pago seleccionado no es valido.", error);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    }
}
