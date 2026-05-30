package br.com.lumyra.core.consent;

import br.com.lumyra.exception.ConsentimentoAusenteException;
import br.com.lumyra.modules.identity.service.ConsentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentimentoGuardAspectTest {

    @Mock
    private ConsentService consentService;

    @InjectMocks
    private ConsentimentoGuardAspect aspect;

    @Test
    @DisplayName("sem consentimento ativo: lança ConsentimentoAusenteException (vira 403)")
    void semConsentimento_deveLancar() {
        when(consentService.alunaAtualPossuiConsentimentoAtivo()).thenReturn(false);

        assertThatThrownBy(aspect::exigirConsentimentoAtivo)
            .isInstanceOf(ConsentimentoAusenteException.class);
    }

    @Test
    @DisplayName("com consentimento ativo: não lança")
    void comConsentimento_deveLiberar() {
        when(consentService.alunaAtualPossuiConsentimentoAtivo()).thenReturn(true);

        assertThatCode(aspect::exigirConsentimentoAtivo).doesNotThrowAnyException();
    }
}
