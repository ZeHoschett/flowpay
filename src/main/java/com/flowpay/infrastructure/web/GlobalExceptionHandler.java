package com.flowpay.infrastructure.web;

import com.flowpay.application.IdempotencyKeyConflictException;
import com.flowpay.domain.CobrancaNaoEncontradaException;
import com.flowpay.domain.TransicaoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Padroniza todas as respostas de erro da API seguindo RFC 7807
 * (Problem Details for HTTP APIs) - em vez de cada endpoint devolver um
 * formato de erro diferente, todos seguem esta mesma estrutura:
 * type, title, status, detail, instance (+ extensões quando fizer sentido).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://flowpay.dev/problems/";

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoInvalidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(BASE_URI + "transicao-invalida"));
        problem.setTitle("Transição de status inválida");
        return problem;
    }

    @ExceptionHandler(CobrancaNaoEncontradaException.class)
    public ProblemDetail handleCobrancaNaoEncontrada(CobrancaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(BASE_URI + "cobranca-nao-encontrada"));
        problem.setTitle("Cobrança não encontrada");
        return problem;
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(BASE_URI + "idempotency-key-conflict"));
        problem.setTitle("Chave de idempotência reutilizada com corpo diferente");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::paraMapaDeErro)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problem.setType(URI.create(BASE_URI + "validacao"));
        problem.setTitle("Erro de validação");
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonMalformado(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "O corpo da requisição não é um JSON válido");
        problem.setType(URI.create(BASE_URI + "json-malformado"));
        problem.setTitle("Requisição malformada");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado");
        problem.setType(URI.create(BASE_URI + "erro-interno"));
        problem.setTitle("Erro interno");
        return problem;
    }

    private Map<String, String> paraMapaDeErro(FieldError erro) {
        return Map.of("campo", erro.getField(), "mensagem", erro.getDefaultMessage());
    }
}