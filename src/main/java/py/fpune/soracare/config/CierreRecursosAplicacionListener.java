package py.fpune.soracare.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import py.fpune.soracare.service.DespachadorCorreoService;
import py.fpune.soracare.service.SentryMonitoreoService;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

@WebListener
public class CierreRecursosAplicacionListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        new ConfiguracionDespliegue().configurarCookieSesion(
                evento.getServletContext()
        );
        SentryMonitoreoService.inicializar();
        if (SentryMonitoreoService.estaActivo()) {
            evento.getServletContext().log(
                    "Monitoreo técnico con Sentry inicializado."
            );
        } else {
            evento.getServletContext().log(
                    "Sentry no está configurado; SoraCare continuará sin monitoreo remoto."
            );
        }

        try {
            MigrateResult migracion = MigradorEsquemaBD.migrar();
            evento.getServletContext().log(
                    "Flyway validó el esquema de SoraCare; migraciones aplicadas: "
                            + migracion.migrationsExecuted + "."
            );
        } catch (SQLException | RuntimeException excepcion) {
            registrarErrorArranque(
                    "No fue posible preparar el esquema PostgreSQL.",
                    excepcion
            );
            evento.getServletContext().log(
                    "No fue posible preparar el esquema PostgreSQL.",
                    excepcion
            );
            throw new IllegalStateException(
                    "SoraCare no iniciará con un esquema sin validar.",
                    excepcion
            );
        }

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            evento.getServletContext().log("Pool de conexiones de SoraCare inicializado.");
            JpaContexto.inicializar();
            evento.getServletContext().log(
                    "Unidad de persistencia " + JpaContexto.UNIDAD_PERSISTENCIA
                            + " inicializada."
            );
        } catch (SQLException | RuntimeException excepcion) {
            registrarErrorArranque(
                    "No fue posible preparar PostgreSQL y JPA durante el arranque.",
                    excepcion
            );
            evento.getServletContext().log(
                    "No fue posible preparar PostgreSQL y JPA durante el arranque.",
                    excepcion
            );
            throw new IllegalStateException(
                    "SoraCare no iniciará sin PostgreSQL y JPA disponibles.",
                    excepcion
            );
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        cerrarSeguro(
                evento,
                "monitoreo técnico",
                SentryMonitoreoService::cerrar
        );
        cerrarSeguro(
                evento,
                "despachador de correo",
                DespachadorCorreoService::cerrar
        );
        cerrarSeguro(evento, "unidad de persistencia", JpaContexto::cerrar);
        cerrarSeguro(evento, "pool de conexiones", ConexionBD::cerrar);
        desregistrarControladoresJdbc(evento);
    }

    private static void cerrarSeguro(
            ServletContextEvent evento,
            String recurso,
            Runnable cierre
    ) {
        try {
            cierre.run();
        } catch (RuntimeException excepcion) {
            evento.getServletContext().log(
                    "No fue posible cerrar correctamente " + recurso + ".",
                    excepcion
            );
        }
    }

    private static void desregistrarControladoresJdbc(
            ServletContextEvent evento
    ) {
        ClassLoader cargadorAplicacion =
                CierreRecursosAplicacionListener.class.getClassLoader();
        Enumeration<Driver> controladores = DriverManager.getDrivers();

        while (controladores.hasMoreElements()) {
            Driver controlador = controladores.nextElement();
            if (controlador.getClass().getClassLoader() != cargadorAplicacion) {
                continue;
            }

            try {
                DriverManager.deregisterDriver(controlador);
            } catch (SQLException excepcion) {
                evento.getServletContext().log(
                        "No fue posible desregistrar el controlador JDBC "
                                + controlador.getClass().getName() + ".",
                        excepcion
                );
            }
        }
    }

    private static void registrarErrorArranque(
            String mensaje,
            Throwable excepcion
    ) {
        System.err.println(mensaje);
        excepcion.printStackTrace(System.err);
    }
}
