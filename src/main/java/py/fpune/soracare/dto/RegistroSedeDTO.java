package py.fpune.soracare.dto;

public class RegistroSedeDTO {

    private final String codigo;
    private final String nombre;
    private final String direccion;
    private final String ciudad;
    private final String departamento;
    private final String telefono;
    private final String correo;

    public RegistroSedeDTO(
            String codigo,
            String nombre,
            String direccion,
            String ciudad,
            String departamento,
            String telefono,
            String correo
    ) {
        this.codigo =
                codigo;

        this.nombre =
                nombre;

        this.direccion =
                direccion;

        this.ciudad =
                ciudad;

        this.departamento =
                departamento;

        this.telefono =
                telefono;

        this.correo =
                correo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }
}