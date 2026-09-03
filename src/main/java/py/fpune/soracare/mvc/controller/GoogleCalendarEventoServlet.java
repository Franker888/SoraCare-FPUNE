package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.GoogleCalendarService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/google/calendar/evento")
public class GoogleCalendarEventoServlet extends HttpServlet {

    private GoogleCalendarService calendar;

    @Override
    public void init() {
        calendar = new GoogleCalendarService();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession sesion = request.getSession(false);
        Object valor = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(valor instanceof UsuarioAutenticadoDTO usuario)
                || !usuario.tieneRol("PACIENTE")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!ProteccionCsrf.esValido(
                sesion,
                request.getParameter("csrfToken")
        )) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "La solicitud no superó la validación de seguridad."
            );
            return;
        }

        try {
            long idCita = Long.parseLong(request.getParameter("idCita"));
            String accion = request.getParameter("accion");
            if ("guardar".equals(accion)) {
                calendar.guardarCita(usuario.getIdUsuario(), idCita);
                sesion.setAttribute(
                        GoogleCalendarOAuthServlet.ATRIBUTO_MENSAJE_CALENDAR,
                        "La cita quedó guardada o actualizada en tu Google Calendar."
                );
            } else if ("retirar".equals(accion)) {
                calendar.retirarCita(usuario.getIdUsuario(), idCita);
                sesion.setAttribute(
                        GoogleCalendarOAuthServlet.ATRIBUTO_MENSAJE_CALENDAR,
                        "La cita fue retirada de tu Google Calendar."
                );
            } else {
                throw new IllegalArgumentException(
                        "La operación de Calendar no es válida."
                );
            }
        } catch (NumberFormatException error) {
            guardarError(sesion, "La cita indicada no es válida.");
        } catch (IllegalArgumentException | IllegalStateException
                 | SecurityException error) {
            guardarError(sesion, error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            guardarError(
                    sesion,
                    "La conexión con Google Calendar fue interrumpida."
            );
        } catch (IOException error) {
            log("No fue posible contactar con Google Calendar.", error);
            guardarError(
                    sesion,
                    "Google Calendar no respondió. Intentá nuevamente."
            );
        } catch (SQLException error) {
            log("No fue posible sincronizar la cita con Calendar.", error);
            guardarError(
                    sesion,
                    "No fue posible actualizar la vinculación de la cita."
            );
        }

        response.sendRedirect(request.getContextPath() + "/mis-citas");
    }

    private static void guardarError(HttpSession sesion, String mensaje) {
        sesion.setAttribute(
                GoogleCalendarOAuthServlet.ATRIBUTO_ERROR_CALENDAR,
                mensaje == null || mensaje.isBlank()
                        ? "No fue posible completar la operación con Calendar."
                        : mensaje
        );
    }
}
