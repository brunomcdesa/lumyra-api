package br.com.lumyra.modules.identity.controller;

import br.com.lumyra.modules.identity.dto.ConsentRequest;
import br.com.lumyra.modules.identity.dto.ConsentResponse;
import br.com.lumyra.modules.identity.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping("/api/v1/consents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ConsentResponse> registrar(@Valid @RequestBody ConsentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentService.registrar(request));
    }

    @GetMapping("/api/v1/me/consents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ConsentResponse> statusAtual() {
        return ResponseEntity.ok(consentService.statusAtual());
    }
}
