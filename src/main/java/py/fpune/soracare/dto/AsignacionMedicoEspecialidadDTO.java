package py.fpune.soracare.dto;

public class AsignacionMedicoEspecialidadDTO {

    private final String idMedico;
    private final String idEspecialidad;
    private final String principal;
    private final String fechaDesde;

    public AsignacionMedicoEspecialidadDTO(
            String idMedico,
            String idEspecialidad,
            String principal,
            String fechaDesde
    ) {
        this.idMedico =
                idMedico;

        this.idEspecialidad =
                idEspecialidad;

        this.principal =
                principal;

        this.fechaDesde =
                fechaDesde;
    }

    public String getIdMedico() {
        return idMedico;
    }

    public String getIdEspecialidad() {
        return idEspecialidad;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getFechaDesde() {
        return fechaDesde;
    }
}