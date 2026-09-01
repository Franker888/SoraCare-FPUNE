package py.fpune.soracare.service;

import py.fpune.soracare.dto.EstadoPreparacionSifenDTO;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConfiguracionSifenService {

    public static final String VARIABLE_AMBIENTE = "SORACARE_SIFEN_AMBIENTE";
    public static final String VARIABLE_RUC = "SORACARE_SIFEN_RUC";
    public static final String VARIABLE_DV = "SORACARE_SIFEN_DV";
    public static final String VARIABLE_TIMBRADO = "SORACARE_SIFEN_TIMBRADO";
    public static final String VARIABLE_ESTABLECIMIENTO =
            "SORACARE_SIFEN_ESTABLECIMIENTO";
    public static final String VARIABLE_PUNTO_EXPEDICION =
            "SORACARE_SIFEN_PUNTO_EXPEDICION";
    public static final String VARIABLE_CERTIFICADO_PATH =
            "SORACARE_SIFEN_CERTIFICADO_PATH";
    public static final String VARIABLE_CERTIFICADO_PASSWORD =
            "SORACARE_SIFEN_CERTIFICADO_PASSWORD";
    public static final String VARIABLE_CSC_ID = "SORACARE_SIFEN_CSC_ID";
    public static final String VARIABLE_CSC = "SORACARE_SIFEN_CSC";

    private final Function<String, String> obtenerVariable;
    private final Predicate<Path> archivoLegible;

    public ConfiguracionSifenService() {
        this(System::getenv, Files::isReadable);
    }

    ConfiguracionSifenService(
            Function<String, String> obtenerVariable,
            Predicate<Path> archivoLegible
    ) {
        this.obtenerVariable = obtenerVariable;
        this.archivoLegible = archivoLegible;
    }

    public EstadoPreparacionSifenDTO consultarEstado() {
        String valorAmbiente = valor(VARIABLE_AMBIENTE);
        if (valorAmbiente == null || "DESHABILITADO".equalsIgnoreCase(valorAmbiente)) {
            return new EstadoPreparacionSifenDTO(
                    EstadoPreparacionSifenDTO.Ambiente.DESHABILITADO,
                    true,
                    List.of()
            );
        }

        EstadoPreparacionSifenDTO.Ambiente ambiente;
        try {
            ambiente = EstadoPreparacionSifenDTO.Ambiente.valueOf(
                    valorAmbiente.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException error) {
            return new EstadoPreparacionSifenDTO(
                    EstadoPreparacionSifenDTO.Ambiente.DESHABILITADO,
                    false,
                    List.of("Ambiente válido: DESHABILITADO, PRUEBA o PRODUCCION")
            );
        }

        List<String> pendientes = new ArrayList<>();
        requerir(VARIABLE_RUC, "RUC del emisor", pendientes);
        requerir(VARIABLE_DV, "dígito verificador del RUC", pendientes);
        requerir(VARIABLE_TIMBRADO, "timbrado electrónico", pendientes);
        requerir(VARIABLE_ESTABLECIMIENTO, "código de establecimiento", pendientes);
        requerir(VARIABLE_PUNTO_EXPEDICION, "punto de expedición", pendientes);
        requerir(VARIABLE_CERTIFICADO_PASSWORD, "contraseña del certificado", pendientes);
        requerir(VARIABLE_CSC_ID, "identificador del CSC", pendientes);
        requerir(VARIABLE_CSC, "CSC", pendientes);
        validarCertificado(pendientes);

        return new EstadoPreparacionSifenDTO(ambiente, true, pendientes);
    }

    private void validarCertificado(List<String> pendientes) {
        String ruta = valor(VARIABLE_CERTIFICADO_PATH);
        if (ruta == null) {
            pendientes.add("archivo del certificado cualificado");
            return;
        }
        try {
            if (!archivoLegible.test(Path.of(ruta))) {
                pendientes.add("certificado cualificado accesible para la aplicación");
            }
        } catch (InvalidPathException error) {
            pendientes.add("ruta válida del certificado cualificado");
        }
    }

    private void requerir(
            String variable,
            String descripcion,
            List<String> pendientes
    ) {
        if (valor(variable) == null) {
            pendientes.add(descripcion);
        }
    }

    private String valor(String nombre) {
        String valor = obtenerVariable.apply(nombre);
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
