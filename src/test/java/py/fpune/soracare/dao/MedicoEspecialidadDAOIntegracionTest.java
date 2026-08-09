package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MedicoEspecialidadDAOIntegracionTest {

    public static void main(
            String[] args
    ) throws Exception {

        PersonaDAO personaDAO =
                new PersonaDAO();

        MedicoDAO medicoDAO =
                new MedicoDAO();

        EspecialidadDAO especialidadDAO =
                new EspecialidadDAO();

        MedicoEspecialidadDAO relacionDAO =
                new MedicoEspecialidadDAO();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        long cedulaTemporal =
                generarCedulaTemporal();

        String matriculaTemporal =
                "ME-"
                        + identificadorTemporal
                        .toUpperCase(Locale.ROOT);

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
                                cedulaTemporal,
                                matriculaTemporal
                        );

                personaDAO.registrar(
                        conexion,
                        medico
                );

                medicoDAO.registrar(
                        conexion,
                        medico
                );

                if (medico.getIdMedico() == null
                        || medico.getIdMedico() <= 0) {

                    throw new AssertionError(
                            "El médico temporal no recibió "
                                    + "un ID válido."
                    );
                }

                System.out.println(
                        "El médico temporal fue creado."
                );

                Especialidad especialidadPrincipal =
                        crearEspecialidadTemporal(
                                especialidadDAO,
                                conexion,
                                "Especialidad Principal "
                                        + identificadorTemporal
                        );

                Especialidad especialidadSecundaria =
                        crearEspecialidadTemporal(
                                especialidadDAO,
                                conexion,
                                "Especialidad Secundaria "
                                        + identificadorTemporal
                        );

                System.out.println(
                        "Las especialidades temporales fueron creadas."
                );

                MedicoEspecialidad relacionPrincipal =
                        new MedicoEspecialidad(
                                medico.getIdMedico(),
                                especialidadPrincipal
                                        .getIdEspecialidad(),
                                true,
                                LocalDate.now()
                                        .minusYears(2)
                        );

                MedicoEspecialidad relacionPrincipalCreada =
                        relacionDAO.crear(
                                conexion,
                                relacionPrincipal
                        );

                verificarRelacionCreada(
                        relacionPrincipalCreada,
                        medico,
                        especialidadPrincipal,
                        true
                );

                System.out.println(
                        "La especialidad principal fue asignada."
                );

                MedicoEspecialidad relacionSecundaria =
                        new MedicoEspecialidad(
                                medico.getIdMedico(),
                                especialidadSecundaria
                                        .getIdEspecialidad(),
                                false,
                                LocalDate.now()
                                        .minusYears(1)
                        );

                MedicoEspecialidad relacionSecundariaCreada =
                        relacionDAO.crear(
                                conexion,
                                relacionSecundaria
                        );

                verificarRelacionCreada(
                        relacionSecundariaCreada,
                        medico,
                        especialidadSecundaria,
                        false
                );

                System.out.println(
                        "La especialidad secundaria fue asignada."
                );

                probarBusquedaPorIds(
                        relacionDAO,
                        conexion,
                        medico,
                        especialidadPrincipal
                );

                probarBusquedaPrincipalActiva(
                        relacionDAO,
                        conexion,
                        medico,
                        especialidadPrincipal
                );

                probarListadoPorMedico(
                        relacionDAO,
                        conexion,
                        medico,
                        especialidadPrincipal,
                        especialidadSecundaria
                );

                probarListadoActivasPorMedico(
                        relacionDAO,
                        conexion,
                        medico,
                        especialidadPrincipal,
                        especialidadSecundaria
                );

                probarRelacionInexistente(
                        relacionDAO,
                        conexion,
                        medico
                );

                System.out.println();
                System.out.println(
                        "MedicoEspecialidadDAO fue probado "
                                + "correctamente."
                );

                System.out.println(
                        "La relación Médico → Especialidad "
                                + "fue verificada."
                );

                System.out.println(
                        "La especialidad principal activa "
                                + "fue localizada correctamente."
                );

                System.out.println(
                        "La especialidad secundaria "
                                + "fue registrada correctamente."
                );

                System.out.println(
                        "Los listados por médico "
                                + "fueron verificados."
                );

                System.out.println(
                        "La relación inexistente "
                                + "no fue encontrada."
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
            String identificador,
            long cedula,
            String matricula
    ) {
        Medico medico =
                new Medico();

        medico.setNombres(
                "Médico"
        );

        medico.setApellidos(
                "Especialidad "
                        + identificador
        );

        medico.setCedula(
                Long.toString(
                        cedula
                )
        );

        medico.setFechaNacimiento(
                LocalDate.of(
                        1980,
                        6,
                        15
                )
        );

        medico.setTelefono(
                "0981 000 001"
        );

        medico.setCorreo(
                "medico.especialidad."
                        + identificador
                        .toLowerCase(Locale.ROOT)
                        + "@soracare.test"
        );

        medico.setDireccion(
                "Dirección temporal"
        );

        medico.setMatriculaProfesional(
                matricula
        );

        medico.setFechaIngreso(
                LocalDate.now()
                        .minusYears(5)
        );

        medico.setEstado(
                EstadoMedico.ACTIVO
        );

        medico.setBiografia(
                "Médico temporal para prueba "
                        + "de especialidades."
        );

        return medico;
    }

    private static Especialidad crearEspecialidadTemporal(
            EspecialidadDAO especialidadDAO,
            Connection conexion,
            String nombre
    ) throws Exception {

        Especialidad especialidad =
                new Especialidad(
                        nombre,
                        "Especialidad temporal utilizada "
                                + "para una prueba de integración.",
                        30
                );

        return especialidadDAO.crear(
                conexion,
                especialidad
        );
    }

    private static void verificarRelacionCreada(
            MedicoEspecialidad relacion,
            Medico medico,
            Especialidad especialidad,
            boolean principal
    ) {

        if (relacion == null) {
            throw new AssertionError(
                    "El DAO no devolvió "
                            + "la relación creada."
            );
        }

        if (!medico.getIdMedico().equals(
                relacion.getIdMedico()
        )) {
            throw new AssertionError(
                    "El médico de la relación "
                            + "no coincide."
            );
        }

        if (!especialidad
                .getIdEspecialidad()
                .equals(
                        relacion.getIdEspecialidad()
                )) {

            throw new AssertionError(
                    "La especialidad de la relación "
                            + "no coincide."
            );
        }

        if (relacion.isPrincipal()
                != principal) {

            throw new AssertionError(
                    "El indicador de especialidad principal "
                            + "no coincide."
            );
        }

        if (!relacion.isActivo()) {
            throw new AssertionError(
                    "La relación nueva debería "
                            + "estar activa."
            );
        }

        if (relacion.getFechaDesde() == null) {
            throw new AssertionError(
                    "La relación debería tener "
                            + "una fecha de inicio."
            );
        }

        if (relacion.getFechaCreacion() == null) {
            throw new AssertionError(
                    "PostgreSQL debería haber generado "
                            + "la fecha de creación."
            );
        }
    }

    private static void probarBusquedaPorIds(
            MedicoEspecialidadDAO relacionDAO,
            Connection conexion,
            Medico medico,
            Especialidad especialidad
    ) throws Exception {

        Optional<MedicoEspecialidad> resultado =
                relacionDAO.buscarPorIds(
                        conexion,
                        medico.getIdMedico(),
                        especialidad.getIdEspecialidad()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La relación no fue encontrada "
                            + "por sus IDs."
            );
        }

        if (!resultado.get().isPrincipal()) {
            throw new AssertionError(
                    "La relación recuperada debería "
                            + "ser la principal."
            );
        }

        System.out.println(
                "La búsqueda por IDs fue verificada."
        );
    }

    private static void probarBusquedaPrincipalActiva(
            MedicoEspecialidadDAO relacionDAO,
            Connection conexion,
            Medico medico,
            Especialidad especialidadPrincipal
    ) throws Exception {

        Optional<MedicoEspecialidad> resultado =
                relacionDAO.buscarPrincipalActiva(
                        conexion,
                        medico.getIdMedico()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "No se encontró la especialidad "
                            + "principal activa."
            );
        }

        if (!especialidadPrincipal
                .getIdEspecialidad()
                .equals(
                        resultado.get()
                                .getIdEspecialidad()
                )) {

            throw new AssertionError(
                    "La especialidad principal recuperada "
                            + "no coincide."
            );
        }

        if (!resultado.get().isPrincipal()
                || !resultado.get().isActivo()) {

            throw new AssertionError(
                    "La relación recuperada debería ser "
                            + "principal y activa."
            );
        }

        System.out.println(
                "La especialidad principal activa "
                        + "fue verificada."
        );
    }

    private static void probarListadoPorMedico(
            MedicoEspecialidadDAO relacionDAO,
            Connection conexion,
            Medico medico,
            Especialidad especialidadPrincipal,
            Especialidad especialidadSecundaria
    ) throws Exception {

        List<MedicoEspecialidad> relaciones =
                relacionDAO.listarPorMedico(
                        conexion,
                        medico.getIdMedico()
                );

        verificarEspecialidadesPresentes(
                relaciones,
                especialidadPrincipal,
                especialidadSecundaria,
                "listado general"
        );

        System.out.println(
                "El listado general por médico "
                        + "fue verificado."
        );
    }

    private static void probarListadoActivasPorMedico(
            MedicoEspecialidadDAO relacionDAO,
            Connection conexion,
            Medico medico,
            Especialidad especialidadPrincipal,
            Especialidad especialidadSecundaria
    ) throws Exception {

        List<MedicoEspecialidad> relaciones =
                relacionDAO.listarActivasPorMedico(
                        conexion,
                        medico.getIdMedico()
                );

        verificarEspecialidadesPresentes(
                relaciones,
                especialidadPrincipal,
                especialidadSecundaria,
                "listado de activas"
        );

        boolean todasActivas =
                relaciones.stream()
                        .allMatch(
                                MedicoEspecialidad::isActivo
                        );

        if (!todasActivas) {
            throw new AssertionError(
                    "El listado de activas contiene "
                            + "una relación inactiva."
            );
        }

        System.out.println(
                "El listado de especialidades activas "
                        + "fue verificado."
        );
    }

    private static void verificarEspecialidadesPresentes(
            List<MedicoEspecialidad> relaciones,
            Especialidad especialidadPrincipal,
            Especialidad especialidadSecundaria,
            String descripcion
    ) {

        boolean principalEncontrada =
                relaciones.stream()
                        .anyMatch(
                                relacion ->
                                        especialidadPrincipal
                                                .getIdEspecialidad()
                                                .equals(
                                                        relacion
                                                                .getIdEspecialidad()
                                                )
                        );

        boolean secundariaEncontrada =
                relaciones.stream()
                        .anyMatch(
                                relacion ->
                                        especialidadSecundaria
                                                .getIdEspecialidad()
                                                .equals(
                                                        relacion
                                                                .getIdEspecialidad()
                                                )
                        );

        if (!principalEncontrada
                || !secundariaEncontrada) {

            throw new AssertionError(
                    "No aparecen ambas especialidades "
                            + "en el "
                            + descripcion
                            + "."
            );
        }
    }

    private static void probarRelacionInexistente(
            MedicoEspecialidadDAO relacionDAO,
            Connection conexion,
            Medico medico
    ) throws Exception {

        Optional<MedicoEspecialidad> resultado =
                relacionDAO.buscarPorIds(
                        conexion,
                        medico.getIdMedico(),
                        Short.MAX_VALUE
                );

        if (resultado.isPresent()) {
            throw new AssertionError(
                    "Una relación inexistente "
                            + "no debería ser encontrada."
            );
        }

        System.out.println(
                "La relación inexistente "
                        + "no fue encontrada."
        );
    }

    private static long generarCedulaTemporal() {

        return 6_000_000_000L
                + Math.abs(
                UUID.randomUUID()
                        .getMostSignificantBits()
                        % 900_000_000L
        );
    }
}