package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.EspecialidadService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/especialidades")
public class CatalogoEspecialidadesServlet extends HttpServlet {

    private static final String VISTA_CATALOGO =
            "/WEB-INF/views/publico/especialidades.jsp";

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

        try {
            request.setAttribute(
                    "especialidades",
                    especialidadService.listarActivas()
            );

        } catch (SQLException error) {

            log(
                    "Error al cargar el catálogo público "
                            + "de especialidades.",
                    error
            );

            request.setAttribute(
                    "especialidades",
                    List.of()
            );

            request.setAttribute(
                    "error",
                    "No fue posible cargar las especialidades "
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