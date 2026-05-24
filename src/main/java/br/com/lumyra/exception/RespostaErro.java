package br.com.lumyra.exception;

import java.time.LocalDateTime;

public record RespostaErro(int status, String mensagem, LocalDateTime timestamp) {}
