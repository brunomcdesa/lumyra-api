package br.com.lumyra.exception;

public class ExcecaoNegocio extends RuntimeException {

    public ExcecaoNegocio(String mensagem) {
        super(mensagem);
    }
}
