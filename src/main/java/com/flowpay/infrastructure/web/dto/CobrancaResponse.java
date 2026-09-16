package com.flowpay.infrastructure.web.dto;

import com.flowpay.domain.Cobranca;
import com.flowpay.domain.StatusCobranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CobrancaResponse(
        UUID id,
        String clienteId,
        BigDecimal valor,
        LocalDate vencimento,
        String referenciaServicoAgendado,
        StatusCobranca status,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static CobrancaResponse from(Cobranca cobranca) {
        return new CobrancaResponse(
                cobranca.getId(),
                cobranca.getClienteId(),
                cobranca.getValor(),
                cobranca.getVencimento(),
                cobranca.getReferenciaServicoAgendado(),
                cobranca.getStatus(),
                cobranca.getCriadoEm(),
                cobranca.getAtualizadoEm()
        );
    }
}
