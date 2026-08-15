package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroMedicamentoDTO;
import py.fpune.soracare.mvc.model.ViaAdministracion;
import py.fpune.soracare.service.MedicamentoService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/medicamentos")
public class MedicamentosServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/admin/medicamentos.jsp";
    private MedicamentoService medicamentoService;

    @Override
    public void init() {
        medicamentoService = new MedicamentoService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        String resultado = request.getParameter("resultado");
        if ("registrado".equals(resultado)) {
            request.setAttribute("mensajeExito", "El medicamento fue registrado correctamente.");
        } else if ("activado".equals(resultado)) {
            request.setAttribute("mensajeExito", "El medicamento volvió a estar disponible.");
        } else if ("inactivado".equals(resultado)) {
            request.setAttribute("mensajeExito", "El medicamento fue inactivado.");
        }
        mostrarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        try {
            if ("estado".equals(accion)) {
                long id = convertirId(request.getParameter("idMedicamento"));
                boolean activo = Boolean.parseBoolean(request.getParameter("activo"));
                medicamentoService.cambiarEstado(id, activo);
                response.sendRedirect(request.getContextPath() + "/admin/medicamentos?resultado="
                        + (activo ? "activado" : "inactivado"));
                return;
            }

            RegistroMedicamentoDTO datos = new RegistroMedicamentoDTO(
                    request.getParameter("nombreComercial"),
                    request.getParameter("principioActivo"),
                    request.getParameter("presentacion"),
                    request.getParameter("concentracion"),
                    request.getParameter("viaAdministracion"),
                    request.getParameter("requiereReceta"));
            medicamentoService.registrar(datos);
            response.sendRedirect(request.getContextPath()
                    + "/admin/medicamentos?resultado=registrado");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            conservarFormulario(request);
            mostrarVista(request, response);
        } catch (SQLException error) {
            log("Error al administrar medicamentos.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible completar la operación.");
            conservarFormulario(request);
            mostrarVista(request, response);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("medicamentos", medicamentoService.listarTodos());
        } catch (SQLException error) {
            log("Error al listar medicamentos.", error);
            request.setAttribute("medicamentos", List.of());
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "No fue posible cargar los medicamentos.");
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        request.setAttribute("viasAdministracion", ViaAdministracion.values());
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static void conservarFormulario(HttpServletRequest request) {
        for (String campo : List.of("nombreComercial", "principioActivo",
                "presentacion", "concentracion", "viaAdministracion")) {
            request.setAttribute(campo, request.getParameter(campo));
        }
        request.setAttribute("requiereReceta", request.getParameter("requiereReceta"));
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El medicamento seleccionado no es válido.", error);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
