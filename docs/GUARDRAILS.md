# GUARDRAILS — Lumyra Backend (Spring Boot)

> Regras invioláveis. Vêm diretamente das seções de risco, segurança e privacidade da análise
> (`docs/ANALISE_DE_SISTEMAS.docx`). Violar qualquer uma é defeito grave, não estilo.
> Confira esta lista antes de finalizar qualquer tarefa.

## G1 — Isolamento entre tenants (a regra nº 1)

**Por quê:** vazar dado de uma aluna para outro profissional é o risco de maior impacto do sistema.

- Toda tabela com dado de aluna tem `tenant_id` e **policy de RLS** habilitada no Postgres.
- Toda requisição autenticada passa por filtro/interceptor que extrai `tenantId` do JWT e executa `SET LOCAL app.current_tenant = ?` na conexão JDBC, antes de qualquer query.
- A camada de serviço também filtra por tenant + vínculo aluna↔profissional (defesa em camadas).
- Existe **teste de integração com Testcontainers que tenta ler dado de outro tenant e DEVE falhar.** Roda no CI. **Nunca desabilitar.**
- A aluna só vê os próprios dados. O profissional só vê alunas do seu vínculo.

## G2 — Consentimento antes de dado de saúde (LGPD)

**Por quê:** ciclo, dor, anamnese e composição são dado pessoal sensível (LGPD Art. 11). Sem base legal, não pode tratar.

- Nenhum endpoint que persiste dado sensível aceita requisição se o consentimento da aluna não estiver ativo.
- Consentimento registrado com data e versão do termo (`consent.registered_at`, `consent.terms_version`).
- Endpoints obrigatórios: revogação, exportação (dados em JSON) e eliminação dos dados da aluna.
- Minimização: coletar apenas o necessário; não reusar dado de ciclo para outro fim sem novo consentimento.

## G3 — Cálculos clínicos corretos e testados

**Por quê:** um erro de fórmula produz laudo errado com aparência de certo — quase indetectável, alto impacto profissional.

- Funções de cálculo (Siri para %G, massa magra/gorda, índices, volume semanal, fase do ciclo) ficam em `shared/clinical/` como **classes utilitárias sem Spring** (POJOs).
- Cada uma tem **golden tests** com casos conhecidos. Caso de referência: laudo Pirigo — %G `6.59`, massa magra `84.06 kg`, soma 3 dobras `20 mm`.
- A função expõe explicitamente o protocolo/fórmula usado (Guedes & Guedes, Siri) — esse dado aparece no laudo.
- **Unknown a validar com profissional antes de cravar:** equações exatas, faixas de referência por sexo/idade, regra de fase para ciclos irregulares/anticoncepcional/amenorreia. Se não confirmado, **pergunte — não chute**.

## G4 — Segurança de dados

- Criptografia em trânsito (TLS 1.2+) e em repouso (Postgres com cripto no disco; object storage com SSE).
- Senhas com **Argon2** (Spring Security `Argon2PasswordEncoder`); JWT curto (~15min) + refresh rotativo; sessão revogável (lista de refresh tokens revogados em Redis).
- PDFs/fotos em bucket privado; **apenas URLs assinadas de curta duração** (TTL ≤ 5min).
- Segredos fora do código (variáveis de ambiente / Spring Cloud Config / cofre). Chaves em KMS.
- Rate limit no login (`/auth/login`) com bucket por IP + por usuário, anti brute-force.
- Validar toda entrada com Bean Validation (`@Valid`, `@NotNull`, `@Pattern`, ranges) — OWASP Top 10 no checklist.

## G5 — Sem dado sensível em log/telemetria

- Nunca logar conteúdo de saúde (ciclo, dor, anamnese, composição), senha, token, header `Authorization`.
- Logs estruturados (JSON, via Logback encoder estruturado), com `requestId` + `tenantId` + `userId` (pseudonimizados) para correlação.
- Nada de `e.printStackTrace()`; usar logger e logar **mensagem + tipo de erro**, sem payload sensível.
- Telemetria/analytics: zero dado sensível.

## G6 — Acesso assimétrico educadora/aluna

- O profissional escreve; a aluna **majoritariamente lê**.
- **Únicas escritas permitidas para a aluna**: RPE, questionário de dor, registro de ciclo/sintomas.
- `@PreAuthorize` (Spring Security) nos endpoints garante o papel e o vínculo. Nenhum endpoint de escrita acessível por aluna sem estar na lista acima.

## G7 — Honestidade de implementação

- Nada de mock que finja ser real, especialmente em cálculo clínico e persistência.
- Se faltar uma fórmula, uma decisão, uma credencial: **pare e avise**. Não simule.
- Não tomar decisão de arquitetura por conta própria — as decisões estão na análise (ADR-1..4). Dúvida nova = perguntar.

## G8 — Avaliação imutável / versionada

- Avaliação tem estado: `DRAFT → FINALIZED`. Após `FINALIZED`, é imutável.
- Reavaliação = nova entidade `Assessment`. Preserva histórico e protege juridicamente o profissional.
- Migração de schema que tocar `assessment` precisa considerar imutabilidade (sem `UPDATE` em registros finalizados).

---

## Checklist rápido de fim de tarefa

- [ ] **G1** — tenant isolado: filtro JDBC + policy RLS + teste de isolamento verde
- [ ] **G2** — consentimento ativo verificado antes de persistir dado sensível
- [ ] **G3** — fórmula clínica com golden test passando
- [ ] **G4** — senha Argon2, JWT curto, validação Bean, sem segredo no código
- [ ] **G5** — log sem dado sensível, com requestId + tenantId
- [ ] **G6** — `@PreAuthorize` impedindo aluna de escrever fora das exceções
- [ ] **G7** — nada simulado; dúvidas levantadas
- [ ] **G8** — avaliação finalizada não pode ser editada
