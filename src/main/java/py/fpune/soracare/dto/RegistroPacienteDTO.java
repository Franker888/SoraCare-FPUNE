package py.fpune.soracare.dto;

public class RegistroPacienteDTO {

    private final String nombres;
    private final String apellidos;
    private final String cedula;
    private final String fechaNacimiento;
    private final String telefono;
    private final String correo;
    private final String direccion;
    private final String grupoSanguineo;
    private final String contactoEmergencia;
    private final String telefonoEmergencia;

    public RegistroPacienteDTO(
            String nombres,
            String apellidos,
            String cedula,
            String fechaNacimiento,
            String telefono,
            String correo,
            String direccion,
            String grupoSanguineo,
            String contactoEmergencia,
            String telefonoEmergencia
    ) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.cedula = cedula;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.grupoSanguineo = grupoSanguineo;
        this.contactoEmergencia = contactoEmergencia;
        this.telefonoEmergencia = telefonoEmergencia;
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

    public String getGrupoSanguineo() {
        return grupoSanguineo;
    }

    public String getContactoEmergencia() {
        return contactoEmergencia;
    }

    public String getTelefonoEmergencia() {
        return telefonoEmergencia;
    }
}