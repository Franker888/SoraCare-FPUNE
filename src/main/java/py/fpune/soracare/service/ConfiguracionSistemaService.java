package py.fpune.soracare.service;

import py.fpune.soracare.dao.ConfiguracionSistemaDAO;
import py.fpune.soracare.dto.ConfiguracionSistemaDTO;
import py.fpune.soracare.dto.HistorialConfiguracionDTO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ConfiguracionSistemaService {

    private final ConfiguracionSistemaDAO configuracionDAO = new ConfiguracionSistemaDAO();

    public List<ConfiguracionSistemaDTO> listar() throws SQLException {
        return configuracionDAO.listar();
    }

    public List<HistorialConfiguracionDTO> listarHistorial() throws SQLException {
        return configuracionDAO.listarHistorial();
    }

    public void actualizar(
            String idConfiguracion,
            String valor,
            String motivo,
            long idUsuario
    ) throws SQLException {
        long id = convertirId(idConfiguracion);
        ConfiguracionSistemaDTO configuracion = listar().stream()
                .filter(item -> item.idConfiguracion() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La configuración seleccionada no existe."));
        String valorValidado = validarValor(valor, configuracion.tipoDato());
        String motivoValidado = validarMotivo(motivo);
        configuracionDAO.actualizar(id, valorValidado, idUsuario, motivoValidado);
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("La configuración seleccionada no es válida.");
        }
    }

    private static String validarValor(String valor, String tipoDato) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El valor de la configuración es obligatorio.");
        }
        if (limpio.length() > 2000) {
            throw new IllegalArgumentException("El valor es demasiado extenso.");
        }

        String tipo = tipoDato == null ? "TEXTO" : tipoDato.strip().toUpperCase();
        try {
            return switch (tipo) {
                case "ENTERO" -> {
                    long numero = Long.parseLong(limpio);
                    if (numero < 0) throw new IllegalArgumentException(
                            "El valor numérico no puede ser negativo.");
                    yield Long.toString(numero);
                }
                case "DECIMAL" -> new BigDecimal(limpio).stripTrailingZeros().toPlainString();
                case "BOOLEANO" -> {
                    if (!limpio.equalsIgnoreCase("true") && !limpio.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("El valor debe ser verdadero o falso.");
                    }
                    yield limpio.toLowerCase();
                }
                case "FECHA" -> LocalDate.parse(limpio).toString();
                case "HORA" -> LocalTime.parse(limpio).toString();
                case "JSON" -> {
                    if (!(limpio.startsWith("{") && limpio.endsWith("}"))
                            && !(limpio.startsWith("[") && limpio.endsWith("]"))) {
                        throw new IllegalArgumentException("El valor JSON debe ser un objeto o una lista.");
                    }
                    yield limpio;
                }
                case "TEXTO" -> limpio;
                default -> throw new IllegalArgumentException("El tipo de configuración no es válido.");
            };
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El valor numérico no es válido.");
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("El valor de fecha u hora no es válido.");
        }
    }

    private static String validarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.strip();
        if (limpio.length() < 5 || limpio.length() > 500) {
            throw new IllegalArgumentException("Explicá el motivo del cambio con al menos 5 caracteres.");
        }
        return limpio;
    }
}
