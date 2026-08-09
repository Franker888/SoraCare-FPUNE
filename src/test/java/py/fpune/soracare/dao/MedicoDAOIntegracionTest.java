package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MedicoDAOIntegracionTest {

    public static void main(
            String[] args
    ) throws Exception {

        PersonaDAO personaDAO =
                new PersonaDAO();

        MedicoDAO medicoDAO =
                new MedicoDAO();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String matriculaTemporal =
                "MP-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

        long cedulaTemporal =
                8_000_000_000L
                        + Math.abs(
                        UUID.randomUUID()
                                .getMostSignificantBits()
                                % 900_000_000L
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                Medico medico =
                        crearMedicoTemporal(
                                identificadorTemporal,
                                matriculaTemporal,
                                cedulaTemporal
                        );

                personaDAO.registrar(
                        conexion,
                        medico
                );

                if (medico.getId() == null
                        || medico.getId() <= 0) {

                    throw new AssertionError(
                            "La persona del médico "
                                    + "no recibió un ID válido."
                    );
                }

                System.out.println(
                        "La persona temporal fue creada."
                );

                medicoDAO.registrar(
                        conexion,
                        medico
                );

                if (medico.getIdMedico() == null
                        || medico.getIdMedico() <= 0) {

                    throw new AssertionError(
                            "El médico no recibió "
                                    + "un ID válido."
                    );
                }

                System.out.println(
                        "El médico temporal fue creado."
                );

                probarExistenciaMatricula(
                        medicoDAO,
                        conexion,
                        matriculaTemporal
                );

                probarBusquedaPorId(
                        medicoDAO,
                        conexion,
                        medico
                );

                probarBusquedaPorMatricula(
                        medicoDAO,
                        conexion,
                        medico
                );

                probarListadoGeneral(
                        medicoDAO,
                        conexion,
                        medico
                );

                probarListadoActivos(
                        medicoDAO,
                        conexion,
                        medico
                );

                probarBusquedaInexistente(
                        medicoDAO,
                        conexion
                );

                System.out.println();
                System.out.println(
                        "MedicoDAO fue probado correctamente."
                );

                System.out.println(
                        "La relación Persona → Médico "
                                + "fue verificada."
                );

                System.out.println(
                        "La matrícula fue localizada "
                                + "sin distinguir mayúsculas "
                                + "y minúsculas."
                );

                System.out.println(
                        "La búsqueda por ID fue verificada."
                );

                System.out.println(
                        "La búsqueda por matrícula fue verificada."
                );

                System.out.println(
                        "El listado general fue verificado."
                );

                System.out.println(
                        "El listado de médicos activos "
                                + "fue verificado."
                );

                System.out.println(
                        "La búsqueda inexistente "
                                + "fue verificada."
                );

            } finally {
                conexion.rollback();

                System.out.println(
                        "La transacción fue revertida. "
                                + "No quedaron datos de prueba."
                );
            }
        }
    }

    private static Medico crearMedicoTemporal(
            String identificadorTemporal,
            String matriculaTemporal,
            long cedulaTemporal
    ) {
        Medico medico =
                new Medico();

        medico.setNombres(
                "Médico"
        );

        medico.setApellidos(
                "Temporal "
                        + identificadorTemporal
        );

        medico.setCedula(
                Long.toString(
                        cedulaTemporal
                )
        );

        medico.setFechaNacimiento(
                LocalDate.of(
                        1985,
                        5,
                        15
                )
        );

        medico.setTelefono(
                "0981 000 000"
        );

        medico.setCorreo(
                "medico."
                        + identificadorTemporal
                        .toLowerCase(Locale.ROOT)
                        + "@soracare.test"
        );

        medico.setDireccion(
                "Dirección temporal"
        );

        medico.setMatriculaProfesional(
                matriculaTemporal
        );

        medico.setFechaIngreso(
                LocalDate.now()
                        .minusYears(1)
        );

        medico.setEstado(
                EstadoMedico.ACTIVO
        );

        medico.setBiografia(
                "Médico temporal utilizado "
                        + "para la prueba de integración."
        );

        return medico;
    }

    private static void probarExistenciaMatricula(
            MedicoDAO medicoDAO,
            Connection conexion,
            String matriculaTemporal
    ) throws Exception {

        boolean existe =
                medicoDAO.existeMatricula(
                        conexion,
                        "   "
                                + matriculaTemporal
                                .toLowerCase(Locale.ROOT)
                                + "   "
                );

        if (!existe) {
            throw new AssertionError(
                    "La matrícula registrada "
                            + "debería existir."
            );
        }

        System.out.println(
                "La existencia normalizada "
                        + "de la matrícula fue verificada."
        );
    }

    private static void probarBusquedaPorId(
            MedicoDAO medicoDAO,
            Connection conexion,
            Medico medicoCreado
    ) throws Exception {

        Optional<Medico> resultado =
                medicoDAO.buscarPorId(
                        conexion,
                        medicoCreado.getIdMedico()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "El médico no fue encontrado por ID."
            );
        }

        Medico medicoRecuperado =
                resultado.get();

        if (!medicoCreado
                .getId()
                .equals(
                        medicoRecuperado.getId()
                )) {

            throw new AssertionError(
                    "El id_persona recuperado "
                            + "no coincide."
            );
        }

        if (!medicoCreado
                .getIdMedico()
                .equals(
                        medicoRecuperado
                                .getIdMedico()
                )) {

            throw new AssertionError(
                    "El id_medico recuperado "
                            + "no coincide."
            );
        }

        if (!medicoCreado
                .getMatriculaProfesional()
                .equals(
                        medicoRecuperado
                                .getMatriculaProfesional()
                )) {

            throw new AssertionError(
                    "La matrícula recuperada "
                            + "no coincide."
            );
        }

        if (medicoRecuperado.getEstado()
                != EstadoMedico.ACTIVO) {

            throw new AssertionError(
                    "El estado recuperado "
                            + "debería ser ACTIVO."
            );
        }

        System.out.println(
                "La búsqueda por ID fue verificada."
        );
    }

    private static void probarBusquedaPorMatricula(
            MedicoDAO medicoDAO,
            Connection conexion,
            Medico medicoCreado
    ) throws Exception {

        Optional<Medico> resultado =
                medicoDAO.buscarPorMatricula(
                        conexion,
                        "  "
                                + medicoCreado
                                .getMatriculaProfesional()
                                .toLowerCase(Locale.ROOT)
                                + "  "
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "El médico no fue encontrado "
                            + "por matrícula."
            );
        }

        if (!medicoCreado
                .getIdMedico()
                .equals(
                        resultado.get()
                                .getIdMedico()
                )) {

            throw new AssertionError(
                    "El médico recuperado por matrícula "
                            + "no coincide."
            );
        }

        System.out.println(
                "La búsqueda normalizada "
                        + "por matrícula fue verificada."
        );
    }

    private static void probarListadoGeneral(
            MedicoDAO medicoDAO,
            Connection conexion,
            Medico medicoCreado
    ) throws Exception {

        List<Medico> medicos =
                medicoDAO.listarTodos(
                        conexion
                );

        boolean encontrado =
                medicos.stream()
                        .anyMatch(
                                medico ->
                                        medicoCreado
                                                .getIdMedico()
                                                .equals(
                                                        medico
                                                                .getIdMedico()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El médico temporal no aparece "
                            + "en el listado general."
            );
        }

        System.out.println(
                "El listado general fue verificado."
        );
    }

    private static void probarListadoActivos(
            MedicoDAO medicoDAO,
            Connection conexion,
            Medico medicoCreado
    ) throws Exception {

        List<Medico> medicos =
                medicoDAO.listarActivos(
                        conexion
                );

        boolean encontrado =
                medicos.stream()
                        .anyMatch(
                                medico ->
                                        medicoCreado
                                                .getIdMedico()
                                                .equals(
                                                        medico
                                                                .getIdMedico()
                                                )
                        );

        if (!encontrado) {
            throw new AssertionError(
                    "El médico temporal no aparece "
                            + "en el listado de activos."
            );
        }

        System.out.println(
                "El listado de médicos activos "
                        + "fue verificado."
        );
    }

    private static void probarBusquedaInexistente(
            MedicoDAO medicoDAO,
            Connection conexion
    ) throws Exception {

        Optional<Medico> resultado =
                medicoDAO.buscarPorId(
                        conexion,
                        Long.MAX_VALUE
                );

        if (resultado.isPresent()) {
            throw new AssertionError(
                    "Un médico inexistente "
                            + "no debería ser encontrado."
            );
        }

        System.out.println(
                "El médico inexistente "
                        + "no fue encontrado."
        );
    }
}