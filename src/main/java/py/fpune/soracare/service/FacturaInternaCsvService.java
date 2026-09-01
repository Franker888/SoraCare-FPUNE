package py.fpune.soracare.service;

import py.fpune.soracare.dto.PagoResumenDTO;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FacturaInternaCsvService {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DocumentoCsv generar(List<PagoResumenDTO> pagos) {
        if (pagos == null) {
            throw new IllegalArgumentException(
                    "Los pagos de facturación son obligatorios."
            );
        }

        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        agregarFila(csv, List.of(
                "ID pago", "Fecha de consulta", "Paciente", "Cédula",
                "Especialidad", "Método", "Importe", "Moneda",
                "Estado factura", "Número interno", "Estado del correo"
        ));

        for (PagoResumenDTO pago : pagos) {
            boolean emitida = pago.idFacturaInterna() != null;
            agregarFila(csv, List.of(
                    Long.toString(pago.idPago()),
                    pago.fechaCita()
                            .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                            .format(FECHA_HORA),
                    valor(pago.nombrePaciente()),
                    valor(pago.cedulaPaciente()),
                    valor(pago.nombreEspecialidad()),
                    valor(pago.nombreMetodo()),
                    pago.monto().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                    valor(pago.moneda()),
                    emitida ? "EMITIDA" : "PENDIENTE",
                    emitida ? valor(pago.numeroFacturaInterna()) : "",
                    emitida ? valor(pago.estadoEnvioFacturaInterna()) : ""
            ));
        }

        String nombre = "facturacion-medico-"
                + LocalDate.now(DisponibilidadService.ZONA_HORARIA)
                + ".csv";
        return new DocumentoCsv(
                nombre,
                csv.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void agregarFila(
            StringBuilder csv,
            List<String> valores
    ) {
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) csv.append(',');
            csv.append(escaparCelda(valores.get(i)));
        }
        csv.append("\r\n");
    }

    private static String escaparCelda(String valor) {
        String seguro = valor(valor);
        if (!seguro.isEmpty()
                && "=+-@\t\r".indexOf(seguro.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        return '"' + seguro.replace("\"", "\"\"") + '"';
    }

    private static String valor(String valor) {
        return valor == null ? "" : valor;
    }

    public record DocumentoCsv(String nombreArchivo, byte[] contenido) {
        public DocumentoCsv {
            contenido = contenido.clone();
        }

        @Override
        public byte[] contenido() {
            return contenido.clone();
        }
    }
}
