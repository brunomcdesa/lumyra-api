package br.com.lumyra.modules.identity.controller;

import br.com.lumyra.modules.identity.dto.ExportacaoDadosResponse;
import br.com.lumyra.modules.identity.service.DadosPessoaisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final DadosPessoaisService dadosPessoaisService;

    @GetMapping("/export")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExportacaoDadosResponse> exportar() {
        return ResponseEntity.ok(dadosPessoaisService.exportar());
    }

    @DeleteMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> eliminar() {
        dadosPessoaisService.eliminar();
        return ResponseEntity.noContent().build();
    }
}
