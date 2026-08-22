package py.fpune.soracare.mvc.util;

import jakarta.servlet.http.HttpServletRequest;

public final class UrlPublicaRequest {

    private UrlPublicaRequest() {
    }

    public static String construir(
            HttpServletRequest request,
            String ruta
    ) {
        String esquema = primerValor(
                request.getHeader("X-Forwarded-Proto")
        );
        boolean usaProxy = esquema != null;

        if (!"http".equalsIgnoreCase(esquema)
                && !"https".equalsIgnoreCase(esquema)) {
            esquema = request.getScheme();
            usaProxy = false;
        }

        int puerto = obtenerPuerto(
                request,
                esquema,
                usaProxy
        );

        StringBuilder url = new StringBuilder()
                .append(esquema)
                .append("://")
                .append(request.getServerName());

        if (!esPuertoPredeterminado(esquema, puerto)) {
            url.append(':').append(puerto);
        }

        return url.append(request.getContextPath())
                .append(ruta)
                .toString();
    }

    private static int obtenerPuerto(
            HttpServletRequest request,
            String esquema,
            boolean usaProxy
    ) {
        String puertoAdelantado = primerValor(
                request.getHeader("X-Forwarded-Port")
        );

        if (puertoAdelantado != null) {
            try {
                return Integer.parseInt(puertoAdelantado);
            } catch (NumberFormatException ignored) {
                // Se utiliza el puerto seguro predeterminado del proxy.
            }
        }

        if (usaProxy) {
            return "https".equalsIgnoreCase(esquema)
                    ? 443
                    : 80;
        }

        return request.getServerPort();
    }

    private static boolean esPuertoPredeterminado(
            String esquema,
            int puerto
    ) {
        return ("http".equalsIgnoreCase(esquema) && puerto == 80)
                || ("https".equalsIgnoreCase(esquema) && puerto == 443);
    }

    private static String primerValor(String encabezado) {
        if (encabezado == null || encabezado.isBlank()) {
            return null;
        }

        String valor = encabezado.split(",", 2)[0].strip();
        return valor.isBlank() ? null : valor;
    }
}
