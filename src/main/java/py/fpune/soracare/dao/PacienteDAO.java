package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Paciente;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

public class PacienteDAO {

    private static final String SQL_REGISTRAR_PACIENTE = """
            INSERT INTO soracare.pacientes (
                id_persona,
                grupo_sanguineo,
                contacto_emergencia,
                telefono_emergencia
            )
            VALUES (?, ?, ?, ?)
            RETURNING
                id_paciente,
                numero_historia_clinica
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
                pac.id_paciente,
                pac.numero_historia_clinica,
                pac.grupo_sanguineo,
                pac.contacto_emergencia,
                pac.telefono_emergencia
            FROM soracare.pacientes pac
            INNER JOIN soracare.personas per
                ON per.id_persona = pac.id_persona
            WHERE pac.id_paciente = ?
            """;

    public long registrar(
            Connection conexion,
            Paciente paciente
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula"
            );
        }

        if (paciente == null) {
            throw new SQLException(
                    "El paciente no puede ser nulo"
            );
        }

        if (paciente.getId() == null
                || paciente.getId() <= 0) {

            throw new SQLException(
                    "El paciente debe tener un id_persona válido"
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_REGISTRAR_PACIENTE
                        )
        ) {
            sentencia.setLong(
                    1,
                    paciente.getId()
            );

            establecerTextoOpcional(
                    sentencia,
                    2,
                    paciente.getGrupoSanguineo()
            );

            establecerTextoOpcional(
                    sentencia,
                    3,
                    paciente.getContactoEmergencia()
            );

            establecerTextoOpcional(
                    sentencia,
                    4,
                    paciente.getTelefonoEmergencia()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "PostgreSQL no devolvió los datos del paciente"
                    );
                }

                long idPaciente =
                        resultado.getLong(
                                "id_paciente"
                        );

                String numeroHistoriaClinica =
                        resultado.getString(
                                "numero_historia_clinica"
                        );

                paciente.setIdPaciente(
                        idPaciente
                );

                paciente.setNumeroHistoriaClinica(
                        numeroHistoriaClinica
                );

                return idPaciente;
            }
        }
    }

    public Optional<Paciente> buscarPorId(
            long idPaciente
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idPaciente
            );
        }
    }

    public Optional<Paciente> buscarPorId(
            Connection conexion,
            long idPaciente
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula"
            );
        }

        if (idPaciente <= 0) {
            throw new SQLException(
                    "El id del paciente debe ser mayor que cero"
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
                    idPaciente
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                Paciente paciente =
                        mapearPaciente(resultado);

                return Optional.of(paciente);
            }
        }
    }

    private static Paciente mapearPaciente(
            ResultSet resultado
    ) throws SQLException {

        Paciente paciente =
                new Paciente();

        paciente.setId(
                resultado.getLong(
                        "id_persona"
                )
        );

        paciente.setIdPaciente(
                resultado.getLong(
                        "id_paciente"
                )
        );

        paciente.setNombres(
                resultado.getString(
                        "nombres"
                )
        );

        paciente.setApellidos(
                resultado.getString(
                        "apellidos"
                )
        );

        paciente.setCedula(
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
            paciente.setFechaNacimiento(
                    fechaNacimiento.toLocalDate()
            );
        }

        paciente.setTelefono(
                resultado.getString(
                        "telefono"
                )
        );

        paciente.setCorreo(
                resultado.getString(
                        "correo"
                )
        );

        paciente.setDireccion(
                resultado.getString(
                        "direccion"
                )
        );

        paciente.setNumeroHistoriaClinica(
                resultado.getString(
                        "numero_historia_clinica"
                )
        );

        paciente.setGrupoSanguineo(
                resultado.getString(
                        "grupo_sanguineo"
                )
        );

        paciente.setContactoEmergencia(
                resultado.getString(
                        "contacto_emergencia"
                )
        );

        paciente.setTelefonoEmergencia(
                resultado.getString(
                        "telefono_emergencia"
                )
        );

        return paciente;
    }

    private static void establecerTextoOpcional(
            PreparedStatement sentencia,
            int posicion,
            String valor
    ) throws SQLException {

        if (valor == null || valor.isBlank()) {
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
}