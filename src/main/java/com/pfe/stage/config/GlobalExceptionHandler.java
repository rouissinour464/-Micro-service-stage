package com.pfe.stage.config;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /* ───────────────── 400 : erreurs de logique ───────────────── */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /* ───────────────── 409 : conflits métier ───────────────── */

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex
    ) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /* ───────────────── 403 : accès refusé ───────────────── */

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        return buildError(HttpStatus.FORBIDDEN, "Accès refusé.");
    }

    /* ───────────────── 404 : ressource introuvable ───────────────── */

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            EntityNotFoundException ex
    ) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /* ───────────────── 400 : validation @Valid ───────────────── */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation échouée");
        body.put("details", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    /* ───────────────── 400 : validation paramètres (@Validated) ───────────────── */

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /* ───────────────── 500 : erreur inconnue ───────────────── */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex
    ) {
        log.error("Erreur serveur inattendue", ex);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne s'est produite."
        );
    }

    /* ───────────────── Méthode utilitaire ───────────────── */

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
