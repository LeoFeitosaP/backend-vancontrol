package com.VanControl.VanControl.endereco.exceptions;

import org.springframework.http.HttpStatus;

public class EnderecoException extends RuntimeException {

    private final HttpStatus status;

    public EnderecoException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}