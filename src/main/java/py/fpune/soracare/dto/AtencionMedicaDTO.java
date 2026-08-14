package py.fpune.soracare.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

public record AtencionMedicaDTO(
        long idAtencion,
        long idCita,
        long idPaciente,
        long idMedico,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        String motivoConsulta,
        String sintomas,
        String antecedentesRelevantes,
        BigDecimal temperaturaC,
        Integer frecuenciaCardiaca,
        Integer frecuenciaRespiratoria,
        Integer presionSistolica,
        Integer presionDiastolica,
        Integer saturacionOxigeno,
        String evaluacionAreaAfectada,
        String examenFisico,
        String evaluacionClinica,
        String planTratamiento,
        String indicaciones,
        String observaciones,
        boolean requiereSeguimiento,
        LocalDate fechaProximoControl,
        String estado,
        String medicoNombreCompleto,
        String especialidad
) {
}
