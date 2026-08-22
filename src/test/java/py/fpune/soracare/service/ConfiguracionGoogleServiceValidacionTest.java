package py.fpune.soracare.service;

import py.fpune.soracare.dto.ResultadoInicioGoogleDTO;
import py.fpune.soracare.dto.PerfilGoogleDTO;

public class ConfiguracionGoogleServiceValidacionTest {

    public static void main(String[] args) {
        validarEstadosDelFlujo();
        validarVariableDocumentada();
        validarCorreoControladoPorGoogle();
        System.out.println("Validaciones del acceso con Google: OK");
    }

    private static void validarEstadosDelFlujo() {
        if (ResultadoInicioGoogleDTO.requiereRegistro().estado()
                != ResultadoInicioGoogleDTO.Estado.REQUIERE_REGISTRO) {
            throw new AssertionError("El estado de registro no es válido.");
        }
        if (ResultadoInicioGoogleDTO.requiereVinculacion(10)
                .idUsuarioExistente() != 10L) {
            throw new AssertionError("No se conservó el usuario por vincular.");
        }
    }

    private static void validarVariableDocumentada() {
        if (!"SORACARE_GOOGLE_CLIENT_ID".equals(
                ConfiguracionGoogleService.VARIABLE_CLIENT_ID
        )) {
            throw new AssertionError(
                    "La variable de Google no coincide con el script local."
            );
        }
    }

    private static void validarCorreoControladoPorGoogle() {
        PerfilGoogleDTO gmail = new PerfilGoogleDTO(
                "sub-1",
                "paciente@gmail.com",
                true,
                "Paciente",
                "Prueba",
                "Paciente Prueba",
                null,
                null
        );
        if (!gmail.correoControladoPorGoogle()) {
            throw new AssertionError(
                    "Una cuenta Gmail verificada debe poder vincularse automáticamente."
            );
        }

        PerfilGoogleDTO correoExterno = new PerfilGoogleDTO(
                "sub-2",
                "paciente@ejemplo.com",
                true,
                "Paciente",
                "Externo",
                "Paciente Externo",
                null,
                null
        );
        if (correoExterno.correoControladoPorGoogle()) {
            throw new AssertionError(
                    "Un correo externo sin dominio alojado debe requerir vinculación segura."
            );
        }
    }
}
