package py.fpune.soracare.sifen;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CalculoFiscalSifenService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(8);
    private static final int ESCALA = 8;

    public ResultadoFiscalSifen calcular(List<ItemDocumentoSifen> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "El Documento Electrónico debe contener al menos un ítem."
            );
        }

        List<ResultadoFiscalSifen.ItemCalculado> calculados = new ArrayList<>();
        BigDecimal subtotalExento = CERO;
        BigDecimal subtotalExonerado = CERO;
        BigDecimal subtotalIva5 = CERO;
        BigDecimal subtotalIva10 = CERO;
        BigDecimal iva5 = CERO;
        BigDecimal iva10 = CERO;
        BigDecimal base5 = CERO;
        BigDecimal base10 = CERO;

        for (ItemDocumentoSifen item : items) {
            ResultadoFiscalSifen.ItemCalculado calculado = calcularItem(item);
            calculados.add(calculado);
            switch (item.afectacionIva()) {
                case EXENTO -> subtotalExento = subtotalExento
                        .add(calculado.totalOperacion());
                case EXONERADO -> subtotalExonerado = subtotalExonerado
                        .add(calculado.totalOperacion());
                case GRAVADO, GRAVADO_PARCIAL -> {
                    if (item.tasaIva() == 5) {
                        subtotalIva5 = subtotalIva5.add(calculado.totalOperacion());
                        iva5 = iva5.add(calculado.ivaLiquidado());
                        base5 = base5.add(calculado.baseGravada());
                    } else {
                        subtotalIva10 = subtotalIva10.add(calculado.totalOperacion());
                        iva10 = iva10.add(calculado.ivaLiquidado());
                        base10 = base10.add(calculado.baseGravada());
                    }
                }
            }
        }

        BigDecimal total = subtotalExento.add(subtotalExonerado)
                .add(subtotalIva5).add(subtotalIva10);
        return new ResultadoFiscalSifen(
                calculados,
                escala(subtotalExento),
                escala(subtotalExonerado),
                escala(subtotalIva5),
                escala(subtotalIva10),
                escala(total),
                escala(iva5),
                escala(iva10),
                escala(iva5.add(iva10)),
                escala(base5),
                escala(base10),
                escala(base5.add(base10))
        );
    }

    private ResultadoFiscalSifen.ItemCalculado calcularItem(
            ItemDocumentoSifen item
    ) {
        validarItem(item);
        BigDecimal bruto = escala(item.precioUnitario().multiply(item.cantidad()));
        BigDecimal proporcion = item.proporcionGravada()
                .divide(CIEN, ESCALA, RoundingMode.HALF_UP);
        BigDecimal parteGravada = escala(bruto.multiply(proporcion));
        BigDecimal baseGravada = CERO;
        BigDecimal iva = CERO;

        if (item.afectacionIva() == AfectacionIvaSifen.GRAVADO
                || item.afectacionIva() == AfectacionIvaSifen.GRAVADO_PARCIAL) {
            BigDecimal divisor = BigDecimal.ONE.add(
                    BigDecimal.valueOf(item.tasaIva())
                            .divide(CIEN, ESCALA, RoundingMode.HALF_UP)
            );
            baseGravada = parteGravada.divide(divisor, ESCALA, RoundingMode.HALF_UP);
            iva = baseGravada.multiply(BigDecimal.valueOf(item.tasaIva()))
                    .divide(CIEN, ESCALA, RoundingMode.HALF_UP);
        }

        BigDecimal baseExenta = escala(bruto.subtract(parteGravada));
        return new ResultadoFiscalSifen.ItemCalculado(
                item,
                bruto,
                bruto,
                escala(baseGravada),
                escala(iva),
                baseExenta
        );
    }

    private static void validarItem(ItemDocumentoSifen item) {
        if (item == null) {
            throw new IllegalArgumentException("El ítem del DE no puede ser nulo.");
        }
        texto(item.codigo(), 1, 50, "código interno del ítem");
        texto(item.descripcion(), 1, 2_000, "descripción del ítem");
        if (item.cantidad() == null || item.cantidad().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad del ítem debe ser positiva.");
        }
        if (item.precioUnitario() == null || item.precioUnitario().signum() <= 0) {
            throw new IllegalArgumentException("El precio unitario debe ser positivo.");
        }
        if (item.afectacionIva() == null || item.proporcionGravada() == null) {
            throw new IllegalArgumentException("La afectación de IVA es obligatoria.");
        }

        BigDecimal proporcion = item.proporcionGravada();
        if (proporcion.signum() < 0 || proporcion.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException(
                    "La proporción gravada debe estar entre 0 y 100."
            );
        }
        switch (item.afectacionIva()) {
            case GRAVADO -> {
                if (proporcion.compareTo(CIEN) != 0) {
                    throw new IllegalArgumentException(
                            "Un ítem gravado debe tener proporción 100."
                    );
                }
                validarTasa(item.tasaIva());
            }
            case GRAVADO_PARCIAL -> {
                if (proporcion.signum() <= 0 || proporcion.compareTo(CIEN) >= 0) {
                    throw new IllegalArgumentException(
                            "Un ítem parcialmente gravado requiere proporción entre 0 y 100."
                    );
                }
                validarTasa(item.tasaIva());
            }
            case EXENTO, EXONERADO -> {
                if (proporcion.signum() != 0 || item.tasaIva() != 0) {
                    throw new IllegalArgumentException(
                            "Los ítems exentos o exonerados deben usar proporción y tasa cero."
                    );
                }
            }
        }
    }

    private static void validarTasa(int tasa) {
        if (tasa != 5 && tasa != 10) {
            throw new IllegalArgumentException("La tasa de IVA debe ser 5 o 10.");
        }
    }

    private static void texto(
            String valor,
            int minimo,
            int maximo,
            String nombre
    ) {
        int longitud = valor == null ? 0 : valor.strip().length();
        if (longitud < minimo || longitud > maximo) {
            throw new IllegalArgumentException("La " + nombre + " no es válida.");
        }
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
