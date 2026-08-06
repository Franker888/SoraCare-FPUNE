package py.fpune.soracare.service;

import py.fpune.soracare.dto.SolicitudCreacionUsuarioDTO;
import py.fpune.soracare.mvc.model.Usuario;

import java.sql.SQLException;

public class CrearRecepcionistaTemporalTest {

    public static void main(
            String[] args
    ) throws SQLException {

        SolicitudCreacionUsuarioDTO solicitud =
                new SolicitudCreacionUsuarioDTO(
                        "22",
                        "prueba.recepcion",
                        "SoraCare-Recepcion-2026!",
                        "SoraCare-Recepcion-2026!",
                        "RECEPCIONISTA",
                        true
                );

        UsuarioService usuarioService =
                new UsuarioService();

        Usuario usuarioCreado =
                usuarioService.crearUsuario(
                        solicitud
                );

        if (usuarioCreado.getIdUsuario() == null
                || usuarioCreado.getIdUsuario() <= 0) {

            throw new AssertionError(
                    "No se obtuvo un ID de usuario válido."
            );
        }

        if (!"prueba.recepcion".equals(
                usuarioCreado.getNombreUsuario()
        )) {
            throw new AssertionError(
                    "El nombre de usuario creado no coincide."
            );
        }

        if (usuarioCreado.getRoles().size() != 1) {
            throw new AssertionError(
                    "La cuenta debería tener exactamente un rol."
            );
        }

        String rolAsignado =
                usuarioCreado
                        .getRoles()
                        .getFirst()
                        .getNombre();

        if (!"RECEPCIONISTA".equals(rolAsignado)) {
            throw new AssertionError(
                    "El rol asignado no es RECEPCIONISTA."
            );
        }

        System.out.println(
                "Cuenta temporal creada correctamente."
        );

        System.out.println(
                "ID de usuario: "
                        + usuarioCreado.getIdUsuario()
        );

        System.out.println(
                "Nombre de usuario: "
                        + usuarioCreado.getNombreUsuario()
        );

        System.out.println(
                "Estado: "
                        + usuarioCreado.getEstado()
        );

        System.out.println(
                "Rol: "
                        + rolAsignado
        );

        System.out.println(
                "Debe cambiar contraseña: "
                        + usuarioCreado
                        .isDebeCambiarContrasena()
        );
    }
}