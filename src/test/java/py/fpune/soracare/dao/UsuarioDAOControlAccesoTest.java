package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.EstadoUsuario;
import py.fpune.soracare.mvc.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAOControlAccesoTest {

    private static final String IDENTIFICADOR =
            "prueba.seguridad";

    private static final short MAXIMO_INTENTOS = 5;

    private static final int MINUTOS_BLOQUEO = 15;

    private static final String SQL_PREPARAR_ESTADO_INICIAL = """
            UPDATE soracare.usuarios
            SET
                estado = 'ACTIVO',
                intentos_fallidos = 0,
                bloqueado_hasta = NULL
            WHERE id_usuario = ?
            """;

    private static final String SQL_EXPIRAR_BLOQUEO = """
            UPDATE soracare.usuarios
            SET bloqueado_hasta =
                    CURRENT_TIMESTAMP - INTERVAL '1 minute'
            WHERE id_usuario = ?
            """;

    public static void main(
            String[] args
    ) throws Exception {

        UsuarioDAO usuarioDAO =
                new UsuarioDAO();

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(false);

            try {
                Usuario usuarioInicial =
                        obtenerUsuario(
                                usuarioDAO,
                                conexion
                        );

                long idUsuario =
                        usuarioInicial.getIdUsuario();

                prepararEstadoInicial(
                        conexion,
                        idUsuario
                );

                probarPrimerosCuatroIntentos(
                        usuarioDAO,
                        conexion,
                        idUsuario
                );

                probarQuintoIntentoYBloqueo(
                        usuarioDAO,
                        conexion,
                        idUsuario
                );

                probarDesbloqueo(
                        usuarioDAO,
                        conexion,
                        idUsuario
                );

                probarAccesoExitoso(
                        usuarioDAO,
                        conexion,
                        idUsuario
                );

                System.out.println();
                System.out.println(
                        "Control de acceso probado correctamente."
                );

                System.out.println(
                        "Los primeros cuatro intentos "
                                + "mantuvieron la cuenta activa."
                );

                System.out.println(
                        "El quinto intento bloqueó la cuenta."
                );

                System.out.println(
                        "El bloqueo vigente impidió "
                                + "el desbloqueo anticipado."
                );

                System.out.println(
                        "El bloqueo vencido permitió "
                                + "reactivar la cuenta."
                );

                System.out.println(
                        "El acceso exitoso reinició "
                                + "los intentos fallidos."
                );

                System.out.println(
                        "La transacción será revertida "
                                + "para conservar los datos originales."
                );

            } finally {
                conexion.rollback();
            }
        }
    }

    private static void probarPrimerosCuatroIntentos(
            UsuarioDAO usuarioDAO,
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        for (
                short intento = 1;
                intento < MAXIMO_INTENTOS;
                intento++
        ) {
            usuarioDAO.registrarIntentoFallido(
                    conexion,
                    idUsuario,
                    MAXIMO_INTENTOS,
                    MINUTOS_BLOQUEO
            );

            Usuario usuario =
                    obtenerUsuario(
                            usuarioDAO,
                            conexion
                    );

            if (usuario.getIntentosFallidos()
                    != intento) {

                throw new AssertionError(
                        "El intento fallido número "
                                + intento
                                + " no fue registrado correctamente."
                );
            }

            if (usuario.getEstado()
                    != EstadoUsuario.ACTIVO) {

                throw new AssertionError(
                        "La cuenta no debería bloquearse "
                                + "antes del quinto intento."
                );
            }

            if (usuario.getBloqueadoHasta()
                    != null) {

                throw new AssertionError(
                        "No debería existir una fecha de bloqueo "
                                + "antes del quinto intento."
                );
            }

            System.out.println(
                    "Intento fallido "
                            + intento
                            + ": cuenta todavía ACTIVA."
            );
        }
    }

    private static void probarQuintoIntentoYBloqueo(
            UsuarioDAO usuarioDAO,
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        usuarioDAO.registrarIntentoFallido(
                conexion,
                idUsuario,
                MAXIMO_INTENTOS,
                MINUTOS_BLOQUEO
        );

        Usuario usuarioBloqueado =
                obtenerUsuario(
                        usuarioDAO,
                        conexion
                );

        if (usuarioBloqueado.getIntentosFallidos()
                != MAXIMO_INTENTOS) {

            throw new AssertionError(
                    "La cuenta debería tener cinco "
                            + "intentos fallidos."
            );
        }

        if (usuarioBloqueado.getEstado()
                != EstadoUsuario.BLOQUEADO) {

            throw new AssertionError(
                    "La cuenta debería estar BLOQUEADA."
            );
        }

        if (usuarioBloqueado.getBloqueadoHasta()
                == null) {

            throw new AssertionError(
                    "La cuenta bloqueada debería tener "
                            + "una fecha de desbloqueo."
            );
        }

        System.out.println(
                "Intento fallido 5: cuenta BLOQUEADA."
        );

        System.out.println(
                "Bloqueada hasta: "
                        + usuarioBloqueado
                        .getBloqueadoHasta()
        );
    }

    private static void probarDesbloqueo(
            UsuarioDAO usuarioDAO,
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        boolean desbloqueadoAntesDeTiempo =
                usuarioDAO.desbloquearSiBloqueoExpiro(
                        conexion,
                        idUsuario
                );

        if (desbloqueadoAntesDeTiempo) {
            throw new AssertionError(
                    "La cuenta no debería desbloquearse "
                            + "antes de vencer el plazo."
            );
        }

        expirarBloqueo(
                conexion,
                idUsuario
        );

        boolean desbloqueado =
                usuarioDAO.desbloquearSiBloqueoExpiro(
                        conexion,
                        idUsuario
                );

        if (!desbloqueado) {
            throw new AssertionError(
                    "La cuenta debería desbloquearse "
                            + "después de vencer el plazo."
            );
        }

        Usuario usuarioDesbloqueado =
                obtenerUsuario(
                        usuarioDAO,
                        conexion
                );

        if (usuarioDesbloqueado.getEstado()
                != EstadoUsuario.ACTIVO) {

            throw new AssertionError(
                    "La cuenta desbloqueada debería "
                            + "estar ACTIVA."
            );
        }

        if (usuarioDesbloqueado.getIntentosFallidos()
                != 0) {

            throw new AssertionError(
                    "Los intentos deberían reiniciarse "
                            + "después del desbloqueo."
            );
        }

        if (usuarioDesbloqueado.getBloqueadoHasta()
                != null) {

            throw new AssertionError(
                    "La fecha de bloqueo debería eliminarse."
            );
        }

        System.out.println(
                "Bloqueo vencido: cuenta nuevamente ACTIVA."
        );
    }

    private static void probarAccesoExitoso(
            UsuarioDAO usuarioDAO,
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        usuarioDAO.registrarIntentoFallido(
                conexion,
                idUsuario,
                MAXIMO_INTENTOS,
                MINUTOS_BLOQUEO
        );

        Usuario usuarioConUnIntento =
                obtenerUsuario(
                        usuarioDAO,
                        conexion
                );

        if (usuarioConUnIntento.getIntentosFallidos()
                != 1) {

            throw new AssertionError(
                    "No se registró el intento previo "
                            + "al acceso exitoso."
            );
        }

        usuarioDAO.registrarAccesoExitoso(
                conexion,
                idUsuario
        );

        Usuario usuarioConAccesoExitoso =
                obtenerUsuario(
                        usuarioDAO,
                        conexion
                );

        if (usuarioConAccesoExitoso
                .getIntentosFallidos() != 0) {

            throw new AssertionError(
                    "El acceso exitoso debería reiniciar "
                            + "los intentos fallidos."
            );
        }

        if (usuarioConAccesoExitoso
                .getUltimoAcceso() == null) {

            throw new AssertionError(
                    "El acceso exitoso debería registrar "
                            + "la fecha del último acceso."
            );
        }

        if (usuarioConAccesoExitoso
                .getBloqueadoHasta() != null) {

            throw new AssertionError(
                    "No debería conservarse una fecha de bloqueo."
            );
        }

        System.out.println(
                "Acceso exitoso registrado: "
                        + usuarioConAccesoExitoso
                        .getUltimoAcceso()
        );
    }

    private static Usuario obtenerUsuario(
            UsuarioDAO usuarioDAO,
            Connection conexion
    ) throws SQLException {

        Optional<Usuario> resultado =
                usuarioDAO.buscarPorIdentificador(
                        conexion,
                        IDENTIFICADOR
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "No se encontró el usuario temporal "
                            + IDENTIFICADOR
                            + "."
            );
        }

        return resultado.get();
    }

    private static void prepararEstadoInicial(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_PREPARAR_ESTADO_INICIAL
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo preparar la cuenta "
                                + "para la prueba."
                );
            }
        }
    }

    private static void expirarBloqueo(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXPIRAR_BLOQUEO
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo simular el vencimiento "
                                + "del bloqueo."
                );
            }
        }
    }
}