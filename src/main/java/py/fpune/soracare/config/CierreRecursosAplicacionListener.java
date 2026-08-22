package py.fpune.soracare.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import py.fpune.soracare.service.DespachadorCorreoService;

import java.sql.Connection;
import java.sql.SQLException;

@WebListener
public class CierreRecursosAplicacionListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            evento.getServletContext().log("Pool de conexiones de SoraCare inicializado.");
        } catch (SQLException excepcion) {
            evento.getServletContext().log(
                    "No fue posible preparar la conexión a PostgreSQL durante el arranque.",
                    excepcion
            );
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        DespachadorCorreoService.cerrar();
        ConexionBD.cerrar();
    }
}
