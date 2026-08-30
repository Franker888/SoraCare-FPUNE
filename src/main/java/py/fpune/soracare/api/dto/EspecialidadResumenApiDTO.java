package py.fpune.soracare.api.dto;

import py.fpune.soracare.mvc.model.Especialidad;

public record EspecialidadResumenApiDTO(
        int id,
        String nombre,
        String descripcion,
        int duracionConsultaMinutos
) {

    public static EspecialidadResumenApiDTO desde(
            Especialidad especialidad
    ) {
        return new EspecialidadResumenApiDTO(
                especialidad.getIdEspecialidad(),
                especialidad.getNombre(),
                especialidad.getDescripcion(),
                especialidad.getDuracionConsultaMinutos()
        );
    }
}
