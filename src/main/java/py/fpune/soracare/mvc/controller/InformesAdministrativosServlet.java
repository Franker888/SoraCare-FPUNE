package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.FiltroInformeDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.InformeAdministrativoService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/informes")
public class InformesAdministrativosServlet extends HttpServlet {

    private InformeAdministrativoService informeService;
    private MedicoService medicoService;
    private MedicoEspecialidadService medicoEspecialidadService;
    private EspecialidadService especialidadService;

    @Override
    public void init() {
        informeService = new InformeAdministrativoService();
        medicoService = new MedicoService();
        medicoEspecialidadService = new MedicoEspecialidadService();
        especialidadService = new EspecialidadService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        FiltroInformeDTO filtro;
        try {
            filtro = informeService.construirFiltro(
                    request.getParameter("desde"),
                    request.getParameter("hasta"),
                    request.getParameter("idMedico"),
                    request.getParameter("idEspecialidad")
            );
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            filtro = informeService.filtroMesActual();
        }

        InformeAdministrativoDTO informe = InformeAdministrativoDTO.vacio();
        try {
            informe = informeService.consultar(filtro);
            cargarCatalogos(request);
        } catch (SQLException error) {
            log("Error al construir los informes administrativos.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error",
                    "No fue posible cargar los indicadores. Verificá la migración de la etapa 8.");
            cargarCatalogosSeguros(request);
        }

        request.setAttribute("filtro", filtro);
        request.setAttribute("informe", informe);
        request.getRequestDispatcher("/WEB-INF/views/admin/informes.jsp")
                .forward(request, response);
    }

    private void cargarCatalogos(HttpServletRequest request) throws SQLException {
        List<Medico> medicos = medicoService.listarActivos();
        Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico = new LinkedHashMap<>();
        for (Medico medico : medicos) {
            especialidadesPorMedico.put(
                    medico.getIdMedico(),
                    medicoEspecialidadService.listarActivasPorMedico(medico.getIdMedico())
            );
        }
        request.setAttribute("medicos", medicos);
        request.setAttribute("especialidades", especialidadService.listarActivas());
        request.setAttribute("especialidadesPorMedico", especialidadesPorMedico);
    }

    private void cargarCatalogosSeguros(HttpServletRequest request) {
        try {
            cargarCatalogos(request);
        } catch (SQLException error) {
            log("Error al cargar filtros de los informes.", error);
            request.setAttribute("medicos", List.of());
            request.setAttribute("especialidades", List.of());
            request.setAttribute("especialidadesPorMedico", Map.of());
        }
    }
}
