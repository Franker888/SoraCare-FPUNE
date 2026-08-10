package py.fpune.soracare.mvc.model;

public class EstadoCita {

    private Integer idEstadoCita;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean finalizado;
    private boolean activo;

    public Integer getIdEstadoCita() { return idEstadoCita; }
    public void setIdEstadoCita(Integer idEstadoCita) { this.idEstadoCita = idEstadoCita; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isFinalizado() { return finalizado; }
    public void setFinalizado(boolean finalizado) { this.finalizado = finalizado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
