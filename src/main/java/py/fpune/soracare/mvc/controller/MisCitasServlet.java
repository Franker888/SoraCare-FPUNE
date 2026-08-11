package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.SolicitudCitaDTO;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.ResultadoReservaDTO;
import py.fpune.soracare.dto.SolicitudPagoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.ConsultorioService;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
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

@WebServlet("/mis-citas")
public class MisCitasServlet extends HttpServlet {

    private CitaService citaService;
    private DisponibilidadService disponibilidadService;
    private PacienteService pacienteService;
    private MedicoService medicoService;
    private MedicoEspecialidadService medicoEspecialidadService;
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
        medicoEspecialidadService = new MedicoEspecialidadService();
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
        if ("efectivo".equals(resultado)) {
            request.setAttribute("mensajeExito",
                    "Tu turno quedo reservado. El pago en efectivo sigue pendiente hasta que recepcion registre el cobro.");
        } else if ("cancelada".equals(resultado)) {
            request.setAttribute("mensajeExito", "Tu cita fue cancelada correctamente.");
        }
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
            Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tu cuenta no posee una ficha de paciente asociada."));

            if ("cancelar".equals(request.getParameter("accion"))) {
                citaService.cancelarPorPaciente(convertirLong(request.getParameter("idCita")),
                        paciente.getIdPaciente(), usuario.getIdUsuario(),
                        request.getParameter("motivoCancelacion"));
                redirigir(request, response, fecha, "cancelada");
                return;
            }

