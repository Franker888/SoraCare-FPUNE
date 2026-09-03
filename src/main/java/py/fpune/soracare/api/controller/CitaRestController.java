package py.fpune.soracare.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.CancelarCitaApiRequest;
import py.fpune.soracare.api.dto.CitaPacienteApiDTO;
import py.fpune.soracare.api.dto.CrearCitaApiRequest;
import py.fpune.soracare.api.dto.ReservaCitaApiDTO;
import py.fpune.soracare.api.security.ApiAccesoDenegadoException;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.ResultadoReservaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.PacienteService;
import py.fpune.soracare.service.ReservaPagoService;

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
    private final ReservaPagoService reservaPagoService;

    public CitaRestController(
            CitaService citaService,
            PacienteService pacienteService,
            ReservaPagoService reservaPagoService
    ) {
        this.citaService = citaService;
        this.pacienteService = pacienteService;
        this.reservaPagoService = reservaPagoService;
    }

    @GetMapping
    public List<CitaPacienteApiDTO> listarPropias(
            HttpServletRequest request
    ) throws SQLException {
        ContextoPaciente contexto = obtenerContextoPaciente(request);
        return citaService.listarPorPaciente(contexto.idPaciente())
                .stream()
                .map(CitaPacienteApiDTO::desde)
                .toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservaCitaApiDTO> crear(
            @RequestBody CrearCitaApiRequest cuerpo,
            HttpServletRequest request
    ) throws SQLException {
        if (cuerpo == null) {
            throw new IllegalArgumentException(
                    "Los datos de la cita son obligatorios."
            );
        }
        ContextoPaciente contexto = obtenerContextoPaciente(request);
        ResultadoReservaDTO resultado = reservaPagoService.reservar(
                cuerpo.aSolicitudCita(contexto.idPaciente()),
                cuerpo.aSolicitudPago(),
                contexto.usuario().getIdUsuario()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservaCitaApiDTO.desde(resultado));
    }

    @PatchMapping(
            path = "/{idCita}/cancelacion",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> cancelar(
            @PathVariable("idCita") long idCita,
            @RequestBody CancelarCitaApiRequest cuerpo,
            HttpServletRequest request
    ) throws SQLException {
        if (cuerpo == null) {
            throw new IllegalArgumentException(
                    "El motivo de cancelación es obligatorio."
            );
        }
        ContextoPaciente contexto = obtenerContextoPaciente(request);
        citaService.cancelarPorPaciente(
                idCita,
                contexto.idPaciente(),
                contexto.usuario().getIdUsuario(),
                cuerpo.motivo()
        );
        return ResponseEntity.noContent().build();
    }

    private ContextoPaciente obtenerContextoPaciente(
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
        return new ContextoPaciente(usuario, paciente.getIdPaciente());
    }

    private static ApiAccesoDenegadoException fichaPacienteNoDisponible() {
        return new ApiAccesoDenegadoException(
                "Tu cuenta no posee una ficha de paciente asociada."
        );
    }

    private record ContextoPaciente(
            UsuarioAutenticadoDTO usuario,
            long idPaciente
    ) {
    }
}
