package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.SolicitudCambioContrasenaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.CodificadorContrasenaBCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;

public class CambioContrasenaServiceIntegracionTest {

    private static final String NOMBRE_USUARIO =
            "prueba.recepcion";

    private static final String CONTRASENA_ORIGINAL =
            "SoraCare-Recepcion-2026!";

    private static final String CONTRASENA_NUEVA =
            "SoraCare-Recepcion-Nueva-2026!";

    private static final String SQL_OBTENER_ESTADO = """
            SELECT
                id_usuario,
                contrasena_hash,
                estado,
                intentos_fallidos,
                bloqueado_hasta,
                debe_cambiar_contrasena,
                ultimo_acceso,
                actualizado_en
            FROM soracare.usuarios
            WHERE nombre_usuario = ?
            """;

    private static final String SQL_RESTAURAR_ESTADO = """
            UPDATE soracare.usuarios
            SET
                contrasena_hash = ?,
                estado = ?,
                intentos_fallidos = ?,
                bloqueado_hasta = ?,
                debe_cambiar_contrasena = ?,
                ultimo_acceso = ?,
                actualizado_en = ?
            WHERE id_usuario = ?
            """;

    public static void main(
            String[] args
    ) throws Exception {

        EstadoCuenta estadoOriginal =
                obtenerEstadoCuenta();

        verificarContrasenaOriginal(
                estadoOriginal
        );

        try {
            probarContrasenaActualIncorrecta(
                    estadoOriginal
            );

            probarConfirmacionDiferente(
                    estadoOriginal
            );

            probarNuevaContrasenaDebil(
                    estadoOriginal
            );

            probarReutilizacionContrasenaActual(
                    estadoOriginal
            );

            probarCambioCorrecto(
                    estadoOriginal
            );

            System.out.println();
            System.out.println(
                    "CambioContrasenaService fue probado correctamente."
            );

            System.out.println(
                    "La contraseña actual incorrecta fue rechazada."
            );

            System.out.println(
                    "La confirmación diferente fue rechazada."
            );

            System.out.println(
                    "La contraseña débil fue rechazada."
            );

            System.out.println(
                    "La reutilización de la contraseña actual "
                            + "fue rechazada."
            );

            System.out.println(
                    "La nueva contraseña fue almacenada con BCrypt."
            );

            System.out.println(
                    "La obligación de cambiar contraseña "
                            + "fue desactivada correctamente."
            );

        } finally {
            restaurarEstadoCuenta(
                    estadoOriginal
            );

            verificarRestauracion(
                    estadoOriginal
            );

            System.out.println(
                    "El estado original de prueba.recepcion "
                            + "fue restaurado."
            );
        }
    }

    private static void probarContrasenaActualIncorrecta(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        CambioContrasenaService servicio =
                new CambioContrasenaService();

        SolicitudCambioContrasenaDTO solicitud =
                new SolicitudCambioContrasenaDTO(
                        "Contrasena-Actual-Incorrecta-2026!",
                        CONTRASENA_NUEVA,
                        CONTRASENA_NUEVA
                );

        try {
            servicio.cambiarContrasena(
                    estadoOriginal.idUsuario,
                    solicitud
            );

            throw new AssertionError(
                    "Una contraseña actual incorrecta "
                            + "no debería permitir el cambio."
            );

        } catch (SecurityException esperado) {
            verificarSinModificaciones(
                    estadoOriginal,
                    "contraseña actual incorrecta"
            );

            System.out.println(
                    "Contraseña actual incorrecta: RECHAZADA."
            );
        }
    }

    private static void probarConfirmacionDiferente(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        CambioContrasenaService servicio =
                new CambioContrasenaService();

        SolicitudCambioContrasenaDTO solicitud =
                new SolicitudCambioContrasenaDTO(
                        CONTRASENA_ORIGINAL,
                        CONTRASENA_NUEVA,
                        "Confirmacion-Diferente-2026!"
                );

        try {
            servicio.cambiarContrasena(
                    estadoOriginal.idUsuario,
                    solicitud
            );

            throw new AssertionError(
                    "Las contraseñas diferentes "
                            + "no deberían permitir el cambio."
            );

        } catch (IllegalArgumentException esperado) {
            verificarSinModificaciones(
                    estadoOriginal,
                    "confirmación diferente"
            );

            System.out.println(
                    "Confirmación diferente: RECHAZADA."
            );
        }
    }

    private static void probarNuevaContrasenaDebil(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        CambioContrasenaService servicio =
                new CambioContrasenaService();

        SolicitudCambioContrasenaDTO solicitud =
                new SolicitudCambioContrasenaDTO(
                        CONTRASENA_ORIGINAL,
                        "abc123",
                        "abc123"
                );

        try {
            servicio.cambiarContrasena(
                    estadoOriginal.idUsuario,
                    solicitud
            );

            throw new AssertionError(
                    "Una contraseña débil "
                            + "no debería permitir el cambio."
            );

        } catch (IllegalArgumentException esperado) {
            verificarSinModificaciones(
                    estadoOriginal,
                    "contraseña débil"
            );

            System.out.println(
                    "Contraseña débil: RECHAZADA."
            );
        }
    }

