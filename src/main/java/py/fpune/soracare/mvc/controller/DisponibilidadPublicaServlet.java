package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;

@WebServlet("/disponibilidad")
public class DisponibilidadPublicaServlet extends HttpServlet {

    private DisponibilidadService disponibilidadService;
    private MedicoService medicoService;
    private EspecialidadService especialidadService;

    @Override
    public void init() {
        disponibilidadService = new DisponibilidadService();
        medicoService = new MedicoService();
        especialidadService = new EspecialidadService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, max-age=0");
        LocalDate fecha = convertirFecha(request.getParameter("fecha"));
        Long idMedico = convertirLongOpcional(request.getParameter("idMedico"));
        Integer idEspecialidad = convertirEnteroOpcional(request.getParameter("idEspecialidad"));
        try {
            List<Medico> medicos = medicoService.listarActivos();
            List<Especialidad> especialidades = especialidadService.listarActivas();
            Map<Long, Medico> medicosPorId = new LinkedHashMap<>();
            Map<Integer, Especialidad> especialidadesPorId = new LinkedHashMap<>();
            for (Medico medico : medicos) medicosPorId.put(medico.getIdMedico(), medico);
            for (Especialidad especialidad : especialidades) especialidadesPorId.put(especialidad.getIdEspecialidad(), especialidad);
            request.setAttribute("fecha", fecha);
            request.setAttribute("idMedicoSeleccionado", idMedico);
            request.setAttribute("idEspecialidadSeleccionada", idEspecialidad);
            request.setAttribute("medicos", medicos);
            request.setAttribute("especialidades", especialidades);
            request.setAttribute("medicosPorId", medicosPorId);
            request.setAttribute("especialidadesPorId", especialidadesPorId);
            request.setAttribute("slots", disponibilidadService.listar(fecha, idMedico, idEspecialidad));
        } catch (SQLException error) {
            log("Error al consultar disponibilidad pública.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible consultar la disponibilidad.");
        }
        request.getRequestDispatcher("/WEB-INF/views/publico/disponibilidad.jsp")
                .forward(request, response);
    }

    private static LocalDate convertirFecha(String valor) {
        if (valor == null || valor.isBlank()) return LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException error) {
            return LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        }
    }

    private static Long convertirLongOpcional(String valor) {
        try {
            long numero = Long.parseLong(valor == null ? "" : valor.strip());
            return numero > 0 ? numero : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static Integer convertirEnteroOpcional(String valor) {
        try {
            int numero = Integer.parseInt(valor == null ? "" : valor.strip());
            return numero > 0 ? numero : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
