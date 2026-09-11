package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.DocumentoClinicoDTO;

public record DocumentoClinicoPacienteApiDTO(
        long idDocumento,
        String codigoTipo,
        String tipo,
        String nombreArchivo,
        String tipoMime,
        long tamanoBytes,
        String descripcion,
        String origen,
        String fechaDocumento,
        String fechaCreacion,
        String cargadoPor
) {

    public static DocumentoClinicoPacienteApiDTO desde(
            DocumentoClinicoDTO documento
    ) {
        return new DocumentoClinicoPacienteApiDTO(
                documento.idDocumento(),
                documento.codigoTipo(),
                documento.nombreTipo(),
                documento.nombreOriginal(),
                documento.tipoMime(),
                documento.tamanoBytes(),
                documento.descripcion(),
                documento.origen(),
                texto(documento.fechaDocumento()),
                texto(documento.fechaCreacion()),
                documento.cargadoPor()
        );
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
