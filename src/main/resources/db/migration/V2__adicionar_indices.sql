CREATE INDEX idx_solicitacao_departamento_status_id ON solicitacao (departamento_id, status, id);
CREATE INDEX idx_historico_solicitacao_id ON historico_solicitacao (solicitacao_id, id);
