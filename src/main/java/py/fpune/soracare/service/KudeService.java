package py.fpune.soracare.service;

import py.fpune.soracare.dao.SolicitudSifenDAO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.sifen.KudePdfService;
import java.sql.SQLException;

public class KudeService {
    private final SolicitudSifenDAO solicitudes;
    private final KudePdfService pdf;

    public KudeService() { this(new SolicitudSifenDAO(), new KudePdfService()); }

    KudeService(SolicitudSifenDAO solicitudes, KudePdfService pdf) {
        this.solicitudes = solicitudes;
        this.pdf = pdf;
    }

    public ComprobantePdfService.DocumentoPdf obtener(long id, UsuarioAutenticadoDTO usuario)
            throws SQLException {
        if (id <= 0 || usuario == null || !usuario.tieneRol("MEDICO")
                || usuario.isDebeCambiarContrasena()) {
            throw new IllegalArgumentException("KuDE no disponible para esta cuenta.");
        }
        var datos = solicitudes.buscarKudeDelMedico(id, usuario.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException("KuDE no disponible para esta cuenta."));
        return pdf.generar(datos);
    }
}
