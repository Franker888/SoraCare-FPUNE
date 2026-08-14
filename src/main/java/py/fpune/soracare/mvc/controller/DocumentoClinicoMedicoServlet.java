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

@WebServlet("/medico/documentos/cargar")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = AlmacenDocumentosClinicos.TAMANO_MAXIMO_BYTES,
        maxRequestSize = AlmacenDocumentosClinicos.TAMANO_MAXIMO_BYTES + 1024 * 1024
)
public class DocumentoClinicoMedicoServlet extends HttpServlet {

    private DocumentoClinicoService documentoService;

    @Override
    public void init() {
        documentoService = new DocumentoClinicoService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        long idCita = convertirLong(request.getParameter("idCita"));
        try {
            Part archivo = request.getPart("archivo");
            documentoService.cargarPorMedico(
                    usuarioAutenticado(request),
                    idCita,
                    convertirTipo(request.getParameter("idTipoDocumento")),
                    request.getParameter("fechaDocumento"),
                    request.getParameter("descripcion"),
                    archivo == null ? null : archivo.getSubmittedFileName(),
                    archivo == null ? null : archivo.getInputStream().readAllBytes()
            );
            redirigir(request, response, idCita, "documento-cargado");
        } catch (SecurityException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, error.getMessage());
        } catch (IllegalArgumentException error) {
            redirigir(request, response, idCita, "documento-invalido");
        } catch (IllegalStateException error) {
            redirigir(request, response, idCita, "documento-grande");
        } catch (SQLException error) {
            log("Error al registrar el documento clínico desde el portal médico.", error);
            redirigir(request, response, idCita, "documento-error");
        }
    }

    private static long convertirLong(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La cita seleccionada no es válida.", error);
        }
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

    private static void redirigir(
            HttpServletRequest request,
            HttpServletResponse response,
            long idCita,
            String resultado
    ) throws IOException {
        response.sendRedirect(request.getContextPath()
                + "/medico/atencion?idCita=" + idCita + "&resultado=" + resultado);
    }
}
