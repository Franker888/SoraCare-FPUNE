package py.fpune.soracare.sifen;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

public class CertificadoSifenService {

    public CredencialFirma cargar(Path archivoPkcs12, char[] password) {
        if (archivoPkcs12 == null || !Files.isRegularFile(archivoPkcs12)
                || !Files.isReadable(archivoPkcs12)) {
            throw new IllegalArgumentException(
                    "El archivo PKCS#12 del certificado no es accesible."
            );
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException(
                    "La contraseña del certificado es obligatoria."
            );
        }
        char[] copia = password.clone();
        try (InputStream entrada = Files.newInputStream(archivoPkcs12)) {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(entrada, copia);
            Enumeration<String> alias = almacen.aliases();
            while (alias.hasMoreElements()) {
                String actual = alias.nextElement();
                if (!almacen.isKeyEntry(actual)) continue;
                var clave = almacen.getKey(actual, copia);
                var certificado = almacen.getCertificate(actual);
                if (clave instanceof PrivateKey privada
                        && certificado instanceof X509Certificate x509) {
                    x509.checkValidity();
                    return new CredencialFirma(privada, x509);
                }
            }
            throw new IllegalArgumentException(
                    "El PKCS#12 no contiene una clave privada con certificado X.509."
            );
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException(
                    "No fue posible abrir el certificado SIFEN.", error
            );
        } finally {
            Arrays.fill(copia, '\0');
        }
    }

    public record CredencialFirma(
            PrivateKey clavePrivada,
            X509Certificate certificado
    ) {
    }
}
