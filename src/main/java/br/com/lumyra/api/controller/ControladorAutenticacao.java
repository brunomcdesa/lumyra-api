package br.com.lumyra.api.controller;

import br.com.lumyra.dto.requisicao.RequisicaoCriarCliente;
import br.com.lumyra.dto.requisicao.RequisicaoLogin;
import br.com.lumyra.dto.resposta.RespostaAutenticacao;
import br.com.lumyra.dto.resposta.RespostaUsuario;
import br.com.lumyra.service.ServicoAutenticacao;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/autenticacao")
@RequiredArgsConstructor
public class ControladorAutenticacao {

    private final ServicoAutenticacao servicoAutenticacao;

    @PostMapping("/login")
    public ResponseEntity<RespostaAutenticacao> login(@Valid @RequestBody RequisicaoLogin requisicao) {
        return ResponseEntity.ok(servicoAutenticacao.autenticar(requisicao));
    }

    @PostMapping("/clientes")
    @PreAuthorize("hasRole('PROFISSIONAL')")
    public ResponseEntity<RespostaUsuario> criarCliente(
        @Valid @RequestBody RequisicaoCriarCliente requisicao,
        Authentication authentication
    ) {
        var resposta = servicoAutenticacao.criarCliente(requisicao, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
