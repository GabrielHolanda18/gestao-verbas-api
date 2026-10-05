# Evidências de validação — 05/10/2026

## Resultado

**33 testes aprovados; zero falhas, zero erros e zero testes ignorados.** Executados com `sh scripts/testar-wsl.sh`, usando Maven Wrapper 3.9.16, Java 21, Spring Boot 3.5.16 e PostgreSQL 16.6 real no Docker do Ubuntu WSL2. A execução completa terminou com `BUILD SUCCESS` em aproximadamente 55 segundos (dependências já no cache).

| Suíte | Execuções | Falhas/erros/ignorados | Cobertura principal |
|---|---:|---|---|
| DominioTest | 9 | 0 / 0 / 0 | Dinheiro, reserva, saldo e transições |
| ApiIT | 22 | 0 / 0 / 0 | HTTP, validação, constraints, rollback e concorrência |
| MigrationIT | 2 | 0 / 0 / 0 | Banco vazio e atualização V1 → V2 preservando dados |

Relatórios de cada execução são gerados em `target/surefire-reports` e `target/failsafe-reports` e não são versionados. As 22 execuções de API incluem casos parametrizados e três repetições de cada disputa concorrente; não significam 22 métodos diferentes.

## Atomicidade e concorrência

- Aprovação de 7.000,00 com orçamento 10.000,00 reserva uma vez e gera um histórico.
- Aprovação sem saldo mantém pedido pendente e não altera reserva ou histórico.
- Repetir aprovação ou decidir novamente pedido encerrado retorna 409.
- Rejeição exige justificativa e preserva orçamento.
- Duas aprovações de pedidos distintos, duas aprovações do mesmo pedido e aprovação versus rejeição do mesmo pedido foram exercitadas com requisições simultâneas e transações separadas. O resultado foi exatamente uma resposta 200 e uma 409, com saldo e histórico coerentes.
- O teste de rollback instala um gatilho **somente no PostgreSQL temporário dos testes**. Ele verifica a reserva e a situação já gravadas pela transação e provoca uma exceção antes da inserção do histórico. Após a requisição falhar com 500, consultas JDBC fora da transação confirmam comprometido zero, pedido PENDENTE e nenhum histórico. O gatilho é removido em `finally`; não há endpoint de falha na aplicação.

## Execução da aplicação e demonstração

O Dockerfile foi construído e a aplicação iniciada com `docker-compose -f compose.yaml --profile app up --build -d` no Ubuntu. PostgreSQL saudável e JPA validando o esquema criado pelo Flyway.

O script `scripts/demo.ps1` foi executado com **Windows PowerShell 5.1** contra a API do WSL. Foram observados:

1. Departamento 1 criado com orçamento 10.000,00.
2. Pedidos 1 e 2 criados com valor 7.000,00 cada.
3. Pedido 1 APROVADA; comprometido 7.000,00; disponível 3.000,00.
4. Um histórico PENDENTE → APROVADA com responsável fictício e instante.
5. Pedido 2 recusado com HTTP 409 e permanecendo PENDENTE.
6. Script encerrado com código zero e verificações explícitas do estado final.

Após reiniciar somente o container da API, foram novamente consultados departamento, pedido e histórico. Saldo, situação, ID e instante do histórico permaneceram iguais. Essa é uma verificação de persistência separada dos 33 testes automatizados.

As consultas de `docs/consultas.sql` foram executadas com `ON_ERROR_STOP=1`. JOIN e agregação retornaram os dados esperados; a reconciliação retornou **zero divergências**. O EXPLAIN dessa pequena amostra utilizou `idx_solicitacao_departamento_status_id`; isso não é promessa de desempenho com outros volumes ou estatísticas.

## Ajustes encontrados na revisão

- Correção de validação de escala monetária para rejeitar zeros decimais excedentes.
- Integração do validador monetário aos construtores e à reserva.
- Correções de imports, mapeamento de Instant nos DTOs, acesso entre módulos e limite da paginação.
- Constraint de responsável não vazio no histórico, incluída antes da primeira aplicação da V1.
- Correção dos testes de exceção SQL e fortalecimento do teste de rollback.
- Mockito configurado com `mock-maker-subclass` nos testes para compatibilidade com o container. A primeira tentativa com o mecanismo padrão de anexação dinâmica falhou; a execução documentada acima ocorreu após o ajuste.
- Correções de compatibilidade do script com PowerShell 5.1 e validação do SQL recebido.

## Limites

Estes testes demonstram o comportamento dos cenários descritos no ambiente local. Não são teste de carga, auditoria de segurança completa ou validação de produção. Não há autenticação/autorização. O responsável informado não comprova autoria e o histórico não é inviolável contra acesso direto ao banco. Não há frontend.

O WSL encerrou os containers quando sua sessão foi fechada nesta máquina. Durante o uso mantenha um terminal Ubuntu aberto, conforme `ambiente-wsl.md`. Os dados da demonstração ficam no volume nomeado do projeto.
