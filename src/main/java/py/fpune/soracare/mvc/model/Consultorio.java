package py.fpune.soracare.mvc.model;

import java.time.OffsetDateTime;

public class Consultorio {

    private Long idConsultorio;
    private Integer idSede;
    private String codigo;
    private String nombre;
    private String piso;
    private String ubicacionReferencia;
    private TipoConsultorio tipo;
    private short capacidad;
    private boolean accesible;
    private EstadoConsultorio estado;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    public Consultorio() {
        this.tipo =
                TipoConsultorio.CONSULTA;

        this.capacidad =
                1;

        this.accesible =
                false;

        this.estado =
                EstadoConsultorio.DISPONIBLE;
    }

    public Consultorio(
            Integer idSede,
            String codigo,
            String nombre,
            String piso,
            String ubicacionReferencia,
            TipoConsultorio tipo,
            short capacidad,
            boolean accesible
    ) {
        this();

        setIdSede(
                idSede
        );

        setCodigo(
                codigo
        );

        setNombre(
                nombre
        );

        setPiso(
                piso
        );

        setUbicacionReferencia(
                ubicacionReferencia
        );

        setTipo(
                tipo
        );

        setCapacidad(
                capacidad
        );

        setAccesible(
                accesible
        );
    }

    public Long getIdConsultorio() {
        return idConsultorio;
    }

    public void setIdConsultorio(
            Long idConsultorio
    ) {
        if (idConsultorio != null
                && idConsultorio <= 0) {

            throw new IllegalArgumentException(
                    "El ID del consultorio debe ser mayor que cero."
            );
        }

        this.idConsultorio =
                idConsultorio;
    }

    public Integer getIdSede() {
        return idSede;
    }

    public void setIdSede(
            Integer idSede
    ) {
        if (idSede == null
                || idSede <= 0) {

            throw new IllegalArgumentException(
                    "La sede del consultorio es obligatoria."
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
                    "El código del consultorio es obligatorio."
            );
        }

        String codigoNormalizado =
                codigo.strip();

        if (codigoNormalizado.length() > 30) {
            throw new IllegalArgumentException(
                    "El código del consultorio no puede superar "
                            + "30 caracteres."
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
                    "El nombre del consultorio es obligatorio."
            );
        }

        String nombreNormalizado =
                nombre.strip();

        if (nombreNormalizado.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre del consultorio no puede superar "
                            + "100 caracteres."
            );
        }

        this.nombre =
                nombreNormalizado;
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(
            String piso
    ) {
        if (piso == null
                || piso.isBlank()) {

            this.piso = null;
            return;
        }

        String pisoNormalizado =
                piso.strip();

        if (pisoNormalizado.length() > 20) {
            throw new IllegalArgumentException(
                    "El piso del consultorio no puede superar "
                            + "20 caracteres."
            );
        }

        this.piso =
                pisoNormalizado;
    }

    public String getUbicacionReferencia() {
        return ubicacionReferencia;
    }

    public void setUbicacionReferencia(
            String ubicacionReferencia
    ) {
        if (ubicacionReferencia == null
                || ubicacionReferencia.isBlank()) {

            this.ubicacionReferencia =
                    null;

            return;
        }

        String ubicacionNormalizada =
                ubicacionReferencia.strip();

        if (ubicacionNormalizada.length() > 255) {
            throw new IllegalArgumentException(
                    "La referencia de ubicación no puede superar "
                            + "255 caracteres."
            );
        }

        this.ubicacionReferencia =
                ubicacionNormalizada;
    }

    public TipoConsultorio getTipo() {
        return tipo;
    }

    public void setTipo(
            TipoConsultorio tipo
    ) {
        if (tipo == null) {
            throw new IllegalArgumentException(
                    "El tipo de consultorio es obligatorio."
            );
        }

        this.tipo =
                tipo;
    }

    public short getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(
            short capacidad
    ) {
        if (capacidad < 1
                || capacidad > 50) {

            throw new IllegalArgumentException(
                    "La capacidad del consultorio debe estar "
                            + "entre 1 y 50 personas."
            );
        }

        this.capacidad =
                capacidad;
    }

    public boolean isAccesible() {
        return accesible;
    }

    public void setAccesible(
            boolean accesible
    ) {
        this.accesible =
                accesible;
    }

    public EstadoConsultorio getEstado() {
        return estado;
    }

    public void setEstado(
            EstadoConsultorio estado
    ) {
        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado del consultorio es obligatorio."
            );
        }

        this.estado =
                estado;
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