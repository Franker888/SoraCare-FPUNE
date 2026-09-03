package py.fpune.soracare.mvc.util;

import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

public record SesionNavegacionPublica(
        boolean iniciada,
        boolean paciente
) {

    private static final String ATRIBUTO_USUARIO =
            "usuarioAutenticado";

    public static SesionNavegacionPublica desde(
            HttpSession sesion
    ) {
        Object valor = sesion == null
                ? null
                : sesion.getAttribute(ATRIBUTO_USUARIO);

        if (!(valor instanceof UsuarioAutenticadoDTO usuario)) {
            return new SesionNavegacionPublica(false, false);
        }

        return new SesionNavegacionPublica(
                true,
                usuario.tieneRol("PACIENTE")
        );
    }

    public String rutaAcceso() {
        return iniciada ? "/dashboard" : "/login";
    }

    public String textoAcceso() {
        return iniciada ? "Mi panel" : "Iniciar sesión";
    }

    public String rutaAgenda() {
        if (!iniciada) {
            return "/login";
        }
        return paciente ? "/mis-citas?nueva=1" : "/dashboard";
    }

    public String textoAgenda() {
        if (!iniciada) {
            return "Iniciar sesión y agendar";
        }
        return paciente ? "Agendar una cita" : "Ir a mi panel";
    }

    public String mensajeAgenda() {
        if (!iniciada) {
            return "Iniciá sesión para consultar fechas, horarios y reservar tu cita.";
        }
        if (paciente) {
            return "Tu sesión sigue activa. Consultá fechas, horarios y reservá tu cita.";
        }
        return "Tu sesión sigue activa. Volvé a tu panel para continuar.";
    }
}
