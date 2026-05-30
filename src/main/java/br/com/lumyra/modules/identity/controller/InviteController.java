package br.com.lumyra.modules.identity.controller;

import br.com.lumyra.modules.identity.dto.InviteRequest;
import br.com.lumyra.modules.identity.dto.InviteResponse;
import br.com.lumyra.modules.identity.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<InviteResponse> convidar(@Valid @RequestBody InviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inviteService.convidar(request));
    }
}
