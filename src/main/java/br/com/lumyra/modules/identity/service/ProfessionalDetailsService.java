package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessionalDetailsService implements UserDetailsService {

    private final ProfessionalRepositorio professionalRepositorio;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var professional = professionalRepositorio.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Profissional não encontrado"));

        return User.builder()
            .username(professional.getEmail())
            .password(professional.getSenhaHash())
            .roles("PROFESSIONAL")
            .build();
    }
}
