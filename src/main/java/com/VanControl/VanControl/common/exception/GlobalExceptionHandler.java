package com.VanControl.VanControl.common.exception;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.InternalServerErrorException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDetails> handleNotFoundException(
            NotFoundException ex
    ) {
        return new ResponseEntity<>(
                new ErrorDetails(ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDetails> handleConflictException(
            ConflictException ex
    ) {
        return new ResponseEntity<>(
                new ErrorDetails(ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequestException(
            BadRequestException ex
    ) {
        return new ResponseEntity<>(
                new ErrorDetails(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + (error.getDefaultMessage() == null
                        ? "Valor inválido"
                        : error.getDefaultMessage()))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));

        if (mensagem.isBlank()) {
            mensagem = "Dados de entrada inválidos";
        }

        return new ResponseEntity<>(
                new ErrorDetails(mensagem),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex
    ) {
        return new ResponseEntity<>(
                new ErrorDetails("Dados já cadastrados"),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ErrorDetails> handleInternalServerErrorException(
            InternalServerErrorException ex
    ) {
        return new ResponseEntity<>(
                new ErrorDetails(ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}