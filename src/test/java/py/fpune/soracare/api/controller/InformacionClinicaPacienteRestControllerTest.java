package py.fpune.soracare.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import py.fpune.soracare.dto.DetalleRecetaDTO;
import py.fpune.soracare.dto.DocumentoClinicoDTO;
import py.fpune.soracare.dto.RecetaMedicaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.ViaAdministracion;
import py.fpune.soracare.service.DocumentoClinicoService;
import py.fpune.soracare.service.RecetaMedicaService;
import py.fpune.soracare.service.RecetaPdfService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class InformacionClinicaPacienteRestControllerTest {

    @Test
    void listaIndicacionesConLaIdentidadDeLaSesionYSinDatosInternos()
            throws Exception {
        RecetaMedicaServicePrueba recetas = new RecetaMedicaServicePrueba();
        recetas.indicaciones = List.of(indicacion());

        MvcResult resultado = crearMockMvc(recetas, new DocumentoServicePrueba())
                .perform(get("/pacientes/me/indicaciones")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals(7L, recetas.ultimaPersonaListado);

        JsonArray respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonArray();
        JsonObject indicacion = respuesta.get(0).getAsJsonObject();
        assertEquals(31L, indicacion.get("idIndicacion").getAsLong());
        assertEquals("Dra. Ana Martínez", indicacion.get("medico").getAsString());
        assertEquals("Oral", indicacion.getAsJsonArray("medicamentos")
                .get(0).getAsJsonObject().get("viaAdministracion").getAsString());
        assertFalse(indicacion.has("idPaciente"));
        assertFalse(indicacion.has("idMedico"));
        assertFalse(indicacion.has("idAtencion"));
        assertFalse(indicacion.has("emailIndicacionesDestino"));
    }

    @Test
    void consultaUnaIndicacionPropiaSinAceptarOtroPaciente()
            throws Exception {
        RecetaMedicaServicePrueba recetas = new RecetaMedicaServicePrueba();
        recetas.indicacion = indicacion();

        MvcResult resultado = crearMockMvc(recetas, new DocumentoServicePrueba())
                .perform(get("/pacientes/me/indicaciones/31")
                        .param("idPaciente", "999")
                        .param("idPersona", "999")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals(31L, recetas.ultimaIndicacionConsultada);
        assertEquals(7L, recetas.ultimaPersonaConsulta);
    }

    @Test
    void ocultaComoNoDisponibleUnaIndicacionAjena()
            throws Exception {
        RecetaMedicaServicePrueba recetas = new RecetaMedicaServicePrueba();
        recetas.errorConsulta = new IllegalArgumentException(
                "No tenés autorización para consultar esta receta."
        );

        MvcResult resultado = crearMockMvc(recetas, new DocumentoServicePrueba())
                .perform(get("/pacientes/me/indicaciones/99")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(404, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "RECURSO_NO_ENCONTRADO");
        assertFalse(resultado.getResponse().getContentAsString()
                .contains("autorización"));
    }

    @Test
    void generaElPdfSolamenteDespuesDeValidarLaPropiedad()
            throws Exception {
        RecetaMedicaServicePrueba recetas = new RecetaMedicaServicePrueba();
        recetas.indicacion = indicacion();
        RecetaPdfServicePrueba pdf = new RecetaPdfServicePrueba();
        byte[] contenido = "%PDF-prueba".getBytes(StandardCharsets.UTF_8);
        pdf.documento = new RecetaPdfService.DocumentoPdf(
                "indicacion-medica-REC-00000031.pdf",
                contenido
        );

        MvcResult resultado = crearMockMvc(
                recetas,
                pdf,
                new DocumentoServicePrueba()
        ).perform(get("/pacientes/me/indicaciones/31/pdf")
                        .param("descargar", "true")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("application/pdf", resultado.getResponse().getContentType());
        assertTrue(resultado.getResponse().getHeader("Content-Disposition")
                .startsWith("attachment;"));
        assertArrayEquals(contenido, resultado.getResponse().getContentAsByteArray());
        assertSame(recetas.indicacion, pdf.ultimaIndicacion);
    }

    @Test
    void listaMetadatosDeDocumentosSinExponerAlmacenamientoNiPaciente()
            throws Exception {
        DocumentoServicePrueba documentos = new DocumentoServicePrueba();
        documentos.documentos = List.of(documento());

        MvcResult resultado = crearMockMvc(
                new RecetaMedicaServicePrueba(),
                documentos
        ).perform(get("/pacientes/me/documentos")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals(7L, documentos.ultimoUsuarioListado.getIdPersona());

        JsonObject documento = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonArray().get(0).getAsJsonObject();
        assertEquals(81L, documento.get("idDocumento").getAsLong());
        assertEquals("Laboratorio.pdf", documento.get("nombreArchivo").getAsString());
        assertFalse(documento.has("idPaciente"));
        assertFalse(documento.has("idAtencion"));
        assertFalse(documento.has("idTipoDocumento"));
        assertFalse(documento.has("rutaAlmacenamiento"));
        assertFalse(documento.has("hashSha256"));
    }

    @Test
    void abreUnDocumentoPropioYRegistraElTipoDeAcceso()
            throws Exception {
        DocumentoServicePrueba documentos = new DocumentoServicePrueba();
        byte[] contenido = "%PDF-documento".getBytes(StandardCharsets.UTF_8);
        documentos.archivo = new DocumentoClinicoService.DocumentoDescargable(
                "Laboratorio.pdf",
                "application/pdf",
                contenido
        );

        MvcResult resultado = crearMockMvc(
                new RecetaMedicaServicePrueba(),
                documentos
        ).perform(get("/pacientes/me/documentos/81/archivo")
                        .param("descargar", "true")
                        .header("User-Agent", "SoraCare-App-Prueba")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("application/pdf", resultado.getResponse().getContentType());
        assertTrue(resultado.getResponse().getHeader("Content-Disposition")
                .startsWith("attachment;"));
        assertArrayEquals(contenido, resultado.getResponse().getContentAsByteArray());
        assertEquals(81L, documentos.ultimoDocumentoAbierto);
        assertEquals(7L, documentos.ultimoUsuarioApertura.getIdPersona());
        assertTrue(documentos.ultimaDescarga);
        assertEquals("SoraCare-App-Prueba", documentos.ultimoAgenteUsuario);
    }

    @Test
    void rechazaElArchivoDeOtroPacienteSinFiltrarDetalles()
            throws Exception {
        DocumentoServicePrueba documentos = new DocumentoServicePrueba();
        documentos.errorApertura = new SecurityException("Paciente 999");

        MvcResult resultado = crearMockMvc(
                new RecetaMedicaServicePrueba(),
                documentos
        ).perform(get("/pacientes/me/documentos/99/archivo")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(403, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "ACCESO_DENEGADO");
        assertFalse(resultado.getResponse().getContentAsString().contains("999"));
    }

    @Test
    void devuelveErrorJsonParaPdfAjenoAunqueElClienteSolicitePdf()
            throws Exception {
        RecetaMedicaServicePrueba recetas = new RecetaMedicaServicePrueba();
        recetas.errorConsulta = new IllegalArgumentException("Receta de otro paciente");
        RecetaPdfServicePrueba pdf = new RecetaPdfServicePrueba();

        MvcResult resultado = crearMockMvc(recetas, pdf, new DocumentoServicePrueba())
                .perform(get("/pacientes/me/indicaciones/99/pdf")
                        .accept(MediaType.APPLICATION_PDF)
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(404, resultado.getResponse().getStatus());
        assertEquals("application/json", resultado.getResponse().getContentType());
        verificarCodigo(resultado, "RECURSO_NO_ENCONTRADO");
        assertNull(pdf.ultimaIndicacion);
    }

    @Test
    void ocultaLasRutasInternasAnteFalloDeLecturaDelArchivo()
            throws Exception {
        DocumentoServicePrueba documentos = new DocumentoServicePrueba();
        documentos.errorLectura = new IOException("/datos/privados/paciente-999.pdf");

        MvcResult resultado = crearMockMvc(new RecetaMedicaServicePrueba(), documentos)
                .perform(get("/pacientes/me/documentos/81/archivo")
                        .accept(MediaType.APPLICATION_PDF)
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(500, resultado.getResponse().getStatus());
        assertEquals("application/json", resultado.getResponse().getContentType());
        verificarCodigo(resultado, "ARCHIVO_NO_DISPONIBLE");
        assertFalse(resultado.getResponse().getContentAsString().contains("privados"));
    }

    @Test
    void validaIdentificadoresAntesDeConsultarLosServicios()
            throws Exception {
        RecetaMedicaServicePrueba recetas = new RecetaMedicaServicePrueba();
        DocumentoServicePrueba documentos = new DocumentoServicePrueba();
        MockMvc mvc = crearMockMvc(recetas, documentos);
        for (String ruta : List.of(
                "/pacientes/me/indicaciones/0",
                "/pacientes/me/indicaciones/-1/pdf",
                "/pacientes/me/documentos/0/archivo",
                "/pacientes/me/documentos/no-numero/archivo"
        )) {
            MvcResult resultado = mvc.perform(get(ruta)
                            .session(sesion(usuario("PACIENTE"))))
                    .andReturn();
            assertEquals(400, resultado.getResponse().getStatus(), ruta);
            verificarCodigo(resultado, "SOLICITUD_INVALIDA");
        }
        assertEquals(0L, recetas.ultimaPersonaConsulta);
        assertNull(documentos.ultimoUsuarioApertura);
    }

    @Test
    void exigeAutenticacionYRolInclusoSinElFiltroServlet()
            throws Exception {
        MockMvc mvc = crearMockMvc(
                new RecetaMedicaServicePrueba(), new DocumentoServicePrueba()
        );
        for (String ruta : List.of(
                "/pacientes/me/indicaciones",
                "/pacientes/me/indicaciones/31",
                "/pacientes/me/indicaciones/31/pdf",
                "/pacientes/me/documentos",
                "/pacientes/me/documentos/81/archivo"
        )) {
            MvcResult sinSesion = mvc.perform(get(ruta)).andReturn();
            assertEquals(401, sinSesion.getResponse().getStatus(), ruta);
            verificarCodigo(sinSesion, "NO_AUTENTICADO");
            MvcResult medico = mvc.perform(get(ruta)
                            .session(sesion(usuario("MEDICO"))))
                    .andReturn();
            assertEquals(403, medico.getResponse().getStatus(), ruta);
            verificarCodigo(medico, "ACCESO_DENEGADO");
        }
    }

    private static MockMvc crearMockMvc(
            RecetaMedicaService recetas,
            DocumentoClinicoService documentos
    ) {
        return crearMockMvc(recetas, new RecetaPdfServicePrueba(), documentos);
    }

    private static MockMvc crearMockMvc(
            RecetaMedicaService recetas,
            RecetaPdfService pdf,
            DocumentoClinicoService documentos
    ) {
        return MockMvcBuilders.standaloneSetup(
                        new InformacionClinicaPacienteRestController(
                                recetas,
                                pdf,
                                documentos
                        )
                )
                .setControllerAdvice(new ApiRestExcepciones())
                .build();
    }

    private static MockHttpSession sesion(UsuarioAutenticadoDTO usuario) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioAutenticado", usuario);
        return sesion;
    }

    private static UsuarioAutenticadoDTO usuario(String rol) {
        return new UsuarioAutenticadoDTO(
                5L,
                7L,
                "paciente.prueba",
                false,
                Set.of(rol)
        );
    }

    private static RecetaMedicaDTO indicacion() {
        DetalleRecetaDTO detalle = new DetalleRecetaDTO(
                301L,
                12L,
                "Paracetamol",
                "Paracetamol",
                "Comprimidos",
                "500 mg",
                ViaAdministracion.ORAL,
                "1 comprimido",
                "Cada 8 horas",
                5,
                new BigDecimal("15.00"),
                "comprimidos",
                "Tomar después de las comidas"
        );
        return new RecetaMedicaDTO(
                31L,
                41L,
                22L,
                9L,
                "REC-00000031",
                OffsetDateTime.parse("2026-09-01T09:00:00-03:00"),
                LocalDate.parse("2026-09-30"),
                "Mantener reposo",
                "EMITIDA",
                null,
                null,
                "Paciente Prueba",
                "Dra. Ana Martínez",
                "Clínica Médica",
                "correo-privado@example.com",
                "ENVIADO",
                OffsetDateTime.parse("2026-09-01T09:01:00-03:00"),
                List.of(detalle)
        );
    }

    private static DocumentoClinicoDTO documento() {
        return new DocumentoClinicoDTO(
                81L,
                22L,
                41L,
                (short) 2,
                "LABORATORIO",
                "Resultado de laboratorio",
                "Laboratorio.pdf",
                "application/pdf",
                4096L,
                "Hemograma",
                "MEDICO",
                LocalDate.parse("2026-08-30"),
                OffsetDateTime.parse("2026-09-01T09:10:00-03:00"),
                "Dra. Ana Martínez"
        );
    }

    private static void verificarCodigo(
            MvcResult resultado,
            String codigo
    ) throws Exception {
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(codigo, respuesta.get("codigo").getAsString());
    }

    private static final class RecetaMedicaServicePrueba
            extends RecetaMedicaService {

        private List<RecetaMedicaDTO> indicaciones = List.of();
        private RecetaMedicaDTO indicacion;
        private IllegalArgumentException errorConsulta;
        private long ultimaPersonaListado;
        private long ultimaIndicacionConsultada;
        private long ultimaPersonaConsulta;

        @Override
        public List<RecetaMedicaDTO> listarParaPaciente(long idPersonaPaciente) {
            ultimaPersonaListado = idPersonaPaciente;
            return indicaciones;
        }

        @Override
        public RecetaMedicaDTO buscarParaPaciente(
                long idReceta,
                long idPersonaPaciente
        ) {
            ultimaIndicacionConsultada = idReceta;
            ultimaPersonaConsulta = idPersonaPaciente;
            if (errorConsulta != null) {
                throw errorConsulta;
            }
            return indicacion;
        }
    }

    private static final class RecetaPdfServicePrueba
            extends RecetaPdfService {

        private DocumentoPdf documento;
        private RecetaMedicaDTO ultimaIndicacion;

        @Override
        public DocumentoPdf generar(RecetaMedicaDTO receta) {
            ultimaIndicacion = receta;
            return documento;
        }
    }

    private static final class DocumentoServicePrueba
            extends DocumentoClinicoService {

        private List<DocumentoClinicoDTO> documentos = List.of();
        private DocumentoDescargable archivo;
        private RuntimeException errorApertura;
        private IOException errorLectura;
        private UsuarioAutenticadoDTO ultimoUsuarioListado;
        private UsuarioAutenticadoDTO ultimoUsuarioApertura;
        private long ultimoDocumentoAbierto;
        private boolean ultimaDescarga;
        private String ultimoAgenteUsuario;

        @Override
        public List<DocumentoClinicoDTO> listarParaPaciente(
                UsuarioAutenticadoDTO usuario
        ) {
            ultimoUsuarioListado = usuario;
            return documentos;
        }

        @Override
        public DocumentoDescargable abrir(
                UsuarioAutenticadoDTO usuario,
                long idDocumento,
                Long idCita,
                boolean descargar,
                String direccionIp,
                String agenteUsuario
        ) throws SQLException, IOException {
            ultimoUsuarioApertura = usuario;
            ultimoDocumentoAbierto = idDocumento;
            ultimaDescarga = descargar;
            ultimoAgenteUsuario = agenteUsuario;
            if (errorApertura != null) {
                throw errorApertura;
            }
            if (errorLectura != null) {
                throw errorLectura;
            }
            return archivo;
        }
    }
}
