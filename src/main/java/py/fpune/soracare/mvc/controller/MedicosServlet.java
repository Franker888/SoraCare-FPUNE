package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.AsignacionMedicoEspecialidadDTO;
import py.fpune.soracare.dto.RegistroMedicoDTO;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/medicos")
public class MedicosServlet extends HttpServlet {

    private static final String VISTA_MEDICOS =
            "/WEB-INF/views/admin/medicos.jsp";

    private static final String RUTA_MEDICOS =
            "/admin/medicos";

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

        String resultado =
                request.getParameter(
                        "resultado"
                );

        if ("registrado".equals(
                resultado
        )) {

            request.setAttribute(
                    "mensajeExito",
                    "El médico fue registrado correctamente."
            );

        } else if ("especialidad-asignada".equals(
                resultado
        )) {

            request.setAttribute(
                    "mensajeExito",
                    "La especialidad fue asignada "
                            + "al médico correctamente."
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

        if ("asignarEspecialidad".equals(
                accion
        )) {

            procesarAsignacionEspecialidad(
                    request,
                    response
            );

            return;
        }

        procesarRegistroMedico(
                request,
                response
        );
    }

    private void procesarRegistroMedico(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String nombres =
                request.getParameter(
                        "nombres"
                );

        String apellidos =
                request.getParameter(
                        "apellidos"
                );

        String cedula =
                request.getParameter(
                        "cedula"
                );

        String fechaNacimiento =
                request.getParameter(
                        "fechaNacimiento"
                );

        String telefono =
                request.getParameter(
                        "telefono"
                );

        String correo =
                request.getParameter(
                        "correo"
                );

        String direccion =
                request.getParameter(
                        "direccion"
                );

        String matriculaProfesional =
                request.getParameter(
                        "matriculaProfesional"
                );

        String fechaIngreso =
                request.getParameter(
                        "fechaIngreso"
                );

        String biografia =
                request.getParameter(
                        "biografia"
                );

        conservarDatosRegistroMedico(
                request,
                nombres,
                apellidos,
                cedula,
                fechaNacimiento,
                telefono,
                correo,
                direccion,
                matriculaProfesional,
                fechaIngreso,
                biografia
        );

        RegistroMedicoDTO datosRegistro =
                new RegistroMedicoDTO(
                        nombres,
                        apellidos,
                        cedula,
                        fechaNacimiento,
                        telefono,
                        correo,
                        direccion,
                        matriculaProfesional,
                        fechaIngreso,
                        biografia
                );

        try {
            medicoService.registrar(
                    datosRegistro
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_MEDICOS
                            + "?resultado=registrado"
            );

        } catch (SQLException error) {

            procesarErrorOperacion(
                    request,
                    response,
                    error,
                    "Error de base de datos al registrar "
                            + "el médico.",
                    "No fue posible registrar el médico. "
                            + "Intentá nuevamente."
            );
        }
    }

    private void procesarAsignacionEspecialidad(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String idMedico =
                request.getParameter(
                        "idMedico"
                );

        String idEspecialidad =
                request.getParameter(
                        "idEspecialidad"
                );

        String principal =
                request.getParameter(
                        "principal"
                );

        String fechaDesde =
                request.getParameter(
                        "fechaDesde"
                );

        conservarDatosAsignacion(
                request,
                idMedico,
                idEspecialidad,
                principal,
                fechaDesde
        );

        AsignacionMedicoEspecialidadDTO datosAsignacion =
                new AsignacionMedicoEspecialidadDTO(
                        idMedico,
                        idEspecialidad,
                        principal,
                        fechaDesde
                );

        try {
            medicoEspecialidadService.asignar(
                    datosAsignacion
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_MEDICOS
                            + "?resultado=especialidad-asignada"
            );

        } catch (SQLException error) {

            procesarErrorOperacion(
                    request,
                    response,
                    error,
                    "Error de base de datos al asignar "
                            + "la especialidad al médico.",
                    "No fue posible asignar la especialidad. "
                            + "Intentá nuevamente."
            );
        }
    }

    private void procesarErrorOperacion(
            HttpServletRequest request,
            HttpServletResponse response,
            SQLException error,
            String mensajeLog,
            String mensajeGenerico
    ) throws ServletException, IOException {

        if (esErrorDeValidacion(
                error
        )) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            request.setAttribute(
                    "error",
                    error.getMessage()
            );

        } else {

            log(
                    mensajeLog,
                    error
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );

            request.setAttribute(
                    "error",
                    mensajeGenerico
            );
        }

        mostrarVista(
                request,
                response
        );
    }

    private void mostrarVista(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        cargarMedicos(
                request,
                response
        );

        cargarEspecialidadesActivas(
                request,
                response
        );

        request.getRequestDispatcher(
                VISTA_MEDICOS
        ).forward(
                request,
                response
        );
    }

    private void cargarMedicos(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            request.setAttribute(
                    "medicos",
                    medicoService.listarTodos()
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar el listado "
                            + "de médicos.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {

                request.setAttribute(
                        "error",
                        "No fue posible cargar "
                                + "los médicos."
                );
            }

            request.setAttribute(
                    "medicos",
                    List.of()
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }
    }

    private void cargarEspecialidadesActivas(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            request.setAttribute(
                    "especialidadesActivas",
                    especialidadService.listarActivas()
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar las especialidades "
                            + "activas.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {

                request.setAttribute(
                        "error",
                        "No fue posible cargar "
                                + "las especialidades disponibles."
                );
            }

            request.setAttribute(
                    "especialidadesActivas",
                    List.of()
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }
    }

    private static boolean esErrorDeValidacion(
            SQLException error
    ) {

        return error.getSQLState() == null;
    }

    private static void conservarDatosRegistroMedico(
            HttpServletRequest request,
            String nombres,
            String apellidos,
            String cedula,
            String fechaNacimiento,
            String telefono,
            String correo,
            String direccion,
            String matriculaProfesional,
            String fechaIngreso,
            String biografia
    ) {

        request.setAttribute(
                "nombres",
                nombres
        );

        request.setAttribute(
                "apellidos",
                apellidos
        );

        request.setAttribute(
                "cedula",
                cedula
        );

        request.setAttribute(
                "fechaNacimiento",
                fechaNacimiento
        );

        request.setAttribute(
                "telefono",
                telefono
        );

        request.setAttribute(
                "correo",
                correo
        );

        request.setAttribute(
                "direccion",
                direccion
        );

        request.setAttribute(
                "matriculaProfesional",
                matriculaProfesional
        );

        request.setAttribute(
                "fechaIngreso",
                fechaIngreso
        );

        request.setAttribute(
                "biografia",
                biografia
        );
    }

    private static void conservarDatosAsignacion(
            HttpServletRequest request,
            String idMedico,
            String idEspecialidad,
            String principal,
            String fechaDesde
    ) {

        request.setAttribute(
                "asignacionIdMedico",
                idMedico
        );

        request.setAttribute(
                "asignacionIdEspecialidad",
                idEspecialidad
        );

        request.setAttribute(
                "asignacionPrincipal",
                principal
        );

        request.setAttribute(
                "asignacionFechaDesde",
                fechaDesde
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