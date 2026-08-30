package py.fpune.soracare.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.SlotDisponibleApiDTO;
import py.fpune.soracare.service.DisponibilidadService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping(
        path = "/disponibilidad",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class DisponibilidadRestController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadRestController(
            DisponibilidadService disponibilidadService
    ) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public List<SlotDisponibleApiDTO> listar(
            @RequestParam(name = "fecha", required = false) String fecha,
            @RequestParam(name = "idMedico", required = false)
            String idMedico,
            @RequestParam(name = "idEspecialidad", required = false)
            String idEspecialidad
    ) throws SQLException {
        LocalDate fechaConsulta = convertirFecha(fecha);
        Long medico = convertirLongOpcional(
                idMedico,
                "El médico seleccionado no es válido."
        );
        Integer especialidad = convertirEnteroOpcional(
                idEspecialidad,
                "La especialidad seleccionada no es válida."
        );

        return disponibilidadService.listar(
                        fechaConsulta,
                        medico,
                        especialidad
                )
                .stream()
                .map(SlotDisponibleApiDTO::desde)
                .toList();
    }

    private static LocalDate convertirFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "La fecha de disponibilidad es obligatoria."
            );
        }

        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La fecha debe usar el formato AAAA-MM-DD.",
                    error
            );
        }
    }

    private static Long convertirLongOpcional(
            String valor,
            String mensajeError
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            long numero = Long.parseLong(valor.strip());
            if (numero <= 0) {
                throw new IllegalArgumentException(mensajeError);
            }
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensajeError, error);
        }
    }

    private static Integer convertirEnteroOpcional(
            String valor,
            String mensajeError
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero <= 0) {
                throw new IllegalArgumentException(mensajeError);
            }
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensajeError, error);
        }
    }
}