    private static void probarReutilizacionContrasenaActual(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        CambioContrasenaService servicio =
                new CambioContrasenaService();

        SolicitudCambioContrasenaDTO solicitud =
                new SolicitudCambioContrasenaDTO(
                        CONTRASENA_ORIGINAL,
                        CONTRASENA_ORIGINAL,
                        CONTRASENA_ORIGINAL
                );

        try {
            servicio.cambiarContrasena(
                    estadoOriginal.idUsuario,
                    solicitud
            );

            throw new AssertionError(
                    "No debería permitirse reutilizar "
                            + "la contraseña actual."
            );

        } catch (IllegalArgumentException esperado) {
            verificarSinModificaciones(
                    estadoOriginal,
                    "reutilización de contraseña"
            );

            System.out.println(
                    "Reutilización de contraseña actual: RECHAZADA."
            );
        }
    }

    private static void probarCambioCorrecto(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        CambioContrasenaService servicio =
                new CambioContrasenaService();

        SolicitudCambioContrasenaDTO solicitud =
                new SolicitudCambioContrasenaDTO(
                        CONTRASENA_ORIGINAL,
                        CONTRASENA_NUEVA,
                        CONTRASENA_NUEVA
                );

        UsuarioAutenticadoDTO usuarioActualizado =
                servicio.cambiarContrasena(
                        estadoOriginal.idUsuario,
                        solicitud
                );

        if (usuarioActualizado == null) {
            throw new AssertionError(
                    "El servicio no devolvió "
                            + "el usuario actualizado."
            );
        }

        if (usuarioActualizado.getIdUsuario()
                != estadoOriginal.idUsuario) {

            throw new AssertionError(
                    "El ID del usuario actualizado no coincide."
            );
        }

        if (!NOMBRE_USUARIO.equals(
                usuarioActualizado.getNombreUsuario()
        )) {
            throw new AssertionError(
                    "El nombre de usuario actualizado no coincide."
            );
        }

        if (!usuarioActualizado.tieneRol(
                "RECEPCIONISTA"
        )) {
            throw new AssertionError(
                    "El usuario debería conservar "
                            + "el rol RECEPCIONISTA."
            );
        }

        if (usuarioActualizado
                .isDebeCambiarContrasena()) {

            throw new AssertionError(
                    "La obligación de cambiar contraseña "
                            + "debería quedar desactivada."
            );
        }

        EstadoCuenta estadoActualizado =
                obtenerEstadoCuenta();

        if (estadoActualizado.contrasenaHash.equals(
                estadoOriginal.contrasenaHash
        )) {
            throw new AssertionError(
                    "El hash nuevo debería ser diferente "
                            + "del hash anterior."
            );
        }

        if (!CodificadorContrasenaBCrypt.verificar(
                CONTRASENA_NUEVA,
                estadoActualizado.contrasenaHash
        )) {
            throw new AssertionError(
                    "El nuevo hash no reconoce "
                            + "la contraseña nueva."
            );
        }

        if (CodificadorContrasenaBCrypt.verificar(
                CONTRASENA_ORIGINAL,
                estadoActualizado.contrasenaHash
        )) {
            throw new AssertionError(
                    "El nuevo hash no debería reconocer "
                            + "la contraseña anterior."
            );
        }

        if (estadoActualizado.debeCambiarContrasena) {
            throw new AssertionError(
                    "La base de datos todavía exige "
                            + "cambiar la contraseña."
            );
        }

        if (estadoActualizado.intentosFallidos != 0) {
            throw new AssertionError(
                    "Los intentos fallidos deberían quedar en cero."
            );
        }

        if (estadoActualizado.bloqueadoHasta != null) {
            throw new AssertionError(
                    "La fecha de bloqueo debería quedar vacía."
            );
        }

        System.out.println(
                "Cambio correcto: NUEVO HASH REGISTRADO."
        );
    }

    private static void verificarContrasenaOriginal(
            EstadoCuenta estadoOriginal
    ) {
        boolean coincide =
                CodificadorContrasenaBCrypt.verificar(
                        CONTRASENA_ORIGINAL,
                        estadoOriginal.contrasenaHash
                );

        if (!coincide) {
            throw new IllegalStateException(
                    "La contraseña conocida de prueba.recepcion "
                            + "no coincide con el hash almacenado."
            );
        }
    }

