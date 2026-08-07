package py.fpune.soracare.mvc.model;

import java.time.OffsetDateTime;

public class Sede {

    private Integer idSede;
    private String codigo;
    private String nombre;
    private String direccion;
    private String ciudad;
    private String departamento;
    private String telefono;
    private String correo;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    public Sede() {
        this.activo = true;
    }

    public Sede(
            String codigo,
            String nombre,
            String direccion,
            String ciudad,
            String departamento,
            String telefono,
            String correo
    ) {
        this();

        setCodigo(
                codigo
        );

        setNombre(
                nombre
        );

        setDireccion(
                direccion
        );

        setCiudad(
                ciudad
        );

        setDepartamento(
                departamento
        );

        setTelefono(
                telefono
        );

        setCorreo(
                correo
        );
    }

    public Integer getIdSede() {
        return idSede;
    }

    public void setIdSede(
            Integer idSede
    ) {
        if (idSede != null
                && idSede <= 0) {

            throw new IllegalArgumentException(
                    "El ID de la sede debe ser mayor que cero."
            );
        }

        this.idSede =
                idSede;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(
            String codigo
    ) {
        if (codigo == null
                || codigo.isBlank()) {

            throw new IllegalArgumentException(
                    "El código de la sede es obligatorio."
            );
        }

        String codigoNormalizado =
                codigo.strip();

        if (codigoNormalizado.length() > 20) {
            throw new IllegalArgumentException(
                    "El código de la sede no puede superar "
                            + "20 caracteres."
            );
        }

        this.codigo =
                codigoNormalizado;
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
                    "El nombre de la sede es obligatorio."
            );
        }

        String nombreNormalizado =
                nombre.strip();

        if (nombreNormalizado.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre de la sede no puede superar "
                            + "120 caracteres."
            );
        }

        this.nombre =
                nombreNormalizado;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(
            String direccion
    ) {
        if (direccion == null
                || direccion.isBlank()) {

            throw new IllegalArgumentException(
                    "La dirección de la sede es obligatoria."
            );
        }

        String direccionNormalizada =
                direccion.strip();

        if (direccionNormalizada.length() > 255) {
            throw new IllegalArgumentException(
                    "La dirección de la sede no puede superar "
                            + "255 caracteres."
            );
        }

        this.direccion =
                direccionNormalizada;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(
            String ciudad
    ) {
        if (ciudad == null
                || ciudad.isBlank()) {

            throw new IllegalArgumentException(
                    "La ciudad de la sede es obligatoria."
            );
        }

        String ciudadNormalizada =
                ciudad.strip();

        if (ciudadNormalizada.length() > 100) {
            throw new IllegalArgumentException(
                    "La ciudad de la sede no puede superar "
                            + "100 caracteres."
            );
        }

        this.ciudad =
                ciudadNormalizada;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(
            String departamento
    ) {
        if (departamento == null
                || departamento.isBlank()) {

            this.departamento = null;
            return;
        }

        String departamentoNormalizado =
                departamento.strip();

        if (departamentoNormalizado.length() > 100) {
            throw new IllegalArgumentException(
                    "El departamento no puede superar "
                            + "100 caracteres."
            );
        }

        this.departamento =
                departamentoNormalizado;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(
            String telefono
    ) {
        if (telefono == null
                || telefono.isBlank()) {

            this.telefono = null;
            return;
        }

        String telefonoNormalizado =
                telefono.strip();

        if (telefonoNormalizado.length() > 30) {
            throw new IllegalArgumentException(
                    "El teléfono de la sede no puede superar "
                            + "30 caracteres."
            );
        }

        this.telefono =
                telefonoNormalizado;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(
            String correo
    ) {
        if (correo == null
                || correo.isBlank()) {

            this.correo = null;
            return;
        }

        String correoNormalizado =
                correo.strip();

        if (correoNormalizado.length() > 150) {
            throw new IllegalArgumentException(
                    "El correo de la sede no puede superar "
                            + "150 caracteres."
            );
        }

        this.correo =
                correoNormalizado;
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