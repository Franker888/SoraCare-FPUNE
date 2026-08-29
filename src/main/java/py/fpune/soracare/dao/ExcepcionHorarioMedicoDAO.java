package py.fpune.soracare.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import py.fpune.soracare.config.JpaContexto;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ExcepcionHorarioMedicoDAO {

    public ExcepcionHorarioMedico crear(ExcepcionHorarioMedico excepcion)
            throws SQLException {
        try (EntityManager entityManager = JpaContexto.crearEntityManager()) {
            EntityTransaction transaccion = entityManager.getTransaction();
            try {
                transaccion.begin();
                entityManager.persist(excepcion);
                entityManager.flush();
                transaccion.commit();
                return excepcion;
            } catch (RuntimeException error) {
                rollback(transaccion, error);
                throw convertir("No fue posible registrar la excepción de agenda.", error);
            }
        }
    }

    public List<ExcepcionHorarioMedico> listarTodas() throws SQLException {
        return consultarLista(
                """
                SELECT e
                FROM ExcepcionHorarioMedico e
                ORDER BY e.fecha DESC,
                         CASE WHEN e.horaInicio IS NULL THEN 0 ELSE 1 END,
                         e.horaInicio,
                         e.idExcepcion DESC
                """,
                null,
                null
        );
    }

    public List<ExcepcionHorarioMedico> listarPorMedico(long idMedico)
            throws SQLException {
        return consultarLista(
                """
                SELECT e
                FROM ExcepcionHorarioMedico e
                WHERE e.idMedico = :idMedico
                ORDER BY e.fecha DESC,
                         CASE WHEN e.horaInicio IS NULL THEN 0 ELSE 1 END,
                         e.horaInicio,
                         e.idExcepcion DESC
                """,
                "idMedico",
                idMedico
        );
    }

    public List<ExcepcionHorarioMedico> listarActivasEnFecha(LocalDate fecha)
            throws SQLException {
        if (fecha == null) {
            throw new SQLException("La fecha de excepciones es obligatoria.");
        }
        return consultarLista(
                """
                SELECT e
                FROM ExcepcionHorarioMedico e
                WHERE e.activo = TRUE AND e.fecha = :fecha
                ORDER BY CASE WHEN e.horaInicio IS NULL THEN 0 ELSE 1 END,
                         e.horaInicio,
                         e.idExcepcion
                """,
                "fecha",
                fecha
        );
    }

    public Optional<ExcepcionHorarioMedico> buscarPorId(long idExcepcion)
            throws SQLException {
        try (EntityManager entityManager = JpaContexto.crearEntityManager()) {
            return Optional.ofNullable(
                    entityManager.find(ExcepcionHorarioMedico.class, idExcepcion)
            );
        } catch (PersistenceException error) {
            throw convertir("No fue posible consultar la excepción de agenda.", error);
        }
    }

    public void actualizarActivo(long idExcepcion, boolean activo)
            throws SQLException {
        try (EntityManager entityManager = JpaContexto.crearEntityManager()) {
            EntityTransaction transaccion = entityManager.getTransaction();
            try {
                transaccion.begin();
                ExcepcionHorarioMedico excepcion = entityManager.find(
                        ExcepcionHorarioMedico.class,
                        idExcepcion
                );
                if (excepcion == null) {
                    throw new IllegalArgumentException(
                            "La excepción seleccionada no existe."
                    );
                }
                excepcion.setActivo(activo);
                transaccion.commit();
            } catch (RuntimeException error) {
                rollback(transaccion, error);
                if (error instanceof IllegalArgumentException validacion) {
                    throw validacion;
                }
                throw convertir("No fue posible actualizar la excepción de agenda.", error);
            }
        }
    }

    private static List<ExcepcionHorarioMedico> consultarLista(
            String jpql,
            String parametro,
            Object valor
    ) throws SQLException {
        try (EntityManager entityManager = JpaContexto.crearEntityManager()) {
            var consulta = entityManager.createQuery(
                    jpql,
                    ExcepcionHorarioMedico.class
            );
            if (parametro != null) consulta.setParameter(parametro, valor);
            return consulta.getResultList();
        } catch (PersistenceException error) {
            throw convertir("No fue posible consultar las excepciones de agenda.", error);
        }
    }

    private static void rollback(
            EntityTransaction transaccion,
            RuntimeException error
    ) {
        if (transaccion != null && transaccion.isActive()) {
            try {
                transaccion.rollback();
            } catch (RuntimeException errorRollback) {
                error.addSuppressed(errorRollback);
            }
        }
    }

    private static SQLException convertir(String mensaje, RuntimeException error) {
        return new SQLException(mensaje, error);
    }
}
