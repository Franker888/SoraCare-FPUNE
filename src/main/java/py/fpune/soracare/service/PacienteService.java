package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.PacienteDAO;
import py.fpune.soracare.dao.PersonaDAO;
import py.fpune.soracare.dto.RegistroPacienteDTO;
import py.fpune.soracare.mvc.model.Paciente;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class PacienteService {

    private static final Set<String> GRUPOS_SANGUINEOS_VALIDOS =
            Set.of(
                    "A+",
                    "A-",
                    "B+",
                    "B-",
                    "AB+",
                    "AB-",
                    "O+",
                    "O-"
            );

    private final PersonaDAO personaDAO =
            new PersonaDAO();

    private final PacienteDAO pacienteDAO =
            new PacienteDAO();

    public Paciente registrar(
            RegistroPacienteDTO datosRegistro
    ) throws SQLException {

        if (datosRegistro == null) {
            throw new SQLException(
                    "Los datos del registro no pueden ser nulos."
            );
        }

        Paciente paciente = new Paciente();

        paciente.setNombres(
                normalizarTextoObligatorio(
                        datosRegistro.getNombres(),
                        "Los nombres son obligatorios."
                )
        );

        paciente.setApellidos(
                normalizarTextoObligatorio(
                        datosRegistro.getApellidos(),
                        "Los apellidos son obligatorios."
                )
        );

        paciente.setCedula(
                normalizarTextoObligatorio(
                        datosRegistro.getCedula(),
                        "La cédula es obligatoria."
                )
        );

        paciente.setFechaNacimiento(
                convertirFechaNacimiento(
                        datosRegistro.getFechaNacimiento()
                )
        );

        paciente.setTelefono(
                normalizarTextoOpcional(
                        datosRegistro.getTelefono()
                )
        );

        paciente.setCorreo(
                normalizarTextoOpcional(
                        datosRegistro.getCorreo()
                )
        );

        paciente.setDireccion(
                normalizarTextoOpcional(
                        datosRegistro.getDireccion()
                )
        );

        paciente.setGrupoSanguineo(
                normalizarTextoOpcional(
                        datosRegistro.getGrupoSanguineo()
                )
        );

        paciente.setContactoEmergencia(
                normalizarTextoOpcional(
                        datosRegistro.getContactoEmergencia()
                )
        );

        paciente.setTelefonoEmergencia(
                normalizarTextoOpcional(
                        datosRegistro.getTelefonoEmergencia()
                )
        );

        registrar(paciente);

        return paciente;
    }

    private void registrar(
            Paciente paciente
    ) throws SQLException {

        if (paciente == null) {
            throw new SQLException(
                    "El paciente no puede ser nulo"
            );
        }

        long cedulaNumerica =
                convertirCedula(
                        paciente.getCedula()
                );

        String grupoSanguineoNormalizado =
                validarYNormalizarGrupoSanguineo(
                        paciente.getGrupoSanguineo()
                );

        paciente.setGrupoSanguineo(
                grupoSanguineoNormalizado
        );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            try {
                conexion.setAutoCommit(false);

                if (personaDAO.existeCedula(
                        conexion,
                        cedulaNumerica
                )) {
                    throw new SQLException(
                            "La cédula ya está registrada."
                    );
                }

                if (personaDAO.existeCorreo(
                        conexion,
                        paciente.getCorreo()
                )) {
                    throw new SQLException(
                            "El correo electrónico ya está registrado."
                    );
                }

                personaDAO.registrar(
                        conexion,
                        paciente
                );

                pacienteDAO.registrar(
                        conexion,
                        paciente
                );

                conexion.commit();

            } catch (SQLException error) {

                try {
                    conexion.rollback();

                } catch (SQLException errorRollback) {
                    error.addSuppressed(
                            errorRollback
                    );
                }

                throw error;
            }
        }
    }

    public Optional<Paciente> buscarPorId(
            long idPaciente
    ) throws SQLException {

        if (idPaciente <= 0) {
            throw new SQLException(
                    "El ID del paciente debe ser mayor que cero."
            );
        }

        return pacienteDAO.buscarPorId(
                idPaciente
        );
    }

    private static LocalDate convertirFechaNacimiento(
            String fechaNacimiento
    ) throws SQLException {

        if (fechaNacimiento == null
                || fechaNacimiento.isBlank()) {

            return null;
        }

        try {
            LocalDate fecha =
                    LocalDate.parse(
                            fechaNacimiento.strip()
                    );

            if (fecha.isAfter(LocalDate.now())) {
                throw new SQLException(
                        "La fecha de nacimiento no puede ser futura."
                );
            }

            return fecha;

        } catch (DateTimeParseException error) {
            throw new SQLException(
                    "La fecha de nacimiento no es válida.",
                    error
            );
        }
    }

    private static String validarYNormalizarGrupoSanguineo(
            String grupoSanguineo
    ) throws SQLException {

        if (grupoSanguineo == null
                || grupoSanguineo.isBlank()) {

            return null;
        }

        String grupoNormalizado =
                grupoSanguineo
                        .strip()
                        .toUpperCase(Locale.ROOT);

        if (!GRUPOS_SANGUINEOS_VALIDOS.contains(
                grupoNormalizado
        )) {
            throw new SQLException(
                    "El grupo sanguíneo no es válido."
            );
        }

        return grupoNormalizado;
    }

    private static long convertirCedula(
            String cedula
    ) throws SQLException {

        if (cedula == null || cedula.isBlank()) {
            throw new SQLException(
                    "La cédula es obligatoria."
            );
        }

        String cedulaLimpia =
                cedula.strip();

        if (!cedulaLimpia.matches("\\d+")) {
            throw new SQLException(
                    "La cédula debe contener solamente números."
            );
        }

        try {
            long cedulaNumerica =
                    Long.parseLong(
                            cedulaLimpia
                    );

            if (cedulaNumerica <= 0) {
                throw new SQLException(
                        "La cédula debe ser mayor que cero."
                );
            }

            return cedulaNumerica;

        } catch (NumberFormatException error) {
            throw new SQLException(
                    "El número de cédula no es válido.",
                    error
            );
        }
    }

    private static String normalizarTextoObligatorio(
            String valor,
            String mensajeError
    ) throws SQLException {

        if (valor == null || valor.isBlank()) {
            throw new SQLException(
                    mensajeError
            );
        }

        return valor.strip();
    }

    private static String normalizarTextoOpcional(
            String valor
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.strip();
    }
}