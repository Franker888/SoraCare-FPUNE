package py.fpune.soracare.mvc.model;

public enum TratamientoMedico {
    DR("Dr."),
    DRA("Dra.");

    private final String abreviatura;

    TratamientoMedico(String abreviatura) {
        this.abreviatura = abreviatura;
    }

    public String getAbreviatura() {
        return abreviatura;
    }
}
