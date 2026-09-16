package com.flowpay.domain;

import java.util.UUID;

public class CobrancaNaoEncontradaException extends RuntimeException {

    public CobrancaNaoEncontradaException(UUID id) {
        super("Cobrança não encontrada: " + id);
    }
}
