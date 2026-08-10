package py.fpune.soracare.mvc.model;

public class MetodoPago {

    private Integer idMetodoPago;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean requiereReferencia;

    public Integer getIdMetodoPago() { return idMetodoPago; }
    public void setIdMetodoPago(Integer idMetodoPago) { this.idMetodoPago = idMetodoPago; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isRequiereReferencia() { return requiereReferencia; }
    public void setRequiereReferencia(boolean requiereReferencia) { this.requiereReferencia = requiereReferencia; }
}
