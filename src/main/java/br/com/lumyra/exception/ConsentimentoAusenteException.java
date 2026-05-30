package br.com.lumyra.exception;

/**
 * Lançada quando a aluna tenta acessar um recurso de dado sensível sem
 * consentimento LGPD ativo (G2). Mapeada para HTTP 403.
 */
public class ConsentimentoAusenteException extends RuntimeException {

    public ConsentimentoAusenteException() {
        super("Consentimento LGPD ativo é obrigatório para esta operação");
    }
}
