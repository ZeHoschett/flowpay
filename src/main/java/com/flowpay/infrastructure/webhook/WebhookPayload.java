package com.flowpay.infrastructure.webhook;

import com.flowpay.domain.StatusCobranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WebhookPayload(
        UUID cobrancaId,
        String clienteId,
        BigDecimal valor,
        StatusCobranca statusAnterior,
        StatusCobranca statusNovo,
        Instant ocorridoEm
) {
}
