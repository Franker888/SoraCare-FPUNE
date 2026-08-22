package py.fpune.soracare.service;

import py.fpune.soracare.dao.AuditoriaDAO;
import py.fpune.soracare.dto.FiltroAuditoriaDTO;
import py.fpune.soracare.dto.ResumenAuditoriaDTO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;

public class AuditoriaService {

    private static final Set<String> TIPOS = Set.of(
            "", "CREACION", "MODIFICACION", "ELIMINACION", "CONSULTA",
            "INICIO_SESION", "CIERRE_SESION", "AUTENTICACION_FALLIDA",
            "ASIGNACION_ROL", "EXPORTACION", "ERROR"
    );
    private static final Set<String> RESULTADOS = Set.of("", "EXITOSO", "FALLIDO");

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public ResumenAuditoriaDTO consultar(FiltroAuditoriaDTO filtro) throws SQLException {
        if (filtro == null || filtro.desde() == null || filtro.hasta() == null) {
            throw new IllegalArgumentException("El período de auditoría es obligatorio.");
        }
        if (filtro.desde().isAfter(filtro.hasta())) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final.");
        }
        if (filtro.desde().isBefore(filtro.hasta().minusYears(1))) {
            throw new IllegalArgumentException("El período de consulta no puede superar un año.");
        }
        if (!TIPOS.contains(filtro.tipoEvento())) {
            throw new IllegalArgumentException("El tipo de evento no es válido.");
        }
        if (!RESULTADOS.contains(filtro.resultado())) {
            throw new IllegalArgumentException("El resultado seleccionado no es válido.");
        }
        return auditoriaDAO.consultar(filtro);
    }

    public FiltroAuditoriaDTO crearFiltro(
            String desde,
            String hasta,
            String tipo,
            String modulo,
            String resultado,
            String busqueda
    ) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaDesde = parsearFecha(desde, hoy.minusDays(30));
        LocalDate fechaHasta = parsearFecha(hasta, hoy);
        return new FiltroAuditoriaDTO(
                fechaDesde,
                fechaHasta,
                normalizar(tipo, 30).toUpperCase(),
                normalizar(modulo, 50),
                normalizar(resultado, 20).toUpperCase(),
                normalizar(busqueda, 100)
        );
    }

    public void registrarSilenciosamente(
            Long idUsuario,
            String tipoEvento,
            String modulo,
            String entidad,
            String descripcion,
            String direccionIp,
            String metodoHttp,
            String rutaSolicitud,
            String agenteUsuario,
            String resultado,
            String mensajeError
    ) {
        try {
            auditoriaDAO.registrar(
                    idUsuario, tipoEvento, modulo, entidad, null, descripcion,
                    direccionIp, metodoHttp, rutaSolicitud, agenteUsuario,
                    resultado, mensajeError
            );
        } catch (SQLException ignored) {
            // La auditoría nunca debe interrumpir la operación principal.
        }
    }

    private static LocalDate parsearFecha(String valor, LocalDate predeterminada) {
        if (valor == null || valor.isBlank()) return predeterminada;
        try {
            return LocalDate.parse(valor.strip());
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Las fechas deben tener un formato válido.");
        }
    }

    private static String normalizar(String valor, int maximo) {
        if (valor == null) return "";
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("Uno de los filtros supera la longitud permitida.");
        }
        return limpio;
    }
}
