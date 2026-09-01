package py.fpune.soracare.sifen;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CdcSifenService {

    private static final DateTimeFormatter FECHA_CDC =
            DateTimeFormatter.BASIC_ISO_DATE;

    public String generar(DocumentoElectronicoSifen documento) {
        if (documento == null || documento.emisor() == null
                || documento.fechaEmision() == null) {
            throw new IllegalArgumentException("Los datos del documento son obligatorios.");
        }
        return generar(new DatosCdc(
                1,
                documento.emisor().ruc(),
                documento.emisor().digitoVerificador(),
                documento.establecimiento(),
                documento.puntoExpedicion(),
                documento.numeroDocumento(),
                documento.emisor().tipoContribuyente(),
                documento.fechaEmision().toLocalDate(),
                1,
                documento.codigoSeguridad()
        ));
    }

    public String generar(DatosCdc datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del CDC son obligatorios.");
        }
        String base = numero(datos.tipoDocumento(), 2, "tipo de documento")
                + numero(datos.rucEmisor(), 8, "RUC del emisor")
                + numero(datos.digitoVerificadorRuc(), 1, "DV del RUC")
                + numero(datos.establecimiento(), 3, "establecimiento")
                + numero(datos.puntoExpedicion(), 3, "punto de expedición")
                + numero(datos.numeroDocumento(), 7, "número de documento")
                + numero(datos.tipoContribuyente(), 1, "tipo de contribuyente")
                + fecha(datos.fechaEmision())
                + numero(datos.tipoEmision(), 1, "tipo de emisión")
                + numero(datos.codigoSeguridad(), 9, "código de seguridad");
        if (base.length() != 43) {
            throw new IllegalStateException("La base del CDC debe contener 43 dígitos.");
        }
        return base + calcularDigitoVerificador(base);
    }

    public int calcularDigitoVerificador(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El valor para calcular el DV es obligatorio.");
        }
        String numerico = convertirAlfanumerico(valor.strip());
        int total = 0;
        int factor = 2;
        for (int indice = numerico.length() - 1; indice >= 0; indice--) {
            total += Character.digit(numerico.charAt(indice), 10) * factor;
            factor = factor == 11 ? 2 : factor + 1;
        }
        int resto = total % 11;
        return resto > 1 ? 11 - resto : 0;
    }

    private static String convertirAlfanumerico(String valor) {
        StringBuilder resultado = new StringBuilder();
        for (char caracter : valor.toUpperCase().toCharArray()) {
            if (Character.isDigit(caracter)) {
                resultado.append(caracter);
            } else if (caracter >= 'A' && caracter <= 'Z') {
                resultado.append((int) caracter);
            } else {
                throw new IllegalArgumentException(
                        "El valor para calcular el DV contiene caracteres no permitidos."
                );
            }
        }
        return resultado.toString();
    }

    private static String fecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de emisión es obligatoria.");
        }
        return fecha.format(FECHA_CDC);
    }

    private static String numero(int valor, int longitud, String nombre) {
        if (valor < 0) {
            throw new IllegalArgumentException("El " + nombre + " no es válido.");
        }
        return numero(Integer.toString(valor), longitud, nombre);
    }

    private static String numero(String valor, int longitud, String nombre) {
        String limpio = valor == null ? "" : valor.strip();
        if (!limpio.matches("[0-9]+") || limpio.length() > longitud) {
            throw new IllegalArgumentException("El " + nombre + " no es válido.");
        }
        return "0".repeat(longitud - limpio.length()) + limpio;
    }

    public record DatosCdc(
            int tipoDocumento,
            String rucEmisor,
            int digitoVerificadorRuc,
            String establecimiento,
            String puntoExpedicion,
            String numeroDocumento,
            int tipoContribuyente,
            LocalDate fechaEmision,
            int tipoEmision,
            String codigoSeguridad
    ) {
    }
}
