package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.DocumentoClinicoService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@WebServlet("/documentos/clinicos/archivo")
public class DocumentoClinicoArchivoServlet extends HttpServlet {

    private DocumentoClinicoService documentoService;

    @Override
    public void init() {
        documentoService = new DocumentoClinicoService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarRespuesta(response);
        try {
            long idDocumento = convertirLong(
                    request.getParameter("id"), "El documento seleccionado no es válido.");
            Long idCita = convertirLongOpcional(request.getParameter("idCita"));
            boolean descargar = "1".equals(request.getParameter("descargar"));
            DocumentoClinicoService.DocumentoDescargable archivo = documentoService.abrir(
                    usuarioAutenticado(request),
                    idDocumento,
                    idCita,
                    descargar,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
            String disposicion = descargar ? "attachment" : "inline";
            String nombre = nombreCabecera(archivo.nombreOriginal());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(archivo.tipoMime());
            response.setContentLength(archivo.contenido().length);
            response.setHeader(
                    "Content-Disposition",
                    disposicion + "; filename=\"documento-clinico\"; filename*=UTF-8''" + nombre
            );
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.getOutputStream().write(archivo.contenido());
        } catch (SecurityException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, error.getMessage());
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, error.getMessage());
        } catch (SQLException | IOException error) {
            log("Error al abrir el documento clínico.", error);
            if (!response.isCommitted()) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No fue posible abrir el documento clínico."
                );
            }
        }
    }

    private static long convertirLong(String valor, String mensaje) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensaje, error);
        }
    }

    private static Long convertirLongOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return convertirLong(valor, "La cita indicada no es válida.");
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return (UsuarioAutenticadoDTO) sesion.getAttribute("usuarioAutenticado");
    }

    private static String nombreCabecera(String nombreOriginal) {
        String nombre = nombreOriginal == null || nombreOriginal.isBlank()
                ? "documento-clinico" : nombreOriginal.strip();
        StringBuilder codificado = new StringBuilder();
        for (byte valor : nombre.getBytes(StandardCharsets.UTF_8)) {
            int caracter = valor & 0xff;
            if ((caracter >= 'a' && caracter <= 'z')
                    || (caracter >= 'A' && caracter <= 'Z')
                    || (caracter >= '0' && caracter <= '9')
                    || caracter == '.' || caracter == '-' || caracter == '_') {
                codificado.append((char) caracter);
            } else {
                codificado.append('%').append(String.format("%02X", caracter));
            }
        }
        return codificado.toString();
    }

    private static void configurarRespuesta(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
