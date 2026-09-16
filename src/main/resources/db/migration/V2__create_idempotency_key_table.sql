CREATE TABLE idempotency_key (
    chave           VARCHAR(255) PRIMARY KEY,
    status_code     INTEGER NOT NULL,
    corpo_resposta  TEXT NOT NULL,
    hash_requisicao VARCHAR(64) NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL
);