    private static void verificarSinModificaciones(
            EstadoCuenta estadoOriginal,
            String nombrePrueba
    ) throws SQLException {

        EstadoCuenta estadoActual =
                obtenerEstadoCuenta();

        if (!estadoOriginal.contrasenaHash.equals(
                estadoActual.contrasenaHash
        )) {
            throw new AssertionError(
                    "La prueba de "
                            + nombrePrueba
                            + " modificó el hash inesperadamente."
            );
        }

        if (estadoOriginal.debeCambiarContrasena
                != estadoActual.debeCambiarContrasena) {

            throw new AssertionError(
                    "La prueba de "
                            + nombrePrueba
                            + " modificó la obligación "
                            + "de cambiar contraseña."
            );
        }
    }

    private static EstadoCuenta obtenerEstadoCuenta()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_OBTENER_ESTADO
                        )
        ) {
            sentencia.setString(
                    1,
                    NOMBRE_USUARIO
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se encontró la cuenta temporal "
                                    + NOMBRE_USUARIO
                                    + "."
                    );
                }

                return new EstadoCuenta(
                        resultado.getLong(
                                "id_usuario"
                        ),
                        resultado.getString(
                                "contrasena_hash"
                        ),
                        resultado.getString(
                                "estado"
                        ),
                        resultado.getShort(
                                "intentos_fallidos"
                        ),
                        resultado.getObject(
                                "bloqueado_hasta",
                                OffsetDateTime.class
                        ),
                        resultado.getBoolean(
                                "debe_cambiar_contrasena"
                        ),
                        resultado.getObject(
                                "ultimo_acceso",
                                OffsetDateTime.class
                        ),
                        resultado.getObject(
                                "actualizado_en",
                                OffsetDateTime.class
                        )
                );
            }
        }
    }

    private static void restaurarEstadoCuenta(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_RESTAURAR_ESTADO
                        )
        ) {
            sentencia.setString(
                    1,
                    estadoOriginal.contrasenaHash
            );

            sentencia.setString(
                    2,
                    estadoOriginal.estado
            );

            sentencia.setShort(
                    3,
                    estadoOriginal.intentosFallidos
            );

            establecerFechaOpcional(
                    sentencia,
                    4,
                    estadoOriginal.bloqueadoHasta
            );

            sentencia.setBoolean(
                    5,
                    estadoOriginal.debeCambiarContrasena
            );

            establecerFechaOpcional(
                    sentencia,
                    6,
                    estadoOriginal.ultimoAcceso
            );

            establecerFechaOpcional(
                    sentencia,
                    7,
                    estadoOriginal.actualizadoEn
            );

            sentencia.setLong(
                    8,
                    estadoOriginal.idUsuario
            );

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo restaurar "
                                + "el estado original de la cuenta."
                );
            }
        }
    }

    private static void verificarRestauracion(
            EstadoCuenta estadoOriginal
    ) throws SQLException {

        EstadoCuenta estadoRestaurado =
                obtenerEstadoCuenta();

        if (!estadoOriginal.contrasenaHash.equals(
                estadoRestaurado.contrasenaHash
        )) {
            throw new AssertionError(
                    "El hash original no fue restaurado."
            );
        }

        if (estadoOriginal.debeCambiarContrasena
                != estadoRestaurado.debeCambiarContrasena) {

            throw new AssertionError(
                    "El indicador de cambio obligatorio "
                            + "no fue restaurado."
            );
        }

        verificarContrasenaOriginal(
                estadoRestaurado
        );
    }

    private static void establecerFechaOpcional(
            PreparedStatement sentencia,
            int posicion,
            OffsetDateTime fecha
    ) throws SQLException {

        if (fecha == null) {
            sentencia.setNull(
                    posicion,
                    Types.TIMESTAMP_WITH_TIMEZONE
            );

        } else {
            sentencia.setObject(
                    posicion,
                    fecha
            );
        }
    }

    private static class EstadoCuenta {

        private final long idUsuario;
        private final String contrasenaHash;
        private final String estado;
        private final short intentosFallidos;
        private final OffsetDateTime bloqueadoHasta;
        private final boolean debeCambiarContrasena;
        private final OffsetDateTime ultimoAcceso;
        private final OffsetDateTime actualizadoEn;

        private EstadoCuenta(
                long idUsuario,
                String contrasenaHash,
                String estado,
                short intentosFallidos,
                OffsetDateTime bloqueadoHasta,
                boolean debeCambiarContrasena,
                OffsetDateTime ultimoAcceso,
                OffsetDateTime actualizadoEn
        ) {
            this.idUsuario = idUsuario;
            this.contrasenaHash = contrasenaHash;
            this.estado = estado;
            this.intentosFallidos = intentosFallidos;
            this.bloqueadoHasta = bloqueadoHasta;
            this.debeCambiarContrasena =
                    debeCambiarContrasena;
            this.ultimoAcceso = ultimoAcceso;
            this.actualizadoEn = actualizadoEn;
        }
    }
}