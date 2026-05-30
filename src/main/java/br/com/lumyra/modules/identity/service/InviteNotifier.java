package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.entity.Invite;

/**
 * Entrega o convite à aluna. Implementação atual é um stub que registra log;
 * envio por SMTP fica para uma tarefa futura (decisão de produto).
 */
public interface InviteNotifier {

    void enviar(Invite invite, String link);
}
