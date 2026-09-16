package com.flowpay.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Guarda, por chave de idempotência (header Idempotency-Key), a resposta
 * já dada na primeira vez que essa chave foi usada para criar uma cobrança.
 * Se a mesma chave chegar de novo, devolvemos exatamente essa resposta em
 * vez de criar uma nova cobrança.
 */
@Entity
@Table(name = "idempotency_key")
@Getter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @Column(name = "chave", nullable = false, updatable = false, length = 255)
    private String chave;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "corpo_resposta", nullable = false, columnDefinition = "TEXT")
    private String corpoResposta;

    @Column(name = "hash_requisicao", nullable = false, length = 64)
    private String hashRequisicao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public IdempotencyRecord(String chave, int statusCode, String corpoResposta, String hashRequisicao) {
        this.chave = chave;
        this.statusCode = statusCode;
        this.corpoResposta = corpoResposta;
        this.hashRequisicao = hashRequisicao;
        this.criadoEm = Instant.now();
    }
}
