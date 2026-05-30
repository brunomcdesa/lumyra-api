package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.dto.ExportacaoDadosResponse;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.repository.ConsentRepositorio;
import br.com.lumyra.modules.identity.repository.ProfessionalStudentLinkRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import br.com.lumyra.modules.identity.service.ContextoUsuarioService;
import br.com.lumyra.modules.identity.service.DadosPessoaisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DadosPessoaisServiceImpl implements DadosPessoaisService {

    private final ContextoUsuarioService contextoUsuario;
    private final StudentRepositorio studentRepositorio;
    private final ConsentRepositorio consentRepositorio;
    private final ProfessionalStudentLinkRepositorio linkRepositorio;

    @Override
    @Transactional(readOnly = true)
    public ExportacaoDadosResponse exportar() {
        Student aluna = contextoUsuario.alunaAtual();

        List<ExportacaoDadosResponse.ConsentimentoItem> consentimentos =
            consentRepositorio.findByStudentIdOrderByRegistradoEmDesc(aluna.getId()).stream()
                .map(c -> new ExportacaoDadosResponse.ConsentimentoItem(
                    c.getTermsVersion(), c.getRegistradoEm(), c.getRevogadoEm()))
                .toList();

        List<String> profissionais = linkRepositorio.findByStudentId(aluna.getId()).stream()
            .map(link -> link.getProfessional().getNome())
            .toList();

        return new ExportacaoDadosResponse(
            aluna.getId(),
            aluna.getNome(),
            aluna.getEmail(),
            aluna.getDataNascimento(),
            aluna.getCriadoEm(),
            aluna.getAtivo(),
            consentimentos,
            profissionais);
    }

    @Override
    @Transactional
    public void eliminar() {
        Student aluna = contextoUsuario.alunaAtual();
        // Soft-delete: o hard-delete acontece no PurgaDadosJob após a retenção.
        // ativo=false também bloqueia login e refresh (UsuarioDetailsService filtra).
        aluna.setDeletadoEm(OffsetDateTime.now());
        aluna.setAtivo(false);
        studentRepositorio.save(aluna);
    }
}
