package py.fpune.soracare.mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "horarios_medicos", schema = "soracare")
public class HorarioMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long idHorario;

    @Column(name = "id_medico", nullable = false)
    private Long idMedico;

    @Column(name = "id_especialidad", nullable = false)
    private Integer idEspecialidad;

    @Column(name = "id_consultorio", nullable = false)
    private Long idConsultorio;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "duracion_turno_minutos", nullable = false)
    private int duracionTurnoMinutos;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public HorarioMedico() {
        this.duracionTurnoMinutos = 30;
        this.fechaDesde = LocalDate.now();
        this.activo = true;
    }

    public Long getIdHorario() {
        return idHorario;
    }

    public void setIdHorario(
            Long idHorario
    ) {
        if (idHorario != null
                && idHorario <= 0) {

            throw new IllegalArgumentException(
                    "El ID del horario debe ser mayor que cero."
            );
        }

        this.idHorario = idHorario;
    }

    public Long getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(
            Long idMedico
    ) {
        if (idMedico == null
                || idMedico <= 0) {

            throw new IllegalArgumentException(
                    "El ID del médico debe ser mayor que cero."
            );
        }

        this.idMedico = idMedico;
    }

    public Integer getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(
            Integer idEspecialidad
    ) {
        if (idEspecialidad == null
                || idEspecialidad <= 0) {

            throw new IllegalArgumentException(
                    "El ID de la especialidad debe ser mayor que cero."
            );
        }

        this.idEspecialidad = idEspecialidad;
    }

    public Long getIdConsultorio() {
        return idConsultorio;
    }

    public void setIdConsultorio(
            Long idConsultorio
    ) {
        if (idConsultorio == null
                || idConsultorio <= 0) {

            throw new IllegalArgumentException(
                    "El ID del consultorio debe ser mayor que cero."
            );
        }

        this.idConsultorio = idConsultorio;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(
            int diaSemana
    ) {
        if (diaSemana < 1
                || diaSemana > 7) {

            throw new IllegalArgumentException(
                    "El día de la semana debe estar entre 1 y 7."
            );
        }

        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(
            LocalTime horaInicio
    ) {
        if (horaInicio == null) {
            throw new IllegalArgumentException(
                    "La hora de inicio es obligatoria."
            );
        }

        if (horaFin != null
                && !horaInicio.isBefore(
                horaFin
        )) {

            throw new IllegalArgumentException(
                    "La hora de inicio debe ser anterior a la hora de fin."
            );
        }

        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(
            LocalTime horaFin
    ) {
        if (horaFin == null) {
            throw new IllegalArgumentException(
                    "La hora de fin es obligatoria."
            );
        }

        if (horaInicio != null
                && !horaInicio.isBefore(
                horaFin
        )) {

            throw new IllegalArgumentException(
                    "La hora de fin debe ser posterior a la hora de inicio."
            );
        }

        this.horaFin = horaFin;
    }

    public int getDuracionTurnoMinutos() {
        return duracionTurnoMinutos;
    }

    public void setDuracionTurnoMinutos(
            int duracionTurnoMinutos
    ) {
        if (duracionTurnoMinutos < 10
                || duracionTurnoMinutos > 240) {

            throw new IllegalArgumentException(
                    "La duración del turno debe estar entre 10 y 240 minutos."
            );
        }

        this.duracionTurnoMinutos =
                duracionTurnoMinutos;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(
            LocalDate fechaDesde
    ) {
        if (fechaDesde == null) {
            throw new IllegalArgumentException(
                    "La fecha inicial es obligatoria."
            );
        }

        if (fechaHasta != null
                && fechaHasta.isBefore(
                fechaDesde
        )) {

            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(
            LocalDate fechaHasta
    ) {
        if (fechaHasta != null
                && fechaDesde != null
                && fechaHasta.isBefore(
                fechaDesde
        )) {

            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la fecha inicial."
            );
        }

        this.fechaHasta = fechaHasta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(
            boolean activo
    ) {
        this.activo = activo;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(
            OffsetDateTime fechaCreacion
    ) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(
            OffsetDateTime fechaActualizacion
    ) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @PrePersist
    private void prepararCreacion() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
    }

    @PreUpdate
    private void prepararActualizacion() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
