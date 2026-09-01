package py.fpune.soracare.sifen;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public class FirmaXmlSifenService {

    public byte[] firmar(
            byte[] xmlSinFirma,
            PrivateKey clavePrivada,
            X509Certificate certificado
    ) {
        if (clavePrivada == null || certificado == null) {
            throw new IllegalArgumentException(
                    "La clave privada y el certificado son obligatorios."
            );
        }
        try {
            certificado.checkValidity();
            Document documento = XmlSifenUtil.leer(xmlSinFirma);
            Element raiz = documento.getDocumentElement();
            Element de = primerElemento(documento, "DE");
            String cdc = de.getAttribute("Id");
            if (!cdc.matches("[0-9]{44}")) {
                throw new IllegalArgumentException("El CDC del XML no es válido.");
            }
            de.setIdAttribute("Id", true);

            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            DigestMethod digest = fabrica.newDigestMethod(DigestMethod.SHA256, null);
            List<Transform> transformaciones = List.of(
                    fabrica.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                    fabrica.newTransform(CanonicalizationMethod.EXCLUSIVE,
                            (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)
            );
            Reference referencia = fabrica.newReference(
                    "#" + cdc,
                    digest,
                    transformaciones,
                    null,
                    null
            );
            SignedInfo informacion = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(
                            CanonicalizationMethod.EXCLUSIVE,
                            (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null
                    ),
                    fabrica.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    List.of(referencia)
            );
            KeyInfoFactory fabricaClave = fabrica.getKeyInfoFactory();
            X509Data datosCertificado = fabricaClave.newX509Data(List.of(
                    certificado,
                    fabricaClave.newX509IssuerSerial(
                            certificado.getIssuerX500Principal().getName(),
                            certificado.getSerialNumber()
                    )
            ));
            KeyInfo clave = fabricaClave.newKeyInfo(List.of(datosCertificado));

            DOMSignContext contexto = new DOMSignContext(clavePrivada, raiz);
            contexto.setDefaultNamespacePrefix("");
            XMLSignature firma = fabrica.newXMLSignature(informacion, clave);
            firma.sign(contexto);
            return XmlSifenUtil.escribir(documento);
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible firmar el XML SIFEN.", error);
        }
    }

    private static Element primerElemento(Document documento, String nombre) {
        var elementos = documento.getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, nombre);
        if (elementos.getLength() != 1 || !(elementos.item(0) instanceof Element elemento)) {
            throw new IllegalArgumentException(
                    "El XML debe contener exactamente un elemento " + nombre + "."
            );
        }
        return elemento;
    }
}
