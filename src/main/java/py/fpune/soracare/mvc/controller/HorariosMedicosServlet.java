package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroHorarioMedicoDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.EstadoConsultorio;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.ConsultorioService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.HorarioMedicoService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/horarios")
public class HorariosMedicosServlet extends HttpServlet {

    private static final String VISTA_HORARIOS =
            "/WEB-INF/views/admin/horarios.jsp";

    private static final String RUTA_HORARIOS =
            "/admin/horarios";

    private HorarioMedicoService horarioMedicoService;
    private MedicoService medicoService;
    private MedicoEspecialidadService
            medicoEspecialidadService;
    private EspecialidadService especialidadService;
    private ConsultorioService consultorioService;

    @Override
    public void init() {
        horarioMedicoService =
                new HorarioMedicoService();
        medicoService =
                new MedicoService();
        medicoEspecialidadService =
                new MedicoEspecialidadService();
        especialidadService =
                new EspecialidadService();
        consultorioService =
                new ConsultorioService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        String resultado =
                request.getParameter(
                        "resultado"
                );

        if ("registrado".equals(
                resultado
        )) {
            request.setAttribute(
                    "mensajeExito",
                    "El horario médico fue registrado correctamente."
            );

        } else if ("estado-actualizado".equals(
                resultado
        )) {
            request.setAttribute(
                    "mensajeExito",
                    "El estado del horario fue actualizado correctamente."
            );
        }

        mostrarVista(
                request,
                response
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        request.setCharacterEncoding(
                "UTF-8"
        );

        String accion =
                request.getParameter(
                        "accion"
                );

        if ("cambiarEstado".equals(
                accion
        )) {
            procesarCambioEstado(
                    request,
                    response
            );

            return;
        }

        procesarRegistro(
                request,
                response
        );
    }

    private void procesarRegistro(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String medicoEspecialidad =
                request.getParameter(
                        "medicoEspecialidad"
                );

        String[] identificadores =
                separarMedicoEspecialidad(
                        medicoEspecialidad
                );

        String idConsultorio =
                request.getParameter(
                        "idConsultorio"
                );

        String diaSemana =
                request.getParameter(
                        "diaSemana"
                );

        String horaInicio =
                request.getParameter(
                        "horaInicio"
                );

        String horaFin =
                request.getParameter(
                        "horaFin"
                );

        String duracionTurnoMinutos =
                request.getParameter(
                        "duracionTurnoMinutos"
                );

        String fechaDesde =
                request.getParameter(
                        "fechaDesde"
                );

        String fechaHasta =
                request.getParameter(
                        "fechaHasta"
                );

        conservarFormulario(
                request,
                medicoEspecialidad,
                idConsultorio,
                diaSemana,
                horaInicio,
                horaFin,
                duracionTurnoMinutos,
                fechaDesde,
                fechaHasta
        );

        RegistroHorarioMedicoDTO datos =
                new RegistroHorarioMedicoDTO(
                        identificadores[0],
                        identificadores[1],
                        idConsultorio,
                        diaSemana,
                        horaInicio,
                        horaFin,
                        duracionTurnoMinutos,
                        fechaDesde,
                        fechaHasta
                );

        try {
            horarioMedicoService.registrar(
                    datos
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_HORARIOS
                            + "?resultado=registrado"
            );

        } catch (IllegalArgumentException error) {
            procesarErrorValidacion(
                    request,
                    response,
                    error.getMessage()
            );

        } catch (SQLException error) {
            log(
                    "Error de base de datos al registrar "
                            + "el horario médico.",
                    error
            );

            procesarErrorServidor(
                    request,
                    response,
                    "No fue posible registrar el horario. "
                            + "Intentá nuevamente."
            );
        }
    }

    private void procesarCambioEstado(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            long idHorario =
                    convertirIdHorario(
                            request.getParameter(
                                    "idHorario"
                            )
                    );

            boolean activo =
                    Boolean.parseBoolean(
                            request.getParameter(
                                    "activo"
                            )
                    );

            horarioMedicoService.actualizarEstado(
                    idHorario,
                    activo
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_HORARIOS
                            + "?resultado=estado-actualizado"
            );

        } catch (IllegalArgumentException error) {
            procesarErrorValidacion(
                    request,
                    response,
                    error.getMessage()
            );

        } catch (SQLException error) {
            log(
                    "Error de base de datos al actualizar "
                            + "el horario médico.",
                    error
            );

            procesarErrorServidor(
                    request,
                    response,
                    "No fue posible actualizar el horario."
            );
        }
    }

    private void procesarErrorValidacion(
            HttpServletRequest request,
            HttpServletResponse response,
            String mensaje
    ) throws ServletException, IOException {

        response.setStatus(
                HttpServletResponse.SC_BAD_REQUEST
        );

        request.setAttribute(
                "error",
                mensaje
        );

        mostrarVista(
                request,
                response
        );
    }

    private void procesarErrorServidor(
            HttpServletRequest request,
            HttpServletResponse response,
            String mensaje
    ) throws ServletException, IOException {

        response.setStatus(
                HttpServletResponse
                        .SC_INTERNAL_SERVER_ERROR
        );

        request.setAttribute(
                "error",
                mensaje
        );

        mostrarVista(
                request,
                response
        );
    }

