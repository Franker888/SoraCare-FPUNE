package py.fpune.soracare.dto;

public class RegistroMedicoDTO {

    private final String nombres;
    private final String apellidos;
    private final String cedula;
    private final String fechaNacimiento;
    private final String telefono;
    private final String correo;
    private final String direccion;

    private final String matriculaProfesional;
    private final String fechaIngreso;
    private final String biografia;
    private final String tratamiento;

    public RegistroMedicoDTO(
            String nombres,
            String apellidos,
            String cedula,
            String fechaNacimiento,
            String telefono,
            String correo,
            String direccion,
            String matriculaProfesional,
            String fechaIngreso,
            String biografia
    ) {
        this(nombres, apellidos, cedula, fechaNacimiento, telefono, correo,
                direccion, matriculaProfesional, fechaIngreso, biografia, "DR");
    }

    public RegistroMedicoDTO(
            String nombres,
            String apellidos,
            String cedula,
            String fechaNacimiento,
            String telefono,
            String correo,
            String direccion,
            String matriculaProfesional,
            String fechaIngreso,
            String biografia,
            String tratamiento
    ) {
        this.nombres =
                nombres;

        this.apellidos =
                apellidos;

        this.cedula =
                cedula;

        this.fechaNacimiento =
                fechaNacimiento;

        this.telefono =
                telefono;

        this.correo =
                correo;

        this.direccion =
                direccion;

        this.matriculaProfesional =
                matriculaProfesional;

        this.fechaIngreso =
                fechaIngreso;

        this.biografia =
                biografia;

        this.tratamiento =
                tratamiento;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCedula() {
        return cedula;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getMatriculaProfesional() {
        return matriculaProfesional;
    }

    public String getFechaIngreso() {
        return fechaIngreso;
    }

    public String getBiografia() {
        return biografia;
    }

    public String getTratamiento() {
        return tratamiento;
    }
}
