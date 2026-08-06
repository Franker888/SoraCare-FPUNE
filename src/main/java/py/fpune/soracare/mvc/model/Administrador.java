package py.fpune.soracare.mvc.model;

public class Administrador extends Persona {

    private String codigoEmpleado;

    public Administrador() {
        super();
    }

    public String getCodigoEmpleado() {
        return codigoEmpleado;
    }

    public void setCodigoEmpleado(String codigoEmpleado) {
        this.codigoEmpleado = codigoEmpleado;
    }
}