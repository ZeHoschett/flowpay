package com.flowpay.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowpay.infrastructure.persistence.IdempotencyRecord;
import com.flowpay.infrastructure.persistence.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Garante que operações marcadas com um header Idempotency-Key só sejam
 * executadas uma vez: se a mesma chave chegar de novo com o mesmo corpo de
 * requisição, devolve a resposta já dada na primeira vez, sem repetir o
 * efeito colateral (ex.: criar uma cobrança duplicada). Se a mesma chave
 * chegar com um corpo diferente, isso é tratado como um erro do cliente
 * (uso incorreto da chave), não como uma repetição legítima.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Procura uma resposta já registrada para essa chave + corpo de requisição.
     * Se não existir, executa a operação, registra o resultado e o devolve.
     */
    public <T> T executar(String chave, Object corpoRequisicao, Class<T> tipoResposta, Supplier<T> operacao) {
        String hash = calcularHash(corpoRequisicao);
        Optional<IdempotencyRecord> existente = repository.findByChave(chave);

        if (existente.isPresent()) {
            IdempotencyRecord record = existente.get();
            if (!record.getHashRequisicao().equals(hash)) {
                throw new IdempotencyKeyConflictException(chave);
            }
            return desserializar(record, tipoResposta);
        }

        T resultado = operacao.get();
        salvar(chave, hash, resultado);
        return resultado;
    }

    private <T> void salvar(String chave, String hash, T resultado) {
        try {
            String json = objectMapper.writeValueAsString(resultado);
            // 201: hoje só usamos idempotência na criação (POST /cobrancas)
            repository.save(new IdempotencyRecord(chave, 201, json, hash));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar resposta para idempotência", e);
        }
    }

    private <T> T desserializar(IdempotencyRecord record, Class<T> tipo) {
        try {
            return objectMapper.readValue(record.getCorpoResposta(), tipo);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao desserializar resposta armazenada para idempotência", e);
        }
    }

    private String calcularHash(Object corpo) {
        try {
            String json = objectMapper.writeValueAsString(corpo);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash da requisição", e);
        }
    }
}
