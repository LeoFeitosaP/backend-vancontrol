package com.VanControl.VanControl.endereco.exceptions;

import com.VanControl.VanControl.common.exception.ErrorDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EnderecoExceptionHandler {

    @ExceptionHandler(EnderecoException.class)
    public ResponseEntity<ErrorDetails> tratar(EnderecoException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorDetails(ex.getMessage()));
    }
}