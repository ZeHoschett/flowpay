package com.flowpay.application;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String chave) {
        super("A chave de idempotência '%s' já foi usada com um corpo de requisição diferente"
                .formatted(chave));
    }
}
