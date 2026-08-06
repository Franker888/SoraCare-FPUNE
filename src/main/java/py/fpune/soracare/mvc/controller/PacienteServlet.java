package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.mvc.model.Paciente;

import java.io.IOException;

@WebServlet("/paciente")
public class PacienteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        Paciente paciente = new Paciente();

        paciente.setNombres("Adrián");
        paciente.setApellidos("González");
        paciente.setCedula("1234567");
        paciente.setNumeroHistoriaClinica("HC-0001");

        request.setAttribute("paciente", paciente);

        request.getRequestDispatcher("/WEB-INF/views/paciente/detalle.jsp")
                .forward(request, response);
    }
}