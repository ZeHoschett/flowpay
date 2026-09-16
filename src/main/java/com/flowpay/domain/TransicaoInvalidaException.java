package com.flowpay.domain;

public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(StatusCobranca de, StatusCobranca para) {
        super("Transição inválida: não é possível mover a cobrança de %s para %s"
                .formatted(de, para));
    }
}
