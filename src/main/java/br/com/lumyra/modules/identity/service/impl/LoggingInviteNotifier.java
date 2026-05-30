package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.entity.Invite;
import br.com.lumyra.modules.identity.service.InviteNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Stub de notificação de convite. Não envia e-mail real (sem SMTP nesta fase).
 * Loga apenas o id do convite (sem e-mail/token — G5). Em profile {@code local}
 * loga também o link, para facilitar testes manuais.
 */
@Slf4j
@Component
public class LoggingInviteNotifier implements InviteNotifier {

    private final boolean perfilLocal;

    public LoggingInviteNotifier(Environment environment) {
        this.perfilLocal = environment.matchesProfiles("local");
    }

    @Override
    public void enviar(Invite invite, String link) {
        log.info("Convite #{} criado (TODO: enviar por e-mail)", invite.getId());
        if (perfilLocal) {
            log.debug("[local] link do convite #{}: {}", invite.getId(), link);
        }
    }
}
