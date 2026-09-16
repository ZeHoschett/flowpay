package com.flowpay.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarCobrancaRequest(

        @NotBlank(message = "clienteId é obrigatório")
        String clienteId,

        @NotNull(message = "valor é obrigatório")
        @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "vencimento é obrigatório")
        @FutureOrPresent(message = "vencimento não pode estar no passado")
        LocalDate vencimento,

        String referenciaServicoAgendado
) {
}
