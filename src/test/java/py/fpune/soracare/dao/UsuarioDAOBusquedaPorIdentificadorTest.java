package py.fpune.soracare.dao;

import py.fpune.soracare.mvc.model.EstadoUsuario;
import py.fpune.soracare.mvc.model.Usuario;

import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAOBusquedaPorIdentificadorTest {

    public static void main(
            String[] args
    ) throws SQLException {

        UsuarioDAO usuarioDAO =
                new UsuarioDAO();

        Optional<Usuario> resultadoPorUsuario =
                usuarioDAO.buscarPorIdentificador(
                        "prueba.seguridad"
                );

        if (resultadoPorUsuario.isEmpty()) {
            throw new AssertionError(
                    "No se encontró la cuenta por nombre de usuario."
            );
        }

        Usuario usuarioPorNombre =
                resultadoPorUsuario.get();

        Optional<Usuario> resultadoPorCorreo =
                usuarioDAO.buscarPorIdentificador(
                        "prueba.usuario.seguridad@soracare.test"
                );

        if (resultadoPorCorreo.isEmpty()) {
            throw new AssertionError(
                    "No se encontró la cuenta por correo electrónico."
            );
        }

        Usuario usuarioPorCorreo =
                resultadoPorCorreo.get();

        if (!usuarioPorNombre.getIdUsuario()
                .equals(usuarioPorCorreo.getIdUsuario())) {

            throw new AssertionError(
                    "El nombre de usuario y el correo "
                            + "deberían recuperar la misma cuenta."
            );
        }

        if (usuarioPorNombre.getEstado()
                != EstadoUsuario.ACTIVO) {

            throw new AssertionError(
                    "La cuenta debería estar en estado ACTIVO."
            );
        }

        if (usuarioPorNombre.getContrasenaHash() == null
                || usuarioPorNombre
                .getContrasenaHash()
                .isBlank()) {

            throw new AssertionError(
                    "No se recuperó el hash de la contraseña."
            );
        }

        if (usuarioPorNombre.getRoles().size() != 1) {
            throw new AssertionError(
                    "La cuenta debería tener exactamente un rol."
            );
        }

        String nombreRol =
                usuarioPorNombre
                        .getRoles()
                        .getFirst()
                        .getNombre();

        if (!"ADMINISTRADOR".equals(nombreRol)) {
            throw new AssertionError(
                    "El rol recuperado no es ADMINISTRADOR."
            );
        }

        Optional<Usuario> resultadoInexistente =
                usuarioDAO.buscarPorIdentificador(
                        "usuario.inexistente"
                );

        if (resultadoInexistente.isPresent()) {
            throw new AssertionError(
                    "No debería encontrarse un usuario inexistente."
            );
        }

        System.out.println(
                "Búsqueda de usuario completada correctamente."
        );

        System.out.println(
                "ID de usuario: "
                        + usuarioPorNombre.getIdUsuario()
        );

        System.out.println(
                "Nombre de usuario: "
                        + usuarioPorNombre.getNombreUsuario()
        );

        System.out.println(
                "Estado: "
                        + usuarioPorNombre.getEstado()
        );

        System.out.println(
                "Rol recuperado: "
                        + nombreRol
        );

        System.out.println(
                "La cuenta fue encontrada por nombre de usuario."
        );

        System.out.println(
                "La misma cuenta fue encontrada por correo."
        );

        System.out.println(
                "El usuario inexistente fue rechazado correctamente."
        );
    }
}