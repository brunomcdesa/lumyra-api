package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.core.rls.TenantContextHolder;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.repository.ConsentRepositorio;
import br.com.lumyra.modules.identity.repository.InviteRepositorio;
import br.com.lumyra.modules.identity.repository.ProfessionalStudentLinkRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Purga (hard-delete) as alunas de UM tenant cujo soft-delete passou da
 * janela de retenção. Método transacional separado para que o
 * {@code RlsTenantContextInterceptor} aplique o {@code SET LOCAL} dentro da
 * transação (G1) — o contexto de tenant é setado aqui, não na requisição.
 */
@Service
@RequiredArgsConstructor
public class PurgaTenantService {

    private final StudentRepositorio studentRepositorio;
    private final ConsentRepositorio consentRepositorio;
    private final InviteRepositorio inviteRepositorio;
    private final ProfessionalStudentLinkRepositorio linkRepositorio;

    @Transactional
    public int purgarTenant(Integer tenantId, OffsetDateTime limite) {
        TenantContextHolder.set(tenantId);
        try {
            List<Student> expiradas =
                studentRepositorio.findByDeletadoEmIsNotNullAndDeletadoEmBefore(limite);

            for (Student aluna : expiradas) {
                consentRepositorio.deleteByStudentId(aluna.getId());
                inviteRepositorio.deleteByStudentId(aluna.getId());
                linkRepositorio.deleteByStudentId(aluna.getId());
                studentRepositorio.delete(aluna);
            }
            return expiradas.size();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
