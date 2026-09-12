package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dao.SolicitudSifenDAO;
import py.fpune.soracare.dto.DocumentoKudeDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.dto.EstadoPreparacionSifenDTO.Ambiente;
import py.fpune.soracare.sifen.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ConsultaSifenServiceTest {
    private static final String CDC = "1".repeat(44);
    private final UsuarioAutenticadoDTO medico = new UsuarioAutenticadoDTO(1, 20, "medico", false, Set.of("MEDICO"));

    @Test void rechazaSesionRolEIdAntesDeConsultarLaBase() {
        var dao = new SolicitudSifenDAO() {
            @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) { fail("No debe acceder a DB"); return Optional.empty(); }
        };
        var servicio = new ConsultaSifenService(dao, new ConfiguracionSifenService(), new ClienteConsultaSifenService());
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(1, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(0, medico));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(1, new UsuarioAutenticadoDTO(1, 20, "paciente", false, Set.of("PACIENTE"))));
    }
    @Test void rechazaDocumentoAjenoYAmbienteDeProduccion() {
        var dao = new SolicitudSifenDAO() {
            @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) {
                assertEquals(20, persona);
                return id == 1 ? Optional.empty() : Optional.of(new DocumentoKudeDTO("PRODUCCION", "ERROR", CDC, null, null, true));
            }
        };
        var servicio = new ConsultaSifenService(dao, new ConfiguracionSifenService(), new ClienteConsultaSifenService());
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(1, medico));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(2, medico));
    }
    @Test void consultaElCdcGuardadoNoModificaElEstadoYBorraPasswordTemporal() throws Exception {
        var dao = new SolicitudSifenDAO() {
            @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) {
                return Optional.of(new DocumentoKudeDTO("PRUEBA", "ERROR", CDC, null, null, true));
            }
        };
        var cfg = new ConfiguracionSifenService() {
            @Override public ConfiguracionFiscalSifen cargarConfiguracion() {
                return new ConfiguracionFiscalSifen(Ambiente.PRUEBA, null, null, null, null, null,
                        Path.of("sintetico.p12"), "sintetico".toCharArray(), null, new char[0], null);
            }
        };
        char[][] copia = {null};
        var cliente = new ClienteConsultaSifenService() {
            @Override public ResultadoConsultaSifen consultarPrueba(long id, String cdc, Path certificado, char[] password) {
                assertEquals(7, id); assertEquals(CDC, cdc); copia[0] = password;
                return new ResultadoConsultaSifen(ResultadoConsultaSifen.Estado.NO_ENCONTRADO, cdc, "0420", "No encontrado", null, 0);
            }
        };
        var pasos = new java.util.ArrayList<String>();
        var historial = new py.fpune.soracare.dao.ConsultaSifenDAO() {
            @Override public void iniciar(long id, long solicitud, long persona, String cdc) {
                assertEquals(7, id); assertEquals(1, solicitud); assertEquals(20, persona); assertEquals(CDC, cdc);
                pasos.add("inicio");
            }
            @Override public void finalizar(long id, ResultadoConsultaSifen resultado) {
                assertEquals(7, id); assertNotNull(copia[0]); pasos.add("fin");
            }
        };
        var servicio = new ConsultaSifenService(dao, cfg, cliente, historial) {
            @Override long siguienteIdentificador() { return 7; }
        };
        assertEquals(ResultadoConsultaSifen.Estado.NO_ENCONTRADO, servicio.consultar(1, medico).estado());
        assertArrayEquals(new char[9], copia[0]);
        assertEquals(java.util.List.of("inicio", "fin"), pasos);
    }
    @Test void historialRequiereMedicoYFiltraPorPersona() throws Exception {
        var historial = new py.fpune.soracare.dao.ConsultaSifenDAO() {
            @Override public java.util.List<py.fpune.soracare.dto.ConsultaSifenHistorialDTO> listarDelMedico(long persona) {
                assertEquals(20, persona); return java.util.List.of();
            }
        };
        var servicio = new ConsultaSifenService(null, null, null, historial);
        assertThrows(IllegalArgumentException.class, () -> servicio.historial(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.historial(new UsuarioAutenticadoDTO(1, 20, "m", true, Set.of("MEDICO"))));
        assertThrows(IllegalArgumentException.class, () -> servicio.historial(new UsuarioAutenticadoDTO(1, 20, "p", false, Set.of("PACIENTE"))));
        assertTrue(servicio.historial(medico).isEmpty());
    }

    @Test void falloDeHistorialImpideConsultarOConservaLaInterrupcionSinReintentar() throws Exception {
        for (boolean fallaInicio : new boolean[]{true, false}) {
            int[] llamadas = {0};
            char[][] passwordUsada = {null};
            var dao = new SolicitudSifenDAO() {
                @Override public Optional<DocumentoKudeDTO> buscarKudeDelMedico(long id, long persona) {
                    return Optional.of(new DocumentoKudeDTO("PRUEBA", "ERROR", CDC, null, null, true));
                }
            };
            var cfg = new ConfiguracionSifenService() {
                @Override public ConfiguracionFiscalSifen cargarConfiguracion() {
                    return new ConfiguracionFiscalSifen(Ambiente.PRUEBA, null, null, null, null, null,
                            Path.of("sintetico.p12"), "sintetico".toCharArray(), null, new char[0], null);
                }
            };
            var historial = new py.fpune.soracare.dao.ConsultaSifenDAO() {
                @Override public void iniciar(long id, long solicitud, long persona, String cdc) throws java.sql.SQLException {
                    if (fallaInicio) throw new java.sql.SQLException("Fallo sintético");
                }
                @Override public void finalizar(long id, ResultadoConsultaSifen resultado) throws java.sql.SQLException {
                    throw new java.sql.SQLException("Fallo sintético");
                }
            };
            var cliente = new ClienteConsultaSifenService() {
                @Override public ResultadoConsultaSifen consultarPrueba(long id, String cdc, Path ruta, char[] password) {
                    llamadas[0]++; passwordUsada[0] = password;
                    return new ResultadoConsultaSifen(ResultadoConsultaSifen.Estado.ERROR, CDC, "TRANSPORTE", "Sin respuesta", null, 0);
                }
            };
            var servicio = new ConsultaSifenService(dao, cfg, cliente, historial) {
                @Override long siguienteIdentificador() { return 7; }
            };
            assertThrows(java.sql.SQLException.class, () -> servicio.consultar(1, medico));
            assertEquals(fallaInicio ? 0 : 1, llamadas[0]);
            if (!fallaInicio) assertArrayEquals(new char[9], passwordUsada[0]);
        }
    }
}
