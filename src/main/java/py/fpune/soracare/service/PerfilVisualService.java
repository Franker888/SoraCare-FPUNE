package py.fpune.soracare.service;

import py.fpune.soracare.dao.PerfilVisualDAO;
import py.fpune.soracare.dto.PerfilVisualDTO;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

public class PerfilVisualService {

    private static final Set<String> AVATARES = Set.of(
            "AZUL", "TURQUESA", "VIOLETA", "CORAL", "VERDE", "DORADO"
    );

    private final PerfilVisualDAO perfilDAO = new PerfilVisualDAO();
    private final AlmacenFotosPerfil almacen = new AlmacenFotosPerfil();

    public PerfilVisualDTO consultar(long idPersona) throws SQLException {
        validarPersona(idPersona);
        return perfilDAO.consultar(idPersona);
    }

    public void elegirAvatar(long idPersona, String codigo) throws SQLException {
        validarPersona(idPersona);
        String normalizado = codigo == null
                ? ""
                : codigo.strip().toUpperCase(Locale.ROOT);
        if (!AVATARES.contains(normalizado)) {
            throw new IllegalArgumentException("El avatar seleccionado no es válido.");
        }
        String fotoAnterior = perfilDAO.elegirAvatar(idPersona, normalizado);
        almacen.eliminarSilenciosamente(fotoAnterior);
    }

    public void guardarFoto(long idPersona, byte[] contenido)
            throws IOException, SQLException {
        validarPersona(idPersona);
        AlmacenFotosPerfil.FotoGuardada nueva = almacen.guardar(idPersona, contenido);
        try {
            String anterior = perfilDAO.guardarFoto(
                    idPersona,
                    nueva.rutaRelativa(),
                    nueva.tipoMime(),
                    nueva.tamanoBytes(),
                    nueva.hashSha256()
            );
            almacen.eliminarSilenciosamente(anterior);
        } catch (SQLException | RuntimeException error) {
            almacen.eliminarSilenciosamente(nueva.rutaRelativa());
            throw error;
        }
    }

    public FotoDescargable abrirFoto(long idPersona) throws SQLException, IOException {
        validarPersona(idPersona);
        PerfilVisualDAO.ArchivoPerfil archivo = perfilDAO.buscarArchivo(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta todavía no posee una fotografía personalizada."));
        byte[] contenido = almacen.leer(archivo.rutaRelativa(), archivo.hashSha256());
        return new FotoDescargable(archivo.tipoMime(), contenido);
    }

    private static void validarPersona(long idPersona) {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta del perfil no es válida.");
        }
    }

    public record FotoDescargable(String tipoMime, byte[] contenido) {
    }
}
