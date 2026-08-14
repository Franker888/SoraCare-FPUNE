package py.fpune.soracare.mvc.model;

public class Paciente extends Persona {

    private Long idPaciente;
    private String numeroHistoriaClinica;
    private String grupoSanguineo;
    private String contactoEmergencia;
    private String telefonoEmergencia;
    private String antecedentesPatologicosPersonales;
    private String farmacosUtilizados;
    private String alergias;

    public Paciente() {
        super();
    }

    public Long getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(Long idPaciente) {
        this.idPaciente = idPaciente;
    }

    public String getNumeroHistoriaClinica() {
        return numeroHistoriaClinica;
    }

    public void setNumeroHistoriaClinica(
            String numeroHistoriaClinica
    ) {
        this.numeroHistoriaClinica =
                numeroHistoriaClinica;
    }

    public String getGrupoSanguineo() {
        return grupoSanguineo;
    }

    public void setGrupoSanguineo(
            String grupoSanguineo
    ) {
        this.grupoSanguineo = grupoSanguineo;
    }

    public String getContactoEmergencia() {
        return contactoEmergencia;
    }

    public void setContactoEmergencia(
            String contactoEmergencia
    ) {
        this.contactoEmergencia =
                contactoEmergencia;
    }

    public String getTelefonoEmergencia() {
        return telefonoEmergencia;
    }

    public void setTelefonoEmergencia(
            String telefonoEmergencia
    ) {
        this.telefonoEmergencia =
                telefonoEmergencia;
    }

    public String getAntecedentesPatologicosPersonales() {
        return antecedentesPatologicosPersonales;
    }

    public void setAntecedentesPatologicosPersonales(
            String antecedentesPatologicosPersonales
    ) {
        this.antecedentesPatologicosPersonales =
                antecedentesPatologicosPersonales;
    }

    public String getFarmacosUtilizados() {
        return farmacosUtilizados;
    }

    public void setFarmacosUtilizados(String farmacosUtilizados) {
        this.farmacosUtilizados = farmacosUtilizados;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }
}
