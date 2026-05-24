package br.com.lumyra.service;

import br.com.lumyra.domain.Usuario;
import br.com.lumyra.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioDetalhesServico implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.buscarPorEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        return new User(
            usuario.getEmail(),
            usuario.getSenha(),
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()))
        );
    }
}
