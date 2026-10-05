# Roteiro de Estudo — gestao-verbas

Este roteiro organiza a exploração do código em módulos, com **um pequeno exercício por módulo**. O código completo já está presente no repositório — não é um tutorial interativo bloqueado por etapas; use os exercícios como forma ativa de consolidar o entendimento, no seu ritmo.

Leituras de apoio: [`arquitetura.md`](arquitetura.md) e [`requisitos.md`](../requisitos.md).

---

## Ordem de exploração sugerida

1. **`shared`** — fundação: exceções de domínio, handler global de erros, validação monetária.
2. **`departamento`** — entidade mais simples; introduz o padrão camadas.
3. **`solicitacao`** — regras de status e o protocolo de bloqueio pessimista.
4. **`historico`** — trilha de auditoria e atomicidade das decisões.
5. **Migrations e testes** — schema Flyway e testes unitários/integração.

---

## Módulo 1 — `shared` (fundação)

**O que estudar:**
- Exceções de domínio (ex.: `RecursoNaoEncontradoException`, `RegraNegocioException`, `ValidacaoException`) e como o `@RestControllerAdvice` as converte em HTTP 400/404/409.
- Validador monetário: como `BigDecimal` é checado para escala (máx. 2 casas), positividade e limite `999999999999.99`.

**Camadas explicadas:**
- *Shared* não é uma camada vertical, mas o suporte transversal: erros e validações usados por todos os módulos.

**Exercício:** adicione uma nova exceção `NomeDuplicadoException` (hipotética) e um caso no handler que a mapeie para HTTP 409. Verifique a resposta JSON sem alterar services.

---

## Módulo 2 — `departamento`

**O que estudar:**
- Entidade JPA: mapeamento `NUMERIC(14,2)` e `TIMESTAMPTZ` (`Instant`).
- Service: criação com `valorComprometido = 0` e cálculo de `saldoDisponivel` no DTO.
- Repository: método de bloqueio pessimista (usado pelo módulo `solicitacao`).
- Controller: POST retornando 201 + `Location`, GET por id.

**Camadas explicadas:**
- *Controller* recebe/valida HTTP; *Service* aplica regras; *Repository* persiste; *DTO* isola o contrato de API da entidade.

**Exercício:** no DTO de resposta, `saldoDisponivel` é derivado (`orcamentoTotal - valorComprometido`). Refaça o cálculo mentalmente para um departamento com orçamento 10000 e comprometido 7000 e confirme via `GET /departamentos/{id}`.

---

## Módulo 3 — `solicitacao`

**O que estudar:**
- Enum de status e transições permitidas (somente `PENDENTE` pode ser decidida).
- **Protocolo de bloqueio:** em uma única transação — (1) bloquear o departamento com lock pessimista, (2) carregar a solicitação, (3) validar status e saldo, (4) atualizar saldo e gravar histórico. Aprovação e rejeição seguem o mesmo protocolo.
- Paginação: `page`/`size` (máx. 100) e filtros `departamentoId`/`status`.

**Camadas explicadas:**
- A ordem de bloqueio (departamento primeiro) reduz o risco de deadlocks neste fluxo: todas as transações adquirem recursos na mesma ordem.

**Exercício:** leia o método de aprovação e identifique: onde o HTTP 409 de saldo insuficiente é gerado; onde o 409 de decisão duplicada; por que o bloqueio ocorre antes de carregar a solicitação. Anote as linhas correspondentes.

---

## Módulo 4 — `historico`

**O que estudar:**
- Registro de transições (`status_anterior` → `status_novo`, `responsavel`, `justificativa`).
- Como a gravação do histórico participa da MESMA transação da decisão (atomicidade: ou tudo persiste, ou nada).
- Imutabilidade: sem endpoints de edição/exclusão.

**Exercício:** execute a demo (`scripts/demo.ps1`) e depois leia o histórico da solicitação aprovada com `GET /solicitacoes/{id}/historico`; confira `statusAnterior = PENDENTE` e `statusNovo = APROVADA`.

---

## Módulo 5 — Migrations e testes

**O que estudar:**
- `V1__criar_estrutura.sql` (tabelas, constraints, FKs) e `V2__adicionar_indices.sql` (índices de consulta).
- Flyway com JPA `validate` e `open-in-view: false` — por que o schema é fonte de verdade no banco.
- Testes unitários (regras isoladas, sem Docker) vs. integração (Testcontainers, PostgreSQL real isolado).

**Exercício:** rode `.\mvnw.cmd test` (unitários) e, com Docker ativo, `.\mvnw.cmd -Pintegration verify`. Compare: qual dos dois consegue provar rollback real e concorrência real? Por quê? (Resposta esperada: apenas integração — unitários simulam dependências e não exercitam transações/bloqueios do banco.)

> Validação registrada em [validacao.md](validacao.md). No ambiente de Gabriel, execute a integração por `sh scripts/testar-wsl.sh` dentro do Ubuntu WSL.

---

## Checklist final de entendimento

- [ ] Sei onde cada regra de negócio vive (camada responsável).
- [ ] Explico por que o bloqueio pessimista é adquirido no departamento antes da solicitação.
- [ ] Sei diferenciar aprovação (reserva) de pagamento (fora de escopo).
- [ ] Entendo por que `responsavel` não é confiável hoje e como a autenticação futura o extrairá de contexto do servidor.
- [ ] Sei ler um plano `EXPLAIN (ANALYZE, BUFFERS)` e por que SEQ SCAN em tabelas pequenas é normal.
