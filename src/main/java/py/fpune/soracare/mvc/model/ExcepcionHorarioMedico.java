package py.fpune.soracare.mvc.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class ExcepcionHorarioMedico {

    private Long idExcepcion;
    private Long idMedico;
    private Integer idEspecialidad;
    private Long idConsultorio;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private TipoExcepcionHorario tipo;
    private String motivo;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    public ExcepcionHorarioMedico() {
        activo = true;
    }

    public Long getIdExcepcion() { return idExcepcion; }
    public void setIdExcepcion(Long idExcepcion) { this.idExcepcion = idExcepcion; }
    public Long getIdMedico() { return idMedico; }
    public void setIdMedico(Long idMedico) { this.idMedico = idMedico; }
    public Integer getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(Integer idEspecialidad) { this.idEspecialidad = idEspecialidad; }
    public Long getIdConsultorio() { return idConsultorio; }
    public void setIdConsultorio(Long idConsultorio) { this.idConsultorio = idConsultorio; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public TipoExcepcionHorario getTipo() { return tipo; }
    public void setTipo(TipoExcepcionHorario tipo) { this.tipo = tipo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public OffsetDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
