package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.ConsultorioService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.ExcepcionHorarioMedicoService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/excepciones-horarios")
public class ExcepcionesHorariosServlet extends HttpServlet {

    private ExcepcionHorarioMedicoService excepcionService;
    private MedicoService medicoService;
    private MedicoEspecialidadService relacionService;
    private EspecialidadService especialidadService;
    private ConsultorioService consultorioService;

    @Override
    public void init() {
        excepcionService = new ExcepcionHorarioMedicoService();
        medicoService = new MedicoService();
        relacionService = new MedicoEspecialidadService();
        especialidadService = new EspecialidadService();
        consultorioService = new ConsultorioService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        String resultado = request.getParameter("resultado");
        if ("registrada".equals(resultado)) request.setAttribute("mensajeExito", "La excepción fue registrada correctamente.");
        if ("estado".equals(resultado)) request.setAttribute("mensajeExito", "El estado de la excepción fue actualizado.");
        mostrarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        try {
            if ("cambiarEstado".equals(request.getParameter("accion"))) {
                excepcionService.actualizarEstado(convertirId(request.getParameter("idExcepcion")),
                        Boolean.parseBoolean(request.getParameter("activo")));
                response.sendRedirect(request.getContextPath() + "/admin/excepciones-horarios?resultado=estado");
                return;
            }
            RegistroExcepcionHorarioDTO datos = new RegistroExcepcionHorarioDTO(
                    request.getParameter("medicoEspecialidad"), request.getParameter("idConsultorio"),
                    request.getParameter("fecha"), request.getParameter("horaInicio"),
                    request.getParameter("horaFin"), request.getParameter("tipo"),
                    request.getParameter("motivo"));
            excepcionService.registrar(datos);
            response.sendRedirect(request.getContextPath() + "/admin/excepciones-horarios?resultado=registrada");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response);
        } catch (SQLException error) {
            log("Error al gestionar una excepción de horario.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible guardar la excepción.");
            mostrarVista(request, response);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Medico> medicos = medicoService.listarActivos();
            List<Especialidad> especialidades = especialidadService.listarActivas();
            List<Consultorio> consultorios = consultorioService.listarTodos();
            Map<Long, Medico> medicosPorId = new LinkedHashMap<>();
            Map<Integer, Especialidad> especialidadesPorId = new LinkedHashMap<>();
            Map<Long, Consultorio> consultoriosPorId = new LinkedHashMap<>();
            Map<Long, List<MedicoEspecialidad>> relacionesPorMedico = new LinkedHashMap<>();
            for (Medico medico : medicos) {
                medicosPorId.put(medico.getIdMedico(), medico);
                relacionesPorMedico.put(medico.getIdMedico(), relacionService.listarActivasPorMedico(medico.getIdMedico()));
            }
            for (Especialidad especialidad : especialidades) especialidadesPorId.put(especialidad.getIdEspecialidad(), especialidad);
            for (Consultorio consultorio : consultorios) consultoriosPorId.put(consultorio.getIdConsultorio(), consultorio);
            request.setAttribute("excepciones", excepcionService.listarTodas());
            request.setAttribute("medicos", medicos);
            request.setAttribute("consultorios", consultorios);
            request.setAttribute("medicosPorId", medicosPorId);
            request.setAttribute("especialidadesPorId", especialidadesPorId);
            request.setAttribute("consultoriosPorId", consultoriosPorId);
            request.setAttribute("relacionesPorMedico", relacionesPorMedico);
        } catch (SQLException error) {
            log("Error al cargar excepciones de horarios.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) request.setAttribute("error", "No fue posible cargar las excepciones.");
            request.setAttribute("excepciones", List.of());
            request.setAttribute("medicos", List.of());
            request.setAttribute("consultorios", List.of());
            request.setAttribute("medicosPorId", Map.of());
            request.setAttribute("especialidadesPorId", Map.of());
            request.setAttribute("consultoriosPorId", Map.of());
            request.setAttribute("relacionesPorMedico", Map.of());
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/excepciones-horarios.jsp")
                .forward(request, response);
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La excepción seleccionada no es válida.", error);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    }
}
