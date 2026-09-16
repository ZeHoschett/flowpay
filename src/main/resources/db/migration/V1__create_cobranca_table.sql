CREATE TABLE cobranca (
    id                          UUID PRIMARY KEY,
    cliente_id                  VARCHAR(255) NOT NULL,
    valor                       NUMERIC(19, 2) NOT NULL,
    vencimento                  DATE NOT NULL,
    referencia_servico_agendado VARCHAR(255),
    status                      VARCHAR(20) NOT NULL,
    criado_em                   TIMESTAMPTZ NOT NULL,
    atualizado_em               TIMESTAMPTZ NOT NULL,
    versao                      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_cobranca_cliente_id ON cobranca (cliente_id);
CREATE INDEX idx_cobranca_cliente_status ON cobranca (cliente_id, status);
