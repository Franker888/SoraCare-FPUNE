package py.fpune.soracare.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Valida exclusivamente tarjetas ficticias habilitadas para la demostracion.
 * El numero completo y el codigo de seguridad nunca forman parte del resultado.
 */
public class TarjetaPruebaService {

    private static final DateTimeFormatter FORMATO_VENCIMIENTO =
            DateTimeFormatter.ofPattern("MM/yy");
    private static final Map<String, String> TARJETAS_HABILITADAS = Map.of(
            "4242424242424242", "VISA",
            "5555555555554444", "MASTERCARD"
    );

    public Resultado validar(String numeroIngresado, String vencimientoIngresado,
                             String titularIngresado, String codigoSeguridadIngresado) {
        String numero = soloDigitos(numeroIngresado);
        String marca = TARJETAS_HABILITADAS.get(numero);
        if (marca == null) {
            throw new IllegalArgumentException(
                    "Usa una de las tarjetas ficticias habilitadas para esta simulacion.");
        }

        String titular = textoRequerido(titularIngresado,
                "Ingresa el nombre del titular de la tarjeta de prueba.");
        if (titular.length() < 3 || titular.length() > 80) {
            throw new IllegalArgumentException("El nombre del titular debe tener entre 3 y 80 caracteres.");
        }

        YearMonth vencimiento = convertirVencimiento(vencimientoIngresado);
        if (vencimiento.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento de prueba ya expiro.");
        }

        String codigoSeguridad = textoRequerido(codigoSeguridadIngresado,
                "Ingresa el codigo de seguridad CVV/CVC de prueba.");
        if (!codigoSeguridad.matches("\\d{3}")) {
            throw new IllegalArgumentException("El codigo de seguridad CVV/CVC debe tener 3 digitos.");
        }

        return new Resultado(marca, numero.substring(numero.length() - 4));
    }

    private static YearMonth convertirVencimiento(String valor) {
        String vencimiento = textoRequerido(valor,
                "Ingresa la fecha de vencimiento de la tarjeta de prueba.");
        if (!vencimiento.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new IllegalArgumentException("La fecha de vencimiento debe tener el formato MM/AA.");
        }
        try {
            return YearMonth.parse(vencimiento, FORMATO_VENCIMIENTO);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha de vencimiento no es valida.", error);
        }
    }

    private static String soloDigitos(String valor) {
        String numero = textoRequerido(valor,
                "Ingresa el numero de una tarjeta ficticia de prueba.")
                .replace(" ", "").replace("-", "");
        if (!numero.matches("\\d{13,19}")) {
            throw new IllegalArgumentException("El numero de tarjeta de prueba no es valido.");
        }
        return numero;
    }

    private static String textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }

    public record Resultado(String marca, String ultimos4) {
    }
}
