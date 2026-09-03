package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.SolicitudCitaDTO;
import py.fpune.soracare.dto.SolicitudPagoDTO;

public record CrearCitaApiRequest(
        String idHorario,
        String idExcepcion,
        String idMedico,
        String idEspecialidad,
        String idConsultorio,
        String fecha,
        String horaInicio,
        String motivoConsulta,
        String observaciones,
        String metodoPago,
        DatosFacturacionApiRequest datosFacturacion
) {

    public SolicitudCitaDTO aSolicitudCita(long idPaciente) {
        return new SolicitudCitaDTO(
                Long.toString(idPaciente),
                idHorario,
                idExcepcion,
                idMedico,
                idEspecialidad,
                idConsultorio,
                fecha,
                horaInicio,
                motivoConsulta,
                observaciones,
                "WEB"
        );
    }

    public SolicitudPagoDTO aSolicitudPago() {
        DatosFacturacionDTO datosInternos = datosFacturacion == null
                ? null
                : datosFacturacion.aSolicitudInterna();
        return new SolicitudPagoDTO(metodoPago, datosInternos);
    }
}
