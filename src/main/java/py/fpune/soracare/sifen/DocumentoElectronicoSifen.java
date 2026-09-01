package py.fpune.soracare.sifen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DocumentoElectronicoSifen(
        String timbrado,
        LocalDate fechaInicioTimbrado,
        String establecimiento,
        String puntoExpedicion,
        String numeroDocumento,
        String serie,
        String codigoSeguridad,
        LocalDateTime fechaEmision,
        EmisorSifen emisor,
        ReceptorSifen receptor,
        List<ItemDocumentoSifen> items,
        int tipoPago,
        String descripcionTipoPago,
        String informacionEmisor
) {
    public DocumentoElectronicoSifen {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
