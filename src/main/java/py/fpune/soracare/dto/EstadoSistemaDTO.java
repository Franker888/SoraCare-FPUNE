package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record EstadoSistemaDTO(
        boolean baseDatosDisponible,
        OffsetDateTime horaBaseDatos,
        String versionPostgreSql,
        long usuariosActivos,
        long citasFuturas,
        long eventosUltimas24Horas,
        int conexionesCreadas,
        int conexionesDisponibles,
        int maximoConexiones,
        long memoriaUsadaMb,
        long memoriaMaximaMb,
        int procesadores,
        long tiempoActivoSegundos,
        String versionJava
) {
}
