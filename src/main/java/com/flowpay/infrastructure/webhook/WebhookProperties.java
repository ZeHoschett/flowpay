package com.flowpay.infrastructure.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowpay.webhook")
public class WebhookProperties {

    /** URL do consumidor que receberá a notificação de mudança de status. */
    private String url = "http://localhost:8080/webhooks/test-receiver";

    /** Número máximo de tentativas antes de marcar como falha permanente. */
    private int maxTentativas = 5;

    /** Delay base, em segundos, usado no backoff exponencial (1s, 2s, 4s, 8s...). */
    private int delayBaseSegundos = 1;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMaxTentativas() {
        return maxTentativas;
    }

    public void setMaxTentativas(int maxTentativas) {
        this.maxTentativas = maxTentativas;
    }

    public int getDelayBaseSegundos() {
        return delayBaseSegundos;
    }

    public void setDelayBaseSegundos(int delayBaseSegundos) {
        this.delayBaseSegundos = delayBaseSegundos;
    }
}
