package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.core.rls.TenantContextHolder;
import br.com.lumyra.exception.ExcecaoNegocio;
import br.com.lumyra.modules.identity.dto.AceitarConviteRequest;
import br.com.lumyra.modules.identity.dto.AuthResponse;
import br.com.lumyra.modules.identity.dto.InviteRequest;
import br.com.lumyra.modules.identity.dto.InviteResponse;
import br.com.lumyra.modules.identity.entity.Invite;
import br.com.lumyra.modules.identity.entity.InviteStatus;
import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.ProfessionalStudentLink;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.InviteRepositorio;
import br.com.lumyra.modules.identity.repository.ProfessionalStudentLinkRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import br.com.lumyra.modules.identity.service.ContextoUsuarioService;
import br.com.lumyra.modules.identity.service.InviteNotifier;
import br.com.lumyra.modules.identity.service.InviteService;
import br.com.lumyra.modules.identity.service.TokenConviteUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements InviteService {

    private final InviteRepositorio inviteRepositorio;
    private final StudentRepositorio studentRepositorio;
    private final ProfessionalStudentLinkRepositorio linkRepositorio;
    private final ContextoUsuarioService contextoUsuario;
    private final InviteNotifier inviteNotifier;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceImpl authService;

    @Value("${app.invite.expiration-hours:72}")
    private long expirationHours;

    @Value("${app.invite.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public InviteResponse convidar(InviteRequest request) {
        Professional profissional = contextoUsuario.profissionalAtual();
        var tenant = profissional.getTenant();

        Student aluna = resolverAluna(tenant, request);
        garantirVinculo(profissional, aluna);

        String tokenBruto = TokenConviteUtil.gerar(tenant.getId());
        OffsetDateTime expiraEm = OffsetDateTime.now().plusHours(expirationHours);
        Invite invite = inviteRepositorio.save(Invite.builder()
            .tenant(tenant)
            .student(aluna)
            .email(request.email())
            .tokenHash(TokenConviteUtil.hash(tokenBruto))
            .status(InviteStatus.PENDING)
            .expiraEm(expiraEm)
            .criadoEm(OffsetDateTime.now())
            .build());

        String link = baseUrl + tokenBruto;
        inviteNotifier.enviar(invite, link);
        return new InviteResponse(link, expiraEm);
    }

    @Override
    @Transactional
    public AuthResponse aceitar(AceitarConviteRequest request) {
        // Rota pública: precisamos setar o tenant do token para ativar o RLS (G1).
        TenantContextHolder.set(TokenConviteUtil.extrairTenant(request.token()));
        try {
            Invite invite = inviteRepositorio.findByTokenHash(TokenConviteUtil.hash(request.token()))
                .orElseThrow(() -> new ExcecaoNegocio("Convite inválido"));
            validarConvite(invite);

            Student aluna = invite.getStudent();
            aluna.setSenhaHash(passwordEncoder.encode(request.senha()));
            aluna.setAtivo(true);
            studentRepositorio.save(aluna);

            invite.setStatus(InviteStatus.ACCEPTED);
            invite.setAceitoEm(OffsetDateTime.now());
            inviteRepositorio.save(invite);

            return authService.emitirTokensAluna(aluna);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Student resolverAluna(Tenant tenant, InviteRequest request) {
        return studentRepositorio.findByEmail(request.email())
            .orElseGet(() -> studentRepositorio.save(Student.builder()
                .tenant(tenant)
                .nome(request.nome())
                .email(request.email())
                .criadoEm(OffsetDateTime.now())
                .ativo(true)
                .build()));
    }

    private void garantirVinculo(Professional profissional, Student aluna) {
        if (linkRepositorio.existsByProfessionalIdAndStudentId(profissional.getId(), aluna.getId())) {
            return;
        }
        linkRepositorio.save(ProfessionalStudentLink.builder()
            .tenant(profissional.getTenant())
            .professional(profissional)
            .student(aluna)
            .criadoEm(OffsetDateTime.now())
            .build());
    }

    private void validarConvite(Invite invite) {
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ExcecaoNegocio("Convite já utilizado ou cancelado");
        }
        if (invite.getExpiraEm().isBefore(OffsetDateTime.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepositorio.save(invite);
            throw new ExcecaoNegocio("Convite expirado");
        }
    }
}
