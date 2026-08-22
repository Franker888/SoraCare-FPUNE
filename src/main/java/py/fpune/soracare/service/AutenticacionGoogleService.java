package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.IdentidadExternaDAO;
import py.fpune.soracare.dao.UsuarioDAO;
import py.fpune.soracare.dto.CredencialesAutenticacionDTO;
import py.fpune.soracare.dto.CuentaPacienteCreadaDTO;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.RegistroCuentaPacienteDTO;
import py.fpune.soracare.dto.ResultadoInicioGoogleDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Rol;
import py.fpune.soracare.mvc.model.Usuario;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class AutenticacionGoogleService {

    private final IdentidadExternaDAO identidadDAO =
            new IdentidadExternaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AutenticacionService autenticacionService =
            new AutenticacionService();
    private final RegistroCuentaPacienteService registroService =
            new RegistroCuentaPacienteService();

    public ResultadoInicioGoogleDTO iniciar(PerfilGoogleDTO perfil)
            throws SQLException {
        validarPerfil(perfil);

        OptionalLong idVinculado;

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                idVinculado = identidadDAO.buscarIdUsuarioGoogle(
                        conexion,
                        perfil.identificadorGoogle()
                );

                if (idVinculado.isPresent()) {
                    identidadDAO.actualizarUltimoAccesoGoogle(
                            conexion,
                            perfil
                    );
                    conexion.commit();
                } else {
                    Optional<Usuario> usuarioExistente =
                            usuarioDAO.buscarPorIdentificador(
                                    conexion,
                                    perfil.correo()
                            );

                    if (usuarioExistente.isPresent()) {
                        Usuario usuario = usuarioExistente.get();
                        exigirCuentaExclusivamentePaciente(usuario);

                        if (perfil.correoControladoPorGoogle()) {
                            identidadDAO.registrarGoogle(
                                    conexion,
                                    usuario.getIdUsuario(),
                                    perfil
                            );
                            conexion.commit();
                            idVinculado = OptionalLong.of(
                                    usuario.getIdUsuario()
                            );
                        } else {
                            conexion.commit();
                            return ResultadoInicioGoogleDTO
                                    .requiereVinculacion(
                                            usuario.getIdUsuario()
                                    );
                        }
                    } else {
                        conexion.commit();
                        return ResultadoInicioGoogleDTO.requiereRegistro();
                    }
                }
            } catch (SQLException | RuntimeException error) {
                ejecutarRollback(conexion, error);
                throw error;
            }
        }

        return ResultadoInicioGoogleDTO.autenticado(
                autenticarPacienteExterno(idVinculado.getAsLong())
        );
    }

    public UsuarioAutenticadoDTO vincularCuentaExistente(
            PerfilGoogleDTO perfil,
            long idUsuarioEsperado,
            String contrasena
    ) throws SQLException {
        validarPerfil(perfil);
        if (idUsuarioEsperado <= 0) {
            throw new SecurityException(
                    "La solicitud de vinculación ya no es válida."
            );
        }

        UsuarioAutenticadoDTO autenticado = autenticacionService.autenticar(
                new CredencialesAutenticacionDTO(
                        perfil.correo(),
                        contrasena
                )
        );
        if (autenticado.getIdUsuario() != idUsuarioEsperado
                || !esExclusivamentePaciente(autenticado)) {
            throw new SecurityException(
                    "La cuenta de Google solo puede vincularse con un perfil de paciente."
            );
        }

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                identidadDAO.registrarGoogle(
                        conexion,
                        autenticado.getIdUsuario(),
                        perfil
                );
                conexion.commit();
            } catch (SQLException | RuntimeException error) {
                ejecutarRollback(conexion, error);
                throw error;
            }
        }
        return autenticado;
    }

    public UsuarioAutenticadoDTO registrarPaciente(
            PerfilGoogleDTO perfil,
            RegistroCuentaPacienteDTO datosFormulario
    ) throws SQLException {
        validarPerfil(perfil);
        if (datosFormulario == null) {
            throw new IllegalArgumentException(
                    "Los datos para completar el perfil son obligatorios."
            );
        }

        String claveInterna = UUID.randomUUID() + "Aa1!";
        RegistroCuentaPacienteDTO datos = new RegistroCuentaPacienteDTO(
                datosFormulario.nombres(),
                datosFormulario.apellidos(),
                datosFormulario.cedula(),
                datosFormulario.fechaNacimiento(),
                datosFormulario.telefono(),
                perfil.correo(),
                datosFormulario.direccion(),
                datosFormulario.grupoSanguineo(),
                datosFormulario.contactoEmergencia(),
                datosFormulario.telefonoEmergencia(),
                datosFormulario.nombreUsuario(),
                claveInterna,
                claveInterna
        );

        CuentaPacienteCreadaDTO cuenta;
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                cuenta = registroService.registrar(conexion, datos);
                identidadDAO.registrarGoogle(
                        conexion,
                        cuenta.idUsuario(),
                        perfil
                );
                conexion.commit();
            } catch (SQLException | RuntimeException error) {
                ejecutarRollback(conexion, error);
                throw error;
            }
        }

        return autenticarPacienteExterno(cuenta.idUsuario());
    }

    private UsuarioAutenticadoDTO autenticarPacienteExterno(
            long idUsuario
    ) throws SQLException {
        UsuarioAutenticadoDTO usuario =
                autenticacionService.autenticarExterno(idUsuario);
        if (!esExclusivamentePaciente(usuario)) {
            throw new SecurityException(
                    "El acceso con Google está habilitado exclusivamente para pacientes."
            );
        }
        return usuario;
    }

    private static void exigirCuentaExclusivamentePaciente(
            Usuario usuario
    ) {
        boolean paciente = false;
        for (Rol rol : usuario.getRoles()) {
            if (rol == null || rol.getNombre() == null) {
                continue;
            }
            if ("PACIENTE".equalsIgnoreCase(rol.getNombre())) {
                paciente = true;
            } else {
                throw new SecurityException(
                        "Las cuentas internas del personal no pueden vincularse con Google."
                );
            }
        }
        if (!paciente) {
            throw new SecurityException(
                    "La cuenta existente no corresponde a un paciente."
            );
        }
    }

    private static boolean esExclusivamentePaciente(
            UsuarioAutenticadoDTO usuario
    ) {
        return usuario != null
                && usuario.getRoles().size() == 1
                && usuario.tieneRol("PACIENTE");
    }

    private static void validarPerfil(PerfilGoogleDTO perfil) {
        if (perfil == null
                || perfil.identificadorGoogle() == null
                || perfil.identificadorGoogle().isBlank()
                || perfil.correo() == null
                || perfil.correo().isBlank()
                || !perfil.correoVerificado()) {
            throw new SecurityException(
                    "La identidad proporcionada por Google no es válida."
            );
        }
    }

    private static void ejecutarRollback(
            Connection conexion,
            Exception original
    ) {
        try {
            conexion.rollback();
        } catch (SQLException error) {
            original.addSuppressed(error);
        }
    }
}
