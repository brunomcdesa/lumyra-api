package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.dto.ConsentRequest;
import br.com.lumyra.modules.identity.dto.ConsentResponse;

public interface ConsentService {

    /** Registra consentimento LGPD ativo da aluna autenticada. */
    ConsentResponse registrar(ConsentRequest request);

    /** Status do consentimento da aluna autenticada (ATIVO ou AUSENTE). */
    ConsentResponse statusAtual();

    /** Indica se há consentimento ativo (não revogado) para a aluna informada. */
    boolean possuiConsentimentoAtivo(Integer studentId);

    /** Indica se a aluna autenticada tem consentimento ativo. Usado pelo guard. */
    boolean alunaAtualPossuiConsentimentoAtivo();
}
