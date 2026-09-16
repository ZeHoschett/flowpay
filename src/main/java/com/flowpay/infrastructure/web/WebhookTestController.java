package com.flowpay.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumidor de teste do webhook. Em produção, quem consumiria isso seria
 * um sistema externo (ex.: AgendaFlow). Para a v1, este endpoint só recebe
 * e loga o payload, para provar que a mecânica de notificação funciona.
 */
@RestController
@Tag(name = "Webhook (teste)", description = "Endpoint de teste que simula um consumidor externo do webhook")
public class WebhookTestController {

    private static final Logger log = LoggerFactory.getLogger(WebhookTestController.class);

    @PostMapping("/webhooks/test-receiver")
    @Operation(summary = "Recebe e loga uma notificação de webhook (uso apenas para testes/demo)")
    public ResponseEntity<Void> receber(@RequestBody String payload) {
        log.info("Webhook de teste recebido: {}", payload);
        return ResponseEntity.ok().build();
    }
}
