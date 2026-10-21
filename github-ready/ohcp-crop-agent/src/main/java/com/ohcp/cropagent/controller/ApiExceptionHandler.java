package com.ohcp.cropagent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + (error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage()))
                .toList();
        String message = details.isEmpty() ? "请求参数不合法" : String.join("；", details);
        return error(HttpStatus.BAD_REQUEST, message, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        String message = "请求体不是有效 JSON，或字段类型不正确。请用 application/json 发送，并确认 crop、part、symptoms 都是字符串。";
        return error(HttpStatus.BAD_REQUEST, message, List.of(message));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        String message = "请求头必须是 Content-Type: application/json。";
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, List.of(message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissingResource(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "请求的资源不存在", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "请求参数不合法" : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, List.of(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "服务暂时不可用：" + exception.getMessage(),
                List.of());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, List<String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
