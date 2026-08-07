package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroSedeDTO;
import py.fpune.soracare.service.SedeService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/sedes")
public class SedesServlet extends HttpServlet {

    private static final String VISTA_SEDES =
            "/WEB-INF/views/admin/sedes.jsp";

    private static final String RUTA_SEDES =
            "/admin/sedes";

    private SedeService sedeService;

    @Override
    public void init() {
        sedeService =
                new SedeService();
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
                    "La sede fue registrada correctamente."
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

        String codigo =
                request.getParameter(
                        "codigo"
                );

        String nombre =
                request.getParameter(
                        "nombre"
                );

        String direccion =
                request.getParameter(
                        "direccion"
                );

        String ciudad =
                request.getParameter(
                        "ciudad"
                );

        String departamento =
                request.getParameter(
                        "departamento"
                );

        String telefono =
                request.getParameter(
                        "telefono"
                );

        String correo =
                request.getParameter(
                        "correo"
                );

        conservarDatosFormulario(
                request,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo
        );

        RegistroSedeDTO datosRegistro =
                new RegistroSedeDTO(
                        codigo,
                        nombre,
                        direccion,
                        ciudad,
                        departamento,
                        telefono,
                        correo
                );

        try {
            sedeService.registrar(
                    datosRegistro
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_SEDES
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
                    "Error de base de datos al registrar la sede.",
                    error
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );

            request.setAttribute(
                    "error",
                    "No fue posible registrar la sede. "
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
                    "sedes",
                    sedeService.listarTodas()
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar el listado de sedes.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar las sedes."
                );
            }

            request.setAttribute(
                    "sedes",
                    List.of()
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }

        request.getRequestDispatcher(
                VISTA_SEDES
        ).forward(
                request,
                response
        );
    }

    private static void conservarDatosFormulario(
            HttpServletRequest request,
            String codigo,
            String nombre,
            String direccion,
            String ciudad,
            String departamento,
            String telefono,
            String correo
    ) {
        request.setAttribute(
                "codigo",
                codigo
        );

        request.setAttribute(
                "nombre",
                nombre
        );

        request.setAttribute(
                "direccion",
                direccion
        );

        request.setAttribute(
                "ciudad",
                ciudad
        );

        request.setAttribute(
                "departamento",
                departamento
        );

        request.setAttribute(
                "telefono",
                telefono
        );

        request.setAttribute(
                "correo",
                correo
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