package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dao.PersonaDAO;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

@WebServlet("/prueba-persona-dao")
public class PersonaDAOServlet extends HttpServlet {

    private final PersonaDAO personaDAO = new PersonaDAO();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        String cedulaTexto = request.getParameter("cedula");

        if (cedulaTexto == null || cedulaTexto.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            try (PrintWriter salida = response.getWriter()) {
                salida.println("<h1>Falta la cédula</h1>");
                salida.println(
                        "<p>Agregá la cédula en la dirección, por ejemplo: "
                                + "?cedula=1234567</p>"
                );
            }

            return;
        }

        try {
            long cedula = Long.parseLong(cedulaTexto);
            boolean existe = personaDAO.existeCedula(cedula);

            try (PrintWriter salida = response.getWriter()) {
                salida.println("<h1>Prueba de PersonaDAO</h1>");
                salida.println("<p>Cédula consultada: " + cedula + "</p>");
                salida.println(
                        "<p>¿Existe en PostgreSQL?: <strong>"
                                + (existe ? "Sí" : "No")
                                + "</strong></p>"
                );
            }

        } catch (NumberFormatException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            response.getWriter().println(
                    "<h1>La cédula debe contener solamente números</h1>"
            );

        } catch (SQLException error) {
            getServletContext().log(
                    "Error consultando la cédula mediante PersonaDAO",
                    error
            );

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().println(
                    "<h1>No se pudo consultar la base de datos</h1>"
            );
        }
    }
}