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
import py.fpune.soracare.service.AlmacenFotosPerfil;
import py.fpune.soracare.service.PerfilVisualService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/perfil")
@MultipartConfig(
        fileSizeThreshold = 256 * 1024,
        maxFileSize = AlmacenFotosPerfil.TAMANO_MAXIMO_BYTES,
        maxRequestSize = AlmacenFotosPerfil.TAMANO_MAXIMO_BYTES + 512 * 1024
)
public class PerfilVisualServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/perfil/editar.jsp";
    private PerfilVisualService perfilService;

    @Override
    public void init() {
        perfilService = new PerfilVisualService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        mostrar(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        UsuarioAutenticadoDTO usuario = usuarioPermitido(request, response);
        if (usuario == null) return;

        String accion = request.getParameter("accion");
        try {
            if ("avatar".equals(accion)) {
                perfilService.elegirAvatar(
                        usuario.getIdPersona(),
                        request.getParameter("avatar")
                );
                response.sendRedirect(request.getContextPath() + "/perfil?resultado=avatar");
                return;
            }
            if ("foto".equals(accion)) {
                Part foto = request.getPart("foto");
                byte[] contenido;
                if (foto == null) {
                    contenido = new byte[0];
                } else {
                    try (var entrada = foto.getInputStream()) {
                        contenido = entrada.readAllBytes();
                    }
                }
                perfilService.guardarFoto(usuario.getIdPersona(), contenido);
                response.sendRedirect(request.getContextPath() + "/perfil?resultado=foto");
                return;
            }
            throw new IllegalArgumentException("La acción de perfil solicitada no es válida.");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mostrar(request, response, error.getMessage());
        } catch (IllegalStateException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mostrar(request, response, "La imagen supera el límite permitido de 5 MB.");
        } catch (SQLException | IOException error) {
            log("No fue posible actualizar el perfil visual.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mostrar(
                    request,
                    response,
                    "No fue posible guardar la imagen. Intentá nuevamente."
            );
        }
    }

    private void mostrar(
            HttpServletRequest request,
            HttpServletResponse response,
            String error
    ) throws ServletException, IOException {
        UsuarioAutenticadoDTO usuario = usuarioPermitido(request, response);
        if (usuario == null || response.isCommitted()) return;
        request.setAttribute("usuarioAutenticado", usuario);
        if (error != null) request.setAttribute("error", error);
        try {
            request.setAttribute(
                    "perfilVisual",
                    perfilService.consultar(usuario.getIdPersona())
            );
        } catch (SQLException problema) {
            log("No fue posible consultar el perfil visual.", problema);
            request.setAttribute(
                    "error",
                    "El perfil visual todavía no está disponible. Aplicá la actualización local."
            );
        }
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioPermitido(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession sesion = request.getSession(false);
        Object valor = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(valor instanceof UsuarioAutenticadoDTO usuario)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        if (!usuario.tieneRol("MEDICO") && !usuario.tieneRol("PACIENTE")) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "El perfil visual corresponde a cuentas médicas o de pacientes."
            );
            return null;
        }
        return usuario;
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
