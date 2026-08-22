package py.fpune.soracare.service;

import py.fpune.soracare.dao.InformeAdministrativoDAO;
import py.fpune.soracare.dto.FiltroInformeDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO.PuntoFechaDTO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InformeAdministrativoService {

    private static final int MAXIMO_DIAS = 366;
    private final InformeAdministrativoDAO informeDAO = new InformeAdministrativoDAO();

    public FiltroInformeDTO construirFiltro(
            String fechaDesde,
            String fechaHasta,
            String idMedico,
            String idEspecialidad
    ) {
        LocalDate hoy = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        YearMonth mesActual = YearMonth.from(hoy);
        LocalDate desde = convertirFecha(fechaDesde, mesActual.atDay(1), "inicio");
        LocalDate hasta = convertirFecha(fechaHasta, mesActual.atEndOfMonth(), "fin");
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la fecha inicial."
            );
        }
        long dias = hasta.toEpochDay() - desde.toEpochDay() + 1;
        if (dias > MAXIMO_DIAS) {
            throw new IllegalArgumentException(
                    "El período del informe no puede superar un año."
            );
        }
        return new FiltroInformeDTO(
                desde,
                hasta,
                convertirLongOpcional(idMedico, "médico"),
                convertirEnteroOpcional(idEspecialidad, "especialidad")
        );
    }

    public FiltroInformeDTO filtroMesActual() {
        return construirFiltro(null, null, null, null);
    }

    public InformeAdministrativoDTO consultar(FiltroInformeDTO filtro) throws SQLException {
        InformeAdministrativoDTO informe = informeDAO.consultar(filtro);
        return new InformeAdministrativoDTO(
                informe.totalCitas(),
                informe.citasPeriodoAnterior(),
                informe.citasConfirmadas(),
                informe.citasAtendidas(),
                informe.citasCanceladas(),
                informe.citasReprogramadas(),
                informe.busquedasTurnos(),
                informe.pacientesRegistrados(),
                informe.ingresosConfirmados(),
                completarSerie(filtro, informe.citasPorFecha()),
                informe.citasPorEspecialidad(),
                informe.citasPorMedico(),
                informe.citasPorEstado(),
                informe.mapaCalor()
        );
    }

    public void registrarBusqueda(
            Long idMedico,
            Integer idEspecialidad,
            LocalDate fechaConsultada,
            int cantidadTurnos,
            String origen
    ) throws SQLException {
        if (fechaConsultada == null) return;
        if (cantidadTurnos < 0 || cantidadTurnos > Short.MAX_VALUE) {
            throw new IllegalArgumentException("La cantidad de turnos no es válida.");
        }
        if (!"PUBLICO".equals(origen) && !"PACIENTE".equals(origen)) {
            throw new IllegalArgumentException("El origen de la búsqueda no es válido.");
        }
        informeDAO.registrarBusqueda(
                idMedico, idEspecialidad, fechaConsultada, cantidadTurnos, origen
        );
    }

    private static List<PuntoFechaDTO> completarSerie(
            FiltroInformeDTO filtro, List<PuntoFechaDTO> datos
    ) {
        Map<LocalDate, Long> porFecha = new LinkedHashMap<>();
        for (PuntoFechaDTO punto : datos) porFecha.put(punto.fecha(), punto.cantidad());
        List<PuntoFechaDTO> serie = new ArrayList<>();
        for (LocalDate fecha = filtro.fechaDesde(); !fecha.isAfter(filtro.fechaHasta());
             fecha = fecha.plusDays(1)) {
            serie.add(new PuntoFechaDTO(fecha, porFecha.getOrDefault(fecha, 0L)));
        }
        return serie;
    }

    private static LocalDate convertirFecha(
            String valor, LocalDate predeterminada, String nombre
    ) {
        if (valor == null || valor.isBlank()) return predeterminada;
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La fecha de " + nombre + " no es válida.", error
            );
        }
    }

    private static Long convertirLongOpcional(String valor, String nombre) {
        if (valor == null || valor.isBlank()) return null;
        try {
            long id = Long.parseLong(valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El " + nombre + " seleccionado no es válido.", error);
        }
    }

    private static Integer convertirEnteroOpcional(String valor, String nombre) {
        if (valor == null || valor.isBlank()) return null;
        try {
            int id = Integer.parseInt(valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La " + nombre + " seleccionada no es válida.", error);
        }
    }
}
