package br.com.lumyra.dto.resposta;

import br.com.lumyra.domain.Usuario;
import br.com.lumyra.domain.enums.Perfil;

public record RespostaUsuario(Integer id, String nome, String email, Perfil perfil) {

    public static RespostaUsuario de(Usuario usuario) {
        return new RespostaUsuario(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil()
        );
    }
}
