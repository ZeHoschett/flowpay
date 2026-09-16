package com.flowpay.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cobranca")
@Getter
@NoArgsConstructor
public class Cobranca {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private String clienteId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(name = "referencia_servico_agendado")
    private String referenciaServicoAgendado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCobranca status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    private Long versao;

    public Cobranca(String clienteId, BigDecimal valor, LocalDate vencimento, String referenciaServicoAgendado) {
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.valor = valor;
        this.vencimento = vencimento;
        this.referenciaServicoAgendado = referenciaServicoAgendado;
        this.status = StatusCobranca.PENDENTE;
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Aplica uma transição de estado, validando se ela é permitida.
     * Lança TransicaoInvalidaException se a transição não fizer parte
     * do conjunto de transições permitidas do estado atual.
     */
    public void transicionarPara(StatusCobranca novoStatus) {
        if (!this.status.podeTransicionarPara(novoStatus)) {
            throw new TransicaoInvalidaException(this.status, novoStatus);
        }
        this.status = novoStatus;
        this.atualizadoEm = Instant.now();
    }
}
