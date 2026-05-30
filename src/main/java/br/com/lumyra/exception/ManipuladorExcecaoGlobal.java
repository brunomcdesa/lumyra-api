package br.com.lumyra.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
@RestControllerAdvice
public class ManipuladorExcecaoGlobal {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<RespostaErro> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new RespostaErro(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ExcecaoNegocio.class)
    public ResponseEntity<RespostaErro> tratarNegocio(ExcecaoNegocio ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new RespostaErro(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
            .map(ee -> ee.getField() + ": " + ee.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new RespostaErro(HttpStatus.BAD_REQUEST.value(), mensagem, LocalDateTime.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RespostaErro> tratarCredenciaisInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new RespostaErro(HttpStatus.UNAUTHORIZED.value(), "Credenciais inválidas", LocalDateTime.now()));
    }

    @ExceptionHandler(ConsentimentoAusenteException.class)
    public ResponseEntity<RespostaErro> tratarConsentimentoAusente(ConsentimentoAusenteException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new RespostaErro(HttpStatus.FORBIDDEN.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErro> tratarAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new RespostaErro(HttpStatus.FORBIDDEN.value(), "Acesso negado", LocalDateTime.now()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<RespostaErro> tratarRateLimit(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new RespostaErro(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage(),
                LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> tratarErroGenerico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RespostaErro(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno do servidor", LocalDateTime.now()));
    }
}
