package py.fpune.soracare.sifen;

import py.fpune.soracare.dto.EstadoPreparacionSifenDTO;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Configuracion fiscal lista para usar. Los secretos se copian al entrar y se
 * borran al cerrar para reducir el tiempo que permanecen en memoria.
 */
public final class ConfiguracionFiscalSifen implements AutoCloseable {

    private final EstadoPreparacionSifenDTO.Ambiente ambiente;
    private final String timbrado;
    private final LocalDate fechaInicioTimbrado;
    private final String establecimiento;
    private final String puntoExpedicion;
    private final EmisorSifen emisor;
    private final Path certificado;
    private final char[] passwordCertificado;
    private final String identificadorCsc;
    private final char[] csc;
    private final String informacionEmisor;

    public ConfiguracionFiscalSifen(
            EstadoPreparacionSifenDTO.Ambiente ambiente,
            String timbrado,
            LocalDate fechaInicioTimbrado,
            String establecimiento,
            String puntoExpedicion,
            EmisorSifen emisor,
            Path certificado,
            char[] passwordCertificado,
            String identificadorCsc,
            char[] csc,
            String informacionEmisor
    ) {
        this.ambiente = ambiente;
        this.timbrado = timbrado;
        this.fechaInicioTimbrado = fechaInicioTimbrado;
        this.establecimiento = establecimiento;
        this.puntoExpedicion = puntoExpedicion;
        this.emisor = emisor;
        this.certificado = certificado;
        this.passwordCertificado = passwordCertificado.clone();
        this.identificadorCsc = identificadorCsc;
        this.csc = csc.clone();
        this.informacionEmisor = informacionEmisor;
    }

    public EstadoPreparacionSifenDTO.Ambiente ambiente() { return ambiente; }
    public String timbrado() { return timbrado; }
    public LocalDate fechaInicioTimbrado() { return fechaInicioTimbrado; }
    public String establecimiento() { return establecimiento; }
    public String puntoExpedicion() { return puntoExpedicion; }
    public EmisorSifen emisor() { return emisor; }
    public Path certificado() { return certificado; }
    public char[] passwordCertificado() { return passwordCertificado.clone(); }
    public String identificadorCsc() { return identificadorCsc; }
    public char[] csc() { return csc.clone(); }
    public String informacionEmisor() { return informacionEmisor; }
    public boolean produccion() {
        return ambiente == EstadoPreparacionSifenDTO.Ambiente.PRODUCCION;
    }

    @Override
    public void close() {
        Arrays.fill(passwordCertificado, '\0');
        Arrays.fill(csc, '\0');
    }

    @Override
    public String toString() {
        return "ConfiguracionFiscalSifen[ambiente=" + ambiente
                + ", timbrado=" + timbrado
                + ", establecimiento=" + establecimiento
                + ", puntoExpedicion=" + puntoExpedicion
                + ", certificado=protegido, secretos=protegidos]";
    }
}
