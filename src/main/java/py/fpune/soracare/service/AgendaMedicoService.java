package py.fpune.soracare.service;

import py.fpune.soracare.dao.AgendaMedicoDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dto.AgendaMedicoDTO;
import py.fpune.soracare.dto.CitaAgendaMedicoDTO;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class AgendaMedicoService {

    private final MedicoDAO medicoDAO = new MedicoDAO();
    private final AgendaMedicoDAO agendaMedicoDAO = new AgendaMedicoDAO();

    public AgendaMedicoDTO consultar(long idPersona, LocalDate fecha)
            throws SQLException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta médica no es válida.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la agenda es obligatoria.");
        }

        Medico medico = medicoDAO.buscarPorIdPersona(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con un médico registrado."));
        if (medico.getEstado() != EstadoMedico.ACTIVO) {
            throw new IllegalArgumentException(
                    "El médico no se encuentra activo para consultar la agenda.");
        }

        OffsetDateTime desde = fecha.atStartOfDay(DisponibilidadService.ZONA_HORARIA)
                .toOffsetDateTime();
        OffsetDateTime hasta = fecha.plusDays(1)
                .atStartOfDay(DisponibilidadService.ZONA_HORARIA)
                .toOffsetDateTime();
        List<CitaAgendaMedicoDTO> citas = agendaMedicoDAO
                .listarPorMedicoYFecha(medico.getIdMedico(), desde, hasta);

        return new AgendaMedicoDTO(medico, fecha, citas);
    }
}
