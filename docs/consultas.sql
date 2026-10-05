-- =====================================================================
-- gestao-verbas — consultas SQL de verificação (somente leitura)
-- Execute contra o banco local: docker-compose -f compose.yaml exec db psql -U verbas -d verbas
-- NENHUMA escrita destrutiva neste arquivo (sem UPDATE/DELETE/DROP).
-- =====================================================================

-- 1) Solicitações com dados do departamento (JOIN)
SELECT s.id            AS solicitacao_id,
       s.descricao,
       s.valor,
       s.status,
       s.criado_em,
       d.id            AS departamento_id,
       d.nome          AS departamento_nome,
       d.orcamento_total,
       d.valor_comprometido
FROM solicitacao s
JOIN departamento d ON d.id = s.departamento_id
ORDER BY s.id;

-- 2) Total APROVADO por departamento, incluindo departamentos sem aprovações
--    (LEFT JOIN + agregado condicional para incluir zeros)
SELECT d.id,
       d.nome,
       d.orcamento_total,
       COALESCE(SUM(s.valor) FILTER (WHERE s.status = 'APROVADA'), 0) AS total_aprovado
FROM departamento d
LEFT JOIN solicitacao s ON s.departamento_id = d.id
GROUP BY d.id, d.nome, d.orcamento_total
ORDER BY d.id;

-- 3) Reconciliação: departamentos cujo valor_comprometido diverge do
--    somatório das solicitações APROVADAS. Deve retornar ZERO linhas.
--    Se retornar linhas, houve inconsistência (ex.: decisão não atômica).
-- Reconciliação de divergências
SELECT d.id,
       d.nome,
       d.valor_comprometido,
       COALESCE(SUM(s.valor) FILTER (WHERE s.status = 'APROVADA'), 0) AS total_aprovado,
       d.valor_comprometido
         - COALESCE(SUM(s.valor) FILTER (WHERE s.status = 'APROVADA'), 0) AS diferenca
FROM departamento d
LEFT JOIN solicitacao s ON s.departamento_id = d.id
GROUP BY d.id, d.nome, d.valor_comprometido
HAVING d.valor_comprometido
       <> COALESCE(SUM(s.valor) FILTER (WHERE s.status = 'APROVADA'), 0)
ORDER BY d.id;

-- 4) Histórico de decisões de uma solicitação
SELECT h.id, h.solicitacao_id, h.status_anterior, h.status_novo,
       h.responsavel, h.justificativa, h.criado_em
FROM historico_solicitacao h
WHERE h.solicitacao_id = 1
ORDER BY h.criado_em, h.id;

-- =====================================================================
-- Plano de execução: filtro por departamento/status ordenado por id.
-- Em produção use parâmetros ($1, $2); aqui o exemplo com literais reais
-- para documentar o plano esperado:
--
--   EXPLAIN (ANALYZE, BUFFERS)
--   SELECT id, departamento_id, descricao, valor, status, criado_em
--   FROM solicitacao
--   WHERE departamento_id = 1 AND status = 'PENDENTE'
   -- ORDER BY id;
--
-- Interpretação:
--   * Em tabelas pequenas o PostgreSQL normalmente escolhe SEQ SCAN
--     (varredura sequencial) mesmo existindo índice — isso é NORMAL e
--     correto: o planejador estima que ler a tabela inteira custa menos
--     que percorrer o índice. Não é problema a ser corrigido.
--   * Com crescimento do volume, o índice adicionado em V2__adicionar_indices.sql
--     (departamento_id, status, id) pode beneficiar a consulta; confirme no plano.
--   * BUFFERS mostra páginas lidas (shared hit/read); útil para comparar
--     antes/depois de criar índices.
-- =====================================================================

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, departamento_id, descricao, valor, status, criado_em
FROM solicitacao WHERE departamento_id = 1 AND status = 'PENDENTE' ORDER BY id;
