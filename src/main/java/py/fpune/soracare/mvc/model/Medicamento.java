package py.fpune.soracare.mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "medicamentos", schema = "soracare")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicamento")
    private Long idMedicamento;

    @Column(name = "nombre_comercial", nullable = false, length = 120)
    private String nombreComercial;

    @Column(name = "principio_activo", nullable = false, length = 120)
    private String principioActivo;

    @Column(name = "presentacion", nullable = false, length = 100)
    private String presentacion;

    @Column(name = "concentracion", length = 60)
    private String concentracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "via_administracion", nullable = false, length = 40)
    private ViaAdministracion viaAdministracion;

    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereReceta = true;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public Long getIdMedicamento() {
        return idMedicamento;
    }

    public void setIdMedicamento(Long idMedicamento) {
        if (idMedicamento != null && idMedicamento <= 0) {
            throw new IllegalArgumentException("El ID del medicamento debe ser mayor que cero.");
        }
        this.idMedicamento = idMedicamento;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = textoObligatorio(
                nombreComercial, "El nombre comercial es obligatorio.", 120);
    }

    public String getPrincipioActivo() {
        return principioActivo;
    }

    public void setPrincipioActivo(String principioActivo) {
        this.principioActivo = textoObligatorio(
                principioActivo, "El principio activo es obligatorio.", 120);
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = textoObligatorio(
                presentacion, "La presentación es obligatoria.", 100);
    }

    public String getConcentracion() {
        return concentracion;
    }

    public void setConcentracion(String concentracion) {
        this.concentracion = textoOpcional(concentracion, 60);
    }

    public ViaAdministracion getViaAdministracion() {
        return viaAdministracion;
    }

    public void setViaAdministracion(ViaAdministracion viaAdministracion) {
        if (viaAdministracion == null) {
            throw new IllegalArgumentException("La vía de administración es obligatoria.");
        }
        this.viaAdministracion = viaAdministracion;
    }

    public boolean isRequiereReceta() {
        return requiereReceta;
    }

    public void setRequiereReceta(boolean requiereReceta) {
        this.requiereReceta = requiereReceta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getDescripcionCompleta() {
        String base = nombreComercial + " · " + principioActivo + " · " + presentacion;
        return concentracion == null ? base : base + " · " + concentracion;
    }

    @PrePersist
    private void prepararCreacion() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    private void prepararActualizacion() {
        fechaActualizacion = OffsetDateTime.now();
    }

    private static String textoObligatorio(
            String valor, String mensaje, int maximo
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        String limpio = valor.strip();
        if (limpio.length() < 2 || limpio.length() > maximo) {
            throw new IllegalArgumentException(
                    "El valor debe contener entre 2 y " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static String textoOpcional(String valor, int maximo) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                    "La concentración no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }
}
