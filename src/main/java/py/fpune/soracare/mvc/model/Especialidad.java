package py.fpune.soracare.mvc.model;

import java.time.OffsetDateTime;

public class Especialidad {

    private Integer idEspecialidad;
    private String nombre;
    private String descripcion;
    private int duracionConsultaMinutos;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    public Especialidad() {
        this.duracionConsultaMinutos = 30;
        this.activo = true;
    }

    public Especialidad(
            String nombre,
            String descripcion,
            int duracionConsultaMinutos
    ) {
        this();

        setNombre(
                nombre
        );

        setDescripcion(
                descripcion
        );

        setDuracionConsultaMinutos(
                duracionConsultaMinutos
        );
    }

    public Integer getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(
            Integer idEspecialidad
    ) {
        if (idEspecialidad != null
                && idEspecialidad <= 0) {

            throw new IllegalArgumentException(
                    "El ID de la especialidad debe ser mayor que cero."
            );
        }

        this.idEspecialidad =
                idEspecialidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(
            String nombre
    ) {
        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de la especialidad es obligatorio."
            );
        }

        String nombreNormalizado =
                nombre.strip();

        if (nombreNormalizado.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre de la especialidad no puede superar "
                            + "100 caracteres."
            );
        }

        this.nombre =
                nombreNormalizado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(
            String descripcion
    ) {
        if (descripcion == null
                || descripcion.isBlank()) {

            this.descripcion = null;
            return;
        }

        String descripcionNormalizada =
                descripcion.strip();

        if (descripcionNormalizada.length() > 500) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar "
                            + "500 caracteres."
            );
        }

        this.descripcion =
                descripcionNormalizada;
    }

    public int getDuracionConsultaMinutos() {
        return duracionConsultaMinutos;
    }

    public void setDuracionConsultaMinutos(
            int duracionConsultaMinutos
    ) {
        if (duracionConsultaMinutos < 10
                || duracionConsultaMinutos > 240) {

            throw new IllegalArgumentException(
                    "La duración de la consulta debe estar "
                            + "entre 10 y 240 minutos."
            );
        }

        this.duracionConsultaMinutos =
                duracionConsultaMinutos;
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
        this.fechaCreacion =
                fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(
            OffsetDateTime fechaActualizacion
    ) {
        this.fechaActualizacion =
                fechaActualizacion;
    }
}