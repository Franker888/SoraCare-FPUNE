package py.fpune.soracare.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.CitaPacienteApiDTO;
import py.fpune.soracare.api.security.ApiAccesoDenegadoException;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.PacienteService;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping(
        path = "/citas",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class CitaRestController {

    private final CitaService citaService;
    private final PacienteService pacienteService;

    public CitaRestController(
            CitaService citaService,
            PacienteService pacienteService
    ) {
        this.citaService = citaService;
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public List<CitaPacienteApiDTO> listarPropias(
            HttpServletRequest request
    ) throws SQLException {
        UsuarioAutenticadoDTO usuario = SeguridadApiRest.requerirRol(
                request,
                "PACIENTE"
        );
        Paciente paciente = pacienteService
                .buscarPorIdPersona(usuario.getIdPersona())
                .orElseThrow(CitaRestController::fichaPacienteNoDisponible);

        if (paciente.getIdPaciente() == null
                || paciente.getIdPaciente() <= 0) {
            throw fichaPacienteNoDisponible();
        }

        return citaService.listarPorPaciente(paciente.getIdPaciente())
                .stream()
                .map(CitaPacienteApiDTO::desde)
                .toList();
    }

    private static ApiAccesoDenegadoException fichaPacienteNoDisponible() {
        return new ApiAccesoDenegadoException(
                "Tu cuenta no posee una ficha de paciente asociada."
        );
    }
}
