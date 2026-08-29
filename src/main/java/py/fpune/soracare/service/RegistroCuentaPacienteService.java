package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.PacienteDAO;
import py.fpune.soracare.dao.PersonaDAO;
import py.fpune.soracare.dao.RolDAO;
import py.fpune.soracare.dao.UsuarioDAO;
import py.fpune.soracare.dto.CuentaPacienteCreadaDTO;
import py.fpune.soracare.dto.RegistroCuentaPacienteDTO;
import py.fpune.soracare.mvc.model.EstadoUsuario;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.mvc.model.Rol;
import py.fpune.soracare.mvc.model.Usuario;
import py.fpune.soracare.security.CodificadorContrasenaBCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

public class RegistroCuentaPacienteService {

    private static final Set<String> GRUPOS_SANGUINEOS = Set.of(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    );

    private final PersonaDAO personaDAO = new PersonaDAO();
    private final PacienteDAO pacienteDAO = new PacienteDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();
    private final PwnedPasswordsService pwnedPasswordsService;

    public RegistroCuentaPacienteService() {
        this(new PwnedPasswordsService());
    }

    RegistroCuentaPacienteService(
            PwnedPasswordsService pwnedPasswordsService
    ) {
        this.pwnedPasswordsService = pwnedPasswordsService;
    }

    public CuentaPacienteCreadaDTO registrar(RegistroCuentaPacienteDTO datos)
            throws SQLException {
        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del registro son obligatorios."
            );
        }
        validarContrasenas(
                datos.contrasena(),
                datos.confirmacionContrasena()
        );
        pwnedPasswordsService.validar(datos.contrasena());

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                CuentaPacienteCreadaDTO cuenta = registrar(
                        conexion,
                        datos
                );
                conexion.commit();
                return cuenta;
            } catch (SQLException | RuntimeException error) {
                ejecutarRollback(conexion, error);
                throw error;
            }
        }
    }

    public CuentaPacienteCreadaDTO registrar(
            Connection conexion,
            RegistroCuentaPacienteDTO datos
    ) throws SQLException {
        if (conexion == null) {
            throw new SQLException("La conexión no puede ser nula.");
        }
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del registro son obligatorios.");
        }

        Paciente paciente = prepararPaciente(datos);
        String nombreUsuario = normalizarUsuario(datos.nombreUsuario());
        validarContrasenas(datos.contrasena(), datos.confirmacionContrasena());
        String hash = CodificadorContrasenaBCrypt.codificar(datos.contrasena());

        long cedula = Long.parseLong(paciente.getCedula());
        if (personaDAO.existeCedula(conexion, cedula)) {
            throw new IllegalArgumentException("La cédula ya está registrada.");
        }
        if (personaDAO.existeCorreo(conexion, paciente.getCorreo())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }
        if (usuarioDAO.existePorNombreUsuario(conexion, nombreUsuario)) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
        }

        Rol rolPaciente = rolDAO.buscarActivoPorNombre(conexion, "PACIENTE")
                .orElseThrow(() -> new SQLException(
                        "El rol PACIENTE no existe o está inactivo."));

        personaDAO.registrar(conexion, paciente);
        pacienteDAO.registrar(conexion, paciente);

        Usuario usuario = new Usuario();
        usuario.setIdPersona(paciente.getId());
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setContrasenaHash(hash);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos((short) 0);
        usuario.setDebeCambiarContrasena(false);
        long idUsuario = usuarioDAO.registrar(conexion, usuario);
        usuarioDAO.asignarRol(conexion, idUsuario, rolPaciente.getIdRol(), null);

        return new CuentaPacienteCreadaDTO(
                paciente.getIdPaciente(), idUsuario, nombreUsuario);
    }

    private static Paciente prepararPaciente(RegistroCuentaPacienteDTO datos) {
        Paciente paciente = new Paciente();
        paciente.setNombres(textoObligatorio(datos.nombres(), 100,
                "Los nombres son obligatorios.", "Los nombres no pueden superar 100 caracteres."));
        paciente.setApellidos(textoObligatorio(datos.apellidos(), 100,
                "Los apellidos son obligatorios.", "Los apellidos no pueden superar 100 caracteres."));
        paciente.setCedula(normalizarCedula(datos.cedula()));
        paciente.setFechaNacimiento(convertirFecha(datos.fechaNacimiento()));
        paciente.setTelefono(textoOpcional(datos.telefono(), 30,
                "El teléfono no puede superar 30 caracteres."));
        paciente.setCorreo(normalizarCorreo(datos.correo()));
        paciente.setDireccion(textoOpcional(datos.direccion(), 255,
                "La dirección no puede superar 255 caracteres."));
        paciente.setGrupoSanguineo(normalizarGrupo(datos.grupoSanguineo()));
        paciente.setContactoEmergencia(textoOpcional(datos.contactoEmergencia(), 150,
                "El contacto de emergencia no puede superar 150 caracteres."));
        paciente.setTelefonoEmergencia(textoOpcional(datos.telefonoEmergencia(), 30,
                "El teléfono de emergencia no puede superar 30 caracteres."));
        return paciente;
    }

    private static String normalizarCedula(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La cédula es obligatoria.");
        }
        String cedula = valor.strip();
        if (!cedula.matches("\\d+")) {
            throw new IllegalArgumentException("La cédula debe contener solamente números.");
        }
        try {
            if (Long.parseLong(cedula) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La cédula no es válida.", error);
        }
        return cedula;
    }

    private static LocalDate convertirFecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            if (fecha.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
            }
            return fecha;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.", error);
        }
    }

    private static String normalizarCorreo(String valor) {
        String correo = textoObligatorio(valor, 150,
                "El correo electrónico es obligatorio.",
                "El correo electrónico no puede superar 150 caracteres.")
                .toLowerCase(Locale.ROOT);
        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }
        return correo;
    }

    private static String normalizarGrupo(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String grupo = valor.strip().toUpperCase(Locale.ROOT);
        if (!GRUPOS_SANGUINEOS.contains(grupo)) {
            throw new IllegalArgumentException("El grupo sanguíneo no es válido.");
        }
        return grupo;
    }

    private static String normalizarUsuario(String valor) {
        String usuario = textoObligatorio(valor, 50,
                "El nombre de usuario es obligatorio.",
                "El nombre de usuario no puede superar 50 caracteres.")
                .toLowerCase(Locale.ROOT);
        if (usuario.length() < 4) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 4 y 50 caracteres.");
        }
        if (!usuario.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos.");
        }
        return usuario;
    }

    private static void validarContrasenas(String contrasena, String confirmacion) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (confirmacion == null || confirmacion.isBlank()) {
            throw new IllegalArgumentException("Debe confirmar la contraseña.");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        if (contrasena.length() < 10 || contrasena.length() > 72) {
            throw new IllegalArgumentException("La contraseña debe tener entre 10 y 72 caracteres.");
        }
        if (!contrasena.matches(".*[A-Z].*") || !contrasena.matches(".*[a-z].*")
                || !contrasena.matches(".*\\d.*") || !contrasena.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException(
                    "La contraseña debe incluir mayúscula, minúscula, número y carácter especial.");
        }
    }

    private static String textoObligatorio(
            String valor, int maximo, String vacio, String largo
    ) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(vacio);
        String texto = valor.strip();
        if (texto.length() > maximo) throw new IllegalArgumentException(largo);
        return texto;
    }

    private static String textoOpcional(String valor, int maximo, String largo) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.strip();
        if (texto.length() > maximo) throw new IllegalArgumentException(largo);
        return texto;
    }

    private static void ejecutarRollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException error) {
            original.addSuppressed(error);
        }
    }
}
