package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.dto.AceitarConviteRequest;
import br.com.lumyra.modules.identity.dto.AuthResponse;
import br.com.lumyra.modules.identity.dto.InviteRequest;
import br.com.lumyra.modules.identity.dto.InviteResponse;

public interface InviteService {

    /** Profissional autenticado convida uma aluna. Retorna o link com o token. */
    InviteResponse convidar(InviteRequest request);

    /** Endpoint público: a aluna define a senha pelo token e já recebe tokens (auto-login). */
    AuthResponse aceitar(AceitarConviteRequest request);
}
