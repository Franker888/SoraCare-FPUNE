package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.DetalleRecetaDTO;

import java.math.BigDecimal;

public record DetalleIndicacionMedicaApiDTO(
        String medicamento,
        String principioActivo,
        String presentacion,
        String concentracion,
        String viaAdministracion,
        String dosis,
        String frecuencia,
        Integer duracionDias,
        BigDecimal cantidad,
        String unidad,
        String instrucciones
) {

    public static DetalleIndicacionMedicaApiDTO desde(
            DetalleRecetaDTO detalle
    ) {
        return new DetalleIndicacionMedicaApiDTO(
                detalle.nombreComercial(),
                detalle.principioActivo(),
                detalle.presentacion(),
                detalle.concentracion(),
                detalle.viaAdministracion() == null
                        ? null
                        : detalle.viaAdministracion().getNombreVisible(),
                detalle.dosis(),
                detalle.frecuencia(),
                detalle.duracionDias(),
                detalle.cantidad(),
                detalle.unidad(),
                detalle.instrucciones()
        );
    }
}
