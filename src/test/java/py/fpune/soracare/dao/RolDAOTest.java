package py.fpune.soracare.dao;

import py.fpune.soracare.mvc.model.Rol;

import java.sql.SQLException;
import java.util.Optional;

public class RolDAOTest {

    public static void main(
            String[] args
    ) throws SQLException {

        RolDAO rolDAO = new RolDAO();

        Optional<Rol> rolEncontrado =
                rolDAO.buscarActivoPorNombre(
                        "MEDICO"
                );

        if (rolEncontrado.isEmpty()) {
            throw new AssertionError(
                    "No se encontró el rol MEDICO."
            );
        }

        Rol rolMedico =
                rolEncontrado.get();

        if (!"MEDICO".equals(
                rolMedico.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre del rol recuperado no es correcto."
            );
        }

        if (!rolMedico.isActivo()) {
            throw new AssertionError(
                    "El rol MEDICO debería estar activo."
            );
        }

        Optional<Rol> rolInexistente =
                rolDAO.buscarActivoPorNombre(
                        "ROL_INEXISTENTE"
                );

        if (rolInexistente.isPresent()) {
            throw new AssertionError(
                    "No debería existir el rol de prueba."
            );
        }

        System.out.println(
                "Prueba de RolDAO completada correctamente."
        );

        System.out.println(
                "ID del rol: "
                        + rolMedico.getIdRol()
        );

        System.out.println(
                "Nombre: "
                        + rolMedico.getNombre()
        );

        System.out.println(
                "Descripción: "
                        + rolMedico.getDescripcion()
        );

        System.out.println(
                "Activo: "
                        + rolMedico.isActivo()
        );

        System.out.println(
                "El rol inexistente fue rechazado correctamente."
        );
    }
}