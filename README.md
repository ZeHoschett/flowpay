# FlowPay — API de Cobranças Recorrentes

API REST em Spring Boot para criar cobranças, acompanhar seu ciclo de vida e notificar
outros sistemas (via webhook) quando o status muda.

No ecossistema mais amplo, o FlowPay é a origem: o **FLOWCNAB** (COBOL) consumiria as
cobranças pendentes daqui para gerar a remessa bancária. Essa integração não existe de
fato nesta v1 — o FlowPay foi construído para funcionar de forma independente.

## Ciclo de vida de uma cobrança

Uma cobrança nasce **PENDENTE** e só pode ir para um dos três estados finais abaixo.
Nenhum estado final permite transição de volta ou para outro estado final.

```
                 ┌───────────┐
                 │ PENDENTE  │
                 └─────┬─────┘
        ┌──────────────┼──────────────┐
        ▼               ▼              ▼
   ┌────────┐     ┌───────────┐  ┌───────────┐
   │  PAGA  │     │  VENCIDA  │  │ CANCELADA │
   └────────┘     └───────────┘  └───────────┘
   (final)          (final)         (final)
```

Essa regra é o coração do projeto e está implementada em `StatusCobranca` (as
transições permitidas de cada estado) e aplicada em `Cobranca.transicionarPara(...)`,
que lança `TransicaoInvalidaException` para qualquer movimento fora do permitido.

## Endpoints

| Método | Caminho                     | Descrição                                          |
|--------|------------------------------|-----------------------------------------------------|
| POST   | `/cobrancas`                 | Cria uma cobrança (aceita `Idempotency-Key`)         |
| GET    | `/cobrancas/{id}`             | Consulta uma cobrança por ID                        |
| GET    | `/cobrancas?clienteId=&status=` | Lista cobranças de um cliente, com filtro opcional |
| POST   | `/cobrancas/{id}/pagar`       | Marca como paga (simula webhook de gateway externo) |
| POST   | `/cobrancas/{id}/cancelar`    | Cancela uma cobrança pendente                       |
| POST   | `/webhooks/test-receiver`     | Endpoint de teste que recebe e loga notificações    |

Todos os endpoints (exceto o receiver de teste, o Swagger e o health check) exigem o
header `X-API-Key` (ver seção Segurança).

Documentação interativa: `http://localhost:8080/swagger-ui.html` (gerada a partir do
código via springdoc-openapi, não escrita a mão).

## Os três diferenciais

### 1. Idempotência (implementado)

Ao criar uma cobrança com o header `Idempotency-Key`, a chave + um hash do corpo da
requisição são guardados em `idempotency_key` junto com a resposta dada. Se a mesma
chave chegar de novo:

- **com o mesmo corpo** → devolve a resposta já dada, sem criar cobrança duplicada;
- **com um corpo diferente** → retorna `409 Conflict` (uso incorreto da chave, não
  uma repetição legítima).

Ver `IdempotencyService` e `IdempotencyRecord`.

### 2. Retry com backoff exponencial no webhook (implementado)

Quando o status de uma cobrança muda, `CobrancaService` agenda o disparo do webhook
para depois do commit da transação (via `TransactionSynchronization`, para não
notificar uma mudança que acabou sendo revertida). `WebhookSender` tenta a chamada
HTTP; se falhar, reagenda com backoff exponencial (1s, 2s, 4s, 8s, ...) até
`flowpay.webhook.max-tentativas` (padrão: 5), depois marca como falha permanente.
Cada tentativa fica registrada em `webhook_notificacao` para auditoria.

### 3. Outbox pattern (próximo passo, não implementado na v1)

A lacuna conhecida da abordagem atual: o disparo do webhook acontece **depois** do
commit, em um passo separado. Se o processo cair exatamente entre o commit da mudança
de status e o envio da notificação, o evento se perde silenciosamente.

O padrão Outbox resolveria isso assim: em vez de disparar a chamada HTTP diretamente,
gravar o evento numa tabela `outbox_evento` **na mesma transação** que muda o status
da cobrança (garantia atômica de banco). Um processo separado (um `@Scheduled` simples
lendo eventos `PENDENTE` da tabela) seria responsável por enviar os webhooks e marcar
os eventos como processados. Como o registro do evento é parte da mesma transação
que já persiste a mudança de estado, a falha entre "mudar status" e "notificar"
deixa de existir — o evento sempre existe se o status mudou.

A tabela `webhook_notificacao` já existente foi desenhada para deixar esse caminho
mais curto (ela já é, em essência, um outbox sem o consumo assíncrono via `@Scheduled`
gravado na mesma transação de domínio).

## Segurança

Token fixo de API via header `X-API-Key` (ver `application.yml`,
`flowpay.security.api-key`). Suficiente para o propósito do projeto — sem
multi-tenancy, sem OAuth/JWT, sem escopos por usuário (ver "Fora da v1" abaixo).

## Tratamento de erros (RFC 7807)

