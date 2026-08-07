package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroConsultorioDTO;
import py.fpune.soracare.service.ConsultorioService;
import py.fpune.soracare.service.SedeService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/consultorios")
public class ConsultoriosServlet extends HttpServlet {

    private static final String VISTA_CONSULTORIOS =
            "/WEB-INF/views/admin/consultorios.jsp";

    private static final String RUTA_CONSULTORIOS =
            "/admin/consultorios";

    private ConsultorioService consultorioService;
    private SedeService sedeService;

    @Override
    public void init() {
        consultorioService =
                new ConsultorioService();

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

        if ("registrado".equals(
                resultado
        )) {
            request.setAttribute(
                    "mensajeExito",
                    "El consultorio fue registrado correctamente."
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

        String idSede =
                request.getParameter(
                        "idSede"
                );

        String codigo =
                request.getParameter(
                        "codigo"
                );

        String nombre =
                request.getParameter(
                        "nombre"
                );

        String piso =
                request.getParameter(
                        "piso"
                );

        String ubicacionReferencia =
                request.getParameter(
                        "ubicacionReferencia"
                );

        String tipo =
                request.getParameter(
                        "tipo"
                );

        String capacidad =
                request.getParameter(
                        "capacidad"
                );

        String accesible =
                request.getParameter(
                        "accesible"
                );

        conservarDatosFormulario(
                request,
                idSede,
                codigo,
                nombre,
                piso,
                ubicacionReferencia,
                tipo,
                capacidad,
                accesible
        );

        RegistroConsultorioDTO datosRegistro =
                new RegistroConsultorioDTO(
                        idSede,
                        codigo,
                        nombre,
                        piso,
                        ubicacionReferencia,
                        tipo,
                        capacidad,
                        accesible
                );

        try {
            consultorioService.registrar(
                    datosRegistro
            );

            response.sendRedirect(
                    request.getContextPath()
                            + RUTA_CONSULTORIOS
                            + "?resultado=registrado"
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
                            + "el consultorio.",
                    error
            );

            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );

            request.setAttribute(
                    "error",
                    "No fue posible registrar el consultorio. "
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
                    "consultorios",
                    consultorioService.listarTodos()
            );

            request.setAttribute(
                    "sedes",
                    sedeService.listarActivas()
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar la administración "
                            + "de consultorios.",
                    error
            );

            if (request.getAttribute(
                    "error"
            ) == null) {

                request.setAttribute(
                        "error",
                        "No fue posible cargar "
                                + "los consultorios y las sedes."
                );
            }

            request.setAttribute(
                    "consultorios",
                    List.of()
            );

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
                VISTA_CONSULTORIOS
        ).forward(
                request,
                response
        );
    }

    private static void conservarDatosFormulario(
            HttpServletRequest request,
            String idSede,
            String codigo,
            String nombre,
            String piso,
            String ubicacionReferencia,
            String tipo,
            String capacidad,
            String accesible
    ) {
        request.setAttribute(
                "idSede",
                idSede
        );

        request.setAttribute(
                "codigo",
                codigo
        );

        request.setAttribute(
                "nombre",
                nombre
        );

        request.setAttribute(
                "piso",
                piso
        );

        request.setAttribute(
                "ubicacionReferencia",
                ubicacionReferencia
        );

        request.setAttribute(
                "tipo",
                tipo
        );

        request.setAttribute(
                "capacidad",
                capacidad
        );

        request.setAttribute(
                "accesible",
                accesible
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