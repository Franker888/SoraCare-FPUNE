package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.PersonaDAO;
import py.fpune.soracare.dto.RegistroMedicoDTO;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class MedicoService {

    private final PersonaDAO personaDAO =
            new PersonaDAO();

    private final MedicoDAO medicoDAO =
            new MedicoDAO();

    public Medico registrar(
            RegistroMedicoDTO datosRegistro
    ) throws SQLException {

        if (datosRegistro == null) {
            throw new SQLException(
                    "Los datos del registro no pueden ser nulos."
            );
        }

        Medico medico =
                new Medico();

        medico.setNombres(
                normalizarTextoObligatorio(
                        datosRegistro.getNombres(),
                        "Los nombres son obligatorios.",
                        100
                )
        );

        medico.setApellidos(
                normalizarTextoObligatorio(
                        datosRegistro.getApellidos(),
                        "Los apellidos son obligatorios.",
                        100
                )
        );

        medico.setCedula(
                normalizarTextoObligatorio(
                        datosRegistro.getCedula(),
                        "La cédula es obligatoria.",
                        30
                )
        );

        medico.setFechaNacimiento(
                convertirFechaNacimiento(
                        datosRegistro.getFechaNacimiento()
                )
        );

        medico.setTelefono(
                normalizarTextoOpcional(
                        datosRegistro.getTelefono(),
                        30,
                        "El teléfono no puede superar "
                                + "los 30 caracteres."
                )
        );

        medico.setCorreo(
                normalizarTextoOpcional(
                        datosRegistro.getCorreo(),
                        150,
                        "El correo electrónico no puede superar "
                                + "los 150 caracteres."
                )
        );

        medico.setDireccion(
                normalizarTextoOpcional(
                        datosRegistro.getDireccion(),
                        255,
                        "La dirección no puede superar "
                                + "los 255 caracteres."
                )
        );

        medico.setMatriculaProfesional(
                normalizarTextoObligatorio(
                        datosRegistro.getMatriculaProfesional(),
                        "La matrícula profesional es obligatoria.",
                        50
                )
        );

        medico.setFechaIngreso(
                convertirFechaIngreso(
                        datosRegistro.getFechaIngreso()
                )
        );

        medico.setEstado(
                EstadoMedico.ACTIVO
        );

        medico.setBiografia(
                normalizarTextoOpcional(
                        datosRegistro.getBiografia(),
                        1000,
                        "La biografía no puede superar "
                                + "los 1000 caracteres."
                )
        );

        registrar(
                medico
        );

        return medico;
    }

    private void registrar(
            Medico medico
    ) throws SQLException {

        if (medico == null) {
            throw new SQLException(
                    "El médico no puede ser nulo."
            );
        }

        long cedulaNumerica =
                convertirCedula(
                        medico.getCedula()
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
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
                        medico.getCorreo()
                )) {
                    throw new SQLException(
                            "El correo electrónico ya está registrado."
                    );
                }

                if (medicoDAO.existeMatricula(
                        conexion,
                        medico.getMatriculaProfesional()
                )) {
                    throw new SQLException(
                            "La matrícula profesional ya está registrada."
                    );
                }

                personaDAO.registrar(
                        conexion,
                        medico
                );

                medicoDAO.registrar(
                        conexion,
                        medico
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

    public Optional<Medico> buscarPorId(
            long idMedico
    ) throws SQLException {

        if (idMedico <= 0) {
            throw new SQLException(
                    "El ID del médico debe ser mayor que cero."
            );
        }

        return medicoDAO.buscarPorId(
                idMedico
        );
    }

    public Optional<Medico> buscarPorMatricula(
            String matriculaProfesional
    ) throws SQLException {

        if (matriculaProfesional == null
                || matriculaProfesional.isBlank()) {

            throw new SQLException(
                    "La matrícula profesional es obligatoria."
            );
        }

        return medicoDAO.buscarPorMatricula(
                matriculaProfesional.strip()
        );
    }

    public List<Medico> listarTodos()
            throws SQLException {

        return medicoDAO.listarTodos();
    }

    public List<Medico> listarActivos()
            throws SQLException {

        return medicoDAO.listarActivos();
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

            if (fecha.isAfter(
                    LocalDate.now()
            )) {
                throw new SQLException(
                        "La fecha de nacimiento "
                                + "no puede ser futura."
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

    private static LocalDate convertirFechaIngreso(
            String fechaIngreso
    ) throws SQLException {

        if (fechaIngreso == null
                || fechaIngreso.isBlank()) {

            return null;
        }

        try {
            LocalDate fecha =
                    LocalDate.parse(
                            fechaIngreso.strip()
                    );

            if (fecha.isAfter(
                    LocalDate.now()
            )) {
                throw new SQLException(
                        "La fecha de ingreso "
                                + "no puede ser futura."
                );
            }

            return fecha;

        } catch (DateTimeParseException error) {

            throw new SQLException(
                    "La fecha de ingreso no es válida.",
                    error
            );
        }
    }

    private static long convertirCedula(
            String cedula
    ) throws SQLException {

        if (cedula == null
                || cedula.isBlank()) {

            throw new SQLException(
                    "La cédula es obligatoria."
            );
        }

        String cedulaLimpia =
                cedula.strip();

        if (!cedulaLimpia.matches(
                "\\d+"
        )) {
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
            String mensajeError,
            int longitudMaxima
    ) throws SQLException {

        if (valor == null
                || valor.isBlank()) {

            throw new SQLException(
                    mensajeError
            );
        }

        String valorNormalizado =
                valor.strip();

        if (valorNormalizado.length()
                > longitudMaxima) {

            throw new SQLException(
                    "El valor ingresado supera "
                            + "la longitud máxima permitida "
                            + "de "
                            + longitudMaxima
                            + " caracteres."
            );
        }

        return valorNormalizado;
    }

    private static String normalizarTextoOpcional(
            String valor,
            int longitudMaxima,
            String mensajeError
    ) throws SQLException {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        String valorNormalizado =
                valor.strip();

        if (valorNormalizado.length()
                > longitudMaxima) {

            throw new SQLException(
                    mensajeError
            );
        }

        return valorNormalizado;
    }
}