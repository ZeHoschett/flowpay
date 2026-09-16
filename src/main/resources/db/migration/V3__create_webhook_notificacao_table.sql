CREATE TABLE webhook_notificacao (
    id             UUID PRIMARY KEY,
    cobranca_id    UUID NOT NULL REFERENCES cobranca (id),
    status_novo    VARCHAR(20) NOT NULL,
    payload        TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    tentativas     INTEGER NOT NULL DEFAULT 0,
    criado_em      TIMESTAMPTZ NOT NULL,
    atualizado_em  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_webhook_notificacao_cobranca_id ON webhook_notificacao (cobranca_id);
