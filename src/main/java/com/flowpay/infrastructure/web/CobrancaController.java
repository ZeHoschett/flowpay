package com.flowpay.infrastructure.web;

import com.flowpay.application.CobrancaService;
import com.flowpay.domain.StatusCobranca;
import com.flowpay.infrastructure.web.dto.CobrancaResponse;
import com.flowpay.infrastructure.web.dto.CriarCobrancaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cobrancas")
@Tag(name = "Cobranças", description = "Criação e acompanhamento do ciclo de vida de cobranças")
public class CobrancaController {

    private final CobrancaService cobrancaService;

    public CobrancaController(CobrancaService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    @PostMapping
    @Operation(summary = "Cria uma nova cobrança",
            description = "Envie o header Idempotency-Key para tornar a requisição segura contra retries.")
    public ResponseEntity<CobrancaResponse> criar(
            @Valid @RequestBody CriarCobrancaRequest request,
            @Parameter(description = "Chave de idempotência opcional, mas recomendada")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        CobrancaResponse cobranca = cobrancaService.criar(request, idempotencyKey);
        return ResponseEntity
                .created(URI.create("/cobrancas/" + cobranca.id()))
                .body(cobranca);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma cobrança por ID")
    public ResponseEntity<CobrancaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(cobrancaService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Lista cobranças de um cliente, opcionalmente filtradas por status")
    public ResponseEntity<List<CobrancaResponse>> listar(
            @RequestParam String clienteId,
            @RequestParam(required = false) StatusCobranca status) {
        return ResponseEntity.ok(cobrancaService.listarPorCliente(clienteId, status));
    }

    @PostMapping("/{id}/pagar")
    @Operation(summary = "Marca uma cobrança como paga",
            description = "Simula o webhook de um gateway de pagamento avisando que o valor foi recebido.")
    public ResponseEntity<CobrancaResponse> marcarComoPaga(@PathVariable UUID id) {
        return ResponseEntity.ok(cobrancaService.marcarComoPaga(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma cobrança pendente")
    public ResponseEntity<CobrancaResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(cobrancaService.cancelar(id));
    }
}
