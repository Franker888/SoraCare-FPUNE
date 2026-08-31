package py.fpune.soracare.service;

import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Construye un contexto breve usando exclusivamente información pública del
 * catálogo. Nunca incorpora datos de pacientes, contacto ni historias clínicas.
 */
public class SoraContextoPublicoService {

    private static final int MAXIMO_ESPECIALIDADES = 20;
    private static final int MAXIMO_MEDICOS = 24;
    private static final long DURACION_CACHE_MILISEGUNDOS = 120_000L;

    private final MedicoService medicoService;
    private final EspecialidadService especialidadService;
    private final MedicoEspecialidadService medicoEspecialidadService;
    private final LongSupplier reloj;

    private volatile ContextoPublico contextoCache;
    private volatile long cacheValidaHasta;

    public SoraContextoPublicoService() {
        this(
                new MedicoService(),
                new EspecialidadService(),
                new MedicoEspecialidadService(),
                System::currentTimeMillis
        );
    }

    SoraContextoPublicoService(
            MedicoService medicoService,
            EspecialidadService especialidadService,
            MedicoEspecialidadService medicoEspecialidadService,
            LongSupplier reloj
    ) {
        this.medicoService = medicoService;
        this.especialidadService = especialidadService;
        this.medicoEspecialidadService = medicoEspecialidadService;
        this.reloj = reloj;
    }

    public ContextoPublico consultar() throws SQLException {
        long ahora = reloj.getAsLong();
        ContextoPublico cache = contextoCache;
        if (cache != null && ahora < cacheValidaHasta) {
            return cache;
        }

        synchronized (this) {
            ahora = reloj.getAsLong();
            if (contextoCache != null && ahora < cacheValidaHasta) {
                return contextoCache;
            }
            contextoCache = construirContexto();
            cacheValidaHasta = ahora + DURACION_CACHE_MILISEGUNDOS;
            return contextoCache;
        }
    }

    private ContextoPublico construirContexto() throws SQLException {
        List<Especialidad> especialidades = especialidadService
                .listarActivas()
                .stream()
                .limit(MAXIMO_ESPECIALIDADES)
                .toList();
        List<Medico> medicos = medicoService
                .listarActivos()
                .stream()
                .limit(MAXIMO_MEDICOS)
                .toList();

        Map<Integer, String> nombresEspecialidades = new LinkedHashMap<>();
        for (Especialidad especialidad : especialidades) {
            nombresEspecialidades.put(
                    especialidad.getIdEspecialidad(),
                    limpiarDato(especialidad.getNombre())
            );
        }

        StringBuilder contexto = new StringBuilder(2_400);
        contexto.append("DATOS_PUBLICOS_SORACARE\n")
                .append("Especialidades activas (")
                .append(especialidades.size())
                .append("): ")
                .append(String.join(", ", nombresEspecialidades.values()))
                .append(".\nProfesionales activos:\n");

        int medicosIncluidos = 0;
        for (Medico medico : medicos) {
            if (medico.getIdMedico() == null) continue;
            List<String> especialidadesMedico = medicoEspecialidadService
                    .listarActivasPorMedico(medico.getIdMedico())
                    .stream()
                    .map(MedicoEspecialidad::getIdEspecialidad)
                    .map(nombresEspecialidades::get)
                    .filter(nombre -> nombre != null && !nombre.isBlank())
                    .toList();

            contexto.append("- ")
                    .append(medico.getTratamiento() == null
                            ? "Dr."
                            : medico.getTratamiento().getAbreviatura())
                    .append(' ')
                    .append(limpiarDato(medico.getNombres()))
                    .append(' ')
                    .append(limpiarDato(medico.getApellidos()))
                    .append(": ")
                    .append(especialidadesMedico.isEmpty()
                            ? "especialidad no publicada"
                            : String.join(", ", especialidadesMedico))
                    .append(".\n");
            medicosIncluidos++;
        }

        if (medicosIncluidos == 0) {
            contexto.append("- No hay profesionales activos publicados.\n");
        }

        contexto.append("FIN_DATOS_PUBLICOS_SORACARE");
        return new ContextoPublico(contexto.toString());
    }

    private static String limpiarDato(String valor) {
        if (valor == null) return "";
        return valor.strip()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }

    public record ContextoPublico(String texto) {

        public static ContextoPublico noDisponible() {
            return new ContextoPublico("");
        }
    }
}
