package py.fpune.soracare.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public class AlmacenFotosPerfil {

    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024L * 1024L;
    private static final long PIXELES_MAXIMOS = 16_000_000L;
    private static final String VARIABLE_DIRECTORIO = "SORACARE_PERFILES_DIR";

    private final Path raiz;

    public AlmacenFotosPerfil() {
        String configurada = System.getenv(VARIABLE_DIRECTORIO);
        Path base = configurada == null || configurada.isBlank()
                ? Path.of(System.getProperty("user.home"), ".soracare", "fotos-perfil")
                : Path.of(configurada.strip());
        raiz = base.toAbsolutePath().normalize();
    }

    public FotoGuardada guardar(long idPersona, byte[] contenido) throws IOException {
        FotoPreparada preparada = preparar(idPersona, contenido);
        String extension = "image/png".equals(preparada.tipoMime()) ? "png" : "jpg";
        String rutaRelativa = "persona-" + idPersona + "/" + UUID.randomUUID() + "." + extension;
        Path destino = resolver(rutaRelativa);
        Files.createDirectories(destino.getParent());
        Files.write(destino, contenido, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new FotoGuardada(
                rutaRelativa,
                preparada.tipoMime(),
                preparada.tamanoBytes(),
                preparada.hashSha256()
        );
    }

    public FotoPreparada preparar(long idPersona, byte[] contenido) throws IOException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta del perfil no es válida.");
        }
        validarTamano(contenido);
        String tipoMime = detectarTipoMime(contenido);
        validarImagenReal(contenido);
        return new FotoPreparada(
                tipoMime,
                contenido.length,
                sha256(contenido)
        );
    }

    public byte[] leer(String rutaRelativa, String hashEsperado) throws IOException {
        Path archivo = resolver(rutaRelativa);
        if (!Files.isRegularFile(archivo)) {
            throw new IOException("La fotografía de perfil no está disponible.");
        }
        long tamano = Files.size(archivo);
        if (tamano <= 0 || tamano > TAMANO_MAXIMO_BYTES) {
            throw new IOException("La fotografía posee un tamaño inválido.");
        }
        byte[] contenido = Files.readAllBytes(archivo);
        return verificarContenido(contenido, hashEsperado);
    }

    public byte[] verificarContenido(byte[] contenido, String hashEsperado)
            throws IOException {
        if (contenido == null || contenido.length == 0
                || contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new IOException("La fotografía posee un tamaño inválido.");
        }
        if (hashEsperado == null
                || hashEsperado.isBlank()
                || !sha256(contenido).equalsIgnoreCase(hashEsperado.strip())) {
            throw new IOException("La fotografía no superó la verificación de integridad.");
        }
        return contenido;
    }

    public void eliminarSilenciosamente(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) return;
        try {
            Files.deleteIfExists(resolver(rutaRelativa));
        } catch (IOException ignored) {
            // La actualización principal conserva la prioridad.
        }
    }

    private Path resolver(String rutaRelativa) throws IOException {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new IOException("La ubicación de la fotografía no está definida.");
        }
        Path resuelta = raiz.resolve(rutaRelativa.replace('\\', '/')).normalize();
        if (!resuelta.startsWith(raiz)) {
            throw new IOException("La ubicación de la fotografía no es segura.");
        }
        return resuelta;
    }

    private static void validarTamano(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("Seleccioná una imagen para cargar.");
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar los 5 MB.");
        }
    }

    private static String detectarTipoMime(byte[] contenido) {
        if (contenido.length >= 8
                && (contenido[0] & 0xff) == 0x89
                && contenido[1] == 'P' && contenido[2] == 'N' && contenido[3] == 'G'
                && contenido[4] == 0x0d && contenido[5] == 0x0a
                && contenido[6] == 0x1a && contenido[7] == 0x0a) {
            return "image/png";
        }
        if (contenido.length >= 3
                && (contenido[0] & 0xff) == 0xff
                && (contenido[1] & 0xff) == 0xd8
                && (contenido[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        throw new IllegalArgumentException("La foto debe ser una imagen JPG, JPEG o PNG válida.");
    }

    private static void validarImagenReal(byte[] contenido) throws IOException {
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
        if (imagen == null || imagen.getWidth() <= 0 || imagen.getHeight() <= 0) {
            throw new IllegalArgumentException("El archivo seleccionado no contiene una imagen válida.");
        }
        long pixeles = (long) imagen.getWidth() * imagen.getHeight();
        if (pixeles > PIXELES_MAXIMOS) {
            throw new IllegalArgumentException("La resolución de la imagen es demasiado grande.");
        }
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(contenido)
            ).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible.", error);
        }
    }

    public record FotoGuardada(
            String rutaRelativa,
            String tipoMime,
            long tamanoBytes,
            String hashSha256
    ) {
    }

    public record FotoPreparada(
            String tipoMime,
            long tamanoBytes,
            String hashSha256
    ) {
    }
}
