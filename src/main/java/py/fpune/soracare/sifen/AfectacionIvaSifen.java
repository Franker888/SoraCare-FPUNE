package py.fpune.soracare.sifen;

public enum AfectacionIvaSifen {
    GRAVADO(1, "Gravado IVA"),
    EXONERADO(2, "Exonerado (Art. 100 - Ley 6380/2019)"),
    EXENTO(3, "Exento"),
    GRAVADO_PARCIAL(4, "Gravado parcial (Grav- Exento)");

    private final int codigo;
    private final String descripcion;

    AfectacionIvaSifen(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int codigo() {
        return codigo;
    }

    public String descripcion() {
        return descripcion;
    }
}
