package com.flowpay.integration;

import com.flowpay.infrastructure.web.dto.CobrancaResponse;
import com.flowpay.infrastructure.web.dto.CriarCobrancaRequest;
import com.flowpay.infrastructure.webhook.WebhookNotificacao;
import com.flowpay.infrastructure.webhook.WebhookNotificacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CobrancaControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebhookNotificacaoRepository webhookNotificacaoRepository;

    private static final String BASE_URL = "/cobrancas";

    private HttpHeaders headersComApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", "test-api-key");
        return headers;
    }

    private CriarCobrancaRequest requestPadrao() {
        return new CriarCobrancaRequest("cliente-abc", new BigDecimal("150.00"),
                LocalDate.now().plusDays(10), "agenda-999");
    }

    @Test
    void criaUmaCobrancaEConsegueBuscarPorId() {
        HttpEntity<CriarCobrancaRequest> entity = new HttpEntity<>(requestPadrao(), headersComApiKey());

        ResponseEntity<CobrancaResponse> criada = restTemplate.postForEntity(BASE_URL, entity, CobrancaResponse.class);

        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criada.getBody()).isNotNull();
        assertThat(criada.getBody().status().name()).isEqualTo("PENDENTE");

        ResponseEntity<CobrancaResponse> consultada = restTemplate.exchange(
                BASE_URL + "/" + criada.getBody().id(), HttpMethod.GET,
                new HttpEntity<>(headersComApiKey()), CobrancaResponse.class);

        assertThat(consultada.getBody().id()).isEqualTo(criada.getBody().id());
    }

    @Test
    void mesmaChaveDeIdempotenciaNaoDuplicaCobranca() {
        HttpHeaders headers = headersComApiKey();
        headers.set("Idempotency-Key", "chave-teste-123");
        HttpEntity<CriarCobrancaRequest> entity = new HttpEntity<>(requestPadrao(), headers);

        ResponseEntity<CobrancaResponse> primeira = restTemplate.postForEntity(BASE_URL, entity, CobrancaResponse.class);
        ResponseEntity<CobrancaResponse> segunda = restTemplate.postForEntity(BASE_URL, entity, CobrancaResponse.class);

        assertThat(primeira.getBody().id()).isEqualTo(segunda.getBody().id());

        ResponseEntity<List> lista = restTemplate.exchange(
                BASE_URL + "?clienteId=cliente-abc", HttpMethod.GET,
                new HttpEntity<>(headersComApiKey()), List.class);
        long ocorrencias = lista.getBody().stream()
                .filter(c -> ((java.util.Map<?, ?>) c).get("id").equals(primeira.getBody().id().toString()))
                .count();
        assertThat(ocorrencias).isEqualTo(1);
    }

    @Test
    void transicaoInvalidaRetornaErroNoFormatoProblemDetails() {
        HttpEntity<CriarCobrancaRequest> entity = new HttpEntity<>(requestPadrao(), headersComApiKey());
        CobrancaResponse criada = restTemplate.postForEntity(BASE_URL, entity, CobrancaResponse.class).getBody();

        restTemplate.postForEntity(BASE_URL + "/" + criada.id() + "/cancelar",
                new HttpEntity<>(headersComApiKey()), CobrancaResponse.class);

        ResponseEntity<String> respostaErro = restTemplate.postForEntity(
                BASE_URL + "/" + criada.id() + "/pagar",
                new HttpEntity<>(headersComApiKey()), String.class);

        assertThat(respostaErro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respostaErro.getHeaders().getContentType().toString()).contains("problem+json");
        assertThat(respostaErro.getBody()).contains("\"status\":409");
    }

    @Test
    void marcarComoPagaDisparaWebhookDeNotificacao() {
        HttpEntity<CriarCobrancaRequest> entity = new HttpEntity<>(requestPadrao(), headersComApiKey());
        CobrancaResponse criada = restTemplate.postForEntity(BASE_URL, entity, CobrancaResponse.class).getBody();

        ResponseEntity<CobrancaResponse> paga = restTemplate.postForEntity(
                BASE_URL + "/" + criada.id() + "/pagar",
                new HttpEntity<>(headersComApiKey()), CobrancaResponse.class);

        assertThat(paga.getBody().status().name()).isEqualTo("PAGA");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<WebhookNotificacao> notificacoes = webhookNotificacaoRepository.findAll().stream()
                    .filter(n -> n.getCobrancaId().equals(criada.id()))
                    .toList();
            assertThat(notificacoes).isNotEmpty();
            assertThat(notificacoes.get(0).getStatus()).isEqualTo(WebhookNotificacao.StatusEnvio.ENVIADA);
        });
    }

    @Test
    void requisicaoSemApiKeyRetorna401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CriarCobrancaRequest> entity = new HttpEntity<>(requestPadrao(), headers);

        ResponseEntity<String> resposta = restTemplate.postForEntity(BASE_URL, entity, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
