package py.fpune.soracare.dto;

import java.io.Serial;
import java.io.Serializable;

public record PerfilGoogleDTO(
        String identificadorGoogle,
        String correo,
        boolean correoVerificado,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String fotoUrl,
        String dominioAlojado
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public boolean correoControladoPorGoogle() {
        String correoNormalizado = correo == null
                ? ""
                : correo.strip().toLowerCase();
        return correoNormalizado.endsWith("@gmail.com")
                || (dominioAlojado != null
                && !dominioAlojado.isBlank());
    }
}
