package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.entity.Consent;
import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.ProfessionalStudentLink;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ConsentRepositorio;
import br.com.lumyra.modules.identity.repository.ProfessionalStudentLinkRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import br.com.lumyra.modules.identity.service.ContextoUsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DadosPessoaisServiceImplTest {

    @Mock
    private ContextoUsuarioService contextoUsuario;
    @Mock
    private StudentRepositorio studentRepositorio;
    @Mock
    private ConsentRepositorio consentRepositorio;
    @Mock
    private ProfessionalStudentLinkRepositorio linkRepositorio;

    @InjectMocks
    private DadosPessoaisServiceImpl service;

    private static final Tenant TENANT = Tenant.builder().id(1).build();
    private static final Student ALUNA = Student.builder().id(20).tenant(TENANT)
        .nome("Ana").email("ana@email.com").ativo(true).criadoEm(OffsetDateTime.now()).build();

    @Test
    @DisplayName("exportar: inclui perfil, consentimentos e profissionais vinculados")
    void exportar_deveIncluirTodosOsDados() {
        var prof = Professional.builder().id(10).nome("Bruno").build();
        var link = ProfessionalStudentLink.builder().id(1).professional(prof).student(ALUNA).build();
        var consent = Consent.builder().id(5).student(ALUNA).termsVersion("1.0")
            .registradoEm(OffsetDateTime.now()).build();
        when(contextoUsuario.alunaAtual()).thenReturn(ALUNA);
        when(consentRepositorio.findByStudentIdOrderByRegistradoEmDesc(20))
            .thenReturn(List.of(consent));
        when(linkRepositorio.findByStudentId(20)).thenReturn(List.of(link));

        var export = service.exportar();

        assertThat(export.id()).isEqualTo(20);
        assertThat(export.email()).isEqualTo("ana@email.com");
        assertThat(export.consentimentos()).singleElement()
            .satisfies(c -> assertThat(c.termsVersion()).isEqualTo("1.0"));
        assertThat(export.profissionaisVinculados()).containsExactly("Bruno");
    }

    @Test
    @DisplayName("eliminar: soft-delete marca deletadoEm e ativo=false")
    void eliminar_deveFazerSoftDelete() {
        var aluna = Student.builder().id(20).tenant(TENANT).email("ana@email.com").ativo(true).build();
        when(contextoUsuario.alunaAtual()).thenReturn(aluna);

        service.eliminar();

        var captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepositorio).save(captor.capture());
        assertThat(captor.getValue().getDeletadoEm()).isNotNull();
        assertThat(captor.getValue().getAtivo()).isFalse();
    }
}
