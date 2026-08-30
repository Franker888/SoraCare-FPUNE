package py.fpune.soracare.mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.OffsetDateTime;

@Entity
@Table(name = "citas", schema = "soracare")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long idCita;

    @Column(name = "id_paciente", nullable = false)
    private Long idPaciente;

    @Column(name = "id_medico", nullable = false)
    private Long idMedico;

    @Column(name = "id_especialidad", nullable = false)
    private Integer idEspecialidad;

    @Column(name = "id_consultorio", nullable = false)
    private Long idConsultorio;

    @Column(name = "id_horario")
    private Long idHorario;

    @Column(name = "id_estado_cita", nullable = false)
    private Integer idEstadoCita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_estado_cita",
            insertable = false,
            updatable = false
    )
    private EstadoCita estado;

    @Transient
    private String codigoEstado;

    @Transient
    private String nombreEstado;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private OffsetDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private OffsetDateTime fechaHoraFin;

    @Column(name = "motivo_consulta", nullable = false, length = 500)
    private String motivoConsulta;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "canal_reserva", nullable = false, length = 20)
    private String canalReserva;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "id_usuario_creador")
    private Long idUsuarioCreador;

    @Column(name = "id_usuario_modificacion")
    private Long idUsuarioModificacion;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public Long getIdCita() { return idCita; }
    public void setIdCita(Long idCita) { this.idCita = idCita; }
    public Long getIdPaciente() { return idPaciente; }
    public void setIdPaciente(Long idPaciente) { this.idPaciente = idPaciente; }
    public Long getIdMedico() { return idMedico; }
    public void setIdMedico(Long idMedico) { this.idMedico = idMedico; }
    public Integer getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(Integer idEspecialidad) { this.idEspecialidad = idEspecialidad; }
    public Long getIdConsultorio() { return idConsultorio; }
    public void setIdConsultorio(Long idConsultorio) { this.idConsultorio = idConsultorio; }
    public Long getIdHorario() { return idHorario; }
    public void setIdHorario(Long idHorario) { this.idHorario = idHorario; }
    public Integer getIdEstadoCita() { return idEstadoCita; }
    public void setIdEstadoCita(Integer idEstadoCita) { this.idEstadoCita = idEstadoCita; }
    public String getCodigoEstado() {
        return estado == null ? codigoEstado : estado.getCodigo();
    }
    public void setCodigoEstado(String codigoEstado) { this.codigoEstado = codigoEstado; }
    public String getNombreEstado() {
        return estado == null ? nombreEstado : estado.getNombre();
    }
    public void setNombreEstado(String nombreEstado) { this.nombreEstado = nombreEstado; }
    public OffsetDateTime getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(OffsetDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
    public OffsetDateTime getFechaHoraFin() { return fechaHoraFin; }
    public void setFechaHoraFin(OffsetDateTime fechaHoraFin) { this.fechaHoraFin = fechaHoraFin; }
    public String getMotivoConsulta() { return motivoConsulta; }
    public void setMotivoConsulta(String motivoConsulta) { this.motivoConsulta = motivoConsulta; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getCanalReserva() { return canalReserva; }
    public void setCanalReserva(String canalReserva) { this.canalReserva = canalReserva; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public Long getIdUsuarioCreador() { return idUsuarioCreador; }
    public void setIdUsuarioCreador(Long idUsuarioCreador) { this.idUsuarioCreador = idUsuarioCreador; }
    public Long getIdUsuarioModificacion() { return idUsuarioModificacion; }
    public void setIdUsuarioModificacion(Long idUsuarioModificacion) { this.idUsuarioModificacion = idUsuarioModificacion; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public OffsetDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

}
