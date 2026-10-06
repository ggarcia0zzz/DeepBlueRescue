package com.deepblue.rescue.exception;

import com.deepblue.rescue.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404: es más específica que BusinessException, por eso Spring la elige primero
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // 409: cualquier otra regla de negocio (duplicados, transiciones, especialista inactivo...)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 400: @Valid sobre el @RequestBody falló
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first));
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    // 400: JSON mal formado o valor de enum inexistente en el body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax and enum values"));
    }

    // 400: query param o path variable con tipo inválido (?status=FLYING, /animals/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter",
                Map.of(ex.getName(), "Invalid value: " + ex.getValue()));
    }

    // Último recurso. Spring MVC lanza muchas excepciones propias (falta un query param,
    // ruta inexistente, método HTTP no permitido...) que ya traen su código HTTP. Si las
    // dejáramos caer como 500 estaríamos mintiendo, así que se respeta su código.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse webException) {
            HttpStatus status = HttpStatus.valueOf(webException.getStatusCode().value());
            return build(status, webException.getBody().getDetail(), Map.of());
        }

        // Se registra en el log, pero NO se expone al cliente (sin stack trace, SQL ni detalles internos)
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                Map<String, String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details));
    }
}
