package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.EstadoUsuario;
import py.fpune.soracare.mvc.model.Rol;
import py.fpune.soracare.mvc.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Optional;

public class UsuarioDAO {

    private static final String SQL_EXISTE_POR_NOMBRE_USUARIO = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.usuarios
                WHERE LOWER(nombre_usuario) = LOWER(?)
            ) AS existe
            """;

    private static final String SQL_EXISTE_PARA_PERSONA = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.usuarios
                WHERE id_persona = ?
            ) AS existe
            """;

    private static final String SQL_REGISTRAR_USUARIO = """
            INSERT INTO soracare.usuarios (
                id_persona,
                nombre_usuario,
                contrasena_hash,
                estado,
                intentos_fallidos,
                bloqueado_hasta,
                debe_cambiar_contrasena,
                ultimo_acceso
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING
                id_usuario,
                creado_en,
                actualizado_en
            """;

    private static final String SQL_ASIGNAR_ROL = """
            INSERT INTO soracare.usuarios_roles (
                id_usuario,
                id_rol,
                asignado_por
            )
            VALUES (?, ?, ?)
            """;

    private static final String SQL_BUSCAR_POR_IDENTIFICADOR = """
            WITH usuario_objetivo AS (
                SELECT
                    usu.id_usuario
                FROM soracare.usuarios usu
                INNER JOIN soracare.personas per
                    ON per.id_persona = usu.id_persona
                WHERE LOWER(usu.nombre_usuario) = LOWER(?)
                   OR LOWER(per.correo) = LOWER(?)
                ORDER BY
                    CASE
                        WHEN LOWER(usu.nombre_usuario) = LOWER(?)
                            THEN 0
                        ELSE 1
                    END,
                    usu.id_usuario
                LIMIT 1
            )

            SELECT
                usu.id_usuario,
                usu.id_persona,
                usu.nombre_usuario,
                usu.contrasena_hash,
                usu.estado,
                usu.intentos_fallidos,
                usu.bloqueado_hasta,
                usu.debe_cambiar_contrasena,
                usu.ultimo_acceso,
                usu.creado_en AS usuario_creado_en,
                usu.actualizado_en,

                rol.id_rol,
                rol.nombre AS rol_nombre,
                rol.descripcion AS rol_descripcion,
                rol.activo AS rol_activo,
                rol.creado_en AS rol_creado_en

            FROM soracare.usuarios usu

            INNER JOIN usuario_objetivo objetivo
                ON objetivo.id_usuario = usu.id_usuario

            INNER JOIN soracare.personas per
                ON per.id_persona = usu.id_persona

            LEFT JOIN soracare.usuarios_roles ur
                ON ur.id_usuario = usu.id_usuario

            LEFT JOIN soracare.roles rol
                ON rol.id_rol = ur.id_rol
               AND rol.activo = TRUE

            ORDER BY rol.nombre
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
                usu.id_usuario,
                usu.id_persona,
                usu.nombre_usuario,
                usu.contrasena_hash,
                usu.estado,
                usu.intentos_fallidos,
                usu.bloqueado_hasta,
                usu.debe_cambiar_contrasena,
                usu.ultimo_acceso,
                usu.creado_en AS usuario_creado_en,
                usu.actualizado_en,

                rol.id_rol,
                rol.nombre AS rol_nombre,
                rol.descripcion AS rol_descripcion,
                rol.activo AS rol_activo,
                rol.creado_en AS rol_creado_en

            FROM soracare.usuarios usu

            INNER JOIN soracare.personas per
                ON per.id_persona = usu.id_persona

            LEFT JOIN soracare.usuarios_roles ur
                ON ur.id_usuario = usu.id_usuario

            LEFT JOIN soracare.roles rol
                ON rol.id_rol = ur.id_rol
               AND rol.activo = TRUE

            WHERE usu.id_usuario = ?

            ORDER BY rol.nombre
            """;

    private static final String SQL_REGISTRAR_INTENTO_FALLIDO = """
            UPDATE soracare.usuarios
            SET
                intentos_fallidos = CAST(
                    LEAST(
                        intentos_fallidos + 1,
                        ?
                    ) AS SMALLINT
                ),

                estado = CASE
                    WHEN intentos_fallidos + 1 >= ?
                        THEN 'BLOQUEADO'
                    ELSE 'ACTIVO'
                END,

                bloqueado_hasta = CASE
                    WHEN intentos_fallidos + 1 >= ?
                        THEN CURRENT_TIMESTAMP
                             + (? * INTERVAL '1 minute')
                    ELSE NULL
                END,

                actualizado_en = CURRENT_TIMESTAMP

            WHERE id_usuario = ?
              AND estado = 'ACTIVO'
            """;

    private static final String SQL_DESBLOQUEAR_SI_EXPIRADO = """
            UPDATE soracare.usuarios
            SET
                estado = 'ACTIVO',
                intentos_fallidos = 0,
                bloqueado_hasta = NULL,
                actualizado_en = CURRENT_TIMESTAMP

            WHERE id_usuario = ?
              AND estado = 'BLOQUEADO'
              AND bloqueado_hasta IS NOT NULL
              AND bloqueado_hasta <= CURRENT_TIMESTAMP
            """;

    private static final String SQL_REGISTRAR_ACCESO_EXITOSO = """
            UPDATE soracare.usuarios
            SET
                intentos_fallidos = 0,
                bloqueado_hasta = NULL,
                ultimo_acceso = CURRENT_TIMESTAMP,
                actualizado_en = CURRENT_TIMESTAMP

            WHERE id_usuario = ?
              AND estado = 'ACTIVO'
            """;

    private static final String SQL_ACTUALIZAR_CONTRASENA = """
            UPDATE soracare.usuarios
            SET
                contrasena_hash = ?,
                debe_cambiar_contrasena = FALSE,
                intentos_fallidos = 0,
                bloqueado_hasta = NULL,
                actualizado_en = CURRENT_TIMESTAMP

            WHERE id_usuario = ?
              AND contrasena_hash = ?
              AND estado = 'ACTIVO'
            """;

    public boolean existePorNombreUsuario(
            Connection conexion,
            String nombreUsuario
    ) throws SQLException {

        validarConexion(conexion);

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new SQLException(
                    "El nombre de usuario es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_POR_NOMBRE_USUARIO
                        )
        ) {
            sentencia.setString(
                    1,
                    nombreUsuario.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se pudo comprobar el nombre de usuario."
                    );
                }

                return resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    public boolean existeParaPersona(
            Connection conexion,
            long idPersona
    ) throws SQLException {

        validarConexion(conexion);

        if (idPersona <= 0) {
            throw new SQLException(
                    "El ID de la persona debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_PARA_PERSONA
                        )
        ) {
            sentencia.setLong(
                    1,
                    idPersona
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se pudo comprobar la cuenta de la persona."
                    );
                }

                return resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    public long registrar(
            Connection conexion,
            Usuario usuario
    ) throws SQLException {

        validarConexion(conexion);

        if (usuario == null) {
            throw new SQLException(
                    "El usuario no puede ser nulo."
            );
        }

        if (usuario.getIdPersona() == null
                || usuario.getIdPersona() <= 0) {

            throw new SQLException(
                    "El usuario debe estar relacionado "
                            + "con una persona válida."
            );
        }

        if (usuario.getNombreUsuario() == null
                || usuario.getNombreUsuario().isBlank()) {

            throw new SQLException(
                    "El nombre de usuario es obligatorio."
            );
        }

        if (usuario.getContrasenaHash() == null
                || usuario.getContrasenaHash().isBlank()) {

            throw new SQLException(
                    "El hash de la contraseña es obligatorio."
            );
        }

        EstadoUsuario estado =
                usuario.getEstado() == null
                        ? EstadoUsuario.ACTIVO
                        : usuario.getEstado();

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_REGISTRAR_USUARIO
                        )
        ) {
            sentencia.setLong(
                    1,
                    usuario.getIdPersona()
            );

            sentencia.setString(
                    2,
                    usuario.getNombreUsuario().strip()
            );

            sentencia.setString(
                    3,
                    usuario.getContrasenaHash()
            );

            sentencia.setString(
                    4,
                    estado.name()
            );

            sentencia.setShort(
                    5,
                    usuario.getIntentosFallidos()
            );

            establecerFechaOpcional(
                    sentencia,
                    6,
                    usuario.getBloqueadoHasta()
            );

            sentencia.setBoolean(
                    7,
                    usuario.isDebeCambiarContrasena()
            );

            establecerFechaOpcional(
                    sentencia,
                    8,
                    usuario.getUltimoAcceso()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "PostgreSQL no devolvió "
                                    + "el usuario registrado."
                    );
                }

                long idUsuario =
                        resultado.getLong(
                                "id_usuario"
                        );

                usuario.setIdUsuario(
                        idUsuario
                );

                usuario.setEstado(
                        estado
                );

                usuario.setCreadoEn(
                        resultado.getObject(
                                "creado_en",
                                OffsetDateTime.class
                        )
                );

                usuario.setActualizadoEn(
                        resultado.getObject(
                                "actualizado_en",
                                OffsetDateTime.class
                        )
                );

                return idUsuario;
            }
        }
    }

    public void asignarRol(
            Connection conexion,
            long idUsuario,
            short idRol,
            Long idUsuarioAsignador
    ) throws SQLException {

        validarConexion(conexion);

        if (idUsuario <= 0) {
            throw new SQLException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        if (idRol <= 0) {
            throw new SQLException(
                    "El ID del rol debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_ASIGNAR_ROL
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            sentencia.setShort(
                    2,
                    idRol
            );

            if (idUsuarioAsignador == null) {
                sentencia.setNull(
                        3,
                        Types.BIGINT
                );
            } else {
                sentencia.setLong(
                        3,
                        idUsuarioAsignador
                );
            }

            int filasAfectadas =
                    sentencia.executeUpdate();

            if (filasAfectadas != 1) {
                throw new SQLException(
                        "No se pudo asignar el rol al usuario."
                );
            }
        }
    }

    public Optional<Usuario> buscarPorIdentificador(
            String identificador
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorIdentificador(
                    conexion,
                    identificador
            );
        }
    }

    public Optional<Usuario> buscarPorIdentificador(
            Connection conexion,
            String identificador
    ) throws SQLException {

        validarConexion(conexion);

        if (identificador == null
                || identificador.isBlank()) {

            throw new SQLException(
                    "El identificador de acceso es obligatorio."
            );
        }

        String identificadorNormalizado =
                identificador.strip();

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_IDENTIFICADOR
                        )
        ) {
            sentencia.setString(
                    1,
                    identificadorNormalizado
            );

            sentencia.setString(
                    2,
                    identificadorNormalizado
            );

            sentencia.setString(
                    3,
                    identificadorNormalizado
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                return mapearResultadoUsuario(
                        resultado
                );
            }
        }
    }

    public Optional<Usuario> buscarPorId(
            long idUsuario
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idUsuario
            );
        }
    }

    public Optional<Usuario> buscarPorId(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        validarConexion(conexion);

        if (idUsuario <= 0) {
            throw new SQLException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_ID
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                return mapearResultadoUsuario(
                        resultado
                );
            }
        }
    }

    public void registrarIntentoFallido(
            Connection conexion,
            long idUsuario,
            short maximoIntentos,
            int minutosBloqueo
    ) throws SQLException {

        validarConexion(conexion);

        if (idUsuario <= 0) {
            throw new SQLException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        if (maximoIntentos <= 0) {
            throw new SQLException(
                    "El máximo de intentos debe ser mayor que cero."
            );
        }

        if (minutosBloqueo <= 0) {
            throw new SQLException(
                    "La duración del bloqueo debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_REGISTRAR_INTENTO_FALLIDO
                        )
        ) {
            sentencia.setShort(
                    1,
                    maximoIntentos
            );

            sentencia.setShort(
                    2,
                    maximoIntentos
            );

            sentencia.setShort(
                    3,
                    maximoIntentos
            );

            sentencia.setInt(
                    4,
                    minutosBloqueo
            );

            sentencia.setLong(
                    5,
                    idUsuario
            );

            int filasAfectadas =
                    sentencia.executeUpdate();

            if (filasAfectadas != 1) {
                throw new SQLException(
                        "No se pudo registrar el intento fallido."
                );
            }
        }
    }

    public boolean desbloquearSiBloqueoExpiro(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        validarConexion(conexion);

        if (idUsuario <= 0) {
            throw new SQLException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_DESBLOQUEAR_SI_EXPIRADO
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            return sentencia.executeUpdate() == 1;
        }
    }

    public void registrarAccesoExitoso(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        validarConexion(conexion);

        if (idUsuario <= 0) {
            throw new SQLException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_REGISTRAR_ACCESO_EXITOSO
                        )
        ) {
            sentencia.setLong(
                    1,
                    idUsuario
            );

            int filasAfectadas =
                    sentencia.executeUpdate();

            if (filasAfectadas != 1) {
                throw new SQLException(
                        "No se pudo registrar el acceso exitoso."
                );
            }
        }
    }

    public void actualizarContrasena(
            Connection conexion,
            long idUsuario,
            String hashActualEsperado,
            String nuevoHash
    ) throws SQLException {

        validarConexion(conexion);

        if (idUsuario <= 0) {
            throw new SQLException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        if (hashActualEsperado == null
                || hashActualEsperado.isBlank()) {

            throw new SQLException(
                    "El hash actual de la contraseña es obligatorio."
            );
        }

        if (nuevoHash == null
                || nuevoHash.isBlank()) {

            throw new SQLException(
                    "El nuevo hash de la contraseña es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_ACTUALIZAR_CONTRASENA
                        )
        ) {
            sentencia.setString(
                    1,
                    nuevoHash
            );

            sentencia.setLong(
                    2,
                    idUsuario
            );

            sentencia.setString(
                    3,
                    hashActualEsperado
            );

            int filasAfectadas =
                    sentencia.executeUpdate();

            if (filasAfectadas != 1) {
                throw new SQLException(
                        "No se pudo actualizar la contraseña. "
                                + "La cuenta no está activa o sus datos "
                                + "fueron modificados durante la operación."
                );
            }
        }
    }

    private static Optional<Usuario> mapearResultadoUsuario(
            ResultSet resultado
    ) throws SQLException {

        if (!resultado.next()) {
            return Optional.empty();
        }

        Usuario usuario =
                mapearUsuario(
                        resultado
                );

        agregarRolSiExiste(
                usuario,
                resultado
        );

        while (resultado.next()) {
            agregarRolSiExiste(
                    usuario,
                    resultado
            );
        }

        return Optional.of(
                usuario
        );
    }

    private static Usuario mapearUsuario(
            ResultSet resultado
    ) throws SQLException {

        Usuario usuario =
                new Usuario();

        usuario.setIdUsuario(
                resultado.getLong(
                        "id_usuario"
                )
        );

        usuario.setIdPersona(
                resultado.getLong(
                        "id_persona"
                )
        );

        usuario.setNombreUsuario(
                resultado.getString(
                        "nombre_usuario"
                )
        );

        usuario.setContrasenaHash(
                resultado.getString(
                        "contrasena_hash"
                )
        );

        String estadoTexto =
                resultado.getString(
                        "estado"
                );

        try {
            usuario.setEstado(
                    EstadoUsuario.valueOf(
                            estadoTexto
                    )
            );

        } catch (
                IllegalArgumentException
                | NullPointerException error
        ) {
            throw new SQLException(
                    "El estado almacenado del usuario no es válido.",
                    error
            );
        }

        usuario.setIntentosFallidos(
                resultado.getShort(
                        "intentos_fallidos"
                )
        );

        usuario.setBloqueadoHasta(
                resultado.getObject(
                        "bloqueado_hasta",
                        OffsetDateTime.class
                )
        );

        usuario.setDebeCambiarContrasena(
                resultado.getBoolean(
                        "debe_cambiar_contrasena"
                )
        );

        usuario.setUltimoAcceso(
                resultado.getObject(
                        "ultimo_acceso",
                        OffsetDateTime.class
                )
        );

        usuario.setCreadoEn(
                resultado.getObject(
                        "usuario_creado_en",
                        OffsetDateTime.class
                )
        );

        usuario.setActualizadoEn(
                resultado.getObject(
                        "actualizado_en",
                        OffsetDateTime.class
                )
        );

        return usuario;
    }

    private static void agregarRolSiExiste(
            Usuario usuario,
            ResultSet resultado
    ) throws SQLException {

        short idRol =
                resultado.getShort(
                        "id_rol"
                );

        if (resultado.wasNull()) {
            return;
        }

        Rol rol =
                new Rol();

        rol.setIdRol(
                idRol
        );

        rol.setNombre(
                resultado.getString(
                        "rol_nombre"
                )
        );

        rol.setDescripcion(
                resultado.getString(
                        "rol_descripcion"
                )
        );

        rol.setActivo(
                resultado.getBoolean(
                        "rol_activo"
                )
        );

        rol.setCreadoEn(
                resultado.getObject(
                        "rol_creado_en",
                        OffsetDateTime.class
                )
        );

        usuario.agregarRol(
                rol
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

    private static void validarConexion(
            Connection conexion
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (conexion.isClosed()) {
            throw new SQLException(
                    "La conexión se encuentra cerrada."
            );
        }
    }
}