package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.dto.ConsentRequest;
import br.com.lumyra.modules.identity.dto.ConsentResponse;
import br.com.lumyra.modules.identity.entity.Consent;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ConsentRepositorio;
import br.com.lumyra.modules.identity.service.ContextoUsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceImplTest {

    @Mock
    private ConsentRepositorio consentRepositorio;
    @Mock
    private ContextoUsuarioService contextoUsuario;

    @InjectMocks
    private ConsentServiceImpl consentService;

    private static final Tenant TENANT = Tenant.builder().id(1).build();
    private static final Student ALUNA = Student.builder().id(20).tenant(TENANT)
        .email("ana@email.com").build();

    @Test
    @DisplayName("registrar: persiste consentimento e devolve status ATIVO")
    void registrar_deveRetornarAtivo() {
        when(contextoUsuario.alunaAtual()).thenReturn(ALUNA);
        when(consentRepositorio.save(any())).thenAnswer(inv -> {
            Consent consent = inv.getArgument(0);
            consent.setId(5);
            return consent;
        });

        var response = consentService.registrar(new ConsentRequest("1.0"));

        assertThat(response.status()).isEqualTo(ConsentResponse.ATIVO);
        assertThat(response.termsVersion()).isEqualTo("1.0");
        assertThat(response.registradoEm()).isNotNull();
    }

    @Test
    @DisplayName("statusAtual: com consentimento ativo devolve ATIVO")
    void statusAtual_comConsentimento_deveRetornarAtivo() {
        var consent = Consent.builder().id(5).tenant(TENANT).student(ALUNA)
            .termsVersion("1.0").registradoEm(OffsetDateTime.now()).build();
        when(contextoUsuario.alunaAtual()).thenReturn(ALUNA);
        when(consentRepositorio.findFirstByStudentIdAndRevogadoEmIsNullOrderByRegistradoEmDesc(20))
            .thenReturn(Optional.of(consent));

        var response = consentService.statusAtual();

        assertThat(response.status()).isEqualTo(ConsentResponse.ATIVO);
        assertThat(response.termsVersion()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("statusAtual: sem consentimento devolve AUSENTE")
    void statusAtual_semConsentimento_deveRetornarAusente() {
        when(contextoUsuario.alunaAtual()).thenReturn(ALUNA);
        when(consentRepositorio.findFirstByStudentIdAndRevogadoEmIsNullOrderByRegistradoEmDesc(20))
            .thenReturn(Optional.empty());

        var response = consentService.statusAtual();

        assertThat(response.status()).isEqualTo(ConsentResponse.AUSENTE);
        assertThat(response.termsVersion()).isNull();
        assertThat(response.registradoEm()).isNull();
    }

    @Test
    @DisplayName("possuiConsentimentoAtivo: true quando há consentimento não revogado")
    void possuiConsentimentoAtivo_deveRefletirRepositorio() {
        when(consentRepositorio.findFirstByStudentIdAndRevogadoEmIsNullOrderByRegistradoEmDesc(20))
            .thenReturn(Optional.of(Consent.builder().id(5).build()));

        assertThat(consentService.possuiConsentimentoAtivo(20)).isTrue();
    }
}