            String[] slot = separarSlot(request.getParameter("slot"));
            SolicitudCitaDTO datos = new SolicitudCitaDTO(
                    paciente.getIdPaciente().toString(), vaciarCero(slot[0]), vaciarCero(slot[1]),
                    slot[2], slot[3], slot[4], slot[5], slot[6],
                    request.getParameter("motivoConsulta"), request.getParameter("observaciones"),
                    "WEB");
            ResultadoReservaDTO resultado = reservaPagoService.reservar(
                    datos, solicitudPago(request), usuario.getIdUsuario());
            if (resultado.pago().esDigital()) {
                response.sendRedirect(request.getContextPath() + "/pagos/procesar?idPago="
                        + resultado.pago().idPago());
            } else {
                redirigir(request, response, LocalDate.parse(slot[5]), "efectivo");
            }
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response, fecha);
        } catch (SQLException error) {
            log("Error al gestionar las citas del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible gestionar tu cita. Intenta nuevamente.");
            mostrarVista(request, response, fecha);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response, LocalDate fecha)
            throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            Paciente paciente = pacienteService.buscarPorIdPersona(usuario.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tu cuenta no posee una ficha de paciente asociada."));
            Long idMedico = convertirLongOpcional(request.getParameter("idMedico"));
            Integer idEspecialidad = convertirEnteroOpcional(request.getParameter("idEspecialidad"));
            List<Medico> medicos = medicoService.listarActivos();
            List<Especialidad> especialidades = especialidadService.listarActivas();
            List<Consultorio> consultorios = consultorioService.listarTodos();
            Map<Long, Medico> medicosPorId = new LinkedHashMap<>();
            Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico = new LinkedHashMap<>();
            Map<Integer, Especialidad> especialidadesPorId = new LinkedHashMap<>();
            Map<Long, Consultorio> consultoriosPorId = new LinkedHashMap<>();
            for (Medico medico : medicos) {
                medicosPorId.put(medico.getIdMedico(), medico);
                especialidadesPorMedico.put(medico.getIdMedico(),
                        medicoEspecialidadService.listarActivasPorMedico(medico.getIdMedico()));
            }
            for (Especialidad especialidad : especialidades) especialidadesPorId.put(especialidad.getIdEspecialidad(), especialidad);
            for (Consultorio consultorio : consultorios) consultoriosPorId.put(consultorio.getIdConsultorio(), consultorio);

            if (!atiendeEspecialidad(especialidadesPorMedico, idMedico, idEspecialidad)) {
                idMedico = null;
            }

            List<LocalDate> fechasDisponibles = disponibilidadService.listarFechasDisponibles(
                    LocalDate.now(DisponibilidadService.ZONA_HORARIA),
                    120, 30, idMedico, idEspecialidad);
            boolean busquedaRealizada = "turnos".equals(request.getParameter("busqueda"));
            LocalDate fechaSeleccionada = fechasDisponibles.contains(fecha)
                    ? fecha
                    : fechasDisponibles.stream().findFirst().orElse(null);
            if (request.getParameter("fecha") != null
                    && fechaSeleccionada != null
                    && !fechaSeleccionada.equals(fecha)) {
                request.setAttribute("mensajeDisponibilidad",
                        "La fecha indicada no tenía turnos. Te mostramos la próxima fecha disponible.");
            }

            request.setAttribute("paciente", paciente);
            request.setAttribute("fecha", fechaSeleccionada);
            request.setAttribute("fechasDisponibles", fechasDisponibles);
            request.setAttribute("busquedaRealizada", busquedaRealizada);
            request.setAttribute("idMedicoSeleccionado", idMedico);
            request.setAttribute("idEspecialidadSeleccionada", idEspecialidad);
            request.setAttribute("slots", !busquedaRealizada || fechaSeleccionada == null
                    ? List.of()
                    : disponibilidadService.listar(fechaSeleccionada, idMedico, idEspecialidad));
            request.setAttribute("citas", citaService.listarPorPaciente(paciente.getIdPaciente()).stream()
                    .filter(cita -> !"CANCELADA".equals(cita.getCodigoEstado()))
                    .toList());
            List<PagoResumenDTO> pagos = pagoService.listarPorPaciente(paciente.getIdPaciente());
            Map<Long, PagoResumenDTO> pagosPorCita = new LinkedHashMap<>();
            for (PagoResumenDTO pago : pagos) pagosPorCita.putIfAbsent(pago.idCita(), pago);
            request.setAttribute("pagos", pagos);
            request.setAttribute("pagosPorCita", pagosPorCita);
            request.setAttribute("metodosPago", pagoService.listarMetodosActivos());
            request.setAttribute("medicos", medicos);
            request.setAttribute("especialidadesPorMedico", especialidadesPorMedico);
            request.setAttribute("especialidades", especialidades);
            request.setAttribute("medicosPorId", medicosPorId);
            request.setAttribute("especialidadesPorId", especialidadesPorId);
            request.setAttribute("consultoriosPorId", consultoriosPorId);
        } catch (SQLException error) {
            log("Error al cargar el portal del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) request.setAttribute("error", "No fue posible cargar tus citas.");
            request.setAttribute("fecha", fecha);
            request.setAttribute("fechasDisponibles", List.of());
            request.setAttribute("busquedaRealizada",
                    "turnos".equals(request.getParameter("busqueda")));
            request.setAttribute("slots", List.of());
            request.setAttribute("citas", List.of());
            request.setAttribute("pagos", List.of());
            request.setAttribute("pagosPorCita", Map.of());
            request.setAttribute("metodosPago", List.of());
            request.setAttribute("medicos", List.of());
            request.setAttribute("especialidadesPorMedico", Map.of());
            request.setAttribute("especialidades", List.of());
            request.setAttribute("medicosPorId", Map.of());
            request.setAttribute("especialidadesPorId", Map.of());
            request.setAttribute("consultoriosPorId", Map.of());
        }
        request.getRequestDispatcher("/WEB-INF/views/paciente/mis-citas.jsp").forward(request, response);
    }

    private static boolean atiendeEspecialidad(
            Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico,
            Long idMedico,
            Integer idEspecialidad
    ) {
        if (idMedico == null || idEspecialidad == null) return true;
        return especialidadesPorMedico.getOrDefault(idMedico, List.of()).stream()
                .anyMatch(relacion -> idEspecialidad.equals(relacion.getIdEspecialidad()));
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return (UsuarioAutenticadoDTO) sesion.getAttribute("usuarioAutenticado");
    }

    private static String[] separarSlot(String valor) {
        if (valor == null) throw new IllegalArgumentException("Selecciona un turno disponible.");
        String[] partes = valor.split("\\|", -1);
        if (partes.length != 7) throw new IllegalArgumentException("El turno seleccionado no es valido.");
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
            throw new IllegalArgumentException("La cita seleccionada no es valida.", error);
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

    private static void redirigir(HttpServletRequest request, HttpServletResponse response,
                                  LocalDate fecha, String resultado) throws IOException {
        response.sendRedirect(request.getContextPath() + "/mis-citas?fecha=" + fecha
                + "&resultado=" + resultado);
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    }
}
