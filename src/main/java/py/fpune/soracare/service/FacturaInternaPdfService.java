package py.fpune.soracare.service;

import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.PagoResumenDTO;

import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FacturaInternaPdfService {

    private static final ZoneId ZONA = ZoneId.of("America/Asuncion");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ComprobantePdfService.DocumentoPdf generar(
            PagoResumenDTO pago,
            DatosFacturacionDTO facturacion,
            String numeroFactura
    ) {
        if (pago == null || facturacion == null
                || numeroFactura == null || numeroFactura.isBlank()) {
            throw new IllegalArgumentException(
                    "Los datos de la factura interna son obligatorios."
            );
        }

        String monto = pago.monto()
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
        List<String> lineas = List.of(
                "SORACARE-FPUNE",
                "FACTURA INTERNA",
                "DOCUMENTO SIN VALIDEZ TRIBUTARIA",
                "",
                "Numero interno: " + numeroFactura,
                "Receptor: " + facturacion.nombreRazonSocial(),
                "Documento: " + facturacion.tipoDocumento() + " "
                        + facturacion.numeroDocumento(),
                "Direccion: " + facturacion.direccion(),
                "Correo: " + facturacion.correo(),
                "",
                "Concepto: Consulta medica - " + pago.nombreEspecialidad(),
                "Fecha de la cita: " + pago.fechaCita()
                        .atZoneSameInstant(ZONA).format(FECHA_HORA),
                "Profesional: " + pago.nombreMedico(),
                "Cantidad: 1",
                "Precio unitario: Gs. " + monto,
                "Total: Gs. " + monto,
                "",
                "Esta factura es un registro comercial interno de SoraCare.",
                "No contiene timbrado, CDC ni aprobacion de e-Kuatia/SIFEN."
        );

        return ComprobantePdfService.crearDocumento(
                "factura-interna-" + numeroFactura + ".pdf",
                lineas
        );
    }
}
