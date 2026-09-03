package py.fpune.soracare.dao;

import jakarta.persistence.PersistenceException;
import py.fpune.soracare.config.JpaContexto;
import py.fpune.soracare.mvc.model.HorarioMedico;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class HorarioMedicoDAO {

    public HorarioMedico crear(HorarioMedico horario) throws SQLException {
        if (horario == null) {
            throw new SQLException("El horario médico no puede ser nulo.");
        }

        try {
            return JpaContexto.ejecutarEnTransaccion(entityManager -> {
                entityManager.persist(horario);
                entityManager.flush();
                return horario;
            });
        } catch (RuntimeException error) {
            throw convertir("No fue posible registrar el horario médico.", error);
        }
    }

    public Optional<HorarioMedico> buscarPorId(long idHorario)
            throws SQLException {
        validarIdHorario(idHorario);

        try {
            return JpaContexto.ejecutarConsulta(
                    entityManager -> Optional.ofNullable(
                            entityManager.find(HorarioMedico.class, idHorario)
                    )
            );
        } catch (PersistenceException error) {
            throw convertir("No fue posible consultar el horario médico.", error);
        }
    }

    public List<HorarioMedico> listarTodos() throws SQLException {
        return consultarLista(
                """
                SELECT h
                FROM HorarioMedico h
                ORDER BY h.activo DESC,
                         h.diaSemana,
                         h.horaInicio,
                         h.idMedico
                """,
                null
        );
    }

    public List<HorarioMedico> listarPorMedico(long idMedico)
            throws SQLException {
        if (idMedico <= 0) {
            throw new SQLException("El médico seleccionado no es válido.");
        }

        try {
            return JpaContexto.ejecutarConsulta(
                    entityManager -> entityManager.createQuery(
                                    """
                                    SELECT h
                                    FROM HorarioMedico h
                                    WHERE h.idMedico = :idMedico
                                    ORDER BY h.activo DESC,
                                             h.diaSemana,
                                             h.horaInicio,
                                             h.idHorario
                                    """,
                                    HorarioMedico.class
                            )
                            .setParameter("idMedico", idMedico)
                            .getResultList()
            );
        } catch (PersistenceException error) {
            throw convertir(
                    "No fue posible consultar los horarios del médico.",
                    error
            );
        }
    }

    public List<HorarioMedico> listarVigentesEnFecha(LocalDate fecha)
            throws SQLException {
        if (fecha == null) {
            throw new SQLException("La fecha de consulta es obligatoria.");
        }

        return consultarLista(
                """
                SELECT h
                FROM HorarioMedico h
                WHERE h.activo = TRUE
                  AND h.fechaDesde <= :fecha
                  AND (h.fechaHasta IS NULL OR h.fechaHasta >= :fecha)
                ORDER BY h.diaSemana,
                         h.horaInicio,
                         h.idMedico
                """,
                fecha
        );
    }

    public boolean existeSuperposicion(
            HorarioMedico horario,
            long idHorarioExcluido
    ) throws SQLException {
        if (horario == null) {
            throw new SQLException("El horario médico no puede ser nulo.");
        }

        try {
            Long coincidencias = JpaContexto.ejecutarConsulta(
                    entityManager -> entityManager.createQuery(
                                    """
                                    SELECT COUNT(h)
                                    FROM HorarioMedico h
                                    WHERE h.activo = TRUE
                                      AND h.idHorario <> :idExcluido
                                      AND h.diaSemana = :diaSemana
                                      AND :horaInicio < h.horaFin
                                      AND :horaFin > h.horaInicio
                                      AND (h.fechaHasta IS NULL
                                           OR h.fechaHasta >= :fechaDesde)
                                      AND (:fechaHasta IS NULL
                                           OR h.fechaDesde <= :fechaHasta)
                                      AND (h.idMedico = :idMedico
                                           OR h.idConsultorio = :idConsultorio)
                                    """,
                                    Long.class
                            )
                            .setParameter(
                                    "idExcluido",
                                    Math.max(idHorarioExcluido, 0)
                            )
                            .setParameter("diaSemana", horario.getDiaSemana())
                            .setParameter("horaInicio", horario.getHoraInicio())
                            .setParameter("horaFin", horario.getHoraFin())
                            .setParameter("fechaDesde", horario.getFechaDesde())
                            .setParameter("fechaHasta", horario.getFechaHasta())
                            .setParameter("idMedico", horario.getIdMedico())
                            .setParameter(
                                    "idConsultorio",
                                    horario.getIdConsultorio()
                            )
                            .getSingleResult()
            );

            return coincidencias > 0;
        } catch (PersistenceException error) {
            throw convertir(
                    "No fue posible verificar la superposición de horarios.",
                    error
            );
        }
    }

    public void actualizarActivo(long idHorario, boolean activo)
            throws SQLException {
        validarIdHorario(idHorario);

        try {
            JpaContexto.ejecutarEnTransaccion(entityManager -> {
                HorarioMedico horario = entityManager.find(
                        HorarioMedico.class,
                        idHorario
                );
                if (horario == null) {
                    throw new PersistenceException(
                            "El horario seleccionado no existe."
                    );
                }
                horario.setActivo(activo);
                return null;
            });
        } catch (RuntimeException error) {
            throw convertir("No fue posible actualizar el horario médico.", error);
        }
    }

    private static List<HorarioMedico> consultarLista(
            String jpql,
            LocalDate fecha
    ) throws SQLException {
        try {
            return JpaContexto.ejecutarConsulta(entityManager -> {
                var consulta = entityManager.createQuery(
                        jpql,
                        HorarioMedico.class
                );
                if (fecha != null) {
                    consulta.setParameter("fecha", fecha);
                }
                return consulta.getResultList();
            });
        } catch (PersistenceException error) {
            throw convertir("No fue posible consultar los horarios médicos.", error);
        }
    }

    private static void validarIdHorario(long idHorario) throws SQLException {
        if (idHorario <= 0) {
            throw new SQLException("El ID del horario debe ser mayor que cero.");
        }
    }

    private static SQLException convertir(
            String mensaje,
            RuntimeException error
    ) {
        return new SQLException(mensaje, error);
    }
}
