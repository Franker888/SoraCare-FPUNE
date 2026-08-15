package py.fpune.soracare.dto;

public record RegistroMedicamentoDTO(
        String nombreComercial,
        String principioActivo,
        String presentacion,
        String concentracion,
        String viaAdministracion,
        String requiereReceta
) {
}
