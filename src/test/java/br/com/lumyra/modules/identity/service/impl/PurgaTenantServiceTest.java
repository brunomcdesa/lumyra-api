package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ConsentRepositorio;
import br.com.lumyra.modules.identity.repository.InviteRepositorio;
import br.com.lumyra.modules.identity.repository.ProfessionalStudentLinkRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurgaTenantServiceTest {

    @Mock
    private StudentRepositorio studentRepositorio;
    @Mock
    private ConsentRepositorio consentRepositorio;
    @Mock
    private InviteRepositorio inviteRepositorio;
    @Mock
    private ProfessionalStudentLinkRepositorio linkRepositorio;

    @InjectMocks
    private PurgaTenantService purgaTenantService;

    @Test
    @DisplayName("purgarTenant: hard-delete de dependências e da aluna expirada")
    void purgarTenant_deveRemoverDependenciasEAluna() {
        var aluna = Student.builder().id(20).tenant(Tenant.builder().id(1).build())
            .deletadoEm(OffsetDateTime.now().minusDays(40)).ativo(false).build();
        var limite = OffsetDateTime.now().minusDays(30);
        when(studentRepositorio.findByDeletadoEmIsNotNullAndDeletadoEmBefore(limite))
            .thenReturn(List.of(aluna));

        int removidas = purgaTenantService.purgarTenant(1, limite);

        assertThat(removidas).isEqualTo(1);
        verify(consentRepositorio).deleteByStudentId(20);
        verify(inviteRepositorio).deleteByStudentId(20);
        verify(linkRepositorio).deleteByStudentId(20);
        verify(studentRepositorio).delete(any(Student.class));
    }

    @Test
    @DisplayName("purgarTenant: nada a remover devolve zero")
    void purgarTenant_semExpiradas_deveRetornarZero() {
        var limite = OffsetDateTime.now().minusDays(30);
        when(studentRepositorio.findByDeletadoEmIsNotNullAndDeletadoEmBefore(limite))
            .thenReturn(List.of());

        assertThat(purgaTenantService.purgarTenant(1, limite)).isZero();
    }
}
