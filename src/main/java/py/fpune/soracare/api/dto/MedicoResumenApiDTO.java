package py.fpune.soracare.api.dto;

import py.fpune.soracare.mvc.model.Medico;

import java.util.List;

public record MedicoResumenApiDTO(
        long id,
        String tratamiento,
        String nombres,
        String apellidos,
        String matriculaProfesional,
        String biografia,
        List<EspecialidadMedicoApiDTO> especialidades
) {

    public static MedicoResumenApiDTO desde(
            Medico medico,
            List<EspecialidadMedicoApiDTO> especialidades
    ) {
        return new MedicoResumenApiDTO(
                medico.getIdMedico(),
                medico.getTratamiento() == null
                        ? "Dr."
                        : medico.getTratamiento().getAbreviatura(),
                medico.getNombres(),
                medico.getApellidos(),
                medico.getMatriculaProfesional(),
                medico.getBiografia(),
                List.copyOf(especialidades)
        );
    }
}
