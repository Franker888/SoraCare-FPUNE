package py.fpune.soracare.service;

import py.fpune.soracare.dto.SolicitudCreacionUsuarioDTO;
import py.fpune.soracare.mvc.model.Usuario;

import java.sql.SQLException;

public class UsuarioServiceIntegracionTest {

    public static void main(
            String[] args
    ) throws SQLException {

        String contrasenaTemporal =
                "SoraCare-Prueba-2026!";

        SolicitudCreacionUsuarioDTO solicitud =
                new SolicitudCreacionUsuarioDTO(
                        "21",
                        "prueba.seguridad",
                        contrasenaTemporal,
                        contrasenaTemporal,
                        "ADMINISTRADOR",
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

        if (usuarioCreado.getContrasenaHash() == null
                || usuarioCreado.getContrasenaHash().isBlank()) {

            throw new AssertionError(
                    "No se generó el hash de la contraseña."
            );
        }

        if (contrasenaTemporal.equals(
                usuarioCreado.getContrasenaHash()
        )) {
            throw new AssertionError(
                    "La contraseña original no puede quedar almacenada."
            );
        }

        if (usuarioCreado.getRoles().size() != 1) {
            throw new AssertionError(
                    "El usuario debería tener exactamente un rol."
            );
        }

        if (!"ADMINISTRADOR".equals(
                usuarioCreado
                        .getRoles()
                        .getFirst()
                        .getNombre()
        )) {
            throw new AssertionError(
                    "El rol asignado no es ADMINISTRADOR."
            );
        }

        System.out.println(
                "Creación de usuario completada correctamente."
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
                        + usuarioCreado
                        .getRoles()
                        .getFirst()
                        .getNombre()
        );

        System.out.println(
                "Debe cambiar contraseña: "
                        + usuarioCreado
                        .isDebeCambiarContrasena()
        );

        System.out.println(
                "La contraseña fue almacenada como hash BCrypt."
        );
    }
}