package com.example.tmta.exception;

import com.example.tmta.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 또는 @Validated 로 binding error 발생시 발생하는 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);
        final String message = resolveValidationMessage(e.getBindingResult().getFieldErrors(), e.getBindingResult().getGlobalErrors());
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * @ModelAttribute 바인딩 검증 실패 예외 처리
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("handleBindException", e);
        final String message = resolveValidationMessage(e.getBindingResult().getFieldErrors(), e.getBindingResult().getGlobalErrors());
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 우리가 정의한 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        log.error("handleBusinessException", e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * 위에 지정되지 않은 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String resolveValidationMessage(java.util.List<FieldError> fieldErrors, java.util.List<ObjectError> globalErrors) {
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            String message = fieldErrors.get(0).getDefaultMessage();
            return (message == null || message.isBlank()) ? ErrorCode.INVALID_INPUT_VALUE.getMessage() : message;
        }
        if (globalErrors != null && !globalErrors.isEmpty()) {
            String message = globalErrors.get(0).getDefaultMessage();
            return (message == null || message.isBlank()) ? ErrorCode.INVALID_INPUT_VALUE.getMessage() : message;
        }
        return ErrorCode.INVALID_INPUT_VALUE.getMessage();
    }
}
