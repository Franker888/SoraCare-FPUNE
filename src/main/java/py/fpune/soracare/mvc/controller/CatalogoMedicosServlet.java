package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.HorarioMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.HorarioMedicoService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/medicos")
public class CatalogoMedicosServlet extends HttpServlet {

    private static final String VISTA_CATALOGO =
            "/WEB-INF/views/publico/medicos.jsp";

    private MedicoService medicoService;

    private EspecialidadService especialidadService;

    private MedicoEspecialidadService
            medicoEspecialidadService;

    private HorarioMedicoService horarioMedicoService;

    @Override
    public void init() {

        medicoService =
                new MedicoService();

        especialidadService =
                new EspecialidadService();

        medicoEspecialidadService =
                new MedicoEspecialidadService();

        horarioMedicoService =
                new HorarioMedicoService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        try {
            List<Medico> medicos =
                    medicoService.listarActivos();

            Map<Integer, Especialidad>
                    especialidadesPorId =
                    cargarEspecialidadesActivasPorId();

            Map<Long, List<MedicoEspecialidad>>
                    especialidadesPorMedico =
                    cargarEspecialidadesPorMedico(
                            medicos,
                            especialidadesPorId
                    );

            Map<Long, List<HorarioMedico>>
                    horariosPorMedico =
                    cargarHorariosPublicosPorMedico(
                            medicos
                    );

            request.setAttribute(
                    "medicos",
                    medicos
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
                    "horariosPorMedico",
                    horariosPorMedico
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar el catálogo público "
                            + "de médicos.",
                    error
            );

            request.setAttribute(
                    "medicos",
                    List.of()
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
                    "horariosPorMedico",
                    Map.of()
            );

            request.setAttribute(
                    "error",
                    "No fue posible cargar los médicos "
                            + "en este momento."
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }

        request.getRequestDispatcher(
                VISTA_CATALOGO
        ).forward(
                request,
                response
        );
    }

    private Map<Integer, Especialidad>
    cargarEspecialidadesActivasPorId()
            throws SQLException {

        Map<Integer, Especialidad>
                especialidadesPorId =
                new LinkedHashMap<>();

        List<Especialidad> especialidades =
                especialidadService.listarActivas();

        for (
                Especialidad especialidad
                : especialidades
        ) {

            if (especialidad.getIdEspecialidad()
                    == null) {

                continue;
            }

            especialidadesPorId.put(
                    especialidad.getIdEspecialidad(),
                    especialidad
            );
        }

        return especialidadesPorId;
    }

    private Map<Long, List<MedicoEspecialidad>>
    cargarEspecialidadesPorMedico(
            List<Medico> medicos,
            Map<Integer, Especialidad>
                    especialidadesPorId
    ) throws SQLException {

        Map<Long, List<MedicoEspecialidad>>
                especialidadesPorMedico =
                new LinkedHashMap<>();

        for (
                Medico medico
                : medicos
        ) {

            if (medico.getIdMedico() == null) {
                continue;
            }

            List<MedicoEspecialidad> relaciones =
                    medicoEspecialidadService
                            .listarActivasPorMedico(
                                    medico.getIdMedico()
                            );

            List<MedicoEspecialidad>
                    relacionesConEspecialidadActiva =
                    new ArrayList<>();

            for (
                    MedicoEspecialidad relacion
                    : relaciones
            ) {

                if (especialidadesPorId.containsKey(
                        relacion.getIdEspecialidad()
                )) {
                    relacionesConEspecialidadActiva.add(
                            relacion
                    );
                }
            }

            especialidadesPorMedico.put(
                    medico.getIdMedico(),
                    relacionesConEspecialidadActiva
            );
        }

        return especialidadesPorMedico;
    }

    private Map<Long, List<HorarioMedico>>
    cargarHorariosPublicosPorMedico(
            List<Medico> medicos
    ) throws SQLException {

        Map<Long, List<HorarioMedico>>
                horariosPorMedico =
                new LinkedHashMap<>();

        for (Medico medico : medicos) {
            if (medico.getIdMedico() != null) {
                horariosPorMedico.put(
                        medico.getIdMedico(),
                        new ArrayList<>()
                );
            }
        }

        LocalDate hoy = LocalDate.now();

        for (
                HorarioMedico horario
                : horarioMedicoService.listarTodos()
        ) {

            if (!horario.isActivo()
                    || horario.getIdMedico() == null
                    || (horario.getFechaHasta() != null
                    && horario.getFechaHasta().isBefore(hoy))) {
                continue;
            }

            List<HorarioMedico> horariosDelMedico =
                    horariosPorMedico.get(
                            horario.getIdMedico()
                    );

            if (horariosDelMedico != null) {
                horariosDelMedico.add(horario);
            }
        }

        return horariosPorMedico;
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
