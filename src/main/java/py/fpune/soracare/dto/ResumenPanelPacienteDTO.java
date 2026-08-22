package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record ResumenPanelPacienteDTO(
        long idPaciente,
        String nombres,
        String apellidos,
        String numeroHistoriaClinica,
        String grupoSanguineo,
        long proximasCitas,
        long indicacionesMedicas,
        long documentosClinicos,
        long notificacionesNoLeidas,
        OffsetDateTime proximaCita
) {
    public String nombreCompleto() {
        return (textoSeguro(nombres) + " " + textoSeguro(apellidos)).strip();
    }

    public String grupoSanguineoVisible() {
        return grupoSanguineo == null || grupoSanguineo.isBlank()
                ? "No informado"
                : grupoSanguineo.strip();
    }

    private static String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
