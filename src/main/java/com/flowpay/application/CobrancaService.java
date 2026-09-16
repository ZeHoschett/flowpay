package com.flowpay.application;

import com.flowpay.domain.Cobranca;
import com.flowpay.domain.CobrancaNaoEncontradaException;
import com.flowpay.domain.StatusCobranca;
import com.flowpay.infrastructure.persistence.CobrancaRepository;
import com.flowpay.infrastructure.web.dto.CobrancaResponse;
import com.flowpay.infrastructure.web.dto.CriarCobrancaRequest;
import com.flowpay.infrastructure.webhook.WebhookPayload;
import com.flowpay.infrastructure.webhook.WebhookSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;
    private final IdempotencyService idempotencyService;
    private final WebhookSender webhookSender;

    public CobrancaService(CobrancaRepository cobrancaRepository,
                            IdempotencyService idempotencyService,
                            WebhookSender webhookSender) {
        this.cobrancaRepository = cobrancaRepository;
        this.idempotencyService = idempotencyService;
        this.webhookSender = webhookSender;
    }

    @Transactional
    public CobrancaResponse criar(CriarCobrancaRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return criarNovaCobranca(request);
        }
        return idempotencyService.executar(
                idempotencyKey,
                request,
                CobrancaResponse.class,
                () -> criarNovaCobranca(request)
        );
    }

    private CobrancaResponse criarNovaCobranca(CriarCobrancaRequest request) {
        Cobranca cobranca = new Cobranca(
                request.clienteId(),
                request.valor(),
                request.vencimento(),
                request.referenciaServicoAgendado()
        );
        cobranca = cobrancaRepository.save(cobranca);
        return CobrancaResponse.from(cobranca);
    }

    @Transactional(readOnly = true)
    public CobrancaResponse buscarPorId(UUID id) {
        return CobrancaResponse.from(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<CobrancaResponse> listarPorCliente(String clienteId, StatusCobranca statusFiltro) {
        List<Cobranca> cobrancas = statusFiltro == null
                ? cobrancaRepository.findByClienteId(clienteId)
                : cobrancaRepository.findByClienteIdAndStatus(clienteId, statusFiltro);

        return cobrancas.stream().map(CobrancaResponse::from).toList();
    }

    @Transactional
    public CobrancaResponse marcarComoPaga(UUID id) {
        return transicionarEnotificar(id, StatusCobranca.PAGA);
    }

    @Transactional
    public CobrancaResponse cancelar(UUID id) {
        return transicionarEnotificar(id, StatusCobranca.CANCELADA);
    }

    private CobrancaResponse transicionarEnotificar(UUID id, StatusCobranca novoStatus) {
        Cobranca cobranca = buscarEntidadePorId(id);
        StatusCobranca statusAnterior = cobranca.getStatus();

        cobranca.transicionarPara(novoStatus);
        cobranca = cobrancaRepository.save(cobranca);

        agendarNotificacaoAposCommit(cobranca, statusAnterior);

        return CobrancaResponse.from(cobranca);
    }

    /**
     * Dispara o webhook só depois que a transação for confirmada no banco,
     * para não notificar uma mudança de status que acabou sendo revertida.
     * Essa é a lacuna que o padrão Outbox (próximo passo, ver README) fecha
     * de forma mais robusta.
     */
    private void agendarNotificacaoAposCommit(Cobranca cobranca, StatusCobranca statusAnterior) {
        WebhookPayload payload = new WebhookPayload(
                cobranca.getId(),
                cobranca.getClienteId(),
                cobranca.getValor(),
                statusAnterior,
                cobranca.getStatus(),
                Instant.now()
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webhookSender.notificar(payload);
                }
            });
        } else {
            webhookSender.notificar(payload);
        }
    }

    private Cobranca buscarEntidadePorId(UUID id) {
        return cobrancaRepository.findById(id)
                .orElseThrow(() -> new CobrancaNaoEncontradaException(id));
    }
}
