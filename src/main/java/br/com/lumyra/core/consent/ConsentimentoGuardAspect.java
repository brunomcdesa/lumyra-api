package br.com.lumyra.core.consent;

import br.com.lumyra.exception.ConsentimentoAusenteException;
import br.com.lumyra.modules.identity.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Barra (403) qualquer método anotado com {@link ExigeConsentimentoAtivo}
 * quando a aluna autenticada não tem consentimento LGPD ativo (G2).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ConsentimentoGuardAspect {

    private final ConsentService consentService;

    @Before("@annotation(br.com.lumyra.core.consent.ExigeConsentimentoAtivo)")
    public void exigirConsentimentoAtivo() {
        if (!consentService.alunaAtualPossuiConsentimentoAtivo()) {
            throw new ConsentimentoAusenteException();
        }
    }
}
