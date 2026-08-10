package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.PagoService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/agenda/pagos")
public class PagosAgendaServlet extends HttpServlet {

    private PagoService pagoService;

    @Override
    public void init() {
        pagoService = new PagoService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        if ("confirmado".equals(request.getParameter("resultado"))) {
            request.setAttribute("mensajeExito",
                    "El cobro en efectivo fue confirmado y el comprobante quedo generado.");
        }
        mostrar(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        try {
            if (!"confirmarEfectivo".equals(request.getParameter("accion"))) {
                throw new IllegalArgumentException("La accion solicitada no es valida.");
            }
            UsuarioAutenticadoDTO usuario = usuario(request);
            pagoService.confirmarEfectivo(convertirLong(request.getParameter("idPago")),
                    usuario.getIdUsuario());
            response.sendRedirect(request.getContextPath() + "/agenda/pagos?resultado=confirmado");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrar(request, response);
        } catch (SQLException error) {
            log("Error al confirmar un cobro en efectivo.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible confirmar el cobro.");
            mostrar(request, response);
        }
    }

    private void mostrar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("pagos", pagoService.listarRecientes());
        } catch (SQLException error) {
            log("Error al listar pagos.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible cargar los pagos.");
            request.setAttribute("pagos", java.util.List.of());
        }
        request.getRequestDispatcher("/WEB-INF/views/pagos/agenda.jsp")
                .forward(request, response);
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
