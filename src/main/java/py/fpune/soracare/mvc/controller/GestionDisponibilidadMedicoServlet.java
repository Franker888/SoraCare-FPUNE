package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.GestionDisponibilidadMedicoService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet("/medico/mi-disponibilidad")
public class GestionDisponibilidadMedicoServlet extends HttpServlet {

    private static final String VISTA =
            "/WEB-INF/views/medico/mi-disponibilidad.jsp";

    private GestionDisponibilidadMedicoService disponibilidadService;

    @Override
    public void init() {
        disponibilidadService = new GestionDisponibilidadMedicoService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        configurarMensaje(request);
        mostrarVista(request, response, convertirFecha(request.getParameter("fecha")));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        HttpSession sesion = request.getSession(false);
        if (!ProteccionCsrf.esValido(sesion, request.getParameter("csrfToken"))) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "La solicitud de disponibilidad no es válida."
            );
            return;
        }

        LocalDate fecha = convertirFecha(request.getParameter("fecha"));
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            String accion = request.getParameter("accion");
            if ("cancelarCita".equals(accion)) {
                disponibilidadService.cancelarCita(
                        usuario.getIdPersona(),
                        usuario.getIdUsuario(),
                        convertirId(request.getParameter("idCita")),
                        request.getParameter("motivoCancelacion")
                );
                redirigir(request, response, fecha, "citaCancelada");
                return;
            }
            if ("cambiarEstado".equals(accion)) {
                disponibilidadService.actualizarEstado(
                        usuario.getIdPersona(),
                        convertirId(request.getParameter("idExcepcion")),
                        Boolean.parseBoolean(request.getParameter("activo"))
                );
                redirigir(request, response, fecha, "estadoActualizado");
                return;
            }
            disponibilidadService.registrarBloqueo(
                    usuario.getIdPersona(),
                    new RegistroExcepcionHorarioDTO(
                            "",
                            "",
                            request.getParameter("fecha"),
                            request.getParameter("horaInicio"),
                            request.getParameter("horaFin"),
                            request.getParameter("tipo"),
                            request.getParameter("motivo")
                    )
            );
            redirigir(request, response, fecha, "bloqueoRegistrado");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response, fecha);
        } catch (SQLException error) {
            log("No fue posible gestionar la disponibilidad del médico.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible actualizar tu disponibilidad. Intentá nuevamente."
            );
            mostrarVista(request, response, fecha);
        }
    }

    private void mostrarVista(
            HttpServletRequest request,
            HttpServletResponse response,
            LocalDate fecha
    ) throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            request.setAttribute(
                    "resumenDisponibilidad",
                    disponibilidadService.consultar(usuario.getIdPersona(), fecha)
            );
            request.setAttribute(
                    "csrfToken",
                    ProteccionCsrf.obtenerOCrear(request.getSession(false))
            );
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", error.getMessage());
            }
        } catch (SQLException error) {
            log("No fue posible cargar la disponibilidad del médico.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar tu disponibilidad. Intentá nuevamente."
                );
            }
        }
        request.setAttribute("fechaSeleccionada", fecha);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(
            HttpServletRequest request
    ) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión médica válida.");
        }
        return autenticado;
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return valor == null || valor.isBlank()
                    ? LocalDate.now(DisponibilidadService.ZONA_HORARIA).plusDays(1)
                    : LocalDate.parse(valor.strip());
        } catch (DateTimeParseException error) {
            return LocalDate.now(DisponibilidadService.ZONA_HORARIA).plusDays(1);
        }
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "El registro seleccionado no es válido.",
                    error
            );
        }
    }

    private static void configurarMensaje(HttpServletRequest request) {
        String resultado = request.getParameter("resultado");
        if ("bloqueoRegistrado".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "Tu bloqueo fue registrado y ya no se ofrecerán turnos en ese período."
            );
        } else if ("citaCancelada".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "La cita fue cancelada y el paciente recibió una notificación."
            );
        } else if ("estadoActualizado".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "El estado del bloqueo fue actualizado."
            );
        }
    }

    private static void redirigir(
            HttpServletRequest request,
            HttpServletResponse response,
            LocalDate fecha,
            String resultado
    ) throws IOException {
        response.sendRedirect(
                request.getContextPath()
                        + "/medico/mi-disponibilidad?fecha=" + fecha
                        + "&resultado=" + resultado
        );
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
