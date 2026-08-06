package py.fpune.soracare.dto;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class UsuarioAutenticadoDTO {

    private final long idUsuario;
    private final long idPersona;
    private final String nombreUsuario;
    private final boolean debeCambiarContrasena;
    private final Set<String> roles;

    public UsuarioAutenticadoDTO(
            long idUsuario,
            long idPersona,
            String nombreUsuario,
            boolean debeCambiarContrasena,
            Set<String> roles
    ) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "El ID del usuario debe ser mayor que cero."
            );
        }

        if (idPersona <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la persona debe ser mayor que cero."
            );
        }

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio."
            );
        }

        this.idUsuario = idUsuario;
        this.idPersona = idPersona;
        this.nombreUsuario =
                nombreUsuario.strip();

        this.debeCambiarContrasena =
                debeCambiarContrasena;

        this.roles = normalizarRoles(
                roles
        );
    }

    public long getIdUsuario() {
        return idUsuario;
    }

    public long getIdPersona() {
        return idPersona;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public boolean isDebeCambiarContrasena() {
        return debeCambiarContrasena;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(
                roles
        );
    }

    public boolean tieneRol(
            String nombreRol
    ) {
        if (nombreRol == null
                || nombreRol.isBlank()) {

            return false;
        }

        String rolNormalizado =
                nombreRol
                        .strip()
                        .toUpperCase(Locale.ROOT);

        return roles.contains(
                rolNormalizado
        );
    }

    private static Set<String> normalizarRoles(
            Set<String> roles
    ) {
        Set<String> rolesNormalizados =
                new LinkedHashSet<>();

        if (roles == null) {
            return rolesNormalizados;
        }

        for (String rol : roles) {

            if (rol == null || rol.isBlank()) {
                continue;
            }

            rolesNormalizados.add(
                    rol.strip()
                            .toUpperCase(Locale.ROOT)
            );
        }

        return rolesNormalizados;
    }
}