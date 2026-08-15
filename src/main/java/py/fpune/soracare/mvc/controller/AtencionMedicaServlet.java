package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.PanelClinicoPacienteDTO;
import py.fpune.soracare.dto.RegistroAtencionMedicaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AtencionMedicaService;
import py.fpune.soracare.service.DocumentoClinicoService;
import py.fpune.soracare.service.MedicamentoService;
import py.fpune.soracare.service.RecetaMedicaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/medico/atencion")
public class AtencionMedicaServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/medico/atencion.jsp";

    private AtencionMedicaService atencionMedicaService;
    private DocumentoClinicoService documentoClinicoService;
    private MedicamentoService medicamentoService;
    private RecetaMedicaService recetaMedicaService;

    @Override
    public void init() {
        atencionMedicaService = new AtencionMedicaService();
        documentoClinicoService = new DocumentoClinicoService();
        medicamentoService = new MedicamentoService();
        recetaMedicaService = new RecetaMedicaService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        configurarSinCache(response);
        String resultado = request.getParameter("resultado");
        if ("iniciada".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "La atención fue iniciada. Ya podés registrar la evolución clínica."
            );
        } else if ("guardada".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "La información clínica fue guardada sin finalizar la atención."
            );
        } else if ("finalizada".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "La atención fue finalizada y la cita quedó registrada como atendida."
            );
        } else if ("documento-cargado".equals(resultado)) {
            request.setAttribute(
                    "mensajeExito",
                    "El documento clínico fue agregado al historial del paciente."
            );
        } else if ("documento-invalido".equals(resultado)) {
            request.setAttribute(
                    "error",
                    "Revisá el tipo, la fecha y el archivo. Se admiten PDF, JPG, JPEG y PNG."
            );
        } else if ("documento-grande".equals(resultado)) {
            request.setAttribute("error", "El archivo no puede superar los 10 MB.");
        } else if ("documento-error".equals(resultado)) {
            request.setAttribute(
                    "error", "No fue posible registrar el documento clínico. Intentá nuevamente."
            );
        } else if ("receta-emitida".equals(resultado)) {
            request.setAttribute("mensajeExito", "La indicación médica fue registrada y ya está disponible para el paciente.");
        } else if ("receta-anulada".equals(resultado)) {
            request.setAttribute("mensajeExito", "La indicación médica fue anulada y permanece en el historial.");
        } else if ("receta-error".equals(resultado)) {
            HttpSession sesion = request.getSession(false);
            Object mensaje = sesion == null ? null : sesion.getAttribute("errorReceta");
            if (mensaje instanceof String texto && !texto.isBlank()) {
                request.setAttribute("error", texto);
                sesion.removeAttribute("errorReceta");
            }
        }
        mostrarVista(request, response, convertirIdCita(request.getParameter("idCita")));
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        long idCita;
        try {
            idCita = convertirIdCita(request.getParameter("idCita"));
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            String accion = request.getParameter("accion");

            if ("iniciar".equals(accion)) {
                atencionMedicaService.iniciar(
                        usuario.getIdPersona(),
                        idCita,
                        usuario.getIdUsuario()
                );
                redirigir(request, response, idCita, "iniciada");
                return;
            }

            RegistroAtencionMedicaDTO datos = datosAtencion(request);
            if ("guardar".equals(accion)) {
                atencionMedicaService.guardar(
                        usuario.getIdPersona(),
                        idCita,
                        usuario.getIdUsuario(),
                        datos
                );
                redirigir(request, response, idCita, "guardada");
                return;
            }
            if ("finalizar".equals(accion)) {
                atencionMedicaService.finalizar(
                        usuario.getIdPersona(),
                        idCita,
                        usuario.getIdUsuario(),
                        datos
                );
                redirigir(request, response, idCita, "finalizada");
                return;
            }

            throw new IllegalArgumentException("La acción clínica seleccionada no es válida.");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            try {
                idCita = convertirIdCita(request.getParameter("idCita"));
                mostrarVista(request, response, idCita);
            } catch (IllegalArgumentException segundoError) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, segundoError.getMessage());
            }
        } catch (SQLException error) {
            log("Error al procesar la atención médica.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible procesar la atención. Intentá nuevamente."
            );
            try {
                idCita = convertirIdCita(request.getParameter("idCita"));
                mostrarVista(request, response, idCita);
            } catch (IllegalArgumentException segundoError) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No fue posible recuperar la cita."
                );
            }
        }
    }

    private void mostrarVista(
            HttpServletRequest request,
            HttpServletResponse response,
            long idCita
    ) throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            PanelClinicoPacienteDTO panel = atencionMedicaService.cargarPanel(
                    usuario.getIdPersona(),
                    idCita
            );
            request.setAttribute("panel", panel);
            request.setAttribute(
                    "documentosClinicos",
                    documentoClinicoService.listarParaMedico(usuario, idCita)
            );
            request.setAttribute("tiposDocumentoClinico", documentoClinicoService.listarTipos());
            if (panel.atencionActual() != null) {
                request.setAttribute(
                        "recetasAtencion",
                        recetaMedicaService.listarPorAtencion(
                                panel.atencionActual().idAtencion())
                );
            }
            request.setAttribute("medicamentosActivos", medicamentoService.listarActivos());
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", error.getMessage());
        } catch (SQLException error) {
            log("Error al cargar el panel clínico.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar la información clínica del paciente."
                );
            }
        }
        request.setAttribute("idCita", idCita);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static RegistroAtencionMedicaDTO datosAtencion(
            HttpServletRequest request
    ) {
        return new RegistroAtencionMedicaDTO(
                request.getParameter("sintomas"),
                request.getParameter("antecedentesRelevantes"),
                request.getParameter("antecedentesPatologicosPersonales"),
                request.getParameter("farmacosUtilizados"),
                request.getParameter("alergias"),
                request.getParameter("temperaturaC"),
                request.getParameter("frecuenciaCardiaca"),
                request.getParameter("frecuenciaRespiratoria"),
                request.getParameter("presionSistolica"),
                request.getParameter("presionDiastolica"),
                request.getParameter("saturacionOxigeno"),
                request.getParameter("evaluacionAreaAfectada"),
                request.getParameter("examenFisico"),
                request.getParameter("evaluacionClinica"),
                request.getParameter("planTratamiento"),
                request.getParameter("estudiosClinicosSolicitados"),
                request.getParameter("indicaciones"),
                request.getParameter("observaciones"),
                request.getParameter("requiereSeguimiento"),
                request.getParameter("fechaProximoControl")
        );
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

    private static long convertirIdCita(String valor) {
        try {
            long idCita = Long.parseLong(valor == null ? "" : valor.strip());
            if (idCita <= 0) {
                throw new NumberFormatException();
            }
            return idCita;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La cita seleccionada no es válida.", error);
        }
    }

    private static void redirigir(
            HttpServletRequest request,
            HttpServletResponse response,
            long idCita,
            String resultado
    ) throws IOException {
        response.sendRedirect(
                request.getContextPath()
                        + "/medico/atencion?idCita="
                        + idCita
                        + "&resultado="
                        + resultado
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
