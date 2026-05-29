package br.com.lumyra.exception;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("Muitas tentativas. Tente novamente em instantes.");
    }
}
