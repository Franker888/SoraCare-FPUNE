package py.fpune.soracare.api.dto;

import py.fpune.soracare.mvc.model.Especialidad;

public record EspecialidadMedicoApiDTO(
        int id,
        String nombre,
        boolean principal
) {

    public static EspecialidadMedicoApiDTO desde(
            Especialidad especialidad,
            boolean principal
    ) {
        return new EspecialidadMedicoApiDTO(
                especialidad.getIdEspecialidad(),
                especialidad.getNombre(),
                principal
        );
    }
}
