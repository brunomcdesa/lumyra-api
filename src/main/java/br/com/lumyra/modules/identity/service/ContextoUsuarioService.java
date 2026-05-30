package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve o usuário autenticado (profissional ou aluna) a partir do e-mail no
 * contexto de segurança. O {@code FiltroAutenticacaoJwt} coloca o e-mail como
 * principal e já injetou o tenant no contexto de RLS.
 */
@Service
@RequiredArgsConstructor
public class ContextoUsuarioService {

    private final ProfessionalRepositorio professionalRepositorio;
    private final StudentRepositorio studentRepositorio;

    public String emailAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BadCredentialsException("Não autenticado");
        }
        return auth.getName();
    }

    @Transactional(readOnly = true)
    public Professional profissionalAtual() {
        return professionalRepositorio.findByEmail(emailAtual())
            .orElseThrow(() -> new BadCredentialsException("Profissional não encontrado"));
    }

    @Transactional(readOnly = true)
    public Student alunaAtual() {
        return studentRepositorio.findByEmail(emailAtual())
            .orElseThrow(() -> new BadCredentialsException("Aluna não encontrada"));
    }
}
