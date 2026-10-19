package py.fpune.soracare.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public class AlmacenDocumentosClinicos {

    public static final long TAMANO_MAXIMO_BYTES = 10L * 1024L * 1024L;
    private static final String VARIABLE_DIRECTORIO = "SORACARE_DOCUMENTOS_DIR";

    private final Path raiz;

    public AlmacenDocumentosClinicos() {
        String configurada = System.getenv(VARIABLE_DIRECTORIO);
        Path base = configurada == null || configurada.isBlank()
                ? Path.of(System.getProperty("user.home"), ".soracare", "documentos-clinicos")
                : Path.of(configurada.strip());
        raiz = base.toAbsolutePath().normalize();
    }

    public ArchivoGuardado guardar(
            long idPaciente,
            String nombreOriginal,
            byte[] contenido
    ) throws IOException {
        ArchivoPreparado preparado = preparar(idPaciente, nombreOriginal, contenido);
        String carpeta = "paciente-" + idPaciente;
        String rutaRelativa = carpeta + "/" + preparado.nombreAlmacenado();
        Path destino = resolverRuta(rutaRelativa);

        Files.createDirectories(destino.getParent());
        Files.write(destino, contenido, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new ArchivoGuardado(
                preparado.nombreAlmacenado(),
                preparado.nombreOriginal(),
                preparado.extension(),
                preparado.tipoMime(),
                preparado.tamanoBytes(),
                rutaRelativa,
                preparado.hashSha256()
        );
    }

    public ArchivoPreparado preparar(
            long idPaciente,
            String nombreOriginal,
            byte[] contenido
    ) {
        if (idPaciente <= 0) {
            throw new IllegalArgumentException("El paciente del documento no es válido.");
        }
        validarTamano(contenido);
        String tipoMime = detectarTipoMime(contenido);
        String extension = extensionPara(tipoMime);
        String nombreSeguro = normalizarNombreOriginal(nombreOriginal, extension);
        String nombreAlmacenado = UUID.randomUUID() + "." + extension;
        return new ArchivoPreparado(
                nombreAlmacenado,
                nombreSeguro,
                extension,
                tipoMime,
                contenido.length,
                sha256(contenido)
        );
    }

    public byte[] leer(String rutaRelativa, String hashEsperado) throws IOException {
        Path archivo = resolverRuta(rutaRelativa);
        if (!Files.isRegularFile(archivo)) {
            throw new IOException("El archivo clínico solicitado no está disponible.");
        }
        long tamano = Files.size(archivo);
        if (tamano <= 0 || tamano > TAMANO_MAXIMO_BYTES) {
            throw new IOException("El archivo clínico posee un tamaño inválido.");
        }
        byte[] contenido = Files.readAllBytes(archivo);
        verificarContenido(contenido, hashEsperado);
        return contenido;
    }

    public byte[] verificarContenido(byte[] contenido, String hashEsperado)
            throws IOException {
        if (contenido == null || contenido.length == 0
                || contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new IOException("El archivo clínico posee un tamaño inválido.");
        }
        if (hashEsperado != null
                && !hashEsperado.isBlank()
                && !sha256(contenido).equalsIgnoreCase(hashEsperado.strip())) {
            throw new IOException("El archivo clínico no superó la verificación de integridad.");
        }
        return contenido;
    }

    public void eliminarSilenciosamente(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) return;
        try {
            Files.deleteIfExists(resolverRuta(rutaRelativa));
        } catch (IOException ignored) {
            // El error original del registro conserva la prioridad.
        }
    }

    private Path resolverRuta(String rutaRelativa) throws IOException {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new IOException("La ubicación del documento no está definida.");
        }
        Path resuelta = raiz.resolve(rutaRelativa.replace('\\', '/')).normalize();
        if (!resuelta.startsWith(raiz)) {
            throw new IOException("La ubicación del documento no es segura.");
        }
        return resuelta;
    }

    private static void validarTamano(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("Seleccioná un archivo para cargar.");
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar los 10 MB.");
        }
    }

    private static String detectarTipoMime(byte[] contenido) {
        if (contenido.length >= 5
                && contenido[0] == '%'
                && contenido[1] == 'P'
                && contenido[2] == 'D'
                && contenido[3] == 'F'
                && contenido[4] == '-') {
            return "application/pdf";
        }
        if (contenido.length >= 8
                && (contenido[0] & 0xff) == 0x89
                && contenido[1] == 'P'
                && contenido[2] == 'N'
                && contenido[3] == 'G'
                && contenido[4] == 0x0d
                && contenido[5] == 0x0a
                && contenido[6] == 0x1a
                && contenido[7] == 0x0a) {
            return "image/png";
        }
        if (contenido.length >= 3
                && (contenido[0] & 0xff) == 0xff
                && (contenido[1] & 0xff) == 0xd8
                && (contenido[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        throw new IllegalArgumentException(
                "El archivo debe ser un PDF o una imagen JPG, JPEG o PNG válida.");
    }

    private static String extensionPara(String tipoMime) {
        return switch (tipoMime) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            default -> throw new IllegalArgumentException("El formato del archivo no es válido.");
        };
    }

    private static String normalizarNombreOriginal(String nombre, String extension) {
        String limpio = nombre == null ? "" : nombre
                .replace('\\', '/')
                .replaceAll("[\\p{Cntrl}]", "")
                .strip();
        int separador = limpio.lastIndexOf('/');
        if (separador >= 0) limpio = limpio.substring(separador + 1);
        if (limpio.isBlank()) limpio = "documento-clinico." + extension;
        if (limpio.length() > 255) {
            int reserva = Math.min(extension.length() + 1, 20);
            limpio = limpio.substring(0, 255 - reserva) + "." + extension;
        }
        return limpio;
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

    public record ArchivoGuardado(
            String nombreAlmacenado,
            String nombreOriginal,
            String extension,
            String tipoMime,
            long tamanoBytes,
            String rutaRelativa,
            String hashSha256
    ) {
    }

    public record ArchivoPreparado(
            String nombreAlmacenado,
            String nombreOriginal,
            String extension,
            String tipoMime,
            long tamanoBytes,
            String hashSha256
    ) {
    }
}
