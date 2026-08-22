package py.fpune.soracare.dto;

public record ResumenPanelMedicoDTO(
        long idMedico,
        String nombres,
        String apellidos,
        String tratamiento,
        String matriculaProfesional,
        String especialidadPrincipal,
        long citasHoy,
        long confirmadasHoy,
        long enAtencionHoy,
        long atendidasMes
) {
    public String nombreCompleto() {
        return (nombres + " " + apellidos).strip();
    }

    public String tratamientoVisible() {
        return "DRA".equals(tratamiento) ? "Dra." : "Dr.";
    }
}
