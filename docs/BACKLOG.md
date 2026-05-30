# BACKLOG — Lumyra Backend (Spring Boot)

> Traduz o roadmap da análise em tarefas pequenas e ordenadas. **Siga a ordem.**
> Como usar: copie **uma** tarefa por vez para o Claude Code, peça para ele ler este arquivo,
> o `CLAUDE.md` e o `docs/GUARDRAILS.md`, e implementar. Rode `./mvnw test`, confira o
> critério de aceite e os guardrails marcados, e só então passe à próxima.
>
> Esforço: S ≈ até 1 dia · M ≈ 2–4 dias · L ≈ 1+ semana (para 1 dev).
> **MVP backend = Fase 0 + Fase 1.** Ciclo (Fase 3) só após validação clínica/de conteúdo.
>
> 🔗 = ponto de sincronia com o repo `lumyra-mobile`. Avise o mobile quando concluir.

---

## FASE 0 — Fundação (M) · pré-requisito de tudo

> Nenhum dado de aluna entra antes desta fase. RLS e consentimento primeiro.

### ✅ T0.1 — Bootstrap do projeto Spring Boot (S)
- **Objetivo:** gerar projeto Spring Boot 3.x · Java 21 · Maven, com dependências: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-redis`, `spring-boot-starter-validation`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, `springdoc-openapi-starter-webmvc-ui`, `mapstruct`, `lombok`, `argon2-jvm`.
- Estrutura de pacotes (`config/`, `core/`, `shared/`, `modules/`) criada e vazia.
- `application.yml` com profiles `local`, `test`, `prod`.
- `docker-compose.yml` com Postgres 16 e Redis 7 para dev local.
- `/health` (Actuator) público; demais endpoints exigem auth (a configurar em T0.4).
- **Aceite:** `./mvnw spring-boot:run` sobe; `GET /health` 200; `./mvnw test` verde.

### ✅ T0.2 — Flyway + esqueleto do banco (S)
- **Objetivo:** Flyway configurado, primeira migração `V001__init.sql` criando extensões necessárias (`pgcrypto`, `citext`) e nada mais.
- **Aceite:** Flyway aplica V001 no Postgres do docker-compose no startup em `local`; `./mvnw test` verde.

### T0.3 — Multi-tenancy com RLS (M) · **toca G1**
- **Objetivo:** modelar `tenant`, `professional`, `student`, `professional_student_link`. Habilitar RLS em `student` e em todas as tabelas com dado de aluna criadas a partir daqui. Implementar `RlsTenantContextInterceptor` que executa `SET LOCAL app.current_tenant = ?` na conexão JDBC antes de cada requisição autenticada, lendo `tenant_id` do contexto de segurança.
- **Migrações:** V002 cria tabelas; V003 habilita RLS + cria policies (`USING (tenant_id::text = current_setting('app.current_tenant', true))`).
- **Aceite:** teste unitário verifica que o interceptor injeta `SET LOCAL app.current_tenant = ?` antes de queries; validação de isolamento feita manualmente contra o docker-compose local.

### T0.4 — Autenticação JWT + cadastro de profissional (M) · **toca G4, G6**
- **Objetivo:** `POST /api/v1/auth/register` (profissional com CREF, e-mail, senha), `POST /api/v1/auth/login` (retorna access + refresh), `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` (revoga refresh em Redis). Senha com Argon2. JWT curto (15min) + refresh rotativo (30 dias). Rate limit no `/login` por IP (anti brute-force).
- Spring Security configurado: endpoints públicos = só auth; resto exige `ROLE_PROFESSIONAL` ou `ROLE_STUDENT`.
- **Aceite:** fluxo completo testado; token expira; refresh funciona; brute-force barrado após N tentativas; lint sem segredo hardcoded.

### ✅ T0.5 — Convite + consentimento LGPD da aluna (M) · **toca G2**
- **Objetivo:**
  - `POST /api/v1/invites` (profissional convida aluna por e-mail);
  - `POST /api/v1/auth/accept-invite` (aluna define senha a partir de link com token);
  - `POST /api/v1/consents` (registra consentimento ativo com versão do termo);
  - `GET /api/v1/me/consents` (status atual);
  - `DELETE /api/v1/me` (eliminação dos dados — soft delete + job de purge);
  - `GET /api/v1/me/export` (exportação JSON dos dados pessoais);
  - Filtro/aspecto que **barra qualquer endpoint de coleta de dado sensível** se a aluna não tiver consentimento ativo.
- **Aceite:** sem consentimento ativo → 403 em endpoints sensíveis (testado); exportação inclui todos os dados pessoais; eliminação remove o que deve remover.

### ✅ 🔗 **Marco F0-PRONTO** — anunciar ao mobile
- OpenAPI disponível em `/v3/api-docs`. O mobile pode rodar `npm run gen:api` para gerar cliente.
- Endpoints prontos: register, login, refresh, logout, invite, accept-invite, consent,
  `GET /me/consents`, `GET /me/export`, `DELETE /me`.

> **Decisões de implementação da T0.5 (2026-05-30):**
> - **Login da aluna habilitado** (`ROLE_STUDENT`): `/auth/login` autentica profissional **e** aluna
>   (composite `UsuarioDetailsService`); `accept-invite` grava a senha Argon2 e já devolve tokens.
> - **Tenant embutido no token de convite** (`"{tenantId}.{segredo}"`): `accept-invite` é público e
>   precisa setar o contexto de RLS manualmente antes de qualquer query (G1). Só o hash SHA-256 do
>   token é persistido (G4).
> - **Envio de convite por e-mail é stub** (`LoggingInviteNotifier`): o link volta na resposta e é
>   logado (em `local`); SMTP real fica como tarefa futura.
> - **Guard de consentimento reutilizável**: anotação `@ExigeConsentimentoAtivo` + aspecto →
>   `ConsentimentoAusenteException` (403). Ainda sem endpoint sensível real (Fases 1+); validado por
>   teste unitário do aspecto.
> - **`DELETE /me`**: soft-delete + `PurgaDadosJob` (`@Scheduled`) que itera tenants e faz hard-delete
>   após `app.lgpd.retention-days` (default 30).
> - **Testes**: padrão do repo (unitários Mockito) — não há infra de Testcontainers; isolamento de RLS
>   validado manualmente contra o docker-compose, como na T0.3.

---

## FASE 1 — Avaliação (L) · MVP backend

> Ao fim desta fase, a API suporta a jornada completa de avaliação. Coração do MVP.

### T1.1 — Cálculos clínicos como POJOs testados (M) · **toca G3**
- **Objetivo:** `shared/clinical/` com classes utilitárias sem Spring:
  - `BodyDensity.guedesGuedes(...)` — protocolo Guedes & Guedes;
  - `BodyComposition.siri(density)` — %G por Siri;
  - `LeanMass.compute(...)`, `Bmi.compute(...)`, índices (cintura/quadril, cintura/estatura, conicidade);
  - `ReferenceRanges.lookup(sex, ageYears)` — faixas de referência.
- **Golden tests (JUnit 5 + AssertJ)** com tolerância de 0,01: caso Pirigo (`%G ≈ 6.59`, `MM ≈ 84.06 kg`, soma 3 dobras `20 mm`) + 2–3 casos adicionais.
- Cada função expõe `protocol()` retornando "Guedes & Guedes (1985)" / "Siri (1961)" etc.
- **⚠ Antes de começar:** confirmar com o profissional as equações exatas e faixas (unknown G3). Se não confirmado, **pergunte**.
- **Aceite:** golden tests verdes; coverage da `shared/clinical` ≥ 95%.

### T1.2 — CRUD de alunas (S) · **toca G1**
- **Objetivo:** `POST /api/v1/students`, `GET /api/v1/students` (paginado, com filtros: ativas, a reavaliar, pausadas), `GET /api/v1/students/{id}`, `PATCH /api/v1/students/{id}`, `DELETE /api/v1/students/{id}` (arquivar — soft delete).
- Tudo escopado por tenant + vínculo. `@PreAuthorize` com `ROLE_PROFESSIONAL`.
- **Aceite:** todos os endpoints filtram corretamente; tentativa de acessar aluna de outro tenant → 404 (não 403, para não vazar existência).

### T1.3 — Anamnese (M) · **toca G2, G8**
- **Objetivo:** `POST /api/v1/students/{id}/assessments` (cria avaliação em `DRAFT`), `POST /api/v1/assessments/{id}/anamnesis` (sinais vitais, IMC calculado no servidor, componentes clínicos). Avaliação tem estado `DRAFT → FINALIZED`.
- **Aceite:** IMC calculado correto; após finalizar, anamnese não pode ser editada (`409 Conflict`).

### T1.4 — Composição corporal (M) · **toca G3, G8**
- **Objetivo:** `POST /api/v1/assessments/{id}/body-composition` (dobras cutâneas, perímetros). Servidor calcula via T1.1 e devolve resultado completo (%G, MM, índices, classificação por referência).
- `POST /api/v1/assessments/{id}/finalize` muda status para `FINALIZED` e torna tudo imutável.
- **Aceite:** resultado bate com os golden tests; finalize bloqueia edições; protocolo aparece no payload.

### T1.5 — Histórico de avaliações (S)
- **Objetivo:** `GET /api/v1/students/{id}/assessments` (lista paginada, ordenada por data), `GET /api/v1/assessments/{id}/compare/{otherId}` (diff de campos comparáveis: %G, MM, perímetros).
- **Aceite:** ordenação correta; comparação devolve estrutura tipada (não diff textual).

### T1.6 — Geração de laudo PDF assíncrona (M) · **toca G4, G5**
- **Objetivo:** ao finalizar avaliação, enfileirar job de geração de PDF (Spring `@Async` + fila simples em Redis ou Spring Batch). Estado `PENDING → GENERATED | FAILED`. PDF gravado em object storage privado; `GET /api/v1/assessments/{id}/report` devolve **URL pré-assinada** (TTL 5min).
- Bibliotecas sugeridas: **OpenPDF** ou **iText** (verificar licença para iText — AGPL pode ser problema; OpenPDF é LGPL).
- **Aceite:** finalização não bloqueia requisição (retorna 202); job retentável; sem dado sensível em log; URL expira.

### 🔗 **Marco F1-PRONTO** — anunciar ao mobile
- Endpoints da jornada de avaliação prontos e versionados.
- OpenAPI atualizado. Mobile pode iniciar telas de avaliação.

---

## FASE 2 — Prescrição e volume (M)

### T2.1 — Catálogo de exercícios + seed (S) · **toca G3**
- **Objetivo:** entidade `Exercise` (nome, grupo muscular, variação); seed via Flyway `V0XX__seed_exercises.sql` com os ~76 exercícios da planilha de volume (peitoral, costas, deltóides A/M/P, bíceps, tríceps, quadríceps, isquiotibiais, glúteos, panturrilhas, trapézio, abdômen). `GET /api/v1/exercises`.
- **Aceite:** seed idempotente; consulta por grupo retorna os exercícios da planilha.

### T2.2 — Prescrição de treino (M)
- **Objetivo:** `POST /api/v1/students/{id}/prescriptions` (mesociclo, sessões com itens — exercício, séries, reps, carga), `GET`, `PATCH`, `DELETE`.
- **Aceite:** escopado por tenant; validação de séries/reps > 0.

### T2.3 — Análise de volume semanal (M) · **toca G3**
- **Objetivo:** `GET /api/v1/prescriptions/{id}/volume` retorna volume semanal por grupo (séries efetivas considerando 1 ou 2 sessões), comparação com zona-alvo, e recomendação (`BELOW | IN_RANGE | ABOVE`).
- Cálculo em `shared/clinical/Volume.java` com testes.
- **⚠ Validar com profissional:** origem e personalização das zonas-alvo (unknown).
- **Aceite:** Pirigo/exemplo da planilha bate; teste com prescrição vazia retorna zeros.

### 🔗 **Marco F2-PRONTO** — anunciar ao mobile

---

## FASE 3 — Ciclo + interações da aluna (L)

### T3.1 — Endpoints somente-leitura da aluna (S) · **toca G6**
- **Objetivo:** versões com `ROLE_STUDENT` dos endpoints de visualização: própria aluna lê suas avaliações, anamnese, treino atual. Nenhuma escrita exposta.
- **Aceite:** aluna autenticada acessa só os próprios dados; tentar acessar de outra aluna → 404.

### T3.2 — Execução de treino + RPE (M) · **toca G6**
- **Objetivo:** `POST /api/v1/executions` (aluna registra início da sessão), `POST /api/v1/executions/{id}/sets` (RPE Borg CR10 por série, com `exerciseId` e número da série). Visível para a educadora vinculada.
- **Aceite:** aluna só pode escrever na própria execução; profissional só lê.

### T3.3 — Questionário de dor (M) · **toca G2, G6**
- **Objetivo:** `POST /api/v1/students/{id}/pain-reports` (região do mapa corporal, VAS 0–10, tipo, gatilho). Entra como anexo da anamnese; visível à educadora.
- **Aceite:** consentimento ativo verificado; aluna escreve só os próprios; profissional lê.

### T3.4 — Ciclo menstrual (L) · **toca G3, G2, G7**
- **Objetivo:**
  - `POST /api/v1/me/cycle` (última menstruação, duração média);
  - `GET /api/v1/me/cycle/current` (fase calculada no servidor — `MENSTRUAL | FOLLICULAR | OVULATION | LUTEAL` — dia do ciclo, recomendações por fase);
  - `POST /api/v1/me/cycle/symptoms` (registro diário: cólica, inchaço, energia, humor...);
  - Fallback para ciclos irregulares / anticoncepcional / amenorreia: retorna fase `UNKNOWN` com mensagem apropriada.
- Cálculo em `shared/clinical/Cycle.java` com golden tests cobrindo: ciclo de 28 dias, 32 dias, 21 dias, transição de mês, ciclo desconhecido.
- **⚠ Antes de começar:** validar com especialista o conteúdo e a regra de fase, **incluindo o fallback**. Sem isso, **não implementar** — perguntar.
- **Aceite:** golden tests verdes; disclaimer presente no payload de recomendações; consentimento obrigatório.

### 🔗 **Marco F3-PRONTO** — anunciar ao mobile

---

## FASE 4 — Testes funcionais 60+ (M)

### T4.1 — Bateria funcional + risco de queda (M) · **toca G3**
- **Objetivo:** endpoints para TUG, sentar-levantar 30s, apoio unipodal, preensão, sentar-e-alcançar, Tinetti. Score composto de risco de queda calculado em `shared/clinical/FallRisk.java`.
- **⚠ Validar:** referências por faixa etária (unknown).
- **Aceite:** score composto testado; aluna 60+ vê seus resultados.

---

## FASE 5 — Endurecimento (M)

### T5.1 — Billing da assinatura do profissional (M)
- Integração com gateway (Stripe ou similar). Webhooks idempotentes. Plano cobra apenas o profissional.

### T5.2 — Observabilidade completa (M) · **toca G5**
- Logback estruturado (JSON) com `requestId` + `tenantId`; métricas Micrometer + Prometheus; tracing OpenTelemetry; Sentry com scrub de payload sensível; dashboards e alertas (taxa de erro, latência p95, profundidade da fila de PDF, tentativas de brute-force).

### T5.3 — Teste de carga (S)
- Cenários da análise §10.2: 200 req/s sustentado, rajada de geração de PDF (turma reavaliada no mesmo dia), custo do RLS com muitos tenants. Ferramenta: k6 ou Gatling.

### T5.4 — Revisão de segurança (S)
- Checklist OWASP API Top 10; pentest leve; revisão das regras de Spring Security; rotina de rotação de chaves.

### T5.5 — Revisão jurídica LGPD (S)
- Termo de consentimento revisado por advogado(a) especializado em proteção de dados; política de privacidade; DPO definido; plano de resposta a incidente para ANPD.

---

## Como pedir uma tarefa ao Claude Code (modelo)

```
Implemente a tarefa T1.4 do docs/BACKLOG.md neste repositório lumyra-backend.

Antes de codar:
- leia CLAUDE.md e docs/GUARDRAILS.md;
- carregue a skill backend-engineer;
- respeite G1 (RLS), G3 (cálculo testado) e G8 (avaliação imutável).

Ao terminar:
- rode ./mvnw verify;
- me mostre o que mudou e qual critério de aceite você cobriu;
- se faltou alguma decisão (ex.: fórmula exata), pare e me pergunte.
```
