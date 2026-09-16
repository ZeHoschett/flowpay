package com.flowpay.infrastructure.webhook;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria de uma notificação de webhook: quantas tentativas
 * foram feitas, se teve sucesso, ou se atingiu o limite e foi marcada como
 * falha permanente. Serve também como base para, no futuro, evoluir para
 * o padrão Outbox (ver README, seção "Próximos passos").
 */
@Entity
@Table(name = "webhook_notificacao")
@Getter
@Setter
@NoArgsConstructor
public class WebhookNotificacao {

    @Id
    private UUID id;

    @Column(name = "cobranca_id", nullable = false)
    private UUID cobrancaId;

    @Column(name = "status_novo", nullable = false, length = 20)
    private String statusNovo;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnvio status;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public WebhookNotificacao(UUID cobrancaId, String statusNovo, String payload) {
        this.id = UUID.randomUUID();
        this.cobrancaId = cobrancaId;
        this.statusNovo = statusNovo;
        this.payload = payload;
        this.status = StatusEnvio.PENDENTE;
        this.tentativas = 0;
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void registrarTentativa() {
        this.tentativas++;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComoEnviada() {
        this.status = StatusEnvio.ENVIADA;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComoFalhaPermanente() {
        this.status = StatusEnvio.FALHA_PERMANENTE;
        this.atualizadoEm = Instant.now();
    }

    public enum StatusEnvio {
        PENDENTE, ENVIADA, FALHA_PERMANENTE
    }
}
