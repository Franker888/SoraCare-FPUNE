package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.RegistroPacienteDTO;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.PacienteService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/pacientes/registro")
public class RegistroPacienteServlet extends HttpServlet {

    private static final String VISTA_REGISTRO =
            "/WEB-INF/views/paciente/registro.jsp";

    private final PacienteService pacienteService =
            new PacienteService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.getRequestDispatcher(VISTA_REGISTRO)
                .forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        RegistroPacienteDTO datosRegistro =
                new RegistroPacienteDTO(
                        request.getParameter("nombres"),
                        request.getParameter("apellidos"),
                        request.getParameter("cedula"),
                        request.getParameter("fechaNacimiento"),
                        request.getParameter("telefono"),
                        request.getParameter("correo"),
                        request.getParameter("direccion"),
                        request.getParameter("grupoSanguineo"),
                        request.getParameter("contactoEmergencia"),
                        request.getParameter("telefonoEmergencia")
                );

        try {
            Paciente paciente =
                    pacienteService.registrar(
                            datosRegistro
                    );

            if (paciente.getIdPaciente() == null
                    || paciente.getIdPaciente() <= 0) {

                throw new SQLException(
                        "No se obtuvo el ID del paciente registrado."
                );
            }

            String direccionDetalle =
                    request.getContextPath()
                            + "/pacientes/detalle?id="
                            + paciente.getIdPaciente();

            response.sendRedirect(
                    direccionDetalle
            );

        } catch (SQLException error) {

            getServletContext().log(
                    "Error al registrar al paciente",
                    error
            );

            request.setAttribute(
                    "error",
                    obtenerMensajeParaUsuario(error)
            );

            request.getRequestDispatcher(VISTA_REGISTRO)
                    .forward(request, response);
        }
    }

    private static String obtenerMensajeParaUsuario(
            SQLException error
    ) {
        String mensaje = error.getMessage();

        if ("Los nombres son obligatorios.".equals(mensaje)
                || "Los apellidos son obligatorios.".equals(mensaje)
                || "La cédula es obligatoria.".equals(mensaje)
                || "La cédula debe contener solamente números."
                .equals(mensaje)
                || "La cédula debe ser mayor que cero."
                .equals(mensaje)
                || "El número de cédula no es válido."
                .equals(mensaje)
                || "La fecha de nacimiento no es válida."
                .equals(mensaje)
                || "La fecha de nacimiento no puede ser futura."
                .equals(mensaje)
                || "El grupo sanguíneo no es válido."
                .equals(mensaje)
                || "La cédula ya está registrada."
                .equals(mensaje)
                || "El correo electrónico ya está registrado."
                .equals(mensaje)) {

            return mensaje;
        }

        return "No se pudo registrar al paciente. "
                + "Revisá los datos e intentá nuevamente.";
    }
}