package py.fpune.soracare.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import py.fpune.soracare.api.dto.ErrorApiDTO;
import py.fpune.soracare.api.security.ApiAccesoDenegadoException;
import py.fpune.soracare.api.security.ApiNoAutenticadoException;
import py.fpune.soracare.api.security.ApiRecursoNoEncontradoException;
import py.fpune.soracare.service.SentryMonitoreoService;

import java.io.IOException;
import java.sql.SQLException;

@RestControllerAdvice
public class ApiRestExcepciones {

    @ExceptionHandler(ApiNoAutenticadoException.class)
    public ResponseEntity<ErrorApiDTO> manejarNoAutenticado(
            ApiNoAutenticadoException error
    ) {
        return respuesta(
                HttpStatus.UNAUTHORIZED,
                "NO_AUTENTICADO",
                error.getMessage()
        );
    }

    @ExceptionHandler(ApiAccesoDenegadoException.class)
    public ResponseEntity<ErrorApiDTO> manejarAccesoDenegado(
            ApiAccesoDenegadoException error
    ) {
        return respuesta(
                HttpStatus.FORBIDDEN,
                "ACCESO_DENEGADO",
                error.getMessage()
        );
    }

    @ExceptionHandler(ApiRecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApiDTO> manejarRecursoNoEncontrado(
            ApiRecursoNoEncontradoException error
    ) {
        return respuesta(
                HttpStatus.NOT_FOUND,
                "RECURSO_NO_ENCONTRADO",
                error.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApiDTO> manejarValidacion(
            IllegalArgumentException error
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                error.getMessage()
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorApiDTO> manejarSolicitudIlegible(
            RuntimeException error
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                "El contenido de la solicitud no posee un formato válido."
        );
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorApiDTO> manejarBaseDatos(
            SQLException error,
            HttpServletRequest request
    ) {
        capturar(error, request);
        return respuesta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "BASE_DATOS_NO_DISPONIBLE",
                "No fue posible procesar la solicitud en este momento."
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorApiDTO> manejarArchivo(
            IOException error,
            HttpServletRequest request
    ) {
        capturar(error, request);
        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ARCHIVO_NO_DISPONIBLE",
                "No fue posible abrir el archivo solicitado."
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorApiDTO> manejarErrorInterno(
            RuntimeException error,
            HttpServletRequest request
    ) {
        capturar(error, request);
        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERROR_INTERNO",
                "Ocurrió un error interno al procesar la solicitud."
        );
    }

    private static ResponseEntity<ErrorApiDTO> respuesta(
            HttpStatus estado,
            String codigo,
            String mensaje
    ) {
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorApiDTO(estado.value(), codigo, mensaje));
    }

    private static void capturar(
            Throwable error,
            HttpServletRequest request
    ) {
        SentryMonitoreoService.capturar(
                error,
                request.getMethod(),
                request.getRequestURI()
        );
    }
}
