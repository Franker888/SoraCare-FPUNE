package py.fpune.soracare.service;

import py.fpune.soracare.dto.EstadoPreparacionSifenDTO;
import py.fpune.soracare.sifen.ConfiguracionFiscalSifen;
import py.fpune.soracare.sifen.EmisorSifen;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    public static final String VARIABLE_FECHA_INICIO_TIMBRADO =
            "SORACARE_SIFEN_FECHA_INICIO_TIMBRADO";
    public static final String VARIABLE_TIPO_CONTRIBUYENTE =
            "SORACARE_SIFEN_TIPO_CONTRIBUYENTE";
    public static final String VARIABLE_TIPO_REGIMEN =
            "SORACARE_SIFEN_TIPO_REGIMEN";
    public static final String VARIABLE_RAZON_SOCIAL =
            "SORACARE_SIFEN_RAZON_SOCIAL";
    public static final String VARIABLE_NOMBRE_FANTASIA =
            "SORACARE_SIFEN_NOMBRE_FANTASIA";
    public static final String VARIABLE_DIRECCION = "SORACARE_SIFEN_DIRECCION";
    public static final String VARIABLE_NUMERO_CASA = "SORACARE_SIFEN_NUMERO_CASA";
    public static final String VARIABLE_DEPARTAMENTO =
            "SORACARE_SIFEN_DEPARTAMENTO";
    public static final String VARIABLE_DEPARTAMENTO_DESCRIPCION =
            "SORACARE_SIFEN_DEPARTAMENTO_DESCRIPCION";
    public static final String VARIABLE_DISTRITO = "SORACARE_SIFEN_DISTRITO";
    public static final String VARIABLE_DISTRITO_DESCRIPCION =
            "SORACARE_SIFEN_DISTRITO_DESCRIPCION";
    public static final String VARIABLE_CIUDAD = "SORACARE_SIFEN_CIUDAD";
    public static final String VARIABLE_CIUDAD_DESCRIPCION =
            "SORACARE_SIFEN_CIUDAD_DESCRIPCION";
    public static final String VARIABLE_TELEFONO = "SORACARE_SIFEN_TELEFONO";
    public static final String VARIABLE_EMAIL = "SORACARE_SIFEN_EMAIL";
    public static final String VARIABLE_ACTIVIDAD_CODIGO =
            "SORACARE_SIFEN_ACTIVIDAD_CODIGO";
    public static final String VARIABLE_ACTIVIDAD_DESCRIPCION =
            "SORACARE_SIFEN_ACTIVIDAD_DESCRIPCION";
    public static final String VARIABLE_INFORMACION_EMISOR =
            "SORACARE_SIFEN_INFORMACION_EMISOR";
    public static final String VARIABLE_HOMOLOGACION_CONFIRMADA =
            "SORACARE_SIFEN_HOMOLOGACION_CONFIRMADA";

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
        requerir(VARIABLE_FECHA_INICIO_TIMBRADO, "fecha de inicio del timbrado", pendientes);
        requerir(VARIABLE_TIPO_CONTRIBUYENTE, "tipo de contribuyente del emisor", pendientes);
        requerir(VARIABLE_RAZON_SOCIAL, "razón social del emisor", pendientes);
        requerir(VARIABLE_DIRECCION, "dirección fiscal del emisor", pendientes);
        requerir(VARIABLE_NUMERO_CASA, "número de casa del emisor", pendientes);
        requerir(VARIABLE_DEPARTAMENTO, "código de departamento del emisor", pendientes);
        requerir(VARIABLE_DEPARTAMENTO_DESCRIPCION, "departamento del emisor", pendientes);
        requerir(VARIABLE_CIUDAD, "código de ciudad del emisor", pendientes);
        requerir(VARIABLE_CIUDAD_DESCRIPCION, "ciudad del emisor", pendientes);
        requerir(VARIABLE_TELEFONO, "teléfono del emisor", pendientes);
        requerir(VARIABLE_EMAIL, "correo del emisor", pendientes);
        requerir(VARIABLE_ACTIVIDAD_CODIGO, "código de actividad económica", pendientes);
        requerir(VARIABLE_ACTIVIDAD_DESCRIPCION, "actividad económica", pendientes);
        validarFormatos(pendientes);
        validarDistrito(pendientes);
        validarCertificado(pendientes);
        if (ambiente == EstadoPreparacionSifenDTO.Ambiente.PRODUCCION
                && !"SI".equalsIgnoreCase(valor(VARIABLE_HOMOLOGACION_CONFIRMADA))) {
            pendientes.add("homologación DNIT confirmada para producción");
        }

        return new EstadoPreparacionSifenDTO(ambiente, true, pendientes);
    }

    public ConfiguracionFiscalSifen cargarConfiguracion() {
        EstadoPreparacionSifenDTO estado = consultarEstado();
        if (!estado.configuracionCompleta()) {
            throw new IllegalArgumentException(
                    "La configuración SIFEN no está completa para preparar el documento."
            );
        }
        Integer distrito = enteroOpcional(VARIABLE_DISTRITO);
        EmisorSifen emisor = new EmisorSifen(
                valor(VARIABLE_RUC),
                entero(VARIABLE_DV),
                entero(VARIABLE_TIPO_CONTRIBUYENTE),
                enteroOpcional(VARIABLE_TIPO_REGIMEN),
                valor(VARIABLE_RAZON_SOCIAL),
                valor(VARIABLE_NOMBRE_FANTASIA),
                valor(VARIABLE_DIRECCION),
                valor(VARIABLE_NUMERO_CASA),
                entero(VARIABLE_DEPARTAMENTO),
                valor(VARIABLE_DEPARTAMENTO_DESCRIPCION),
                distrito,
                distrito == null ? null : valor(VARIABLE_DISTRITO_DESCRIPCION),
                entero(VARIABLE_CIUDAD),
                valor(VARIABLE_CIUDAD_DESCRIPCION),
                valor(VARIABLE_TELEFONO),
                valor(VARIABLE_EMAIL),
                valor(VARIABLE_ACTIVIDAD_CODIGO),
                valor(VARIABLE_ACTIVIDAD_DESCRIPCION)
        );
        return new ConfiguracionFiscalSifen(
                estado.ambiente(),
                valor(VARIABLE_TIMBRADO),
                LocalDate.parse(valor(VARIABLE_FECHA_INICIO_TIMBRADO)),
                valor(VARIABLE_ESTABLECIMIENTO),
                valor(VARIABLE_PUNTO_EXPEDICION),
                emisor,
                Path.of(valor(VARIABLE_CERTIFICADO_PATH)),
                valor(VARIABLE_CERTIFICADO_PASSWORD).toCharArray(),
                valor(VARIABLE_CSC_ID),
                valor(VARIABLE_CSC).toCharArray(),
                valor(VARIABLE_INFORMACION_EMISOR)
        );
    }

    private void validarFormatos(List<String> pendientes) {
        formato(VARIABLE_RUC, "[0-9]{3,8}", "RUC del emisor con 3 a 8 dígitos", pendientes);
        formato(VARIABLE_DV, "[0-9]", "DV del RUC con un dígito", pendientes);
        formato(VARIABLE_TIMBRADO, "[0-9]{8}", "timbrado con 8 dígitos", pendientes);
        formato(VARIABLE_ESTABLECIMIENTO, "[0-9]{1,3}", "establecimiento con hasta 3 dígitos", pendientes);
        formato(VARIABLE_PUNTO_EXPEDICION, "[0-9]{1,3}", "punto de expedición con hasta 3 dígitos", pendientes);
        formato(VARIABLE_CSC_ID, "[0-9]{4}", "IdCSC con 4 dígitos", pendientes);
        formato(VARIABLE_CSC, ".{32}", "CSC con 32 caracteres", pendientes);
        formato(VARIABLE_TIPO_CONTRIBUYENTE, "[12]", "tipo de contribuyente 1 o 2", pendientes);
        formato(VARIABLE_TIPO_REGIMEN, "[0-9]{1,2}", "tipo de régimen válido", pendientes);
        formato(VARIABLE_DEPARTAMENTO, "[0-9]{1,2}", "código de departamento válido", pendientes);
        formato(VARIABLE_CIUDAD, "[0-9]{1,5}", "código de ciudad válido", pendientes);
        String fecha = valor(VARIABLE_FECHA_INICIO_TIMBRADO);
        if (fecha != null) {
            try {
                LocalDate.parse(fecha);
            } catch (DateTimeParseException error) {
                pendientes.add("fecha de inicio del timbrado en formato AAAA-MM-DD");
            }
        }
    }

    private void validarDistrito(List<String> pendientes) {
        boolean tieneCodigo = valor(VARIABLE_DISTRITO) != null;
        boolean tieneDescripcion = valor(VARIABLE_DISTRITO_DESCRIPCION) != null;
        if (tieneCodigo != tieneDescripcion) {
            pendientes.add("código y descripción de distrito informados juntos");
        }
        if (tieneCodigo) {
            formato(VARIABLE_DISTRITO, "[0-9]{1,5}", "código de distrito válido", pendientes);
        }
    }

    private void formato(
            String variable,
            String expresion,
            String descripcion,
            List<String> pendientes
    ) {
        String contenido = valor(variable);
        if (contenido != null && !contenido.matches(expresion)) {
            pendientes.add(descripcion);
        }
    }

    private int entero(String variable) {
        return Integer.parseInt(valor(variable));
    }

    private Integer enteroOpcional(String variable) {
        String contenido = valor(variable);
        return contenido == null ? null : Integer.valueOf(contenido);
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
