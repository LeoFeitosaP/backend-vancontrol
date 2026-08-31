package com.VanControl.VanControl.common.exception;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.InternalServerErrorException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDetails> handleNotFoundException(NotFoundException ex) {
        return new ResponseEntity<>(new ErrorDetails(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDetails> handleConflictException(ConflictException ex) {
         return new ResponseEntity<>(new ErrorDetails(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(new ErrorDetails(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(new ErrorDetails("Dados já cadastrados"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ErrorDetails> handleInternalServerErrorException(InternalServerErrorException ex) {
        return new ResponseEntity<>(new ErrorDetails(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