    private void mostrarVista(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            List<Medico> medicos =
                    medicoService.listarActivos();

            List<Especialidad> especialidades =
                    especialidadService.listarActivas();

            List<Consultorio> consultorios =
                    consultorioService.listarTodos();

            Map<Long, Medico> medicosPorId =
                    new LinkedHashMap<>();

            Map<Integer, Especialidad>
                    especialidadesPorId =
                    new LinkedHashMap<>();

            Map<Long, Consultorio>
                    consultoriosPorId =
                    new LinkedHashMap<>();

            Map<Long, List<MedicoEspecialidad>>
                    especialidadesPorMedico =
                    new LinkedHashMap<>();

            for (Medico medico : medicos) {
                if (medico.getIdMedico() == null) {
                    continue;
                }

                medicosPorId.put(
                        medico.getIdMedico(),
                        medico
                );

                especialidadesPorMedico.put(
                        medico.getIdMedico(),
                        medicoEspecialidadService
                                .listarActivasPorMedico(
                                        medico.getIdMedico()
                                )
                );
            }

            for (Especialidad especialidad : especialidades) {
                if (especialidad.getIdEspecialidad()
                        != null) {
                    especialidadesPorId.put(
                            especialidad.getIdEspecialidad(),
                            especialidad
                    );
                }
            }

            List<Consultorio> consultoriosDisponibles =
                    consultorios.stream()
                            .filter(
                                    consultorio ->
                                            consultorio.getEstado()
                                                    == EstadoConsultorio
                                                    .DISPONIBLE
                            )
                            .toList();

            for (Consultorio consultorio : consultorios) {
                if (consultorio.getIdConsultorio()
                        != null) {
                    consultoriosPorId.put(
                            consultorio.getIdConsultorio(),
                            consultorio
                    );
                }
            }

            request.setAttribute(
                    "horarios",
                    horarioMedicoService.listarTodos()
            );
            request.setAttribute(
                    "medicos",
                    medicos
            );
            request.setAttribute(
                    "medicosPorId",
                    medicosPorId
            );
            request.setAttribute(
                    "especialidadesPorId",
                    especialidadesPorId
            );
            request.setAttribute(
                    "especialidadesPorMedico",
                    especialidadesPorMedico
            );
            request.setAttribute(
                    "consultoriosDisponibles",
                    consultoriosDisponibles
            );
            request.setAttribute(
                    "consultoriosPorId",
                    consultoriosPorId
            );

        } catch (SQLException error) {
            log(
                    "Error al cargar la administración de horarios.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar los horarios médicos."
                );
            }

            request.setAttribute(
                    "horarios",
                    List.of()
            );
            request.setAttribute(
                    "medicos",
                    List.of()
            );
            request.setAttribute(
                    "medicosPorId",
                    Map.of()
            );
            request.setAttribute(
                    "especialidadesPorId",
                    Map.of()
            );
            request.setAttribute(
                    "especialidadesPorMedico",
                    Map.of()
            );
            request.setAttribute(
                    "consultoriosDisponibles",
                    List.of()
            );
            request.setAttribute(
                    "consultoriosPorId",
                    Map.of()
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }

        request.getRequestDispatcher(
                VISTA_HORARIOS
        ).forward(
                request,
                response
        );
    }

    private static String[] separarMedicoEspecialidad(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return new String[]{
                    null,
                    null
            };
        }

        String[] partes =
                valor.strip()
                        .split(
                                ":",
                                -1
                        );

        if (partes.length != 2) {
            return new String[]{
                    null,
                    null
            };
        }

        return partes;
    }

    private static long convertirIdHorario(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    "El horario seleccionado no es válido."
            );
        }

        try {
            long idHorario =
                    Long.parseLong(
                            valor.strip()
                    );

            if (idHorario <= 0) {
                throw new NumberFormatException();
            }

            return idHorario;

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "El horario seleccionado no es válido.",
                    error
            );
        }
    }

    private static void conservarFormulario(
            HttpServletRequest request,
            String medicoEspecialidad,
            String idConsultorio,
            String diaSemana,
            String horaInicio,
            String horaFin,
            String duracionTurnoMinutos,
            String fechaDesde,
            String fechaHasta
    ) {

        request.setAttribute(
                "medicoEspecialidadSeleccionada",
                medicoEspecialidad
        );
        request.setAttribute(
                "idConsultorioSeleccionado",
                idConsultorio
        );
        request.setAttribute(
                "diaSemanaSeleccionado",
                diaSemana
        );
        request.setAttribute(
                "horaInicio",
                horaInicio
        );
        request.setAttribute(
                "horaFin",
                horaFin
        );
        request.setAttribute(
                "duracionTurnoMinutos",
                duracionTurnoMinutos
        );
        request.setAttribute(
                "fechaDesde",
                fechaDesde
        );
        request.setAttribute(
                "fechaHasta",
                fechaHasta
        );
    }

    private static void configurarRespuestaSinCache(
            HttpServletResponse response
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, "
                        + "must-revalidate, max-age=0"
        );
        response.setHeader(
                "Pragma",
                "no-cache"
        );
        response.setDateHeader(
                "Expires",
                0
        );
    }
}
