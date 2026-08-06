package py.fpune.soracare.dto;

public class RegistroEspecialidadDTO {

    private final String nombre;
    private final String descripcion;
    private final String duracionConsultaMinutos;

    public RegistroEspecialidadDTO(
            String nombre,
            String descripcion,
            String duracionConsultaMinutos
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionConsultaMinutos =
                duracionConsultaMinutos;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDuracionConsultaMinutos() {
        return duracionConsultaMinutos;
    }
}