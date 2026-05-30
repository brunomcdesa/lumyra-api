package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega o usuário autenticado pelo e-mail, seja profissional ou aluna.
 *
 * <p>Profissional tem prioridade na resolução; se não houver, tenta a aluna
 * (que só autentica após aceitar o convite e definir senha — {@code senhaHash}
 * preenchida — e enquanto estiver ativa). É {@link Primary} porque o
 * {@code FiltroAutenticacaoJwt} injeta {@link UserDetailsService} por tipo.
 */
@Service
@Primary
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final ProfessionalRepositorio professionalRepositorio;
    private final StudentRepositorio studentRepositorio;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var professional = professionalRepositorio.findByEmail(email);
        if (professional.isPresent()) {
            return User.builder()
                .username(professional.get().getEmail())
                .password(professional.get().getSenhaHash())
                .roles("PROFESSIONAL")
                .build();
        }

        var student = studentRepositorio.findByEmail(email)
            .filter(s -> s.getSenhaHash() != null && Boolean.TRUE.equals(s.getAtivo()))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return User.builder()
            .username(student.getEmail())
            .password(student.getSenhaHash())
            .roles("STUDENT")
            .build();
    }
}
