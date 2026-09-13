package py.fpune.soracare.mvc.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Usuario {

    private Long idUsuario;
    private Long idPersona;

    private String nombreUsuario;
    private String contrasenaHash;

    private EstadoUsuario estado;
    private long versionSesion;

    public long getVersionSesion() { return versionSesion; }

    public void setVersionSesion(long versionSesion) { this.versionSesion = versionSesion; }

    private short intentosFallidos;
    private OffsetDateTime bloqueadoHasta;

    private boolean debeCambiarContrasena;

    private OffsetDateTime ultimoAcceso;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    private final List<Rol> roles;

    public Usuario() {
        this.estado = EstadoUsuario.ACTIVO;
        this.intentosFallidos = 0;
        this.debeCambiarContrasena = true;
        this.roles = new ArrayList<>();
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Long idPersona) {
        this.idPersona = idPersona;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public short getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(short intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(
            OffsetDateTime bloqueadoHasta
    ) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public boolean isDebeCambiarContrasena() {
        return debeCambiarContrasena;
    }

    public void setDebeCambiarContrasena(
            boolean debeCambiarContrasena
    ) {
        this.debeCambiarContrasena =
                debeCambiarContrasena;
    }

    public OffsetDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(
            OffsetDateTime ultimoAcceso
    ) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(
            OffsetDateTime creadoEn
    ) {
        this.creadoEn = creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(
            OffsetDateTime actualizadoEn
    ) {
        this.actualizadoEn = actualizadoEn;
    }

    public List<Rol> getRoles() {
        return Collections.unmodifiableList(
                roles
        );
    }

    public void agregarRol(
            Rol rol
    ) {
        if (rol != null) {
            roles.add(rol);
        }
    }

    public void limpiarRoles() {
        roles.clear();
    }
}
