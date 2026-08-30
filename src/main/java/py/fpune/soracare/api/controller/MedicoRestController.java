package py.fpune.soracare.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.EspecialidadMedicoApiDTO;
import py.fpune.soracare.api.dto.MedicoResumenApiDTO;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(
        path = "/medicos",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class MedicoRestController {

    private final MedicoService medicoService;
    private final EspecialidadService especialidadService;
    private final MedicoEspecialidadService medicoEspecialidadService;

    public MedicoRestController(
            MedicoService medicoService,
            EspecialidadService especialidadService,
            MedicoEspecialidadService medicoEspecialidadService
    ) {
        this.medicoService = medicoService;
        this.especialidadService = especialidadService;
        this.medicoEspecialidadService = medicoEspecialidadService;
    }

    @GetMapping
    public List<MedicoResumenApiDTO> listarActivos()
            throws SQLException {
        Map<Integer, Especialidad> especialidadesPorId =
                cargarEspecialidadesActivas();
        List<MedicoResumenApiDTO> respuesta = new ArrayList<>();

        for (Medico medico : medicoService.listarActivos()) {
            respuesta.add(
                    MedicoResumenApiDTO.desde(
                            medico,
                            cargarEspecialidadesDelMedico(
                                    medico,
                                    especialidadesPorId
                            )
                    )
            );
        }

        return respuesta;
    }

    private Map<Integer, Especialidad> cargarEspecialidadesActivas()
            throws SQLException {
        Map<Integer, Especialidad> especialidadesPorId =
                new LinkedHashMap<>();

        for (Especialidad especialidad : especialidadService.listarActivas()) {
            especialidadesPorId.put(
                    especialidad.getIdEspecialidad(),
                    especialidad
            );
        }

        return especialidadesPorId;
    }

    private List<EspecialidadMedicoApiDTO> cargarEspecialidadesDelMedico(
            Medico medico,
            Map<Integer, Especialidad> especialidadesPorId
    ) throws SQLException {
        if (medico.getIdMedico() == null) {
            throw new IllegalStateException(
                    "El médico consultado no posee un identificador."
            );
        }

        List<EspecialidadMedicoApiDTO> especialidades =
                new ArrayList<>();

        for (MedicoEspecialidad relacion
                : medicoEspecialidadService.listarActivasPorMedico(
                        medico.getIdMedico()
                )) {
            Especialidad especialidad = especialidadesPorId.get(
                    relacion.getIdEspecialidad()
            );

            if (especialidad != null) {
                especialidades.add(
                        EspecialidadMedicoApiDTO.desde(
                                especialidad,
                                relacion.isPrincipal()
                        )
                );
            }
        }

        return especialidades;
    }
}
