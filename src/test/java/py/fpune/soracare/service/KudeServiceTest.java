package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dao.SolicitudSifenDAO;
import py.fpune.soracare.dto.DocumentoKudeDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.sifen.KudePdfService;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class KudeServiceTest {
    @Test void generaSolamenteElDocumentoAutorizadoPorDao() throws Exception {
        var datos = new DocumentoKudeDTO("PRUEBA", "VALIDADO", "cdc", new byte[]{1}, "hash", true);
        var dao = new SolicitudSifenDAO() {
            @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) {
                assertEquals(8, id); assertEquals(20, persona); return Optional.of(datos);
            }
        };
        var pdf = new KudePdfService() {
            @Override public ComprobantePdfService.DocumentoPdf generar(DocumentoKudeDTO documento) {
                assertSame(datos, documento);
                return new ComprobantePdfService.DocumentoPdf("kude.pdf", new byte[]{1}, "hash");
            }
        };
        assertEquals("kude.pdf", new KudeService(dao, pdf).obtener(8,
                new UsuarioAutenticadoDTO(10, 20, "medico", false, Set.of("MEDICO"))).nombreArchivo());
    }
    @Test void deniegaSinSesionOtrosRolesYCambioPendienteSinConsultarBase() {
        var dao = new SolicitudSifenDAO() {
            @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) { fail("No debe consultar la base"); return Optional.empty(); }
        };
        var service = new KudeService(dao, new KudePdfService());
        assertThrows(IllegalArgumentException.class, () -> service.obtener(1, null));
        for (String rol : Set.of("PACIENTE", "ADMINISTRADOR", "RECEPCION")) {
            assertThrows(IllegalArgumentException.class, () -> service.obtener(1, new UsuarioAutenticadoDTO(1, 2, "prueba", false, Set.of(rol))));
        }
        assertThrows(IllegalArgumentException.class, () -> service.obtener(1, new UsuarioAutenticadoDTO(1, 2, "prueba", true, Set.of("MEDICO"))));
    }

    @Test void usaLaPersonaDeSesionYDeniegaDocumentoAjeno() {
        var dao = new SolicitudSifenDAO() {
            @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) {
                assertEquals(8, id); assertEquals(20, persona); return Optional.empty();
            }
        };
        assertThrows(IllegalArgumentException.class, () -> new KudeService(dao, new KudePdfService())
                .obtener(8, new UsuarioAutenticadoDTO(10, 20, "medico", false, Set.of("MEDICO"))));
    }
}
