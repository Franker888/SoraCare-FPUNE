package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.DisponibilidadDiaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.service.ConsultorioService;
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

@WebServlet(urlPatterns = {
        "/recepcion/disponibilidad",
        "/medico/disponibilidad"
})
public class DisponibilidadInternaServlet extends HttpServlet {

    private static final String VISTA =
            "/WEB-INF/views/agenda/disponibilidad-interna.jsp";
    private static final int DIAS_A_CONSULTAR = 45;
    private static final int MAXIMO_DIAS_CON_TURNOS = 12;

    private DisponibilidadService disponibilidadService;
    private MedicoService medicoService;
    private EspecialidadService especialidadService;
    private ConsultorioService consultorioService;

    @Override
    public void init() {
        disponibilidadService = new DisponibilidadService();
        medicoService = new MedicoService();
        especialidadService = new EspecialidadService();
        consultorioService = new ConsultorioService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        boolean modoMedico = request.getServletPath().startsWith("/medico/");
        LocalDate desde = convertirFecha(request.getParameter("desde"));
        request.setAttribute("modoMedico", modoMedico);
        request.setAttribute("desde", desde);
        request.setAttribute("diasConsultados", DIAS_A_CONSULTAR);

        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            List<Medico> medicos = medicoService.listarActivos();
            List<Especialidad> especialidades = especialidadService.listarActivas();
            List<Consultorio> consultorios = consultorioService.listarTodos();

            Long idMedico = modoMedico
                    ? medicoService.buscarPorIdPersona(usuario.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La cuenta no está vinculada con un médico registrado."
                    )).getIdMedico()
                    : convertirLongOpcional(request.getParameter("idMedico"));
            Integer idEspecialidad = modoMedico
                    ? null
                    : convertirEnteroOpcional(request.getParameter("idEspecialidad"));

            Map<Long, Medico> medicosPorId = new LinkedHashMap<>();
            Map<Integer, Especialidad> especialidadesPorId = new LinkedHashMap<>();
            Map<Long, Consultorio> consultoriosPorId = new LinkedHashMap<>();
            for (Medico medico : medicos) {
                medicosPorId.put(medico.getIdMedico(), medico);
            }
            for (Especialidad especialidad : especialidades) {
                especialidadesPorId.put(especialidad.getIdEspecialidad(), especialidad);
            }
            for (Consultorio consultorio : consultorios) {
                consultoriosPorId.put(consultorio.getIdConsultorio(), consultorio);
            }

            if (idMedico != null && !medicosPorId.containsKey(idMedico)) {
                throw new IllegalArgumentException(
                        "El médico seleccionado no se encuentra activo."
                );
            }
            if (idEspecialidad != null
                    && !especialidadesPorId.containsKey(idEspecialidad)) {
                throw new IllegalArgumentException(
                        "La especialidad seleccionada no se encuentra activa."
                );
            }

            List<DisponibilidadDiaDTO> agenda = idMedico == null
                    ? List.of()
                    : disponibilidadService.listarAgendaDisponible(
                    desde, DIAS_A_CONSULTAR, MAXIMO_DIAS_CON_TURNOS,
                    idMedico, idEspecialidad
            );
            int totalTurnos = agenda.stream()
                    .mapToInt(DisponibilidadDiaDTO::cantidadTurnos)
                    .sum();

            request.setAttribute("medicos", medicos);
            request.setAttribute("especialidades", especialidades);
            request.setAttribute("medicosPorId", medicosPorId);
            request.setAttribute("especialidadesPorId", especialidadesPorId);
            request.setAttribute("consultoriosPorId", consultoriosPorId);
            request.setAttribute("idMedico", idMedico);
            request.setAttribute("idEspecialidad", idEspecialidad);
            request.setAttribute("medicoSeleccionado",
                    idMedico == null ? null : medicosPorId.get(idMedico));
            request.setAttribute("agendaDisponible", agenda);
            request.setAttribute("totalTurnos", totalTurnos);
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            completarValoresVacios(request);
        } catch (SQLException error) {
            log("No fue posible consultar la disponibilidad interna.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible consultar los horarios libres. Intentá nuevamente."
            );
            completarValoresVacios(request);
        }

        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(
            HttpServletRequest request
    ) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión válida.");
        }
        return autenticado;
    }

    private static LocalDate convertirFecha(String valor) {
        LocalDate hoy = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        if (valor == null || valor.isBlank()) {
            return hoy;
        }
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            return fecha.isBefore(hoy) ? hoy : fecha;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha de consulta no es válida.", error);
        }
    }

    private static Long convertirLongOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            long numero = Long.parseLong(valor.strip());
            if (numero <= 0) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El médico seleccionado no es válido.", error);
        }
    }

    private static Integer convertirEnteroOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero <= 0) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La especialidad seleccionada no es válida.", error
            );
        }
    }

    private static void completarValoresVacios(HttpServletRequest request) {
        if (request.getAttribute("medicos") == null) {
            request.setAttribute("medicos", List.of());
        }
        if (request.getAttribute("especialidades") == null) {
            request.setAttribute("especialidades", List.of());
        }
        request.setAttribute("medicosPorId", Map.of());
        request.setAttribute("especialidadesPorId", Map.of());
        request.setAttribute("consultoriosPorId", Map.of());
        request.setAttribute("agendaDisponible", List.of());
        request.setAttribute("totalTurnos", 0);
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
