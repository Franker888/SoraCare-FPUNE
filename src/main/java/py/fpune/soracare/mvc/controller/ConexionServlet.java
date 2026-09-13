package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.config.ConexionBD;
import java.io.IOException;
import java.sql.SQLException;

/** Healthcheck público: solo disponibilidad, nunca información interna. */
@WebServlet("/conexion-bd")
public class ConexionServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        boolean disponible;
        try {
            disponible = baseDisponible();
        } catch (SQLException error) {
            disponible = false;
        }
        response.setStatus(disponible ? 200 : 503);
        response.getWriter().write(disponible ? "OK" : "NO_DISPONIBLE");
    }

    protected boolean baseDisponible() throws SQLException {
        try (var conexion = ConexionBD.obtenerConexion();
             var sentencia = conexion.prepareStatement("SELECT 1")) {
            sentencia.setQueryTimeout(3);
            try (var resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getInt(1) == 1;
            }
        }
    }
}
