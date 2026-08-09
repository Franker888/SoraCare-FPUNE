package py.fpune.soracare.mvc.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class MedicoEspecialidad {

    private Long idMedico;
    private Integer idEspecialidad;
    private boolean principal;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private boolean activo;
    private OffsetDateTime fechaCreacion;

    public MedicoEspecialidad() {
        this.principal = false;
        this.fechaDesde = LocalDate.now();
        this.activo = true;
    }

    public MedicoEspecialidad(
            Long idMedico,
            Integer idEspecialidad,
            boolean principal,
            LocalDate fechaDesde
    ) {
        this();

        setIdMedico(
                idMedico
        );

        setIdEspecialidad(
                idEspecialidad
        );

        setPrincipal(
                principal
        );

        if (fechaDesde != null) {
            setFechaDesde(
                    fechaDesde
            );
        }
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

        this.idMedico =
                idMedico;
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
                    "El ID de la especialidad debe ser "
                            + "mayor que cero."
            );
        }

        this.idEspecialidad =
                idEspecialidad;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public void setPrincipal(
            boolean principal
    ) {
        this.principal =
                principal;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(
            LocalDate fechaDesde
    ) {
        if (fechaDesde == null) {
            throw new IllegalArgumentException(
                    "La fecha de inicio de la especialidad "
                            + "es obligatoria."
            );
        }

        if (fechaHasta != null
                && fechaHasta.isBefore(
                fechaDesde
        )) {

            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser "
                            + "posterior a la fecha de finalización."
            );
        }

        this.fechaDesde =
                fechaDesde;
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
                    "La fecha de finalización no puede ser "
                            + "anterior a la fecha de inicio."
            );
        }

        this.fechaHasta =
                fechaHasta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(
            boolean activo
    ) {
        this.activo =
                activo;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(
            OffsetDateTime fechaCreacion
    ) {
        this.fechaCreacion =
                fechaCreacion;
    }
}