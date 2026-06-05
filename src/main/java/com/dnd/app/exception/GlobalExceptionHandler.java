package com.dnd.app.exception;

import com.dnd.app.config.RequestLoggingFilter;
import com.dnd.app.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logControllerException(HttpStatus.NOT_FOUND, "NOT_FOUND", ex, request, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logControllerException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex, request, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        logControllerException(HttpStatus.CONFLICT, "DUPLICATE", ex, request, null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        logControllerException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex, request, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnprocessable(UnprocessableEntityException ex, HttpServletRequest request) {
        logControllerException(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", ex, request, null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("UNPROCESSABLE_ENTITY", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        logControllerException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", ex, request, "bad credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("BAD_CREDENTIALS", "Неверное имя пользователя или пароль"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String details = "method=" + ex.getMethod() + ", supported=" + ex.getSupportedHttpMethods();
        logControllerException(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex, request, details);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String paramName = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String details = "param=" + paramName + ", value=" + value + ", expectedType=" + requiredType;
        logControllerException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex, request, details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST",
                        String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                                value, paramName, requiredType)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        logControllerException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex, request, "fields=" + fields);
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("Validation failed", fields));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex, HttpServletRequest request) {
        logControllerException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex, request, null);
        logDebugStackTrace(ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private void logControllerException(
            HttpStatus status,
            String errorCode,
            Exception ex,
            HttpServletRequest request,
            String details) {
        String requestId = attributeAsString(request, RequestLoggingFilter.REQUEST_ID_ATTRIBUTE, "-");
        String handler = resolveHandler(request);
        String user = currentUsername(request);
        String message = messageFor(ex);
        String rootCause = describeRootCause(ex);
        String compactDetails = compact(details);

        if (status.is5xxServerError()) {
            log.error(
                    "Controller exception: id={}, status={}, error={}, handler={}, method={}, path={}, user={}, exception={}, message='{}', rootCause='{}', details='{}'",
                    requestId, status.value(), errorCode, handler, request.getMethod(), buildPath(request),
                    user, ex.getClass().getSimpleName(), message, rootCause, compactDetails);
        } else {
            log.warn(
                    "Controller exception: id={}, status={}, error={}, handler={}, method={}, path={}, user={}, exception={}, message='{}', rootCause='{}', details='{}'",
                    requestId, status.value(), errorCode, handler, request.getMethod(), buildPath(request),
                    user, ex.getClass().getSimpleName(), message, rootCause, compactDetails);
        }
    }

    private void logDebugStackTrace(Exception ex, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            String requestId = attributeAsString(request, RequestLoggingFilter.REQUEST_ID_ATTRIBUTE, "-");
            log.debug("Controller exception stack trace: id={}", requestId, ex);
        }
    }

    private String resolveHandler(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod method) {
            return method.getBeanType().getSimpleName() + "#" + method.getMethod().getName();
        }
        return "-";
    }

    private String buildPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String currentUsername(HttpServletRequest request) {
        Object authenticatedUsername = request.getAttribute("authenticatedUsername");
        if (authenticatedUsername instanceof String username && !username.isBlank()) {
            return username;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
    }

    private String describeRootCause(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root == ex) {
            return "-";
        }
        return root.getClass().getSimpleName() + ": " + compact(root.getMessage());
    }

    private String messageFor(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            return "Validation failed";
        }
        return compact(ex.getMessage());
    }

    private String attributeAsString(HttpServletRequest request, String attribute, String fallback) {
        Object value = request.getAttribute(attribute);
        return value == null ? fallback : value.toString();
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String singleLine = value.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 300 ? singleLine : singleLine.substring(0, 300) + "...";
    }
}
