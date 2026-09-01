package py.fpune.soracare.dto;

import java.util.List;

public record EstadoPreparacionSifenDTO(
        Ambiente ambiente,
        boolean ambienteValido,
        List<String> requisitosPendientes
) {

    public enum Ambiente {
        DESHABILITADO,
        PRUEBA,
        PRODUCCION
    }

    public EstadoPreparacionSifenDTO {
        ambiente = ambiente == null ? Ambiente.DESHABILITADO : ambiente;
        requisitosPendientes = requisitosPendientes == null
                ? List.of()
                : List.copyOf(requisitosPendientes);
    }

    public boolean habilitada() {
        return ambiente != Ambiente.DESHABILITADO;
    }

    public boolean configuracionCompleta() {
        return habilitada() && ambienteValido && requisitosPendientes.isEmpty();
    }

    public boolean esProduccion() {
        return ambiente == Ambiente.PRODUCCION;
    }
}
