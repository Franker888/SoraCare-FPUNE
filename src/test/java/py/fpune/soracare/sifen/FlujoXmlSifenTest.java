package py.fpune.soracare.sifen;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlujoXmlSifenTest {

    @Test
    void generaFirmaQrYValidaContraXsdOficial() throws Exception {
        KeyPair claves = claves();
        X509Certificate certificado = certificado(claves);
        DocumentoElectronicoSifen datos = documento();

        var sinFirma = new XmlDocumentoSifenService().generar(datos);
        byte[] firmado = new FirmaXmlSifenService().firmar(
                sinFirma.contenido(), claves.getPrivate(), certificado
        );
        var conQr = new QrSifenService().agregar(
                firmado, false, "0001", "A".repeat(32).toCharArray()
        );

        new ValidadorXsdSifenService().validar(conQr.contenido());
        assertEquals(44, sinFirma.cdc().length());
        assertTrue(conQr.urlQr().startsWith(
                "https://ekuatia.set.gov.py/consultas-test/qr?"
        ));
        assertTrue(firmaValida(conQr.contenido(), certificado));
    }

    private static DocumentoElectronicoSifen documento() {
        EmisorSifen emisor = new EmisorSifen(
                "80069563", 1, 2, 8,
                "DE generado en ambiente de prueba - sin valor comercial ni fiscal",
                "SoraCare-FPUNE",
                "Barrio Carolina", "0",
                11, "ALTO PARANA",
                145, "CIUDAD DEL ESTE",
                3432, "PUERTO PTE.STROESSNER (MUNIC)",
                "0973527155", "pruebas@soracare.test",
                "86201", "ACTIVIDADES DE MEDICOS Y ODONTOLOGOS"
        );
        ReceptorSifen receptor = new ReceptorSifen(
                false, null, null, null,
                1, "Cédula paraguaya", "1234567",
                "PACIENTE DE PRUEBA", "paciente@soracare.test", "PAC001"
        );
        ItemDocumentoSifen item = new ItemDocumentoSifen(
                "CONS-001", "Consulta médica general", BigDecimal.ONE,
                new BigDecimal("150000"), AfectacionIvaSifen.GRAVADO,
                new BigDecimal("100"), 10
        );
        return new DocumentoElectronicoSifen(
                "12558946", LocalDate.of(2026, 1, 1),
                "001", "001", "0000001", null,
                "298398001", LocalDateTime.of(2026, 9, 1, 10, 11),
                emisor, receptor, List.of(item),
                5, "Transferencia", "Consulta médica SoraCare"
        );
    }

    private static KeyPair claves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    private static X509Certificate certificado(KeyPair claves) throws Exception {
        Instant ahora = Instant.now();
        X500Name nombre = new X500Name("CN=SoraCare Test,O=FPUNE,C=PY");
        var constructor = new JcaX509v3CertificateBuilder(
                nombre,
                BigInteger.valueOf(1),
                Date.from(ahora.minusSeconds(60)),
                Date.from(ahora.plusSeconds(86_400)),
                nombre,
                claves.getPublic()
        );
        var firmante = new JcaContentSignerBuilder("SHA256withRSA")
                .build(claves.getPrivate());
        return new JcaX509CertificateConverter()
                .getCertificate(constructor.build(firmante));
    }

    private static boolean firmaValida(
            byte[] xml,
            X509Certificate certificado
    ) throws Exception {
        var documento = XmlSifenUtil.leer(xml);
        var de = (org.w3c.dom.Element) documento
                .getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, "DE").item(0);
        de.setIdAttribute("Id", true);
        var firma = documento.getElementsByTagNameNS(
                "http://www.w3.org/2000/09/xmldsig#", "Signature"
        ).item(0);
        DOMValidateContext contexto = new DOMValidateContext(
                certificado.getPublicKey(), firma
        );
        contexto.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
        return XMLSignatureFactory.getInstance("DOM")
                .unmarshalXMLSignature(contexto)
                .validate(contexto);
    }
}
