package com.flowpay.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsável por notificar o consumidor externo quando o status de uma
 * cobrança muda.
 *
 * Estratégia de retry: se a chamada HTTP falhar, tentamos de novo com
 * backoff exponencial (1s, 2s, 4s, 8s, ...) até `flowpay.webhook.max-tentativas`
 * tentativas. Depois disso, a notificação é marcada como falha permanente
 * e fica registrada na tabela webhook_notificacao para inspeção manual.
 *
 * Nota: hoje o disparo acontece de forma assíncrona logo após o commit da
 * transação que mudou o status. Isso é suficiente para a v1, mas tem uma
 * lacuna conhecida: se o processo cair entre o commit e o envio, a
 * notificação se perde. O padrão Outbox (ver README) resolve isso
 * persistindo o evento na mesma transação do banco.
 */
@Service
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookNotificacaoRepository repository;
    private final WebhookProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public WebhookSender(RestTemplate restTemplate,
                          ObjectMapper objectMapper,
                          WebhookNotificacaoRepository repository,
                          WebhookProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.properties = properties;
    }

    public void notificar(WebhookPayload payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Falha ao serializar payload de webhook para a cobrança {}", payload.cobrancaId(), e);
            return;
        }

        WebhookNotificacao notificacao = new WebhookNotificacao(
                payload.cobrancaId(),
                payload.statusNovo().name(),
                payloadJson
        );
        repository.save(notificacao);

        tentarEnviar(notificacao.getId(), payloadJson);
    }

    private void tentarEnviar(java.util.UUID notificacaoId, String payloadJson) {
        WebhookNotificacao notificacao = repository.findById(notificacaoId).orElse(null);
        if (notificacao == null || notificacao.getStatus() != WebhookNotificacao.StatusEnvio.PENDENTE) {
            return;
        }

        notificacao.registrarTentativa();
        repository.save(notificacao);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);

            restTemplate.postForEntity(properties.getUrl(), request, Void.class);

            notificacao.marcarComoEnviada();
            repository.save(notificacao);
            log.info("Webhook enviado com sucesso para a cobrança {} (tentativa {})",
                    notificacao.getCobrancaId(), notificacao.getTentativas());

        } catch (RestClientException e) {
            log.warn("Falha ao enviar webhook para a cobrança {} (tentativa {}/{}): {}",
                    notificacao.getCobrancaId(), notificacao.getTentativas(),
                    properties.getMaxTentativas(), e.getMessage());

            if (notificacao.getTentativas() >= properties.getMaxTentativas()) {
                notificacao.marcarComoFalhaPermanente();
                repository.save(notificacao);
                log.error("Webhook para a cobrança {} marcado como FALHA PERMANENTE após {} tentativas",
                        notificacao.getCobrancaId(), notificacao.getTentativas());
                return;
            }

            long delaySegundos = (long) (properties.getDelayBaseSegundos()
                    * Math.pow(2, notificacao.getTentativas() - 1));
            scheduler.schedule(() -> tentarEnviar(notificacaoId, payloadJson), delaySegundos, TimeUnit.SECONDS);
        }
    }
}
