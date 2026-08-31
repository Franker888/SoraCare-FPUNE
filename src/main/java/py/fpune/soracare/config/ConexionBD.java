package py.fpune.soracare.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConexionBD {

    private static final String VARIABLE_URL = "SORACARE_DB_URL";
    private static final String VARIABLE_USUARIO = "SORACARE_DB_USER";
    private static final String VARIABLE_CONTRASENA = "SORACARE_DB_PASSWORD";

    private static final String URL_PREDETERMINADA =
            "jdbc:postgresql://127.0.0.1:5432/soracare_db"
                    + "?sslmode=disable&ApplicationName=SoraCare-FPUNE";

    private static final String USUARIO_PREDETERMINADO = "soracare_app";

    private static final int MAXIMO_CONEXIONES = 10;
    private static final long ESPERA_CONEXION_SEGUNDOS = 10;
    private static final Object MONITOR = new Object();
    private static final LinkedBlockingQueue<Connection> DISPONIBLES =
            new LinkedBlockingQueue<>();
    private static final Set<Connection> TODAS =
            ConcurrentHashMap.newKeySet();

    private static int conexionesCreadas;
    private static volatile boolean cerrado;

    private ConexionBD() {
        // Evita crear objetos de esta clase.
    }

    public static Connection obtenerConexion() throws SQLException {
        Configuracion configuracion = resolverConfiguracion(System::getenv);

        if (cerrado) {
            throw new SQLException("El conjunto de conexiones ya fue cerrado.");
        }

        long limite = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(ESPERA_CONEXION_SEGUNDOS);

        while (true) {
            Connection disponible = DISPONIBLES.poll();
            if (esUtil(disponible)) return envolver(disponible);
            if (disponible != null) descartar(disponible);

            boolean crear = false;
            synchronized (MONITOR) {
                if (!cerrado && conexionesCreadas < MAXIMO_CONEXIONES) {
                    conexionesCreadas++;
                    crear = true;
                }
            }

            if (crear) {
                try {
                    Connection nueva = crearConexion(configuracion);
                    TODAS.add(nueva);
                    return envolver(nueva);
                } catch (SQLException error) {
                    synchronized (MONITOR) {
                        conexionesCreadas--;
                    }
                    throw error;
                }
            }

            long restante = limite - System.nanoTime();
            if (restante <= 0) {
                throw new SQLException(
                        "No hay conexiones disponibles para atender la solicitud."
                );
            }

            try {
                disponible = DISPONIBLES.poll(restante, TimeUnit.NANOSECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new SQLException("La espera de conexión fue interrumpida.", error);
            }
            if (esUtil(disponible)) return envolver(disponible);
            if (disponible != null) descartar(disponible);
        }
    }

    public static void cerrar() {
        cerrado = true;
        DISPONIBLES.clear();
        for (Connection conexion : TODAS) cerrarFisicamente(conexion);
        TODAS.clear();
        synchronized (MONITOR) {
            conexionesCreadas = 0;
        }
    }

    public static int obtenerMaximoConexiones() {
        return MAXIMO_CONEXIONES;
    }

    public static int obtenerConexionesCreadas() {
        synchronized (MONITOR) {
            return conexionesCreadas;
        }
    }

    public static int obtenerConexionesDisponibles() {
        return DISPONIBLES.size();
    }

    static Configuracion resolverConfiguracion(
            Function<String, String> entorno
    ) throws SQLException {
        String url = valorOEntornoPredeterminado(
                entorno.apply(VARIABLE_URL),
                URL_PREDETERMINADA
        );
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new SQLException(
                    "SORACARE_DB_URL debe ser una dirección JDBC de PostgreSQL."
            );
        }

        String usuario = valorOEntornoPredeterminado(
                entorno.apply(VARIABLE_USUARIO),
                USUARIO_PREDETERMINADO
        );
        String contrasena = entorno.apply(VARIABLE_CONTRASENA);
        if (contrasena == null || contrasena.isBlank()) {
            throw new SQLException(
                    "No se configuró la variable SORACARE_DB_PASSWORD"
            );
        }
        return new Configuracion(url, usuario, contrasena);
    }

    private static String valorOEntornoPredeterminado(
            String valor,
            String predeterminado
    ) {
        return valor == null || valor.isBlank()
                ? predeterminado
                : valor.strip();
    }

    private static Connection crearConexion(
            Configuracion configuracion
    ) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException error) {
            throw new SQLException(
                    "No se pudo cargar el controlador JDBC de PostgreSQL", error
            );
        }
        return DriverManager.getConnection(
                configuracion.url(),
                configuracion.usuario(),
                configuracion.contrasena()
        );
    }

    private static boolean esUtil(Connection conexion) {
        if (conexion == null) return false;
        try {
            return !conexion.isClosed() && conexion.isValid(2);
        } catch (SQLException error) {
            return false;
        }
    }

    private static Connection envolver(Connection fisica) {
        AtomicBoolean devuelta = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    String nombre = metodo.getName();
                    if ("close".equals(nombre)) {
                        if (devuelta.compareAndSet(false, true)) devolver(fisica);
                        return null;
                    }
                    if ("isClosed".equals(nombre)) {
                        return devuelta.get() || fisica.isClosed();
                    }
                    if ("toString".equals(nombre)) {
                        return "ConexionSoraCare[disponible=" + !devuelta.get() + "]";
                    }
                    if ("hashCode".equals(nombre)) return System.identityHashCode(proxy);
                    if ("equals".equals(nombre)) return proxy == argumentos[0];
                    if (devuelta.get()) {
                        throw new SQLException("La conexión ya fue devuelta al conjunto.");
                    }
                    try {
                        return metodo.invoke(fisica, argumentos);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                }
        );
    }

    private static void devolver(Connection conexion) {
        if (cerrado) {
            descartar(conexion);
            return;
        }
        try {
            if (conexion.isClosed()) {
                descartar(conexion);
                return;
            }
            if (!conexion.getAutoCommit()) {
                conexion.rollback();
                conexion.setAutoCommit(true);
            }
            if (conexion.isReadOnly()) conexion.setReadOnly(false);
            conexion.clearWarnings();
            DISPONIBLES.offer(conexion);
        } catch (SQLException error) {
            descartar(conexion);
        }
    }

    private static void descartar(Connection conexion) {
        if (conexion == null) return;
        if (TODAS.remove(conexion)) {
            synchronized (MONITOR) {
                conexionesCreadas--;
            }
        }
        cerrarFisicamente(conexion);
    }

    private static void cerrarFisicamente(Connection conexion) {
        try {
            if (conexion != null && !conexion.isClosed()) conexion.close();
        } catch (SQLException ignorado) {
            // El contenedor se está cerrando y no puede recuperar esta conexión.
        }
    }

    record Configuracion(String url, String usuario, String contrasena) {
    }
}
