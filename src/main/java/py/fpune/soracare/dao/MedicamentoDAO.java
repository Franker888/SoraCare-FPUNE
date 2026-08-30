package py.fpune.soracare.dao;

import jakarta.persistence.PersistenceException;
import py.fpune.soracare.config.JpaContexto;
import py.fpune.soracare.mvc.model.Medicamento;
import py.fpune.soracare.mvc.model.ViaAdministracion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class MedicamentoDAO {

    private static final String COLUMNAS = """
            id_medicamento, nombre_comercial, principio_activo, presentacion,
            concentracion, via_administracion, requiere_receta, activo,
            fecha_creacion, fecha_actualizacion
            """;

    public Medicamento crear(Medicamento medicamento) throws SQLException {
        if (medicamento == null) {
            throw new SQLException("El medicamento no puede ser nulo.");
        }

        try {
            return JpaContexto.ejecutarEnTransaccion(entityManager -> {
                entityManager.persist(medicamento);
                entityManager.flush();
                return medicamento;
            });
        } catch (RuntimeException error) {
            throw convertir("No fue posible registrar el medicamento.", error);
        }
    }

    public List<Medicamento> listarTodos() throws SQLException {
        return consultarLista(
                """
                SELECT m
                FROM Medicamento m
                ORDER BY m.activo DESC,
                         m.nombreComercial,
                         m.principioActivo
                """
        );
    }

    public List<Medicamento> listarActivos() throws SQLException {
        return consultarLista(
                """
                SELECT m
                FROM Medicamento m
                WHERE m.activo = TRUE
                ORDER BY m.nombreComercial,
                         m.principioActivo
                """
        );
    }

    public Optional<Medicamento> buscarPorId(Connection conexion, long idMedicamento)
            throws SQLException {
        String sql = "SELECT " + COLUMNAS
                + " FROM soracare.medicamentos WHERE id_medicamento = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idMedicamento);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    public void cambiarEstado(long idMedicamento, boolean activo)
            throws SQLException {
        try {
            JpaContexto.ejecutarEnTransaccion(entityManager -> {
                Medicamento medicamento = entityManager.find(
                        Medicamento.class,
                        idMedicamento
                );
                if (medicamento == null) {
                    throw new IllegalArgumentException(
                            "El medicamento seleccionado no existe."
                    );
                }
                medicamento.setActivo(activo);
                return null;
            });
        } catch (IllegalArgumentException validacion) {
            throw validacion;
        } catch (RuntimeException error) {
            throw convertir("No fue posible actualizar el medicamento.", error);
        }
    }

    private static List<Medicamento> consultarLista(String jpql)
            throws SQLException {
        try {
            return JpaContexto.ejecutarConsulta(
                    entityManager -> entityManager.createQuery(
                                    jpql,
                                    Medicamento.class
                            )
                            .getResultList()
            );
        } catch (PersistenceException error) {
            throw convertir("No fue posible consultar los medicamentos.", error);
        }
    }

    private static Medicamento mapear(ResultSet resultado) throws SQLException {
        Medicamento medicamento = new Medicamento();
        medicamento.setIdMedicamento(resultado.getLong("id_medicamento"));
        medicamento.setNombreComercial(resultado.getString("nombre_comercial"));
        medicamento.setPrincipioActivo(resultado.getString("principio_activo"));
        medicamento.setPresentacion(resultado.getString("presentacion"));
        medicamento.setConcentracion(resultado.getString("concentracion"));
        medicamento.setViaAdministracion(ViaAdministracion.valueOf(
                resultado.getString("via_administracion")));
        medicamento.setRequiereReceta(resultado.getBoolean("requiere_receta"));
        medicamento.setActivo(resultado.getBoolean("activo"));
        medicamento.setFechaCreacion(resultado.getObject(
                "fecha_creacion", OffsetDateTime.class));
        medicamento.setFechaActualizacion(resultado.getObject(
                "fecha_actualizacion", OffsetDateTime.class));
        return medicamento;
    }

    private static SQLException convertir(
            String mensaje,
            RuntimeException error
    ) {
        return new SQLException(mensaje, error);
    }
}
