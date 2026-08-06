package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.config.ConexionBD;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/conexion-bd")
public class ConexionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String consulta = """
                SELECT
                    current_database() AS base_datos,
                    current_user AS usuario
                """;

        try (
                Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia =
                        conexion.prepareStatement(consulta);
                ResultSet resultado = sentencia.executeQuery();
                PrintWriter salida = response.getWriter()
        ) {

            if (resultado.next()) {
                salida.println("<h1>Conexión exitosa</h1>");
                salida.println("<p>Java se conectó correctamente con PostgreSQL.</p>");
                salida.println("<p><strong>Base de datos:</strong> "
                        + resultado.getString("base_datos") + "</p>");
                salida.println("<p><strong>Usuario:</strong> "
                        + resultado.getString("usuario") + "</p>");
            }

        } catch (SQLException error) {

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            try (PrintWriter salida = response.getWriter()) {
                salida.println("<h1>Error de conexión</h1>");
                salida.println("<p>" + error.getMessage() + "</p>");
            }
        }
    }
}