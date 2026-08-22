package py.fpune.soracare.dto;

import java.util.List;

public record ResumenUsuariosDTO(
        List<UsuarioAdministrativoDTO> usuarios,
        long total,
        long activos,
        long bloqueados,
        long inactivos
) {
}
