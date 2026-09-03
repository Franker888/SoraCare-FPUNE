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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.CancelarCitaApiRequest;
import py.fpune.soracare.api.dto.CrearBloqueoMedicoApiRequest;
import py.fpune.soracare.api.dto.EstadoBloqueoMedicoApiRequest;
import py.fpune.soracare.api.dto.ExcepcionDisponibilidadMedicoApiDTO;
import py.fpune.soracare.api.dto.ResumenDisponibilidadMedicoApiDTO;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.service.GestionDisponibilidadMedicoService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping(
        path = "/medicos/me/disponibilidad",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class DisponibilidadMedicoRestController {

    private final GestionDisponibilidadMedicoService disponibilidadService;

    public DisponibilidadMedicoRestController(
            GestionDisponibilidadMedicoService disponibilidadService
    ) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public ResumenDisponibilidadMedicoApiDTO consultar(
            @RequestParam(name = "fecha") String fecha,
            HttpServletRequest request
    ) throws SQLException {
        UsuarioAutenticadoDTO usuario = medicoAutenticado(request);
        return ResumenDisponibilidadMedicoApiDTO.desde(
                disponibilidadService.consultar(
                        usuario.getIdPersona(),
                        convertirFecha(fecha)
                )
        );
    }

    @PostMapping(
            path = "/bloqueos",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ExcepcionDisponibilidadMedicoApiDTO> crearBloqueo(
            @RequestBody CrearBloqueoMedicoApiRequest cuerpo,
            HttpServletRequest request
    ) throws SQLException {
        if (cuerpo == null) {
            throw new IllegalArgumentException(
                    "Los datos del bloqueo son obligatorios."
            );
        }
        UsuarioAutenticadoDTO usuario = medicoAutenticado(request);
        ExcepcionHorarioMedico creada = disponibilidadService.registrarBloqueo(
                usuario.getIdPersona(),
                cuerpo.aSolicitudInterna()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ExcepcionDisponibilidadMedicoApiDTO.desde(creada));
    }

    @PatchMapping(
            path = "/bloqueos/{idExcepcion}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable("idExcepcion") long idExcepcion,
            @RequestBody EstadoBloqueoMedicoApiRequest cuerpo,
            HttpServletRequest request
    ) throws SQLException {
        if (cuerpo == null || cuerpo.activo() == null) {
            throw new IllegalArgumentException(
                    "El estado del bloqueo es obligatorio."
            );
        }
        UsuarioAutenticadoDTO usuario = medicoAutenticado(request);
        disponibilidadService.actualizarEstado(
                usuario.getIdPersona(),
                idExcepcion,
                cuerpo.activo()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(
            path = "/citas/{idCita}/cancelacion",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> cancelarCitaAfectada(
            @PathVariable("idCita") long idCita,
            @RequestBody CancelarCitaApiRequest cuerpo,
            HttpServletRequest request
    ) throws SQLException {
        if (cuerpo == null) {
            throw new IllegalArgumentException(
                    "El motivo de cancelación es obligatorio."
            );
        }
        UsuarioAutenticadoDTO usuario = medicoAutenticado(request);
        disponibilidadService.cancelarCita(
                usuario.getIdPersona(),
                usuario.getIdUsuario(),
                idCita,
                cuerpo.motivo()
        );
        return ResponseEntity.noContent().build();
    }

    private static UsuarioAutenticadoDTO medicoAutenticado(
            HttpServletRequest request
    ) {
        return SeguridadApiRest.requerirRol(request, "MEDICO");
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return LocalDate.parse(valor == null ? "" : valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La fecha debe usar el formato AAAA-MM-DD.",
                    error
            );
        }
    }
}
