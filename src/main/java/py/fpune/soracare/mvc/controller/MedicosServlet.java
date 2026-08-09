package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroMedicoDTO;
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

    @Override
    public void init() {

        medicoService =
                new MedicoService();
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

        conservarDatosFormulario(
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
                        "Error de base de datos al registrar "
                                + "el médico.",
                        error
                );

                response.setStatus(
                        HttpServletResponse
                                .SC_INTERNAL_SERVER_ERROR
                );

                request.setAttribute(
                        "error",
                        "No fue posible registrar el médico. "
                                + "Intentá nuevamente."
                );
            }

            mostrarVista(
                    request,
                    response
            );
        }
    }

    private void mostrarVista(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

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

        request.getRequestDispatcher(
                VISTA_MEDICOS
        ).forward(
                request,
                response
        );
    }

    private static boolean esErrorDeValidacion(
            SQLException error
    ) {

        return error.getSQLState() == null;
    }

    private static void conservarDatosFormulario(
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