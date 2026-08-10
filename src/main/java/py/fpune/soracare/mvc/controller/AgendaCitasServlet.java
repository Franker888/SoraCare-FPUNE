package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.SolicitudCitaDTO;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.SolicitudPagoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.ConsultorioService;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoService;
import py.fpune.soracare.service.PacienteService;
import py.fpune.soracare.service.PagoService;
import py.fpune.soracare.service.ReservaPagoService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/agenda/citas")
public class AgendaCitasServlet extends HttpServlet {

    private CitaService citaService;
    private DisponibilidadService disponibilidadService;
    private PacienteService pacienteService;
    private MedicoService medicoService;
    private EspecialidadService especialidadService;
    private ConsultorioService consultorioService;
    private PagoService pagoService;
    private ReservaPagoService reservaPagoService;

    @Override
    public void init() {
        citaService = new CitaService();
        disponibilidadService = new DisponibilidadService();
        pacienteService = new PacienteService();
        medicoService = new MedicoService();
        especialidadService = new EspecialidadService();
        consultorioService = new ConsultorioService();
        pagoService = new PagoService();
        reservaPagoService = new ReservaPagoService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        String resultado = request.getParameter("resultado");
        if ("reservada".equals(resultado)) request.setAttribute("mensajeExito", "La cita fue solicitada correctamente.");
        if ("cancelada".equals(resultado)) request.setAttribute("mensajeExito", "La cita fue cancelada correctamente.");
        mostrarVista(request, response, convertirFecha(request.getParameter("fecha")));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        LocalDate fecha = convertirFecha(request.getParameter("fecha"));
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            if ("cancelar".equals(request.getParameter("accion"))) {
                citaService.cancelar(convertirLong(request.getParameter("idCita")),
                        usuario.getIdUsuario(), request.getParameter("motivoCancelacion"));
                redirigir(request, response, fecha, "cancelada");
                return;
            }

            String[] slot = separarSlot(request.getParameter("slot"));
            SolicitudCitaDTO datos = new SolicitudCitaDTO(
                    request.getParameter("idPaciente"), vaciarCero(slot[0]), vaciarCero(slot[1]),
                    slot[2], slot[3], slot[4], slot[5], slot[6],
                    request.getParameter("motivoConsulta"), request.getParameter("observaciones"),
                    request.getParameter("canalReserva"));
            reservaPagoService.reservar(datos, solicitudPago(request), usuario.getIdUsuario());
            redirigir(request, response, LocalDate.parse(slot[5]), "reservada");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response, fecha);
        } catch (SQLException error) {
            log("Error al gestionar una cita.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible gestionar la cita. Intentá nuevamente.");
            mostrarVista(request, response, fecha);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response, LocalDate fecha)
            throws ServletException, IOException {
        try {
            List<Medico> medicos = medicoService.listarActivos();
            List<Especialidad> especialidades = especialidadService.listarActivas();
            List<Consultorio> consultorios = consultorioService.listarTodos();
            List<Paciente> pacientes = pacienteService.listarTodos();
            Map<Long, Medico> medicosPorId = new LinkedHashMap<>();
            Map<Integer, Especialidad> especialidadesPorId = new LinkedHashMap<>();
            Map<Long, Consultorio> consultoriosPorId = new LinkedHashMap<>();
            Map<Long, Paciente> pacientesPorId = new LinkedHashMap<>();
            for (Medico medico : medicos) medicosPorId.put(medico.getIdMedico(), medico);
            for (Especialidad especialidad : especialidades) especialidadesPorId.put(especialidad.getIdEspecialidad(), especialidad);
            for (Consultorio consultorio : consultorios) consultoriosPorId.put(consultorio.getIdConsultorio(), consultorio);
            for (Paciente paciente : pacientes) pacientesPorId.put(paciente.getIdPaciente(), paciente);

            request.setAttribute("fecha", fecha);
            request.setAttribute("slots", disponibilidadService.listar(fecha, null, null));
            request.setAttribute("citas", citaService.listarPorFecha(fecha));
            request.setAttribute("pacientes", pacientes);
            request.setAttribute("metodosPago", pagoService.listarMetodosActivos());
            request.setAttribute("medicosPorId", medicosPorId);
            request.setAttribute("especialidadesPorId", especialidadesPorId);
            request.setAttribute("consultoriosPorId", consultoriosPorId);
            request.setAttribute("pacientesPorId", pacientesPorId);
        } catch (SQLException error) {
            log("Error al cargar la agenda.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) request.setAttribute("error", "No fue posible cargar la agenda.");
            request.setAttribute("fecha", fecha);
            request.setAttribute("slots", List.of());
            request.setAttribute("citas", List.of());
            request.setAttribute("pacientes", List.of());
            request.setAttribute("metodosPago", List.of());
            request.setAttribute("medicosPorId", Map.of());
            request.setAttribute("especialidadesPorId", Map.of());
            request.setAttribute("consultoriosPorId", Map.of());
            request.setAttribute("pacientesPorId", Map.of());
        }
        request.getRequestDispatcher("/WEB-INF/views/agenda/citas.jsp").forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return (UsuarioAutenticadoDTO) sesion.getAttribute("usuarioAutenticado");
    }

    private static String[] separarSlot(String valor) {
        if (valor == null) throw new IllegalArgumentException("Seleccioná un turno disponible.");
        String[] partes = valor.split("\\|", -1);
        if (partes.length != 7) throw new IllegalArgumentException("El turno seleccionado no es válido.");
        return partes;
    }

    private static String vaciarCero(String valor) { return "0".equals(valor) ? "" : valor; }

    private static SolicitudPagoDTO solicitudPago(HttpServletRequest request) {
        return new SolicitudPagoDTO(
                request.getParameter("metodoPago"),
                new DatosFacturacionDTO(
                        request.getParameter("tipoDocumentoFactura"),
                        request.getParameter("numeroDocumentoFactura"),
                        request.getParameter("nombreFactura"),
                        request.getParameter("direccionFactura"),
                        request.getParameter("correoFactura")
                )
        );
    }
    private static long convertirLong(String valor) {
        try {
            long numero = Long.parseLong(valor);
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

    private static void redirigir(HttpServletRequest request, HttpServletResponse response,
                                  LocalDate fecha, String resultado) throws IOException {
        response.sendRedirect(request.getContextPath() + "/agenda/citas?fecha=" + fecha
                + "&resultado=" + resultado);
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    }
}
