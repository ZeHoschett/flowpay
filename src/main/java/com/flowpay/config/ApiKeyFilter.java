package com.flowpay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autenticação mínima para a v1: um token fixo de API enviado no header
 * X-API-Key. Não é multi-tenant nem tem escopos/roles - só resolve o
 * propósito do projeto (ver README, seção "O que fica de fora da v1").
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final List<String> CAMINHOS_PUBLICOS = List.of(
            "/swagger-ui", "/v3/api-docs", "/webhooks/test-receiver", "/actuator/health"
    );

    @Value("${flowpay.security.api-key:changeme-local-dev}")
    private String apiKeyEsperada;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return CAMINHOS_PUBLICOS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String apiKeyRecebida = request.getHeader("X-API-Key");

        if (apiKeyRecebida == null || !apiKeyRecebida.equals(apiKeyEsperada)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {
                      "type": "about:blank",
                      "title": "Não autorizado",
                      "status": 401,
                      "detail": "Header X-API-Key ausente ou inválido"
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
