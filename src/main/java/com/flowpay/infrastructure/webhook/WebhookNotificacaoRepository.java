package com.flowpay.infrastructure.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookNotificacaoRepository extends JpaRepository<WebhookNotificacao, UUID> {
}
