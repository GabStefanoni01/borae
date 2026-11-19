package com.borae.config;

import com.borae.security.ClientIpResolver;
import com.borae.security.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento global de exceções da API.
 *
 * <p>Padroniza respostas de erro e garante que stack traces, instruções SQL, nomes de tabelas,
 * caminhos internos do servidor ou detalhes de infraestrutura jamais sejam expostos ao cliente.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final SecurityAuditLogger auditLogger;
    private final ClientIpResolver ipResolver;

    public ApiExceptionHandler(SecurityAuditLogger auditLogger, ClientIpResolver ipResolver) {
        this.auditLogger = auditLogger;
        this.ipResolver = ipResolver;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        int status = exception.getStatusCode().value();
        String reason = exception.getReason();
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            auditLogger.logSecurityEvent(
                    "MEDIA",
                    status == 401 ? "auth_rejected" : "access_denied",
                    request.getMethod(),
                    request.getRequestURI(),
                    ipResolver.resolve(request),
                    true,
                    reason != null ? reason : "Acesso negado"
            );
        }
        String safeMessage = (reason != null && !reason.isBlank())
                ? reason
                : defaultMessageForStatus(status);
        return buildResponse(status, errorCodeForStatus(status), safeMessage);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        auditLogger.logSecurityEvent(
                "MEDIA",
                "payload_too_large",
                request.getMethod(),
                request.getRequestURI(),
                ipResolver.resolve(request),
                false,
                "Upload excedeu o tamanho máximo permitido."
        );
        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "payload_too_large",
                "O arquivo enviado excede o tamanho máximo permitido."
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(
            MultipartException exception,
            HttpServletRequest request
    ) {
        if (exception.getCause() instanceof IllegalStateException
                && exception.getMessage() != null
                && exception.getMessage().toLowerCase().contains("size")) {
            return handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(-1L, exception), request);
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST.value(),
                "bad_request",
                "Requisição multipart inválida."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        FieldError firstError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = "Dados da requisição inválidos.";
        if (firstError != null && firstError.getDefaultMessage() != null && !firstError.getDefaultMessage().isBlank()) {
            message = "Campo '" + firstError.getField() + "': " + firstError.getDefaultMessage();
        }
        return buildResponse(HttpStatus.BAD_REQUEST.value(), "validation_error", message);
    }

    @ExceptionHandler({
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequestExceptions(Exception exception) {
        return buildResponse(
                HttpStatus.BAD_REQUEST.value(),
                "bad_request",
                "Dados ou parâmetros da requisição inválidos."
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "method_not_allowed",
                "Método HTTP não suportado para este recurso."
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "unsupported_media_type",
                "Tipo de conteúdo (Content-Type) não suportado."
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        auditLogger.logSecurityEvent(
                "MEDIA",
                "unauthenticated",
                request.getMethod(),
                request.getRequestURI(),
                ipResolver.resolve(request),
                true,
                "Tentativa de acesso sem autenticação válida."
        );
        return buildResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "unauthorized",
                "Autenticação obrigatória."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        auditLogger.logSecurityEvent(
                "MEDIA",
                "forbidden",
                request.getMethod(),
                request.getRequestURI(),
                ipResolver.resolve(request),
                true,
                "Acesso negado por permissão insuficiente."
        );
        return buildResponse(
                HttpStatus.FORBIDDEN.value(),
                "forbidden",
                "Você não tem permissão para realizar esta operação."
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        auditLogger.logInternalError(request.getMethod(), request.getRequestURI(), ipResolver.resolve(request), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "internal_error",
                "Erro interno do servidor."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        auditLogger.logInternalError(request.getMethod(), request.getRequestURI(), ipResolver.resolve(request), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "internal_error",
                "Erro interno do servidor."
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", SecurityAuditLogger.redactSensitive(message));
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }

    private static String errorCodeForStatus(int status) {
        return switch (status) {
            case 400 -> "bad_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 409 -> "conflict";
            case 410 -> "gone";
            case 413 -> "payload_too_large";
            case 429 -> "rate_limit";
            case 503 -> "service_unavailable";
            default -> status >= 500 ? "internal_error" : "request_error";
        };
    }

    private static String defaultMessageForStatus(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida.";
            case 401 -> "Autenticação obrigatória.";
            case 403 -> "Acesso negado.";
            case 404 -> "Recurso não encontrado.";
            case 409 -> "Conflito ao processar a solicitação.";
            case 410 -> "Recurso expirado ou indisponível.";
            case 413 -> "O conteúdo enviado excede o tamanho máximo permitido.";
            case 429 -> "Muitas solicitações. Aguarde antes de tentar novamente.";
            default -> "Erro interno do servidor.";
        };
    }
}
