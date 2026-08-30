package py.fpune.soracare.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.EspecialidadResumenApiDTO;
import py.fpune.soracare.service.EspecialidadService;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping(
        path = "/especialidades",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class EspecialidadRestController {

    private final EspecialidadService especialidadService;

    public EspecialidadRestController(
            EspecialidadService especialidadService
    ) {
        this.especialidadService = especialidadService;
    }

    @GetMapping
    public List<EspecialidadResumenApiDTO> listarActivas()
            throws SQLException {
        return especialidadService.listarActivas()
                .stream()
                .map(EspecialidadResumenApiDTO::desde)
                .toList();
    }
}
