package py.fpune.soracare.dto;

public record RegistroAtencionMedicaDTO(
        String sintomas,
        String antecedentesRelevantes,
        String antecedentesPatologicosPersonales,
        String farmacosUtilizados,
        String alergias,
        String temperaturaC,
        String frecuenciaCardiaca,
        String frecuenciaRespiratoria,
        String presionSistolica,
        String presionDiastolica,
        String saturacionOxigeno,
        String evaluacionAreaAfectada,
        String examenFisico,
        String evaluacionClinica,
        String planTratamiento,
        String indicaciones,
        String observaciones,
        String requiereSeguimiento,
        String fechaProximoControl
) {
}
