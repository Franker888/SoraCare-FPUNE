package py.fpune.soracare.dto;

public class RegistroHorarioMedicoDTO {

    private final String idMedico;
    private final String idEspecialidad;
    private final String idConsultorio;
    private final String diaSemana;
    private final String horaInicio;
    private final String horaFin;
    private final String duracionTurnoMinutos;
    private final String fechaDesde;
    private final String fechaHasta;

    public RegistroHorarioMedicoDTO(
            String idMedico,
            String idEspecialidad,
            String idConsultorio,
            String diaSemana,
            String horaInicio,
            String horaFin,
            String duracionTurnoMinutos,
            String fechaDesde,
            String fechaHasta
    ) {
        this.idMedico = idMedico;
        this.idEspecialidad = idEspecialidad;
        this.idConsultorio = idConsultorio;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionTurnoMinutos =
                duracionTurnoMinutos;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
    }

    public String getIdMedico() {
        return idMedico;
    }

    public String getIdEspecialidad() {
        return idEspecialidad;
    }

    public String getIdConsultorio() {
        return idConsultorio;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public String getDuracionTurnoMinutos() {
        return duracionTurnoMinutos;
    }

    public String getFechaDesde() {
        return fechaDesde;
    }

    public String getFechaHasta() {
        return fechaHasta;
    }
}
