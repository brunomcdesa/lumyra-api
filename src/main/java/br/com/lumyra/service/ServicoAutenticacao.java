package br.com.lumyra.service;

import br.com.lumyra.dto.requisicao.RequisicaoCriarCliente;
import br.com.lumyra.dto.requisicao.RequisicaoLogin;
import br.com.lumyra.dto.resposta.RespostaAutenticacao;
import br.com.lumyra.dto.resposta.RespostaUsuario;

public interface ServicoAutenticacao {

    RespostaAutenticacao autenticar(RequisicaoLogin requisicao);

    RespostaUsuario criarCliente(RequisicaoCriarCliente requisicao, String emailProfissional);
}
