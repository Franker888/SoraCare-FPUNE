package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.PacienteService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/pacientes/detalle")
public class DetallePacienteServlet extends HttpServlet {

    private static final String VISTA_DETALLE =
            "/WEB-INF/views/paciente/detalle.jsp";

    private final PacienteService pacienteService =
            new PacienteService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String idTexto =
                request.getParameter("id");

        if (idTexto == null || idTexto.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Debe indicar el ID del paciente."
            );

            return;
        }

        long idPaciente;

        try {
            idPaciente =
                    Long.parseLong(
                            idTexto.strip()
                    );

        } catch (NumberFormatException error) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "El ID del paciente no es válido."
            );

            return;
        }

        if (idPaciente <= 0) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "El ID del paciente debe ser mayor que cero."
            );

            return;
        }

        try {
            Optional<Paciente> pacienteEncontrado =
                    pacienteService.buscarPorId(
                            idPaciente
                    );

            if (pacienteEncontrado.isEmpty()) {

                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "El paciente solicitado no existe."
                );

                return;
            }

            request.setAttribute(
                    "paciente",
                    pacienteEncontrado.get()
            );

            request.getRequestDispatcher(VISTA_DETALLE)
                    .forward(request, response);

        } catch (SQLException error) {

            getServletContext().log(
                    "Error al consultar el detalle del paciente",
                    error
            );

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No se pudo consultar al paciente."
            );
        }
    }
}