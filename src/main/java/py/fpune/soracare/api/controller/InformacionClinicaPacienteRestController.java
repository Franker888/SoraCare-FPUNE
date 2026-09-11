package py.fpune.soracare.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import py.fpune.soracare.api.dto.DocumentoClinicoPacienteApiDTO;
import py.fpune.soracare.api.dto.IndicacionMedicaPacienteApiDTO;
import py.fpune.soracare.api.security.ApiAccesoDenegadoException;
import py.fpune.soracare.api.security.ApiRecursoNoEncontradoException;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.RecetaMedicaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.DocumentoClinicoService;
import py.fpune.soracare.service.RecetaMedicaService;
import py.fpune.soracare.service.RecetaPdfService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping(path = "/pacientes/me")
public class InformacionClinicaPacienteRestController {

    private final RecetaMedicaService recetaService;
    private final RecetaPdfService recetaPdfService;
    private final DocumentoClinicoService documentoService;

    public InformacionClinicaPacienteRestController(
            RecetaMedicaService recetaService,
            RecetaPdfService recetaPdfService,
            DocumentoClinicoService documentoService
    ) {
        this.recetaService = recetaService;
        this.recetaPdfService = recetaPdfService;
        this.documentoService = documentoService;
    }

    @GetMapping(
            path = "/indicaciones",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<IndicacionMedicaPacienteApiDTO> listarIndicaciones(
            HttpServletRequest request
    ) throws SQLException {
        UsuarioAutenticadoDTO usuario = pacienteAutenticado(request);
        return recetaService.listarParaPaciente(usuario.getIdPersona())
                .stream()
                .map(IndicacionMedicaPacienteApiDTO::desde)
                .toList();
    }

    @GetMapping(
            path = "/indicaciones/{idIndicacion}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public IndicacionMedicaPacienteApiDTO consultarIndicacion(
            @PathVariable("idIndicacion") long idIndicacion,
            HttpServletRequest request
    ) throws SQLException {
        return IndicacionMedicaPacienteApiDTO.desde(
                buscarIndicacionPropia(idIndicacion, request)
        );
    }

    @GetMapping(
            path = "/indicaciones/{idIndicacion}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> abrirPdfIndicacion(
            @PathVariable("idIndicacion") long idIndicacion,
            @RequestParam(name = "descargar", defaultValue = "false")
            boolean descargar,
            HttpServletRequest request
    ) throws SQLException {
        RecetaPdfService.DocumentoPdf pdf = recetaPdfService.generar(
                buscarIndicacionPropia(idIndicacion, request)
        );
        return respuestaArchivo(
                pdf.nombreArchivo(),
                MediaType.APPLICATION_PDF_VALUE,
                pdf.contenido(),
                descargar
        );
    }

    @GetMapping(
            path = "/documentos",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<DocumentoClinicoPacienteApiDTO> listarDocumentos(
            HttpServletRequest request
    ) throws SQLException {
        UsuarioAutenticadoDTO usuario = pacienteAutenticado(request);
        return documentoService.listarParaPaciente(usuario)
                .stream()
                .map(DocumentoClinicoPacienteApiDTO::desde)
                .toList();
    }

    @GetMapping(path = "/documentos/{idDocumento}/archivo")
    public ResponseEntity<byte[]> abrirDocumento(
            @PathVariable("idDocumento") long idDocumento,
            @RequestParam(name = "descargar", defaultValue = "false")
            boolean descargar,
            HttpServletRequest request
    ) throws SQLException, IOException {
        UsuarioAutenticadoDTO usuario = pacienteAutenticado(request);
        validarId(idDocumento, "El documento seleccionado no es válido.");
        try {
            DocumentoClinicoService.DocumentoDescargable archivo =
                    documentoService.abrir(
                            usuario,
                            idDocumento,
                            null,
                            descargar,
                            request.getRemoteAddr(),
                            request.getHeader("User-Agent")
                    );
            return respuestaArchivo(
                    archivo.nombreOriginal(),
                    archivo.tipoMime(),
                    archivo.contenido(),
                    descargar
            );
        } catch (SecurityException error) {
            throw new ApiAccesoDenegadoException(
                    "No tenés autorización para consultar este documento clínico."
            );
        } catch (IllegalArgumentException error) {
            throw new ApiRecursoNoEncontradoException(
                    "El documento clínico no existe o no está disponible."
            );
        }
    }

    private RecetaMedicaDTO buscarIndicacionPropia(
            long idIndicacion,
            HttpServletRequest request
    ) throws SQLException {
        UsuarioAutenticadoDTO usuario = pacienteAutenticado(request);
        validarId(idIndicacion, "La indicación médica seleccionada no es válida.");
        try {
            return recetaService.buscarParaPaciente(
                    idIndicacion,
                    usuario.getIdPersona()
            );
        } catch (IllegalArgumentException error) {
            throw new ApiRecursoNoEncontradoException(
                    "La indicación médica no existe o no está disponible."
            );
        }
    }

    private static UsuarioAutenticadoDTO pacienteAutenticado(
            HttpServletRequest request
    ) {
        return SeguridadApiRest.requerirRol(request, "PACIENTE");
    }

    private static void validarId(long id, String mensaje) {
        if (id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private static ResponseEntity<byte[]> respuestaArchivo(
            String nombre,
            String tipoMime,
            byte[] contenido,
            boolean descargar
    ) {
        byte[] cuerpo = contenido == null ? new byte[0] : contenido;
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(tipoContenido(tipoMime));
        cabeceras.setContentLength(cuerpo.length);
        cabeceras.setCacheControl("no-store");
        cabeceras.setContentDisposition(
                ContentDisposition.builder(descargar ? "attachment" : "inline")
                        .filename(nombreSeguro(nombre), StandardCharsets.UTF_8)
                        .build()
        );
        return ResponseEntity.ok().headers(cabeceras).body(cuerpo);
    }

    private static MediaType tipoContenido(String tipoMime) {
        try {
            return MediaType.parseMediaType(tipoMime);
        } catch (RuntimeException error) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String nombreSeguro(String nombre) {
        return nombre == null || nombre.isBlank()
                ? "documento-clinico"
                : nombre.strip();
    }
}
