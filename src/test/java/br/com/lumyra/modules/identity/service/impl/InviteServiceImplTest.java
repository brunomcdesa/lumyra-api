package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.exception.ExcecaoNegocio;
import br.com.lumyra.modules.identity.dto.AceitarConviteRequest;
import br.com.lumyra.modules.identity.dto.AuthResponse;
import br.com.lumyra.modules.identity.dto.InviteRequest;
import br.com.lumyra.modules.identity.entity.Invite;
import br.com.lumyra.modules.identity.entity.InviteStatus;
import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.InviteRepositorio;
import br.com.lumyra.modules.identity.repository.ProfessionalStudentLinkRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import br.com.lumyra.modules.identity.service.ContextoUsuarioService;
import br.com.lumyra.modules.identity.service.InviteNotifier;
import br.com.lumyra.modules.identity.service.TokenConviteUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteServiceImplTest {

    @Mock
    private InviteRepositorio inviteRepositorio;
    @Mock
    private StudentRepositorio studentRepositorio;
    @Mock
    private ProfessionalStudentLinkRepositorio linkRepositorio;
    @Mock
    private ContextoUsuarioService contextoUsuario;
    @Mock
    private InviteNotifier inviteNotifier;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthServiceImpl authService;

    @InjectMocks
    private InviteServiceImpl inviteService;

    private static final Tenant TENANT = Tenant.builder().id(1).nome("Bruno").build();
    private static final Professional PROFESSIONAL = Professional.builder()
        .id(10).tenant(TENANT).nome("Bruno").email("bruno@email.com").build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inviteService, "expirationHours", 72L);
        ReflectionTestUtils.setField(inviteService, "baseUrl",
            "https://app.lumyra.com.br/aceitar-convite?token=");
    }

    @Test
    @DisplayName("convidar: cria aluna, vínculo e convite PENDING; notifica e devolve link")
    void convidar_deveCriarConviteELink() {
        var alunaSalva = Student.builder().id(20).tenant(TENANT).nome("Ana")
            .email("ana@email.com").ativo(true).criadoEm(OffsetDateTime.now()).build();
        when(contextoUsuario.profissionalAtual()).thenReturn(PROFESSIONAL);
        when(studentRepositorio.findByEmail("ana@email.com")).thenReturn(Optional.empty());
        when(studentRepositorio.save(any())).thenReturn(alunaSalva);
        when(linkRepositorio.existsByProfessionalIdAndStudentId(10, 20)).thenReturn(false);
        when(inviteRepositorio.save(any())).thenAnswer(inv -> {
            Invite invite = inv.getArgument(0);
            invite.setId(100);
            return invite;
        });

        var response = inviteService.convidar(new InviteRequest("Ana", "ana@email.com"));

        assertThat(response.link()).startsWith("https://app.lumyra.com.br/aceitar-convite?token=1.");
        assertThat(response.expiraEm()).isAfter(OffsetDateTime.now());
        verify(linkRepositorio).save(any());
        verify(inviteNotifier).enviar(any(), anyString());
    }

    @Test
    @DisplayName("convidar: aluna existente não duplica vínculo")
    void convidar_alunaExistente_naoDuplicaVinculo() {
        var aluna = Student.builder().id(20).tenant(TENANT).nome("Ana")
            .email("ana@email.com").ativo(true).build();
        when(contextoUsuario.profissionalAtual()).thenReturn(PROFESSIONAL);
        when(studentRepositorio.findByEmail("ana@email.com")).thenReturn(Optional.of(aluna));
        when(linkRepositorio.existsByProfessionalIdAndStudentId(10, 20)).thenReturn(true);
        when(inviteRepositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inviteService.convidar(new InviteRequest("Ana", "ana@email.com"));

        verify(linkRepositorio, never()).save(any());
        verify(studentRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("aceitar: token válido grava senha, marca ACCEPTED e devolve tokens")
    void aceitar_tokenValido_deveAtivarAlunaERetornarTokens() {
        String token = TokenConviteUtil.gerar(1);
        var aluna = Student.builder().id(20).tenant(TENANT).nome("Ana").email("ana@email.com").build();
        var invite = Invite.builder().id(100).tenant(TENANT).student(aluna).email("ana@email.com")
            .tokenHash(TokenConviteUtil.hash(token)).status(InviteStatus.PENDING)
            .expiraEm(OffsetDateTime.now().plusHours(1)).criadoEm(OffsetDateTime.now()).build();
        when(inviteRepositorio.findByTokenHash(TokenConviteUtil.hash(token)))
            .thenReturn(Optional.of(invite));
        when(passwordEncoder.encode("senhaForte1")).thenReturn("$argon2aluna");
        when(authService.emitirTokensAluna(aluna))
            .thenReturn(new AuthResponse("access", "refresh"));

        var response = inviteService.aceitar(new AceitarConviteRequest(token, "senhaForte1"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(aluna.getSenhaHash()).isEqualTo("$argon2aluna");
        assertThat(aluna.getAtivo()).isTrue();
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        assertThat(invite.getAceitoEm()).isNotNull();
    }

    @Test
    @DisplayName("aceitar: token inexistente lança ExcecaoNegocio")
    void aceitar_tokenInexistente_deveLancar() {
        String token = TokenConviteUtil.gerar(1);
        when(inviteRepositorio.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.aceitar(new AceitarConviteRequest(token, "senhaForte1")))
            .isInstanceOf(ExcecaoNegocio.class)
            .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("aceitar: convite expirado é marcado EXPIRED e lança ExcecaoNegocio")
    void aceitar_expirado_deveLancar() {
        String token = TokenConviteUtil.gerar(1);
        var aluna = Student.builder().id(20).tenant(TENANT).email("ana@email.com").build();
        var invite = Invite.builder().id(100).tenant(TENANT).student(aluna)
            .tokenHash(TokenConviteUtil.hash(token)).status(InviteStatus.PENDING)
            .expiraEm(OffsetDateTime.now().minusMinutes(1)).build();
        when(inviteRepositorio.findByTokenHash(TokenConviteUtil.hash(token)))
            .thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.aceitar(new AceitarConviteRequest(token, "senhaForte1")))
            .isInstanceOf(ExcecaoNegocio.class)
            .hasMessageContaining("expirado");
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.EXPIRED);
    }

    @Test
    @DisplayName("aceitar: convite já utilizado (ACCEPTED) lança ExcecaoNegocio")
    void aceitar_jaUtilizado_deveLancar() {
        String token = TokenConviteUtil.gerar(1);
        var invite = Invite.builder().id(100).tenant(TENANT)
            .student(Student.builder().id(20).build())
            .tokenHash(TokenConviteUtil.hash(token)).status(InviteStatus.ACCEPTED)
            .expiraEm(OffsetDateTime.now().plusHours(1)).build();
        when(inviteRepositorio.findByTokenHash(TokenConviteUtil.hash(token)))
            .thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.aceitar(new AceitarConviteRequest(token, "senhaForte1")))
            .isInstanceOf(ExcecaoNegocio.class)
            .hasMessageContaining("já utilizado");
    }
}
