package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
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

    @Override
    public void init() {

        medicoService =
                new MedicoService();

        especialidadService =
                new EspecialidadService();

        medicoEspecialidadService =
                new MedicoEspecialidadService();
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
