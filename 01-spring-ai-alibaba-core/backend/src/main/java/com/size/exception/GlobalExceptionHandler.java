package com.size.exception;

import cn.hutool.core.util.IdUtil;
import com.size.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation (MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数不合法" : error.getDefaultMessage())
                .orElse("请求参数不合法");

        return ResponseEntity.badRequest().body(
                new ApiError(IdUtil.fastSimpleUUID(), "INVALID_REQUEST", message, Instant.now())
        );

    }

    @ExceptionHandler(value = ModelCallException.class)
    public ResponseEntity<ApiError> handleModelCallException(ModelCallException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                new ApiError(IdUtil.fastSimpleUUID(), "MODEL_CALL_FAILED", exception.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(ToolOperationException.class)
    public ResponseEntity<ApiError> handleToolOperation(
            ToolOperationException exception) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        IdUtil.fastSimpleUUID(),
                        "TOOL_OPERATION_FAILED",
                        exception.getMessage(),
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(KnowledgeBaseException.class)
    public ResponseEntity<ApiError> handleKnowledgeBase(
            KnowledgeBaseException exception) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        IdUtil.fastSimpleUUID(),
                        "KNOWLEDGE_BASE_FAILED",
                        exception.getMessage(),
                        Instant.now()
                )
        );
    }
}