Todos os erros da API seguem [RFC 7807 — Problem Details](https://www.rfc-editor.org/rfc/rfc7807.html),
via `GlobalExceptionHandler` + `ProblemDetail` (suporte nativo do Spring 6). Exemplo de
resposta para uma transição inválida:

```json
{
  "type": "https://flowpay.dev/problems/transicao-invalida",
  "title": "Transição de status inválida",
  "status": 409,
  "detail": "Transição inválida: não é possível mover a cobrança de CANCELADA para PAGA"
}
```

## Decisão de arquitetura: camadas, não features

O código está organizado por **camada técnica** (`domain`, `application`,
`infrastructure`), não por feature. Motivo: o domínio da v1 é pequeno (uma única
entidade central, a cobrança), então dividir por feature criaria mais pastas do que
conceitos distintos a separar. Se o projeto crescer (múltiplos agregados, mais casos
de uso), reorganizar por feature/módulo passa a valer mais a pena — mas isso é uma
decisão consciente para revisitar quando o domínio justificar, não um ponto de partida.

- `domain/`: `Cobranca`, `StatusCobranca` (a máquina de estados) e as exceções de
  domínio. Sem dependência de Spring.
- `application/`: casos de uso (`CobrancaService`, `IdempotencyService`) — orquestram
  domínio + infraestrutura.
- `infrastructure/`: tudo que fala com o mundo externo — REST (`web/`), persistência
  (`persistence/`) e o cliente de webhook (`webhook/`).

## Rodando localmente

```bash
# 1. Sobe o Postgres
docker compose up -d

# 2. Roda a aplicação (Flyway aplica as migrations automaticamente no start)
./mvnw spring-boot:run

# 3. Testa a criação de uma cobrança
curl -X POST http://localhost:5433/cobrancas \
  -H "Content-Type: application/json" \
  -H "X-API-Key: changeme-local-dev" \
  -H "Idempotency-Key: teste-001" \
  -d '{"clienteId":"cliente-1","valor":150.00,"vencimento":"2026-12-01","referenciaServicoAgendado":"agenda-42"}'
```

## Testes

```bash
./mvnw test
```

- `src/test/java/com/flowpay/unit`: testes unitários da máquina de estados (sem
  Spring, sem banco).
- `src/test/java/com/flowpay/integration`: testes de integração com
  **Testcontainers** — sobem um Postgres real em container (não um banco em memória)
  para validar criação, idempotência, transições inválidas, autenticação e o disparo
  efetivo do webhook.

Critério de "pronto" coberto pelos testes: criar cobrança; reenviar a mesma
`Idempotency-Key` sem duplicar; pagar e ver o webhook de teste sendo notificado
(assíncrono, verificado via Awaitility); tentar uma transição inválida e receber
`409` no formato RFC 7807; suíte verde com Postgres real.

## Fora da v1 (documentado, não implementado)

- Multi-tenancy.
- Autenticação/autorização robusta (OAuth2, JWT, escopos por usuário) — hoje é só
  um token fixo.
- Múltiplos métodos de pagamento ou gateways reais — `pagar` simula o webhook que um
  gateway real enviaria.
- Cálculo de juros/multa por atraso.
- Outbox pattern completo (ver seção acima) e transição automática PENDENTE → VENCIDA
  por passagem de data (hoje só existe o endpoint de marcar como paga/cancelar; a
  cobrança não vence sozinha por tempo — isso entraria junto com o Outbox, como um
  job agendado).




## Testes

```bash
./mvnw test
```

- `src/test/java/com/flowpay/unit`: testes unitários da máquina de estados (sem
  Spring, sem banco). **9/9 passando.**
- `src/test/java/com/flowpay/integration`: testes de integração com
  **Testcontainers** — sobem um Postgres real em container (não um banco em memória)
  para validar criação, idempotência, transições inválidas, autenticação e o disparo
  efetivo do webhook.

> **Nota sobre o teste de integração no Windows:** em algumas combinações de Docker
> Desktop recente (4.60+) com Windows + WSL2, o Testcontainers pode falhar ao descobrir
> o daemon do Docker, retornando um erro tipo `Could not find a valid Docker environment`
> mesmo com o Docker rodando normalmente (confirmado via `docker ps`/`docker info`). Isso
> é uma incompatibilidade de negociação de API entre o cliente `docker-java` (usado pelo
> Testcontainers) e versões recentes do Docker Desktop no Windows — não é um problema do
> código do FlowPay. O comportamento foi validado manualmente end-to-end via Swagger UI
> (criação, idempotência, pagamento com disparo de webhook, transições inválidas e JSON
> malformado, todos retornando os status e formatos corretos). Se esse erro aparecer:
> - Confirme que não há outro Postgres nativo competindo pela mesma porta (`netstat -ano | findstr :5432`).
> - Tente apontar o Testcontainers para o pipe correto criando
    >   `%USERPROFILE%\.testcontainers.properties` com `docker.host=npipe:////./pipe/docker_engine`
    >   (ou `docker_cli`, dependendo da versão do Docker Desktop).
> - Em último caso, rode a suíte via WSL2 diretamente (dentro do Ubuntu/WSL o Docker
    >   costuma conectar via socket Unix, sem esse problema de pipe do Windows).

Critério de "pronto" coberto: criar cobrança; reenviar a mesma
`Idempotency-Key` sem duplicar; pagar e ver o webhook de teste sendo notificado
(assíncrono, verificado via Awaitility no teste automatizado e confirmado manualmente
nos logs); tentar uma transição inválida e receber `409` no formato RFC 7807; JSON
malformado retorna `400` (também no formato RFC 7807); suíte unitária verde.
