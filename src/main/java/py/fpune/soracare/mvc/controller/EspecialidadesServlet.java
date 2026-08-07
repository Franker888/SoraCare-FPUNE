package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroEspecialidadDTO;
import py.fpune.soracare.service.EspecialidadService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/especialidades")
public class EspecialidadesServlet extends HttpServlet {

    private static final String VISTA_ESPECIALIDADES =
            "/WEB-INF/views/admin/especialidades.jsp";

    private static final String RUTA_ESPECIALIDADES =
            "/admin/especialidades";

    private EspecialidadService especialidadService;

    @Override
    public void init() {
        especialidadService =
                new EspecialidadService();
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

        if ("registrada".equals(
                resultado
        )) {
            request.setAttribute(
                    "mensajeExito",
                    "La especialidad fue registrada correctamente."
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

        String nombre =
                request.getParameter(
                        "nombre"
                );

        String descripcion =
                request.getParameter(
                        "descripcion"
                );

        String duracionConsultaMinutos =
                request.getParameter(
                        "duracionConsultaMinutos"
                );

        conservarDatosFormulario(
                request,
                nombre,
                descripcion,
                duracionConsultaMinutos
        );

        RegistroEspecialidadDTO datosRegistro =
                new RegistroEspecialidadDTO(
                        nombre,
                        descripcion,
                        duracionConsultaMinutos
                );

        try {
            especialidadService.registrar(
                    datosRegistro
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_ESPECIALIDADES
                            + "?resultado=registrada"
            );

        } catch (IllegalArgumentException error) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            request.setAttribute(
                    "error",
                    error.getMessage()
            );

            mostrarVista(
                    request,
                    response
            );

        } catch (SQLException error) {

            log(
                    "Error de base de datos al registrar "
                            + "la especialidad.",
                    error
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );

            request.setAttribute(
                    "error",
                    "No fue posible registrar la especialidad. "
                            + "Intentá nuevamente."
            );

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
                    "especialidades",
                    especialidadService.listarTodas()
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar el listado "
                            + "de especialidades.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar "
                                + "las especialidades."
                );
            }

            request.setAttribute(
                    "especialidades",
                    List.of()
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }

        request.getRequestDispatcher(
                VISTA_ESPECIALIDADES
        ).forward(
                request,
                response
        );
    }

    private static void conservarDatosFormulario(
            HttpServletRequest request,
            String nombre,
            String descripcion,
            String duracionConsultaMinutos
    ) {
        request.setAttribute(
                "nombre",
                nombre
        );

        request.setAttribute(
                "descripcion",
                descripcion
        );

        request.setAttribute(
                "duracionConsultaMinutos",
                duracionConsultaMinutos
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