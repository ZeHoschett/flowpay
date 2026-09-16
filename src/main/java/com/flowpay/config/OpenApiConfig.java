package com.flowpay.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flowPayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlowPay - API de Cobranças Recorrentes")
                        .description("API para criar cobranças, acompanhar seu ciclo de vida e notificar "
                                + "outros sistemas via webhook quando o status muda.")
                        .version("v0.1"))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
                .schemaRequirement("ApiKeyAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key"));
    }
}
