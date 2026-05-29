package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.dto.AuthResponse;
import br.com.lumyra.modules.identity.dto.LoginRequest;
import br.com.lumyra.modules.identity.dto.RefreshRequest;
import br.com.lumyra.modules.identity.dto.RegisterRequest;

public interface AuthService {

    AuthResponse registrar(RegisterRequest request);

    AuthResponse autenticar(LoginRequest request);

    AuthResponse renovar(RefreshRequest request);

    void sair(RefreshRequest request);
}
