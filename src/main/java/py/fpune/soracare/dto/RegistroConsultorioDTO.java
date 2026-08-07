package py.fpune.soracare.dto;

public class RegistroConsultorioDTO {

    private final String idSede;
    private final String codigo;
    private final String nombre;
    private final String piso;
    private final String ubicacionReferencia;
    private final String tipo;
    private final String capacidad;
    private final String accesible;

    public RegistroConsultorioDTO(
            String idSede,
            String codigo,
            String nombre,
            String piso,
            String ubicacionReferencia,
            String tipo,
            String capacidad,
            String accesible
    ) {
        this.idSede =
                idSede;

        this.codigo =
                codigo;

        this.nombre =
                nombre;

        this.piso =
                piso;

        this.ubicacionReferencia =
                ubicacionReferencia;

        this.tipo =
                tipo;

        this.capacidad =
                capacidad;

        this.accesible =
                accesible;
    }

    public String getIdSede() {
        return idSede;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPiso() {
        return piso;
    }

    public String getUbicacionReferencia() {
        return ubicacionReferencia;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCapacidad() {
        return capacidad;
    }

    public String getAccesible() {
        return accesible;
    }
}