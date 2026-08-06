package py.fpune.soracare.dto;

public class CredencialesAutenticacionDTO {

    private final String identificador;
    private final String contrasena;

    public CredencialesAutenticacionDTO(
            String identificador,
            String contrasena
    ) {
        this.identificador = identificador;
        this.contrasena = contrasena;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getContrasena() {
        return contrasena;
    }
}