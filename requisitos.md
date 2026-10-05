# Requisitos e critérios de aceitação

API de estudo para orçamento de departamentos e solicitações internas. Uma aprovação reserva orçamento; não efetua pagamento. Solicitante e aprovador são papéis conceituais. Nesta versão não existe autenticação: o responsável informado é fictício e não comprova autoria.

## Regras

1. **R1 — Orçamento:** total positivo; comprometido inicialmente zero.
2. **R2 — Solicitação:** departamento existente, descrição preenchida, valor positivo e situação inicial PENDENTE.
3. **R3 — Aprovação:** apenas PENDENTE e com saldo suficiente.
4. **R4 — Reserva:** aprovação compromete o valor exatamente uma vez.
5. **R5 — Rejeição:** apenas PENDENTE, com justificativa; não altera orçamento.
6. **R6 — Histórico:** cada decisão registra situação anterior/nova, responsável e instante.
7. **R7 — Atomicidade:** reserva, situação e histórico confirmados ou desfeitos juntos.

## Aceitação

- Orçamento 10.000,00 e solicitação 7.000,00: aprovação deixa saldo 3.000,00 e um histórico.
- Saldo 3.000,00 e pedido 7.000,00: recusa sem alterar saldo, situação ou histórico.
- Repetir decisão: conflito; não duplica reserva nem histórico.
- Rejeitar: exige justificativa e preserva saldo.
- Falha durante histórico: rollback das três alterações, verificado no PostgreSQL fora da transação com falha.
- Duas aprovações simultâneas de 7.000,00 em orçamento 10.000,00: apenas uma pode ter sucesso.
- Persistência após reinício; migrations de banco vazio e atualização preservando dados.
- Validação de campos, filtros e paginação; 201/200, 400, 404 e 409 documentados.

## Escopo e integridade

Uma aplicação Java e um PostgreSQL. Sem alteração de valor/departamento de pedidos, exclusão, cancelamento, transferência, renovação anual, pagamento, frontend ou autenticação nesta entrega. Dinheiro usa BigDecimal / NUMERIC(14,2), rejeitando mais de duas casas e valores fora da precisão. Saldo é calculado. Instantes usam Instant / TIMESTAMPTZ. Banco garante constraints simples; serviço garante transições e transações. Histórico não possui edição ou exclusão via API, mas não é inviolável ao administrador do banco.
