package br.com.lumyra.service.impl;

import br.com.lumyra.domain.Usuario;
import br.com.lumyra.domain.enums.Perfil;
import br.com.lumyra.dto.requisicao.RequisicaoCriarCliente;
import br.com.lumyra.dto.requisicao.RequisicaoLogin;
import br.com.lumyra.dto.resposta.RespostaAutenticacao;
import br.com.lumyra.dto.resposta.RespostaUsuario;
import br.com.lumyra.exception.ExcecaoNegocio;
import br.com.lumyra.exception.RecursoNaoEncontradoException;
import br.com.lumyra.repository.UsuarioRepositorio;
import br.com.lumyra.service.ServicoAutenticacao;
import br.com.lumyra.service.ServicoJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicoAutenticacaoImpl implements ServicoAutenticacao {

    private final UsuarioRepositorio usuarioRepositorio;
    private final ServicoJwt servicoJwt;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RespostaAutenticacao autenticar(RequisicaoLogin requisicao) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(requisicao.email(), requisicao.senha())
        );

        var usuario = usuarioRepositorio.buscarPorEmail(requisicao.email())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        var detalhes = userDetailsService.loadUserByUsername(usuario.getEmail());
        var token = servicoJwt.gerarToken(detalhes, Map.of("perfil", usuario.getPerfil().name()));

        return new RespostaAutenticacao(token, RespostaUsuario.de(usuario));
    }

    @Override
    @Transactional
    public RespostaUsuario criarCliente(RequisicaoCriarCliente requisicao, String emailProfissional) {
        if (usuarioRepositorio.existePorEmail(requisicao.email())) {
            throw new ExcecaoNegocio("Email já em uso");
        }

        var profissional = usuarioRepositorio.buscarPorEmail(emailProfissional)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado"));

        var senhaTemporaria = passwordEncoder.encode(UUID.randomUUID().toString());

        var cliente = Usuario.builder()
            .nome(requisicao.nome())
            .email(requisicao.email())
            .senha(senhaTemporaria)
            .perfil(Perfil.CLIENTE)
            .profissional(profissional)
            .build();

        return RespostaUsuario.de(usuarioRepositorio.save(cliente));
    }
}
