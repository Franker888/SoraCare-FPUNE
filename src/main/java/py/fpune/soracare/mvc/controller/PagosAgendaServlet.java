package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.FiltroAgendaPagosDTO;
import py.fpune.soracare.dto.ResultadoAgendaPagosDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.MetodoPago;
import py.fpune.soracare.service.PagoService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/agenda/pagos")
public class PagosAgendaServlet extends HttpServlet {

    private static final int TAMANO_PAGINA = 20;

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
                    "El cobro en efectivo fue confirmado y el comprobante quedó generado; "
                            + "el envío al correo se está procesando.");
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
            String filtros = construirConsultaFiltros(request);
            response.sendRedirect(request.getContextPath()
                    + "/agenda/pagos?resultado=confirmado"
                    + (filtros.isBlank() ? "" : "&" + filtros));
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
        FiltroAgendaPagosDTO filtro;
        try {
            filtro = construirFiltro(request);
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            filtro = FiltroAgendaPagosDTO.vacio(TAMANO_PAGINA);
        }

        ResultadoAgendaPagosDTO resultado = ResultadoAgendaPagosDTO.vacio(TAMANO_PAGINA);
        List<MetodoPago> metodos = List.of();
        try {
            resultado = pagoService.buscarParaAgenda(filtro);
            metodos = pagoService.listarMetodosActivos();
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            filtro = FiltroAgendaPagosDTO.vacio(TAMANO_PAGINA);
            try {
                resultado = pagoService.buscarParaAgenda(filtro);
                metodos = pagoService.listarMetodosActivos();
            } catch (SQLException sqlError) {
                log("Error al restablecer la agenda de pagos.", sqlError);
            }
        } catch (SQLException error) {
            log("Error al listar pagos.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible cargar los pagos.");
        }
        request.setAttribute("filtro", filtro);
        request.setAttribute("resultado", resultado);
        request.setAttribute("pagos", resultado.pagos());
        request.setAttribute("metodosPago", metodos);
        request.getRequestDispatcher("/WEB-INF/views/pagos/agenda.jsp")
                .forward(request, response);
    }

    private static FiltroAgendaPagosDTO construirFiltro(HttpServletRequest request) {
        return new FiltroAgendaPagosDTO(
                parametro(request, "cedula"),
                parametro(request, "metodo"),
                "",
                null,
                null,
                convertirPagina(request.getParameter("pagina")),
                TAMANO_PAGINA
        );
    }

    private static int convertirPagina(String valor) {
        if (valor == null || valor.isBlank()) return 1;
        try {
            int pagina = Integer.parseInt(valor.strip());
            if (pagina < 1) throw new NumberFormatException();
            return pagina;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La pagina solicitada no es valida.", error);
        }
    }

    private static String parametro(HttpServletRequest request, String nombre) {
        String valor = request.getParameter(nombre);
        return valor == null ? "" : valor.strip();
    }

    private static String construirConsultaFiltros(HttpServletRequest request) {
        StringBuilder consulta = new StringBuilder();
        for (String nombre : new String[]{"cedula", "metodo", "pagina"}) {
            String valor = parametro(request, nombre);
            if (valor.isBlank()) continue;
            if (!consulta.isEmpty()) consulta.append('&');
            consulta.append(URLEncoder.encode(nombre, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
        }
        return consulta.toString();
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
