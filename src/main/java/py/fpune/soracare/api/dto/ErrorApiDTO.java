package py.fpune.soracare.api.dto;

public record ErrorApiDTO(
        int estado,
        String codigo,
        String mensaje
) {
}
