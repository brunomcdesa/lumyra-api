package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.dto.ConsentRequest;
import br.com.lumyra.modules.identity.dto.ConsentResponse;
import br.com.lumyra.modules.identity.entity.Consent;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.repository.ConsentRepositorio;
import br.com.lumyra.modules.identity.service.ConsentService;
import br.com.lumyra.modules.identity.service.ContextoUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ConsentServiceImpl implements ConsentService {

    private final ConsentRepositorio consentRepositorio;
    private final ContextoUsuarioService contextoUsuario;

    @Override
    @Transactional
    public ConsentResponse registrar(ConsentRequest request) {
        Student aluna = contextoUsuario.alunaAtual();

        Consent consent = consentRepositorio.save(Consent.builder()
            .tenant(aluna.getTenant())
            .student(aluna)
            .termsVersion(request.termsVersion())
            .registradoEm(OffsetDateTime.now())
            .build());

        return new ConsentResponse(ConsentResponse.ATIVO, consent.getTermsVersion(),
            consent.getRegistradoEm());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentResponse statusAtual() {
        Student aluna = contextoUsuario.alunaAtual();
        return consentRepositorio
            .findFirstByStudentIdAndRevogadoEmIsNullOrderByRegistradoEmDesc(aluna.getId())
            .map(c -> new ConsentResponse(ConsentResponse.ATIVO, c.getTermsVersion(),
                c.getRegistradoEm()))
            .orElseGet(() -> new ConsentResponse(ConsentResponse.AUSENTE, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean possuiConsentimentoAtivo(Integer studentId) {
        return consentRepositorio
            .findFirstByStudentIdAndRevogadoEmIsNullOrderByRegistradoEmDesc(studentId)
            .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean alunaAtualPossuiConsentimentoAtivo() {
        return possuiConsentimentoAtivo(contextoUsuario.alunaAtual().getId());
    }
}
