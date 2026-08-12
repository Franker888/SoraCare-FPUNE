package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedicoDAO {

    private static final String SQL_EXISTE_MATRICULA = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.medicos
                WHERE LOWER(BTRIM(matricula_profesional)) =
                      LOWER(BTRIM(?))
            ) AS existe
            """;

    private static final String SQL_REGISTRAR_MEDICO = """
            INSERT INTO soracare.medicos (
                id_persona,
                matricula_profesional,
                fecha_ingreso,
                estado,
                biografia
            )
            VALUES (?, ?, ?, ?, ?)
            RETURNING id_medico
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
                per.id_persona,
                per.nombres,
                per.apellidos,
                per.cedula,
                per.fecha_nacimiento,
                per.telefono,
                per.correo,
                per.direccion,
                med.id_medico,
                med.matricula_profesional,
                med.fecha_ingreso,
                med.estado,
                med.biografia
            FROM soracare.medicos med
            INNER JOIN soracare.personas per
                ON per.id_persona = med.id_persona
            WHERE med.id_medico = ?
            """;

    private static final String SQL_BUSCAR_POR_MATRICULA = """
            SELECT
                per.id_persona,
                per.nombres,
                per.apellidos,
                per.cedula,
                per.fecha_nacimiento,
                per.telefono,
                per.correo,
                per.direccion,
                med.id_medico,
                med.matricula_profesional,
                med.fecha_ingreso,
                med.estado,
                med.biografia
            FROM soracare.medicos med
            INNER JOIN soracare.personas per
                ON per.id_persona = med.id_persona
            WHERE LOWER(BTRIM(med.matricula_profesional)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_BUSCAR_POR_ID_PERSONA = """
            SELECT
                per.id_persona,
                per.nombres,
                per.apellidos,
                per.cedula,
                per.fecha_nacimiento,
                per.telefono,
                per.correo,
                per.direccion,
                med.id_medico,
                med.matricula_profesional,
                med.fecha_ingreso,
                med.estado,
                med.biografia
            FROM soracare.medicos med
            INNER JOIN soracare.personas per
                ON per.id_persona = med.id_persona
            WHERE med.id_persona = ?
            """;

    private static final String SQL_LISTAR_TODOS = """
            SELECT
                per.id_persona,
                per.nombres,
                per.apellidos,
                per.cedula,
                per.fecha_nacimiento,
                per.telefono,
                per.correo,
                per.direccion,
                med.id_medico,
                med.matricula_profesional,
                med.fecha_ingreso,
                med.estado,
                med.biografia
            FROM soracare.medicos med
            INNER JOIN soracare.personas per
                ON per.id_persona = med.id_persona
            ORDER BY
                per.apellidos,
                per.nombres,
                med.id_medico
            """;

    private static final String SQL_LISTAR_ACTIVOS = """
            SELECT
                per.id_persona,
                per.nombres,
                per.apellidos,
                per.cedula,
                per.fecha_nacimiento,
                per.telefono,
                per.correo,
                per.direccion,
                med.id_medico,
                med.matricula_profesional,
                med.fecha_ingreso,
                med.estado,
                med.biografia
            FROM soracare.medicos med
            INNER JOIN soracare.personas per
                ON per.id_persona = med.id_persona
            WHERE med.estado = 'ACTIVO'
              AND per.activo = TRUE
            ORDER BY
                per.apellidos,
                per.nombres,
                med.id_medico
            """;

    public boolean existeMatricula(
            String matriculaProfesional
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return existeMatricula(
                    conexion,
                    matriculaProfesional
            );
        }
    }

    public boolean existeMatricula(
            Connection conexion,
            String matriculaProfesional
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (matriculaProfesional == null
                || matriculaProfesional.isBlank()) {

            return false;
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_MATRICULA
                        )
        ) {
            sentencia.setString(
                    1,
                    matriculaProfesional.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se pudo comprobar la matrícula profesional."
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
            Medico medico
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (medico == null) {
            throw new SQLException(
                    "El médico no puede ser nulo."
            );
        }

        if (medico.getId() == null
                || medico.getId() <= 0) {

            throw new SQLException(
                    "El médico debe tener un id_persona válido."
            );
        }

        if (medico.getMatriculaProfesional() == null
                || medico.getMatriculaProfesional().isBlank()) {

            throw new SQLException(
                    "La matrícula profesional es obligatoria."
            );
        }

        if (medico.getEstado() == null) {
            throw new SQLException(
                    "El estado del médico es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_REGISTRAR_MEDICO
                        )
        ) {
            sentencia.setLong(
                    1,
                    medico.getId()
            );

            sentencia.setString(
                    2,
                    medico.getMatriculaProfesional()
                            .strip()
            );

            if (medico.getFechaIngreso() == null) {
                sentencia.setNull(
                        3,
                        Types.DATE
                );
            } else {
                sentencia.setDate(
                        3,
                        Date.valueOf(
                                medico.getFechaIngreso()
                        )
                );
            }

            sentencia.setString(
                    4,
                    medico.getEstado()
                            .name()
            );

            establecerTextoOpcional(
                    sentencia,
                    5,
                    medico.getBiografia()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "PostgreSQL no devolvió el ID del médico."
                    );
                }

                long idMedico =
                        resultado.getLong(
                                "id_medico"
                        );

                medico.setIdMedico(
                        idMedico
                );

                return idMedico;
            }
        }
    }

    public Optional<Medico> buscarPorId(
            long idMedico
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idMedico
            );
        }
    }

    public Optional<Medico> buscarPorId(
            Connection conexion,
            long idMedico
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idMedico <= 0) {
            throw new SQLException(
                    "El ID del médico debe ser mayor que cero."
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
                    idMedico
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearMedico(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<Medico> buscarPorMatricula(
            String matriculaProfesional
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorMatricula(
                    conexion,
                    matriculaProfesional
            );
        }
    }

    public Optional<Medico> buscarPorIdPersona(
            long idPersona
    ) throws SQLException {

        if (idPersona <= 0) {
            throw new SQLException(
                    "El ID de la persona debe ser mayor que cero."
            );
        }

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_ID_PERSONA
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
                    return Optional.empty();
                }

                return Optional.of(
                        mapearMedico(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<Medico> buscarPorMatricula(
            Connection conexion,
            String matriculaProfesional
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (matriculaProfesional == null
                || matriculaProfesional.isBlank()) {

            throw new SQLException(
                    "La matrícula profesional es obligatoria."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_MATRICULA
                        )
        ) {
            sentencia.setString(
                    1,
                    matriculaProfesional.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearMedico(
                                resultado
                        )
                );
            }
        }
    }

    public List<Medico> listarTodos()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarTodos(
                    conexion
            );
        }
    }

    public List<Medico> listarTodos(
            Connection conexion
    ) throws SQLException {

        validarConexion(
                conexion
        );

        return ejecutarListado(
                conexion,
                SQL_LISTAR_TODOS
        );
    }

    public List<Medico> listarActivos()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarActivos(
                    conexion
            );
        }
    }

    public List<Medico> listarActivos(
            Connection conexion
    ) throws SQLException {

        validarConexion(
                conexion
        );

        return ejecutarListado(
                conexion,
                SQL_LISTAR_ACTIVOS
        );
    }

    private static List<Medico> ejecutarListado(
            Connection conexion,
            String sql
    ) throws SQLException {

        List<Medico> medicos =
                new ArrayList<>();

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                sql
                        );

                ResultSet resultado =
                        sentencia.executeQuery()
        ) {
            while (resultado.next()) {
                medicos.add(
                        mapearMedico(
                                resultado
                        )
                );
            }
        }

        return medicos;
    }

    private static Medico mapearMedico(
            ResultSet resultado
    ) throws SQLException {

        Medico medico =
                new Medico();

        medico.setId(
                resultado.getLong(
                        "id_persona"
                )
        );

        medico.setIdMedico(
                resultado.getLong(
                        "id_medico"
                )
        );

        medico.setNombres(
                resultado.getString(
                        "nombres"
                )
        );

        medico.setApellidos(
                resultado.getString(
                        "apellidos"
                )
        );

        medico.setCedula(
                Long.toString(
                        resultado.getLong(
                                "cedula"
                        )
                )
        );

        Date fechaNacimiento =
                resultado.getDate(
                        "fecha_nacimiento"
                );

        if (fechaNacimiento != null) {
            medico.setFechaNacimiento(
                    fechaNacimiento.toLocalDate()
            );
        }

        medico.setTelefono(
                resultado.getString(
                        "telefono"
                )
        );

        medico.setCorreo(
                resultado.getString(
                        "correo"
                )
        );

        medico.setDireccion(
                resultado.getString(
                        "direccion"
                )
        );

        medico.setMatriculaProfesional(
                resultado.getString(
                        "matricula_profesional"
                )
        );

        Date fechaIngreso =
                resultado.getDate(
                        "fecha_ingreso"
                );

        if (fechaIngreso != null) {
            medico.setFechaIngreso(
                    fechaIngreso.toLocalDate()
            );
        }

        medico.setEstado(
                EstadoMedico.valueOf(
                        resultado.getString(
                                "estado"
                        )
                )
        );

        medico.setBiografia(
                resultado.getString(
                        "biografia"
                )
        );

        return medico;
    }

    private static void establecerTextoOpcional(
            PreparedStatement sentencia,
            int posicion,
            String valor
    ) throws SQLException {

        if (valor == null
                || valor.isBlank()) {

            sentencia.setNull(
                    posicion,
                    Types.VARCHAR
            );

        } else {
            sentencia.setString(
                    posicion,
                    valor.strip()
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
    }
}
