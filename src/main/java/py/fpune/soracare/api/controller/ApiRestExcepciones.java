package py.fpune.soracare.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import py.fpune.soracare.api.dto.ErrorApiDTO;
import py.fpune.soracare.service.SentryMonitoreoService;

import java.sql.SQLException;

@RestControllerAdvice
public class ApiRestExcepciones {

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

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorApiDTO> manejarBaseDatos(
            SQLException error,
            HttpServletRequest request
    ) {
        capturar(error, request);
        return respuesta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "BASE_DATOS_NO_DISPONIBLE",
                "No fue posible consultar la información en este momento."
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
