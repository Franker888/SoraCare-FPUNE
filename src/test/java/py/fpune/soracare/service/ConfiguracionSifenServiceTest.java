package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.EstadoPreparacionSifenDTO;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionSifenServiceTest {

    @Test
    void permaneceDeshabilitadoPorDefecto() {
        EstadoPreparacionSifenDTO estado = servicio(Map.of(), false).consultarEstado();

        assertEquals(EstadoPreparacionSifenDTO.Ambiente.DESHABILITADO, estado.ambiente());
        assertFalse(estado.habilitada());
        assertFalse(estado.configuracionCompleta());
    }

    @Test
    void informaCadaRequisitoSinExponerSecretos() {
        Map<String, String> variables = new HashMap<>();
        variables.put(ConfiguracionSifenService.VARIABLE_AMBIENTE, "PRUEBA");
        variables.put(ConfiguracionSifenService.VARIABLE_CERTIFICADO_PATH, "certificado.p12");
        variables.put(ConfiguracionSifenService.VARIABLE_CERTIFICADO_PASSWORD, "secreto-certificado");
        variables.put(ConfiguracionSifenService.VARIABLE_CSC, "secreto-csc");

        EstadoPreparacionSifenDTO estado = servicio(variables, false).consultarEstado();

        assertTrue(estado.habilitada());
        assertFalse(estado.configuracionCompleta());
        assertTrue(estado.requisitosPendientes().contains("RUC del emisor"));
        assertTrue(estado.requisitosPendientes().contains(
                "certificado cualificado accesible para la aplicación"
        ));
        assertFalse(estado.toString().contains("secreto-certificado"));
        assertFalse(estado.toString().contains("secreto-csc"));
    }

    @Test
    void reconoceConfiguracionCompletaDePrueba() {
        Map<String, String> variables = configuracionCompleta();

        EstadoPreparacionSifenDTO estado = servicio(variables, true).consultarEstado();

        assertEquals(EstadoPreparacionSifenDTO.Ambiente.PRUEBA, estado.ambiente());
        assertTrue(estado.configuracionCompleta());
        assertTrue(estado.requisitosPendientes().isEmpty());
    }

    @Test
    void rechazaUnAmbienteDesconocido() {
        EstadoPreparacionSifenDTO estado = servicio(
                Map.of(ConfiguracionSifenService.VARIABLE_AMBIENTE, "LOCAL"),
                true
        ).consultarEstado();

        assertFalse(estado.ambienteValido());
        assertFalse(estado.configuracionCompleta());
        assertEquals(1, estado.requisitosPendientes().size());
    }

    private static ConfiguracionSifenService servicio(
            Map<String, String> variables,
            boolean certificadoLegible
    ) {
        return new ConfiguracionSifenService(
                variables::get,
                ruta -> certificadoLegible
        );
    }

    private static Map<String, String> configuracionCompleta() {
        Map<String, String> variables = new HashMap<>();
        variables.put(ConfiguracionSifenService.VARIABLE_AMBIENTE, "PRUEBA");
        variables.put(ConfiguracionSifenService.VARIABLE_RUC, "valor-prueba");
        variables.put(ConfiguracionSifenService.VARIABLE_DV, "valor-prueba");
        variables.put(ConfiguracionSifenService.VARIABLE_TIMBRADO, "valor-prueba");
        variables.put(ConfiguracionSifenService.VARIABLE_ESTABLECIMIENTO, "valor-prueba");
        variables.put(ConfiguracionSifenService.VARIABLE_PUNTO_EXPEDICION, "valor-prueba");
        variables.put(ConfiguracionSifenService.VARIABLE_CERTIFICADO_PATH, "certificado.p12");
        variables.put(ConfiguracionSifenService.VARIABLE_CERTIFICADO_PASSWORD, "secreto");
        variables.put(ConfiguracionSifenService.VARIABLE_CSC_ID, "valor-prueba");
        variables.put(ConfiguracionSifenService.VARIABLE_CSC, "secreto");
        return variables;
    }
}
