package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.RegistroEspecialidadDTO;
import py.fpune.soracare.mvc.model.Especialidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EspecialidadServiceIntegracionTest {

    private static final String SQL_ELIMINAR_POR_NOMBRE = """
            DELETE FROM soracare.especialidades
            WHERE LOWER(BTRIM(nombre)) =
                  LOWER(BTRIM(?))
            """;

    public static void main(
            String[] args
    ) throws Exception {

        EspecialidadService servicio =
                new EspecialidadService();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String nombreTemporal =
                "Especialidad Servicio "
                        + identificadorTemporal;

        try {
            probarSolicitudNula(
                    servicio
            );

            probarNombreVacio(
                    servicio
            );

            probarDuracionNoNumerica(
                    servicio
            );

            probarDuracionFueraDeRango(
                    servicio
            );

            Especialidad especialidadCreada =
                    probarRegistroCorrecto(
                            servicio,
                            nombreTemporal
                    );

            probarBusquedaPorId(
                    servicio,
                    especialidadCreada
            );

            probarListadoGeneral(
                    servicio,
                    especialidadCreada
            );

            probarListadoActivo(
                    servicio,
                    especialidadCreada
            );

            probarNombreDuplicado(
                    servicio,
                    nombreTemporal
            );

            System.out.println();
            System.out.println(
                    "EspecialidadService fue probado correctamente."
            );

            System.out.println(
                    "La solicitud nula fue rechazada."
            );

            System.out.println(
                    "El nombre vacío fue rechazado."
            );

            System.out.println(
                    "La duración no numérica fue rechazada."
            );

            System.out.println(
                    "La duración fuera de rango fue rechazada."
            );

            System.out.println(
                    "La especialidad fue registrada correctamente."
            );

            System.out.println(
                    "La búsqueda y los listados fueron verificados."
            );

            System.out.println(
                    "El nombre duplicado fue rechazado."
            );

        } finally {
            eliminarEspecialidadTemporal(
                    nombreTemporal
            );

            verificarEliminacion(
                    servicio,
                    nombreTemporal
            );

            System.out.println(
                    "La especialidad temporal fue eliminada. "
                            + "No quedaron datos de prueba."
            );
        }
    }

    private static void probarSolicitudNula(
            EspecialidadService servicio
    ) throws SQLException {

        try {
            servicio.registrar(
                    null
            );

            throw new AssertionError(
                    "Una solicitud nula debería ser rechazada."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    "Solicitud nula: RECHAZADA."
            );
        }
    }

    private static void probarNombreVacio(
            EspecialidadService servicio
    ) throws SQLException {

        RegistroEspecialidadDTO datos =
                new RegistroEspecialidadDTO(
                        "   ",
                        "Descripción de prueba.",
                        "30"
                );

        try {
            servicio.registrar(
                    datos
            );

            throw new AssertionError(
                    "Un nombre vacío debería ser rechazado."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    "Nombre vacío: RECHAZADO."
            );
        }
    }

    private static void probarDuracionNoNumerica(
            EspecialidadService servicio
    ) throws SQLException {

        RegistroEspecialidadDTO datos =
                new RegistroEspecialidadDTO(
                        "Especialidad Duración No Numérica",
                        null,
                        "treinta"
                );

        try {
            servicio.registrar(
                    datos
            );

            throw new AssertionError(
                    "Una duración no numérica debería ser rechazada."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    "Duración no numérica: RECHAZADA."
            );
        }
    }

    private static void probarDuracionFueraDeRango(
            EspecialidadService servicio
    ) throws SQLException {

        RegistroEspecialidadDTO datos =
                new RegistroEspecialidadDTO(
                        "Especialidad Duración Inválida",
                        null,
                        "5"
                );

        try {
            servicio.registrar(
                    datos
            );

            throw new AssertionError(
                    "Una duración menor que diez minutos "
                            + "debería ser rechazada."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    "Duración fuera de rango: RECHAZADA."
            );
        }
    }

    private static Especialidad probarRegistroCorrecto(
            EspecialidadService servicio,
            String nombreTemporal
    ) throws SQLException {

        RegistroEspecialidadDTO datos =
                new RegistroEspecialidadDTO(
                        "  " + nombreTemporal + "  ",
                        "  Especialidad temporal para probar "
                                + "la capa de servicio.  ",
                        "45"
                );

        Especialidad especialidad =
                servicio.registrar(
                        datos
                );

        if (especialidad == null) {
            throw new AssertionError(
                    "El servicio no devolvió "
                            + "la especialidad registrada."
            );
        }

        if (especialidad.getIdEspecialidad() == null
                || especialidad.getIdEspecialidad() <= 0) {

            throw new AssertionError(
                    "La especialidad registrada "
                            + "no posee un ID válido."
            );
        }

        if (!nombreTemporal.equals(
                especialidad.getNombre()
        )) {
            throw new AssertionError(
                    "El nombre no fue normalizado correctamente."
            );
        }

        if (especialidad.getDuracionConsultaMinutos()
                != 45) {

            throw new AssertionError(
                    "La duración registrada no coincide."
            );
        }

        if (!especialidad.isActivo()) {
            throw new AssertionError(
                    "La especialidad nueva debería estar activa."
            );
        }

        System.out.println(
                "Registro correcto: ESPECIALIDAD CREADA."
        );

        return especialidad;
    }

    private static void probarBusquedaPorId(
            EspecialidadService servicio,
            Especialidad especialidadCreada
    ) throws SQLException {

        Optional<Especialidad> resultado =
                servicio.buscarPorId(
                        especialidadCreada
                                .getIdEspecialidad()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "La especialidad no fue encontrada por ID."
            );
        }

        if (!especialidadCreada.getNombre().equals(
                resultado.get().getNombre()
        )) {
            throw new AssertionError(
                    "La especialidad recuperada por ID "
                            + "no coincide."
            );
        }
    }

    private static void probarListadoGeneral(
            EspecialidadService servicio,
            Especialidad especialidadCreada
    ) throws SQLException {

        List<Especialidad> especialidades =
                servicio.listarTodas();

        boolean encontrada =
                especialidades.stream()
                        .anyMatch(
                                especialidad ->
                                        especialidadCreada
                                                .getIdEspecialidad()
                                                .equals(
                                                        especialidad
                                                                .getIdEspecialidad()
                                                )
                        );

        if (!encontrada) {
            throw new AssertionError(
                    "La especialidad no aparece "
                            + "en el listado general."
            );
        }
    }

    private static void probarListadoActivo(
            EspecialidadService servicio,
            Especialidad especialidadCreada
    ) throws SQLException {

        List<Especialidad> especialidades =
                servicio.listarActivas();

        boolean encontrada =
                especialidades.stream()
                        .anyMatch(
                                especialidad ->
                                        especialidadCreada
                                                .getIdEspecialidad()
                                                .equals(
                                                        especialidad
                                                                .getIdEspecialidad()
                                                )
                        );

        if (!encontrada) {
            throw new AssertionError(
                    "La especialidad no aparece "
                            + "en el listado de activas."
            );
        }
    }

    private static void probarNombreDuplicado(
            EspecialidadService servicio,
            String nombreTemporal
    ) throws SQLException {

        RegistroEspecialidadDTO datosDuplicados =
                new RegistroEspecialidadDTO(
                        "   "
                                + nombreTemporal.toUpperCase()
                                + "   ",
                        "Intento de duplicación.",
                        "60"
                );

        try {
            servicio.registrar(
                    datosDuplicados
            );

            throw new AssertionError(
                    "El nombre duplicado debería ser rechazado."
            );

        } catch (IllegalArgumentException esperado) {
            System.out.println(
                    "Nombre duplicado: RECHAZADO."
            );
        }
    }

    private static void eliminarEspecialidadTemporal(
            String nombreTemporal
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_ELIMINAR_POR_NOMBRE
                        )
        ) {
            sentencia.setString(
                    1,
                    nombreTemporal
            );

            sentencia.executeUpdate();
        }
    }

    private static void verificarEliminacion(
            EspecialidadService servicio,
            String nombreTemporal
    ) throws SQLException {

        boolean permaneceRegistrada =
                servicio.listarTodas()
                        .stream()
                        .anyMatch(
                                especialidad ->
                                        nombreTemporal.equalsIgnoreCase(
                                                especialidad.getNombre()
                                        )
                        );

        if (permaneceRegistrada) {
            throw new AssertionError(
                    "La especialidad temporal "
                            + "no fue eliminada."
            );
        }
    }
}