package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AlmacenDocumentosClinicos;
import py.fpune.soracare.service.DocumentoClinicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet("/mis-documentos")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = AlmacenDocumentosClinicos.TAMANO_MAXIMO_BYTES,
        maxRequestSize = AlmacenDocumentosClinicos.TAMANO_MAXIMO_BYTES + 1024 * 1024
)
public class MisDocumentosClinicosServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/paciente/mis-documentos.jsp";
    private DocumentoClinicoService documentoService;

    @Override
    public void init() {
        documentoService = new DocumentoClinicoService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        if ("cargado".equals(request.getParameter("resultado"))) {
            request.setAttribute(
                    "mensajeExito",
                    "El documento clínico fue cargado y ya está disponible en tu historial."
            );
        }
        mostrarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        try {
            Part archivo = request.getPart("archivo");
            documentoService.cargarPorPaciente(
                    usuarioAutenticado(request),
                    convertirTipo(request.getParameter("idTipoDocumento")),
                    request.getParameter("fechaDocumento"),
                    request.getParameter("descripcion"),
                    archivo == null ? null : archivo.getSubmittedFileName(),
                    archivo == null ? null : archivo.getInputStream().readAllBytes()
            );
            response.sendRedirect(request.getContextPath()
                    + "/mis-documentos?resultado=cargado");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response);
        } catch (IllegalStateException error) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            request.setAttribute("error", "El archivo no puede superar los 10 MB.");
            mostrarVista(request, response);
        } catch (SQLException error) {
            log("Error al registrar el documento clínico del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error", "No fue posible registrar el documento clínico. Intentá nuevamente."
            );
            mostrarVista(request, response);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            request.setAttribute("documentos", documentoService.listarParaPaciente(usuario));
            request.setAttribute("tiposDocumento", documentoService.listarTipos());
            request.setAttribute("hoy", LocalDate.now());
        } catch (SQLException error) {
            log("Error al cargar los documentos clínicos del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "No fue posible cargar tus documentos clínicos.");
            }
            request.setAttribute("documentos", java.util.List.of());
            request.setAttribute("tiposDocumento", java.util.List.of());
            request.setAttribute("hoy", LocalDate.now());
        }
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static short convertirTipo(String valor) {
        try {
            short id = Short.parseShort(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Seleccioná el tipo de documento clínico.", error);
        }
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return (UsuarioAutenticadoDTO) sesion.getAttribute("usuarioAutenticado");
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
