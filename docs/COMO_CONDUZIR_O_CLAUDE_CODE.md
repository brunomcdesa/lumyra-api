# Como conduzir o Claude Code para construir o Lumyra (backend)

Guia prático de operação no dia a dia. Os "o quê" e "como" já estão nos outros arquivos; aqui é o "como usar".

## As camadas e o que cada uma faz

| Arquivo | Papel | Quando é lido |
|---|---|---|
| `CLAUDE.md` | Contexto sempre-presente: stack, regras, comandos | Toda sessão, automaticamente |
| `docs/GUARDRAILS.md` | Regras invioláveis (G1–G8) | Referenciado pelo CLAUDE.md; cite nas tarefas |
| `docs/BACKLOG.md` | O que construir e em que ordem | Você cola tarefa por tarefa |
| `docs/ANALISE_DE_SISTEMAS.docx` | Spec do produto (fonte da verdade) | Quando precisar de detalhe de requisito/arquitetura |
| Skill `backend-engineer` | Como fazer bem (Spring Boot, NestJS, etc.) | Carrega sozinha pelo gatilho |

**Regra de divisão:** específico do Lumyra → CLAUDE.md/docs. Genérico de engenharia → skill. Não duplique.

## Por que não criar uma skill "lumyra"

Skills servem para conhecimento **reutilizável entre projetos**. O Lumyra é um projeto único; seu conhecimento específico pertence ao repo (CLAUDE.md + docs), versionado junto do código. Sua skill `backend-engineer` já cobre Java/Spring Boot a fundo — deixe-a genérica. Skill por projeto vira manutenção dobrada e gatilhos que se atropelam.

## O loop de trabalho (uma tarefa por vez)

1. Abra o Claude Code na raiz do repo. Ele lê o `CLAUDE.md` automaticamente.
2. Cole **uma** tarefa do `BACKLOG.md` usando o modelo do fim daquele arquivo.
3. Deixe implementar. A skill `backend-engineer` carrega pelo gatilho. Os guardrails citados na tarefa devem ser respeitados.
4. Rode `./mvnw verify` (unit + integração com Testcontainers) e revise o critério de aceite.
5. Commit pequeno e descritivo. Só então a próxima tarefa.

Tarefas pequenas e frequentes batem tarefas grandes: o Claude Code erra menos com escopo estreito, e você revisa melhor.

## Onde o caminho costuma entortar (e como evitar)

- **Pular a Fase 0.** É a tentação clássica — querer chegar logo na avaliação. Não. Sem RLS (T0.3) e consentimento (T0.5), todo dado de aluna depois nasce inseguro.
- **Deixar cálculo clínico sem golden test.** É o erro mais caro e mais silencioso. T1.1 é antes de T1.4 por isso.
- **Construir o ciclo cedo.** Diferencial dá vontade, mas T3.4 tem um aviso explícito: validar conteúdo e regra de fase **antes** de implementar.
- **Deixar o Claude Code "decidir" arquitetura.** As decisões estão na análise (ADR-1..4). Se ele propuser trocar de banco, virar microsserviço ou pular o RLS por simplicidade, **recuse**.
- **Tarefa grande demais.** "Implemente a avaliação inteira" → ele se perde. Quebre como o backlog já faz.

## Comandos úteis no Claude Code

- **Leitura antes de ação:** "leia `CLAUDE.md` e `docs/GUARDRAILS.md` antes de codar."
- **Plano antes de código** em tarefas M/L: "primeiro me mostre o plano e os arquivos que vai tocar; espere meu ok."
- **Prova de guardrail:** "mostre o teste de isolamento de tenant verde" (T0.3), "mostre o golden test do cálculo de %G" (T1.1).
- **Forçar honestidade:** "se faltar uma decisão (fórmula, regra), pare e me pergunte em vez de inventar."
- **Diff focado:** "me mostre apenas os arquivos novos e modificados, não rode resumos genéricos."

## Sincronia com o repo mobile

Pontos de sincronia estão marcados com 🔗 no backlog. Após concluir um marco (F0-PRONTO, F1-PRONTO, etc.):

1. Suba `/v3/api-docs` (OpenAPI) atualizado no ambiente que o mobile consome.
2. Avise o mobile (mensagem, PR comment, etc.) — ele precisa rodar `npm run gen:api`.
3. Se houve **breaking change** de API, **suba a versão** (`/api/v1` → `/api/v2`) e mantenha v1 por um ciclo de release. App mobile demora a atualizar.

## Ordem de montagem do repo (primeira vez)

1. Crie o repo `lumyra-backend` e copie estes arquivos.
2. Coloque `Lumyra_Analise_de_Sistemas.docx` em `docs/ANALISE_DE_SISTEMAS.docx`.
3. Abra o Claude Code na raiz. Cole **T0.1**.
4. Siga o backlog na ordem.

```
lumyra-backend/
├── CLAUDE.md
└── docs/
    ├── ANALISE_DE_SISTEMAS.docx
    ├── BACKLOG.md
    ├── GUARDRAILS.md
    └── COMO_CONDUZIR_O_CLAUDE_CODE.md   (este arquivo)
```
