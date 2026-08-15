package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.RegistroDetalleRecetaDTO;
import py.fpune.soracare.dto.RegistroRecetaMedicaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.RecetaMedicaService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/medico/recetas")
public class RecetaMedicaServlet extends HttpServlet {

    private RecetaMedicaService recetaMedicaService;

    @Override
    public void init() {
        recetaMedicaService = new RecetaMedicaService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        long idCita = 0;
        try {
            idCita = convertirId(request.getParameter("idCita"), "La cita no es válida.");
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            String accion = request.getParameter("accion");
            if ("anular".equals(accion)) {
                long idReceta = convertirId(
                        request.getParameter("idReceta"), "La indicación médica no es válida.");
                recetaMedicaService.anular(
                        usuario.getIdPersona(), usuario.getIdUsuario(), idReceta,
                        request.getParameter("motivoAnulacion"));
                redirigir(request, response, idCita, "receta-anulada");
                return;
            }
            if (!"emitir".equals(accion)) {
                throw new IllegalArgumentException("La acción de la indicación médica no es válida.");
            }
            recetaMedicaService.emitir(
                    usuario.getIdPersona(), idCita, construirSolicitud(request));
            redirigir(request, response, idCita, "receta-emitida");
        } catch (IllegalArgumentException error) {
            guardarError(request, error.getMessage());
            if (idCita > 0) {
                redirigir(request, response, idCita, "receta-error");
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, error.getMessage());
            }
        } catch (SQLException error) {
            log("Error al procesar una indicación médica.", error);
            guardarError(request, "No fue posible procesar la indicación médica. Intentá nuevamente.");
            if (idCita > 0) {
                redirigir(request, response, idCita, "receta-error");
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No fue posible procesar la indicación médica.");
            }
        }
    }

    private static RegistroRecetaMedicaDTO construirSolicitud(HttpServletRequest request) {
        String[] medicamentos = valores(request, "idMedicamento");
        String[] dosis = valores(request, "dosis");
        String[] frecuencias = valores(request, "frecuencia");
        String[] duraciones = valores(request, "duracionDias");
        String[] cantidades = valores(request, "cantidad");
        String[] unidades = valores(request, "unidad");
        String[] instrucciones = valores(request, "instruccionesDetalle");
        int cantidadFilas = medicamentos.length;
        if (cantidadFilas == 0
                || dosis.length != cantidadFilas
                || frecuencias.length != cantidadFilas
                || duraciones.length != cantidadFilas
                || cantidades.length != cantidadFilas
                || unidades.length != cantidadFilas
                || instrucciones.length != cantidadFilas) {
            throw new IllegalArgumentException(
                    "Los medicamentos de la indicación están incompletos.");
        }
        List<RegistroDetalleRecetaDTO> detalles = new ArrayList<>();
        for (int i = 0; i < cantidadFilas; i++) {
            detalles.add(new RegistroDetalleRecetaDTO(
                    medicamentos[i], dosis[i], frecuencias[i], duraciones[i],
                    cantidades[i], unidades[i], instrucciones[i]));
        }
        return new RegistroRecetaMedicaDTO(
                request.getParameter("fechaVencimiento"),
                request.getParameter("indicacionesGenerales"),
                detalles);
    }

    private static String[] valores(HttpServletRequest request, String nombre) {
        String[] valores = request.getParameterValues(nombre);
        return valores == null ? new String[0] : valores;
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)
                || !autenticado.tieneRol("MEDICO")) {
            throw new IllegalArgumentException("No existe una sesión médica válida.");
        }
        return autenticado;
    }

    private static void guardarError(HttpServletRequest request, String mensaje) {
        request.getSession().setAttribute("errorReceta", mensaje);
    }

    private static void redirigir(
            HttpServletRequest request,
            HttpServletResponse response,
            long idCita,
            String resultado
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + "/medico/atencion?idCita="
                + idCita + "&resultado=" + resultado);
    }

    private static long convertirId(String valor, String mensaje) {
        try {
            long id = Long.parseLong(valor);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensaje, error);
        }
    }
}
