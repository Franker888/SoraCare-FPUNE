package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.AsignacionMedicoEspecialidadDTO;
import py.fpune.soracare.dto.RegistroMedicoDTO;
import py.fpune.soracare.dto.SolicitudCreacionUsuarioDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;
import py.fpune.soracare.service.UsuarioService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private UsuarioService usuarioService;

    @Override
    public void init() {

        medicoService =
                new MedicoService();

        especialidadService =
                new EspecialidadService();

        medicoEspecialidadService =
                new MedicoEspecialidadService();

        usuarioService =
                new UsuarioService();
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

        } else if ("cuenta-creada".equals(
                resultado
        )) {

            request.setAttribute(
                    "mensajeExito",
                    "La cuenta del médico fue creada correctamente. "
                            + "Deberá cambiar la contraseña en su primer ingreso."
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

        if ("crearCuentaMedico".equals(
                accion
        )) {

            procesarCreacionCuentaMedico(
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

    private void procesarCreacionCuentaMedico(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String idMedico = request.getParameter("cuentaIdMedico");
        String nombreUsuario = request.getParameter("cuentaNombreUsuario");
        String contrasena = request.getParameter("cuentaContrasena");
        String confirmacion = request.getParameter("cuentaConfirmacion");

        request.setAttribute("cuentaIdMedico", idMedico);
        request.setAttribute("cuentaNombreUsuario", nombreUsuario);

        try {
            long idMedicoNumerico = convertirIdMedico(idMedico);
            Medico medico = medicoService.buscarPorId(idMedicoNumerico)
                    .orElseThrow(() -> new SQLException(
                            "El médico seleccionado no existe."));
            if (medico.getEstado() != EstadoMedico.ACTIVO) {
                throw new SQLException(
                        "La cuenta solamente puede crearse para un médico activo."
                );
            }

            SolicitudCreacionUsuarioDTO solicitud =
                    new SolicitudCreacionUsuarioDTO(
                            String.valueOf(medico.getId()),
                            nombreUsuario,
                            contrasena,
                            confirmacion,
                            "MEDICO",
                            true
                    );

            usuarioService.crearUsuario(
                    solicitud,
                    usuarioAutenticado(request).getIdUsuario()
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_MEDICOS
                            + "?resultado=cuenta-creada"
            );

        } catch (SQLException error) {
            procesarErrorOperacion(
                    request,
                    response,
                    error,
                    "Error de base de datos al crear la cuenta del médico.",
                    "No fue posible crear la cuenta del médico. Intentá nuevamente."
            );
        }
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

        List<Medico> medicos =
                cargarMedicos(
                        request,
                        response
                );

        cargarEspecialidadesActivas(
                request,
                response
        );

        cargarEspecialidadesAsignadas(
                request,
                response,
                medicos
        );

        cargarCuentasMedicos(
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

    private List<Medico> cargarMedicos(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            List<Medico> medicos =
                    medicoService.listarTodos();

            request.setAttribute(
                    "medicos",
                    medicos
            );

            return medicos;

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

            return List.of();
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

    private void cargarEspecialidadesAsignadas(
            HttpServletRequest request,
            HttpServletResponse response,
            List<Medico> medicos
    ) {

        Map<Long, List<MedicoEspecialidad>>
                especialidadesPorMedico =
                new LinkedHashMap<>();

        Map<Integer, Especialidad>
                especialidadesPorId =
                new LinkedHashMap<>();

        try {
            List<Especialidad> todasEspecialidades =
                    especialidadService.listarTodas();

            for (
                    Especialidad especialidad
                    : todasEspecialidades
            ) {

                if (especialidad.getIdEspecialidad()
                        != null) {

                    especialidadesPorId.put(
                            especialidad
                                    .getIdEspecialidad(),
                            especialidad
                    );
                }
            }

            for (
                    Medico medico
                    : medicos
            ) {

                if (medico.getIdMedico()
                        == null) {

                    continue;
                }

                List<MedicoEspecialidad> relaciones =
                        medicoEspecialidadService
                                .listarActivasPorMedico(
                                        medico.getIdMedico()
                                );

                especialidadesPorMedico.put(
                        medico.getIdMedico(),
                        relaciones
                );
            }

            request.setAttribute(
                    "especialidadesPorMedico",
                    especialidadesPorMedico
            );

            request.setAttribute(
                    "especialidadesPorId",
                    especialidadesPorId
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar las especialidades "
                            + "asignadas a los médicos.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {

                request.setAttribute(
                        "error",
                        "No fue posible cargar "
                                + "las especialidades asignadas."
                );
            }

            request.setAttribute(
                    "especialidadesPorMedico",
                    Map.of()
            );

            request.setAttribute(
                    "especialidadesPorId",
                    Map.of()
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }
    }

    private void cargarCuentasMedicos(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            Set<Long> idsPersonasConCuenta =
                    usuarioService.listarIdsPersonasConCuenta();
            request.setAttribute(
                    "idsPersonasConCuenta",
                    idsPersonasConCuenta
            );
        } catch (SQLException error) {
            log("Error al cargar las cuentas de médicos.", error);
            if (request.getAttribute("error") == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar el estado de las cuentas médicas."
                );
            }
            request.setAttribute("idsPersonasConCuenta", Set.of());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static long convertirIdMedico(String valor) throws SQLException {
        try {
            long idMedico = Long.parseLong(valor == null ? "" : valor.strip());
            if (idMedico <= 0) {
                throw new NumberFormatException();
            }
            return idMedico;
        } catch (NumberFormatException error) {
            throw new SQLException("Seleccioná un médico válido.", error);
        }
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(
            HttpServletRequest request
    ) throws SQLException {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new SQLException("No existe una sesión administrativa válida.");
        }
        return autenticado;
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
