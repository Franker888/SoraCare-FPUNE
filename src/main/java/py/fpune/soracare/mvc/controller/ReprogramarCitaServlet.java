package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.SolicitudReprogramacionCitaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.ConsultorioService;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoService;
import py.fpune.soracare.service.PacienteService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/mis-citas/reprogramar")
public class ReprogramarCitaServlet extends HttpServlet {

    private CitaService citaService;
    private DisponibilidadService disponibilidadService;
    private PacienteService pacienteService;
    private MedicoService medicoService;
    private EspecialidadService especialidadService;
    private ConsultorioService consultorioService;

    @Override
    public void init() {
        citaService = new CitaService();
        disponibilidadService = new DisponibilidadService();
        pacienteService = new PacienteService();
        medicoService = new MedicoService();
        especialidadService = new EspecialidadService();
        consultorioService = new ConsultorioService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        try {
            mostrarVista(request, response,
                    convertirLong(request.getParameter("idCita")),
                    convertirFecha(request.getParameter("fecha")));
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/paciente/reprogramar-cita.jsp")
                    .forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        long idCita = 0;
        LocalDate fecha = convertirFecha(request.getParameter("fecha"));
        try {
            idCita = convertirLong(request.getParameter("idCita"));
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tu cuenta no posee una ficha de paciente asociada."));
            String[] slot = separarSlot(request.getParameter("slot"));
            SolicitudReprogramacionCitaDTO datos = new SolicitudReprogramacionCitaDTO(
                    Long.toString(idCita), vaciarCero(slot[0]), vaciarCero(slot[1]),
                    slot[2], slot[3], slot[4], slot[5], slot[6],
                    request.getParameter("motivoReprogramacion"));
            citaService.reprogramarPorPaciente(
                    datos, paciente.getIdPaciente(), usuario.getIdUsuario());
            response.sendRedirect(request.getContextPath()
                    + "/mis-citas?resultado=reprogramada");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            if (idCita > 0) mostrarVista(request, response, idCita, fecha);
            else request.getRequestDispatcher("/WEB-INF/views/paciente/reprogramar-cita.jsp")
                    .forward(request, response);
        } catch (SQLException error) {
            log("Error al reprogramar la cita del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error",
                    "No fue posible reprogramar la cita. Intentá nuevamente.");
            if (idCita > 0) mostrarVista(request, response, idCita, fecha);
            else request.getRequestDispatcher("/WEB-INF/views/paciente/reprogramar-cita.jsp")
                    .forward(request, response);
        }
    }

    private void mostrarVista(
            HttpServletRequest request,
            HttpServletResponse response,
            long idCita,
            LocalDate fecha
    ) throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tu cuenta no posee una ficha de paciente asociada."));
            Cita cita = citaService.buscarParaReprogramarPorPaciente(
                    idCita, paciente.getIdPaciente());
            List<LocalDate> fechas = disponibilidadService.listarFechasDisponibles(
                    LocalDate.now(DisponibilidadService.ZONA_HORARIA),
                    120, 30, cita.getIdMedico(), cita.getIdEspecialidad());
            LocalDate fechaSeleccionada = fechas.contains(fecha)
                    ? fecha : fechas.stream().findFirst().orElse(null);
            boolean busquedaRealizada = "turnos".equals(request.getParameter("busqueda"));
            Map<Long, Consultorio> consultorios = new LinkedHashMap<>();
            for (Consultorio consultorio : consultorioService.listarTodos()) {
                consultorios.put(consultorio.getIdConsultorio(), consultorio);
            }

            request.setAttribute("paciente", paciente);
            request.setAttribute("cita", cita);
            request.setAttribute("medico", medicoService.buscarPorId(cita.getIdMedico()).orElse(null));
            request.setAttribute("especialidad",
                    especialidadService.buscarPorId(cita.getIdEspecialidad()).orElse(null));
            request.setAttribute("consultoriosPorId", consultorios);
            request.setAttribute("fechasDisponibles", fechas);
            request.setAttribute("fecha", fechaSeleccionada);
            request.setAttribute("busquedaRealizada", busquedaRealizada);
            request.setAttribute("slots", !busquedaRealizada || fechaSeleccionada == null
                    ? List.of()
                    : disponibilidadService.listar(fechaSeleccionada,
                            cita.getIdMedico(), cita.getIdEspecialidad()));
        } catch (SQLException error) {
            log("Error al cargar la reprogramación de la cita.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute("error",
                        "No fue posible cargar los turnos disponibles.");
            }
            request.setAttribute("fechasDisponibles", List.of());
            request.setAttribute("slots", List.of());
            request.setAttribute("consultoriosPorId", Map.of());
        }
        request.getRequestDispatcher("/WEB-INF/views/paciente/reprogramar-cita.jsp")
                .forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return (UsuarioAutenticadoDTO) sesion.getAttribute("usuarioAutenticado");
    }

    private static String[] separarSlot(String valor) {
        if (valor == null) throw new IllegalArgumentException("Seleccioná un nuevo turno.");
        String[] partes = valor.split("\\|", -1);
        if (partes.length != 7) {
            throw new IllegalArgumentException("El nuevo turno seleccionado no es válido.");
        }
        return partes;
    }

    private static String vaciarCero(String valor) {
        return "0".equals(valor) ? "" : valor;
    }

    private static long convertirLong(String valor) {
        try {
            long numero = Long.parseLong(valor == null ? "" : valor.strip());
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La cita seleccionada no es válida.", error);
        }
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return valor == null || valor.isBlank()
                    ? LocalDate.now(DisponibilidadService.ZONA_HORARIA)
                    : LocalDate.parse(valor.strip());
        } catch (DateTimeParseException error) {
            return LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    }
}
