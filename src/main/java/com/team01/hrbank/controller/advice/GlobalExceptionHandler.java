package com.team01.hrbank.controller.advice;

import com.team01.hrbank.dto.error.ErrorResponse;
import com.team01.hrbank.exception.BackupFailedException;
import com.team01.hrbank.exception.DuplicateException;
import com.team01.hrbank.exception.EntityNotFoundException;
import com.team01.hrbank.exception.InvalidSortParameterException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 400: 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult()
            .getFieldError()
            .getDefaultMessage();
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청입니다.",
            details
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    // 500: 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleServerError(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "서버 오류입니다.",
            ex.getMessage()
        );
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }

    // 400: 비즈니스 로직 검증 실패 시 발생하는 예외 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청입니다.",
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // 400: 등록하는 것이 이미 존재할 경우 발생하는 예외 처리
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청입니다.",
            ex.getMessage()
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "찾을 수 없습니다.",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Enum 파라미터 에러
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청 형식입니다. 파라미터를 확인해주세요.",
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // 400 error
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "요청 파라미터가 잘못되었습니다.",
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }


    @ExceptionHandler(InvalidSortParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(InvalidSortParameterException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청입니다.",
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // 400 : 타입 매치 에러
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "잘못된 요청입니다.",
            ex.getName() +  " : " + Objects.requireNonNull(ex.getValue())
        );
        return ResponseEntity.badRequest().body(error);
    }
    @ExceptionHandler(BackupFailedException.class)
    public ResponseEntity<ErrorResponse> handleBackupFailed(BackupFailedException ex) {

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "백업 작업 실패",
            ex.getMessage()

        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
