# CLAUDE.md — lumyra-backend

> Este arquivo é lido pelo Claude Code em toda sessão. Mantê-lo enxuto.
> Detalhes longos estão em `docs/` e na skill **backend-engineer**.

## Sobre o produto (resumo)

Backend do **Lumyra**, app mobile **B2B2C** para profissionais de educação física que atendem **mulheres**. A spec completa está em `docs/ANALISE_DE_SISTEMAS.docx` (fonte da verdade do produto).

- **Educador(a)** (CREF, paga pela assinatura): cria alunas, faz avaliações, prescreve treino.
- **Aluna**: acesso majoritariamente somente-leitura ao próprio histórico, com poucas escritas (RPE, dor, ciclo menstrual — o diferencial).
- **App mobile (Expo/React Native)** vive em repo separado (`lumyra-mobile`) e consome esta API via REST + OpenAPI.

## Stack

- **Java 21** (LTS) · **Spring Boot 3.x** · **Maven**.
- **Spring Web** (REST), **Spring Security** (JWT + refresh), **Spring Data JPA** (entidades), **Caffeine** (cache local in-memory — sem Redis no momento).
- **PostgreSQL 16** com **Row-Level Security (RLS)** ligado em toda tabela com dado de aluna.
- **Flyway** para migrações versionadas.
- **MapStruct** para mapeamento entidade↔DTO.
- **springdoc-openapi** para gerar o contrato consumido pelo mobile.
- **Argon2** (Spring Security PasswordEncoder) para senhas.
- **JUnit 5** + **Testcontainers** (Postgres real nos testes de integração) + **AssertJ**.
- **Object storage** S3-compatível (laudos PDF, fotos) — URLs pré-assinadas de curta duração.

## Estrutura por módulo de domínio

Espelha os módulos da análise. Cada um é autocontido (controller, service, repository, dto, entity, mapper):

```
src/main/java/com/lumyra/
├── LumyraApplication.java
├── config/              # SecurityConfig, OpenApiConfig, RlsContext...
├── core/                # filtros JWT, exception handler, audit, RLS interceptor
├── shared/
│   ├── clinical/        # FUNÇÕES PURAS de cálculo clínico (sem Spring); testáveis isoladas
│   └── exception/
├── modules/
│   ├── identity/        # tenant, profissional, aluna, convite, consentimento LGPD
│   ├── assessment/      # avaliação: anamnese, composição corporal, dor, testes funcionais
│   ├── prescription/    # catálogo de exercícios, prescrição, análise de volume semanal
│   ├── cycle/           # ciclo menstrual: cálculo de fase, conteúdo educativo, sintomas
│   └── execution/       # execução de treino + RPE
└── ...

src/main/resources/
├── application.yml          # profiles: local, test, prod
├── db/migration/            # Flyway: V001__tenant.sql, V002__rls_policies.sql, ...
└── ...

src/test/java/com/lumyra/
├── integration/             # @SpringBootTest com Testcontainers
└── unit/                    # ServiceTest puro com mocks
```

## Regras invioláveis (resumo — detalhe em `docs/GUARDRAILS.md`)

Antes de finalizar qualquer tarefa, confira:

1. **Isolamento por tenant.** Toda tabela com dado de aluna tem `tenant_id` + **policy de RLS no Postgres**. Toda requisição autenticada injeta o tenant no contexto do banco antes de qualquer query. **Defesa em camadas**: filtro de aplicação + RLS no banco.
2. **Consentimento antes de dado sensível.** Nenhum dado de saúde (ciclo, dor, anamnese, composição) é persistido antes de a aluna registrar consentimento LGPD ativo (com data e versão do termo).
3. **Fórmulas clínicas têm golden tests.** Cálculo de %G (Siri), massa magra, índices (cintura/quadril, conicidade), volume semanal e fase do ciclo só entram com teste passando contra casos conhecidos do laudo real.
4. **Sem dado sensível em log.** Nunca logar conteúdo de saúde, senha, token. Logs estruturados (JSON), correlacionados por requestId + tenant.
5. **Nada de mock que finja ser real.** Se faltar decisão (fórmula, regra), **pare e pergunte** em vez de simular.

## Convenções específicas do backend

### Multi-tenancy / RLS — a regra nº 1
- Toda requisição autenticada passa por um filtro/interceptor que extrai `tenant_id` do JWT e executa `SET LOCAL app.current_tenant = ?` na conexão JDBC antes de qualquer query.
- Toda tabela com dado de aluna tem RLS habilitado (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`) e policy filtrando por `app.current_tenant`.
- Existe **teste de integração que tenta ler dado de outro tenant e DEVE falhar** — roda no CI. Nunca desabilitar.

### Cálculos clínicos
- Ficam em `shared/clinical/` como **classes utilitárias sem Spring** (POJOs), totalmente testáveis sem subir contexto.
- Golden tests usam o laudo real (Pirigo): `%G 6.59`, massa magra `84.06 kg`, soma 3 dobras `20 mm`.
- A função expõe qual protocolo/fórmula usou (Guedes & Guedes, Siri) — vai no laudo.

### Avaliação versionada
- Nunca sobrescrever uma avaliação finalizada. Reavaliação = novo registro (status `draft` → `finalized` → imutável).

### API
- **REST** em `/api/v1/...`. JSON. Documentada via **springdoc-openapi** (`/v3/api-docs` e Swagger UI em dev).
- DTOs com `@Valid` + Bean Validation (`@NotNull`, `@Email`, ranges).
- Erros padronizados via `@RestControllerAdvice` (formato problem+json ou similar). Nunca vazar stack trace.
- O **OpenAPI gerado é o contrato** com o repo mobile — toda mudança de breaking change sobe versão.

### Jobs assíncronos
- Geração de laudo PDF, push e e-mail de convite vão para fila (Spring `@Async` + fila in-memory, ou Spring Batch; reavaliar fila externa ao escalar). Síncrono só CRUD e cálculo.
- Laudo tem estado explícito: `PENDING | GENERATED | FAILED`. Reprocessamento idempotente.

### Migrações
- Toda migração que cria tabela com dado de aluna **já cria a policy de RLS junto** — nunca em migração separada "para depois".
- Migrações são **reversíveis** (rollback documentado, mesmo que manual).

## Comandos

```bash
./mvnw spring-boot:run                # API em dev
./mvnw flyway:migrate                 # rodar migrações
./mvnw test                           # unitários (sem Spring)
./mvnw verify                         # tudo: unit + integração (Testcontainers)
./mvnw spotless:apply                 # formatação
docker compose up -d                  # postgres local
```

## Definição de pronto (toda tarefa de backend)

- [ ] Testes unitários passando; cálculos clínicos com golden test
- [ ] Endpoint escopado por tenant + RLS na tabela
- [ ] Bean Validation no DTO; erro tratado pelo handler global
- [ ] OpenAPI atualizado; sem breaking change não anunciado
- [ ] Sem dado sensível em log
- [ ] Migração reversível com policy de RLS quando aplicável
- [ ] `./mvnw verify` verde

## Quando pedir ao Claude Code

Sempre referencie este arquivo e a skill: "leia `CLAUDE.md` e `docs/GUARDRAILS.md`; carregue a skill **backend-engineer**". Siga `docs/BACKLOG.md` na ordem. Idioma: código em inglês, comentários e mensagens ao usuário em pt-BR.
