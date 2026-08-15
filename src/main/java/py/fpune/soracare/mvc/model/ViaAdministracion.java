package py.fpune.soracare.mvc.model;

public enum ViaAdministracion {
    ORAL("Oral"),
    INTRAVENOSA("Intravenosa"),
    INTRAMUSCULAR("Intramuscular"),
    SUBCUTANEA("Subcutánea"),
    TOPICA("Tópica"),
    OFTALMICA("Oftálmica"),
    OTICA("Ótica"),
    INHALATORIA("Inhalatoria"),
    RECTAL("Rectal"),
    VAGINAL("Vaginal"),
    OTRA("Otra");

    private final String nombreVisible;

    ViaAdministracion(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }
}
