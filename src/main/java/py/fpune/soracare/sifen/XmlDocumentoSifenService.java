package py.fpune.soracare.sifen;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class XmlDocumentoSifenService {

    private static final String NAMESPACE_XSI =
            "http://www.w3.org/2001/XMLSchema-instance";
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CdcSifenService cdcService;
    private final CalculoFiscalSifenService calculoService;

    public XmlDocumentoSifenService() {
        this(new CdcSifenService(), new CalculoFiscalSifenService());
    }

    XmlDocumentoSifenService(
            CdcSifenService cdcService,
            CalculoFiscalSifenService calculoService
    ) {
        this.cdcService = cdcService;
        this.calculoService = calculoService;
    }

    public DocumentoXmlSinFirma generar(DocumentoElectronicoSifen datos) {
        validar(datos);
        ResultadoFiscalSifen fiscal = calculoService.calcular(datos.items());
        String cdc = cdcService.generar(datos);
        Document documento = XmlSifenUtil.nuevoDocumento();

        Element raiz = documento.createElementNS(XmlSifenUtil.NAMESPACE, "rDE");
        raiz.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi", NAMESPACE_XSI);
        raiz.setAttributeNS(
                NAMESPACE_XSI,
                "xsi:schemaLocation",
                XmlSifenUtil.NAMESPACE + " siRecepDE_v150.xsd"
        );
        documento.appendChild(raiz);
        agregar(documento, raiz, "dVerFor", "150");

        Element de = elemento(documento, raiz, "DE");
        de.setAttribute("Id", cdc);
        agregar(documento, de, "dDVId", cdc.substring(cdc.length() - 1));
        agregar(documento, de, "dFecFirma", datos.fechaEmision().format(FECHA_HORA));
        agregar(documento, de, "dSisFact", "1");
        agregarOperacion(documento, de, datos);
        agregarTimbrado(documento, de, datos);
        agregarDatosGenerales(documento, de, datos);
        agregarDetalle(documento, de, datos, fiscal);
        agregarTotales(documento, de, fiscal);

        return new DocumentoXmlSinFirma(cdc, XmlSifenUtil.escribir(documento), fiscal);
    }

    private static void agregarOperacion(
            Document documento,
            Element de,
            DocumentoElectronicoSifen datos
    ) {
        Element grupo = elemento(documento, de, "gOpeDE");
        agregar(documento, grupo, "iTipEmi", "1");
        agregar(documento, grupo, "dDesTipEmi", "Normal");
        agregar(documento, grupo, "dCodSeg", datos.codigoSeguridad());
        agregarOpcional(documento, grupo, "dInfoEmi", datos.informacionEmisor());
    }

    private static void agregarTimbrado(
            Document documento,
            Element de,
            DocumentoElectronicoSifen datos
    ) {
        Element grupo = elemento(documento, de, "gTimb");
        agregar(documento, grupo, "iTiDE", "1");
        agregar(documento, grupo, "dDesTiDE", "Factura electrónica");
        agregar(documento, grupo, "dNumTim", datos.timbrado());
        agregar(documento, grupo, "dEst", completar(datos.establecimiento(), 3));
        agregar(documento, grupo, "dPunExp", completar(datos.puntoExpedicion(), 3));
        agregar(documento, grupo, "dNumDoc", completar(datos.numeroDocumento(), 7));
        agregarOpcional(documento, grupo, "dSerieNum", datos.serie());
        agregar(documento, grupo, "dFeIniT", datos.fechaInicioTimbrado().toString());
    }

    private static void agregarDatosGenerales(
            Document documento,
            Element de,
            DocumentoElectronicoSifen datos
    ) {
        Element generales = elemento(documento, de, "gDatGralOpe");
        agregar(documento, generales, "dFeEmiDE", datos.fechaEmision().format(FECHA_HORA));

        Element operacion = elemento(documento, generales, "gOpeCom");
        agregar(documento, operacion, "iTipTra", "2");
        agregar(documento, operacion, "dDesTipTra", "Prestación de servicios");
        agregar(documento, operacion, "iTImp", "1");
        agregar(documento, operacion, "dDesTImp", "IVA");
        agregar(documento, operacion, "cMoneOpe", "PYG");
        agregar(documento, operacion, "dDesMoneOpe", "Guarani");

        agregarEmisor(documento, generales, datos.emisor());
        agregarReceptor(documento, generales, datos.receptor());
    }

    private static void agregarEmisor(
            Document documento,
            Element generales,
            EmisorSifen emisor
    ) {
        Element grupo = elemento(documento, generales, "gEmis");
        agregar(documento, grupo, "dRucEm", emisor.ruc());
        agregar(documento, grupo, "dDVEmi", Integer.toString(emisor.digitoVerificador()));
        agregar(documento, grupo, "iTipCont", Integer.toString(emisor.tipoContribuyente()));
        if (emisor.tipoRegimen() != null) {
            agregar(documento, grupo, "cTipReg", emisor.tipoRegimen().toString());
        }
        agregar(documento, grupo, "dNomEmi", emisor.razonSocial());
        agregarOpcional(documento, grupo, "dNomFanEmi", emisor.nombreFantasia());
        agregar(documento, grupo, "dDirEmi", emisor.direccion());
        agregar(documento, grupo, "dNumCas", emisor.numeroCasa());
        agregar(documento, grupo, "cDepEmi", Integer.toString(emisor.departamento()));
        agregar(documento, grupo, "dDesDepEmi", emisor.descripcionDepartamento());
        if (emisor.distrito() != null) {
            agregar(documento, grupo, "cDisEmi", emisor.distrito().toString());
            agregar(documento, grupo, "dDesDisEmi", emisor.descripcionDistrito());
        }
        agregar(documento, grupo, "cCiuEmi", Integer.toString(emisor.ciudad()));
        agregar(documento, grupo, "dDesCiuEmi", emisor.descripcionCiudad());
        agregar(documento, grupo, "dTelEmi", emisor.telefono());
        agregar(documento, grupo, "dEmailE", emisor.correo());
        Element actividad = elemento(documento, grupo, "gActEco");
        agregar(documento, actividad, "cActEco", emisor.codigoActividad());
        agregar(documento, actividad, "dDesActEco", emisor.descripcionActividad());
    }

    private static void agregarReceptor(
            Document documento,
            Element generales,
            ReceptorSifen receptor
    ) {
        Element grupo = elemento(documento, generales, "gDatRec");
        agregar(documento, grupo, "iNatRec", receptor.contribuyente() ? "1" : "2");
        agregar(documento, grupo, "iTiOpe", receptor.contribuyente() ? "1" : "2");
        agregar(documento, grupo, "cPaisRec", "PRY");
        agregar(documento, grupo, "dDesPaisRe", "Paraguay");
        if (receptor.contribuyente()) {
            agregar(documento, grupo, "iTiContRec", receptor.tipoContribuyente().toString());
            agregar(documento, grupo, "dRucRec", receptor.ruc());
            agregar(documento, grupo, "dDVRec", receptor.digitoVerificador().toString());
        } else {
            agregar(documento, grupo, "iTipIDRec",
                    Integer.toString(receptor.tipoDocumentoIdentidad()));
            agregar(documento, grupo, "dDTipIDRec",
                    receptor.descripcionDocumentoIdentidad());
            agregar(documento, grupo, "dNumIDRec",
                    receptor.numeroDocumentoIdentidad());
        }
        agregar(documento, grupo, "dNomRec", receptor.nombreRazonSocial());
        agregarOpcional(documento, grupo, "dEmailRec", receptor.correo());
        agregarOpcional(documento, grupo, "dCodCliente", receptor.codigoCliente());
    }

    private static void agregarDetalle(
            Document documento,
            Element de,
            DocumentoElectronicoSifen datos,
            ResultadoFiscalSifen fiscal
    ) {
        Element tipo = elemento(documento, de, "gDtipDE");
        Element factura = elemento(documento, tipo, "gCamFE");
        agregar(documento, factura, "iIndPres", "1");
        agregar(documento, factura, "dDesIndPres", "Operación presencial");

        Element condicion = elemento(documento, tipo, "gCamCond");
        agregar(documento, condicion, "iCondOpe", "1");
        agregar(documento, condicion, "dDCondOpe", "Contado");
        Element pago = elemento(documento, condicion, "gPaConEIni");
        agregar(documento, pago, "iTiPago", Integer.toString(datos.tipoPago()));
        agregar(documento, pago, "dDesTiPag", datos.descripcionTipoPago());
        agregar(documento, pago, "dMonTiPag", numero(fiscal.totalOperacion()));
        agregar(documento, pago, "cMoneTiPag", "PYG");
        agregar(documento, pago, "dDMoneTiPag", "Guarani");

        for (ResultadoFiscalSifen.ItemCalculado calculado : fiscal.items()) {
            ItemDocumentoSifen item = calculado.item();
            Element grupo = elemento(documento, tipo, "gCamItem");
            agregar(documento, grupo, "dCodInt", item.codigo());
            agregar(documento, grupo, "dDesProSer", item.descripcion());
            agregar(documento, grupo, "cUniMed", "77");
            agregar(documento, grupo, "dDesUniMed", "UNI");
            agregar(documento, grupo, "dCantProSer", numero(item.cantidad()));

            Element valor = elemento(documento, grupo, "gValorItem");
            agregar(documento, valor, "dPUniProSer", numero(item.precioUnitario()));
            agregar(documento, valor, "dTotBruOpeItem", numero(calculado.totalBruto()));
            Element resta = elemento(documento, valor, "gValorRestaItem");
            agregar(documento, resta, "dDescItem", "0");
            agregar(documento, resta, "dPorcDesIt", "0");
            agregar(documento, resta, "dDescGloItem", "0");
            agregar(documento, resta, "dTotOpeItem", numero(calculado.totalOperacion()));

            Element iva = elemento(documento, grupo, "gCamIVA");
            agregar(documento, iva, "iAfecIVA", Integer.toString(item.afectacionIva().codigo()));
            agregar(documento, iva, "dDesAfecIVA", item.afectacionIva().descripcion());
            agregar(documento, iva, "dPropIVA", numero(item.proporcionGravada()));
            agregar(documento, iva, "dTasaIVA", Integer.toString(item.tasaIva()));
            agregar(documento, iva, "dBasGravIVA", numero(calculado.baseGravada()));
            agregar(documento, iva, "dLiqIVAItem", numero(calculado.ivaLiquidado()));
            agregar(documento, iva, "dBasExe", numero(calculado.baseExenta()));
        }
    }

    private static void agregarTotales(
            Document documento,
            Element de,
            ResultadoFiscalSifen fiscal
    ) {
        Element grupo = elemento(documento, de, "gTotSub");
        agregarSiPositivo(documento, grupo, "dSubExe", fiscal.subtotalExento());
        agregarSiPositivo(documento, grupo, "dSubExo", fiscal.subtotalExonerado());
        agregarSiPositivo(documento, grupo, "dSub5", fiscal.subtotalIva5());
        agregarSiPositivo(documento, grupo, "dSub10", fiscal.subtotalIva10());
        agregar(documento, grupo, "dTotOpe", numero(fiscal.totalOperacion()));
        agregar(documento, grupo, "dTotDesc", "0");
        agregar(documento, grupo, "dTotDescGlotem", "0");
        agregar(documento, grupo, "dTotAntItem", "0");
        agregar(documento, grupo, "dTotAnt", "0");
        agregar(documento, grupo, "dPorcDescTotal", "0");
        agregar(documento, grupo, "dDescTotal", "0");
        agregar(documento, grupo, "dAnticipo", "0");
        agregar(documento, grupo, "dRedon", "0");
        agregar(documento, grupo, "dTotGralOpe", numero(fiscal.totalOperacion()));
        agregarSiPositivo(documento, grupo, "dIVA5", fiscal.iva5());
        agregarSiPositivo(documento, grupo, "dIVA10", fiscal.iva10());
        agregar(documento, grupo, "dTotIVA", numero(fiscal.totalIva()));
        agregarSiPositivo(documento, grupo, "dBaseGrav5", fiscal.baseGravada5());
        agregarSiPositivo(documento, grupo, "dBaseGrav10", fiscal.baseGravada10());
        agregar(documento, grupo, "dTBasGraIVA", numero(fiscal.totalBaseGravada()));
    }

    private static void validar(DocumentoElectronicoSifen datos) {
        if (datos == null || datos.emisor() == null || datos.receptor() == null
                || datos.fechaEmision() == null || datos.fechaInicioTimbrado() == null) {
            throw new IllegalArgumentException("Los datos principales del DE son obligatorios.");
        }
        if (datos.fechaEmision().toLocalDate().isBefore(datos.fechaInicioTimbrado())) {
            throw new IllegalArgumentException(
                    "La emisión no puede ser anterior al inicio del timbrado."
            );
        }
        if (!datos.timbrado().matches("[0-9]{8}")) {
            throw new IllegalArgumentException("El timbrado debe contener 8 dígitos.");
        }
        if (!datos.codigoSeguridad().matches("[0-9]{9}")
                || "000000000".equals(datos.codigoSeguridad())) {
            throw new IllegalArgumentException(
                    "El código de seguridad debe contener 9 dígitos y no puede ser cero."
            );
        }
        EmisorSifen emisor = datos.emisor();
        if (!emisor.ruc().matches("[0-9]{3,8}")) {
            throw new IllegalArgumentException("El RUC del emisor no es válido.");
        }
        if (emisor.tipoContribuyente() != 1 && emisor.tipoContribuyente() != 2) {
            throw new IllegalArgumentException("El tipo de contribuyente no es válido.");
        }
        ReceptorSifen receptor = datos.receptor();
        if (receptor.contribuyente()) {
            if (receptor.tipoContribuyente() == null || receptor.ruc() == null
                    || receptor.digitoVerificador() == null) {
                throw new IllegalArgumentException(
                        "El receptor contribuyente requiere tipo, RUC y dígito verificador."
                );
            }
        } else if (receptor.numeroDocumentoIdentidad() == null
                || receptor.numeroDocumentoIdentidad().isBlank()) {
            throw new IllegalArgumentException(
                    "El receptor no contribuyente requiere documento de identidad."
            );
        }
    }

    private static Element elemento(Document documento, Element padre, String nombre) {
        Element elemento = documento.createElementNS(XmlSifenUtil.NAMESPACE, nombre);
        padre.appendChild(elemento);
        return elemento;
    }

    private static void agregar(
            Document documento,
            Element padre,
            String nombre,
            String valor
    ) {
        Element elemento = elemento(documento, padre, nombre);
        elemento.setTextContent(valor == null ? "" : valor.strip());
    }

    private static void agregarOpcional(
            Document documento,
            Element padre,
            String nombre,
            String valor
    ) {
        if (valor != null && !valor.isBlank()) {
            agregar(documento, padre, nombre, valor);
        }
    }

    private static void agregarSiPositivo(
            Document documento,
            Element padre,
            String nombre,
            BigDecimal valor
    ) {
        if (valor.signum() > 0) {
            agregar(documento, padre, nombre, numero(valor));
        }
    }

    private static String completar(String valor, int longitud) {
        String limpio = valor == null ? "" : valor.strip();
        if (!limpio.matches("[0-9]+") || limpio.length() > longitud) {
            throw new IllegalArgumentException("La numeración del DE no es válida.");
        }
        return "0".repeat(longitud - limpio.length()) + limpio;
    }

    private static String numero(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0
                ? limpio.setScale(0).toPlainString()
                : limpio.toPlainString();
    }

    public record DocumentoXmlSinFirma(
            String cdc,
            byte[] contenido,
            ResultadoFiscalSifen calculoFiscal
    ) {
        public DocumentoXmlSinFirma {
            contenido = contenido == null ? new byte[0] : contenido.clone();
        }

        @Override
        public byte[] contenido() {
            return contenido.clone();
        }
    }
}
