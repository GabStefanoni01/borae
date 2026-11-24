# Arquitetura Multi-Provider de IA e Fallback (`Gemini → Grok`)

**Status:** estrutura técnica implementada e endurecida (Sessões 04 e 04.1), validada integralmente com provedores simulados e testes determinísticos.  
**Ativação operacional:** pendente de homologação futura com credenciais reais, validação de faturamento e aprovação dos termos de processamento de dados de cada fornecedor.  
**Prioridade obrigatória:** `PRIORIDADE 1 — GEMINI` → `PRIORIDADE 2 — GROK (xAI)` → `Contingência segura do backend`.

---

## 1. Arquitetura de provedores (`Gemini → Grok`)

O módulo `com.borae.assistant` mantém o domínio isolado dos protocolos proprietários por meio de abstrações enxutas:

| Componente | Responsabilidade |
| --- | --- |
| `AiProvider` | Interface comum (`id()`, `isAvailable()`, `recommend(AiProviderRequest)`). |
| `AiProviderId` | Enum determinístico dos provedores suportados (`GEMINI`, `GROK`). |
| `AiRequestDeadline` | Deadline absoluto compartilhado por solicitação (`remainingMillis()`, `isExpired()`, `hasOperationalBudget()`, `timeoutForProvider(...)`). |
| `DeadlineAwareClientHttpRequestFactory` | Compartilha um único `java.net.http.HttpClient` por adaptador (sem explosão de threads/pools) e aplica o timeout efetivo por requisição com cancelamento real do stream HTTP. |
| `AiProviderRequest` | Contrato único de entrada (`systemInstruction`, `userMessage`, `catalogJson` <= 4.000 chars, `maxOutputTokens`, `deadline`). |
| `AiProviderResponse` | Contrato normalizado de saída (`provider`, `answer`, `recommendations[]`, `inputTokens`, `outputTokens`, `usedFallbackProvider`, `primaryFailureCode`, `attempts[]`). |
| `AiProviderFailure` / `AiProviderException` | Classificação tipada de falhas com preservação de tokens eventualmente faturados antes da falha e mensagens sempre sanitizadas. |
| `GeminiFlashLiteProvider` | Adaptador principal (**Prioridade 1**): Google Gemini REST `POST /v1beta/models/{model}:generateContent` com cabeçalho `x-goog-api-key`. |
| `GrokAiProvider` | Adaptador de contingência (**Prioridade 2**): xAI **Responses API** `POST {baseUrl}/v1/responses` com `Authorization: Bearer`, `store=false` e `text.format` (`json_schema`). |
| `AiProviderCircuitBreaker` | Circuit breaker independente por provedor (`CLOSED` → `OPEN` → `HALF_OPEN`). |
| `AiProviderConfiguration` | Centraliza prioridades, deadline global, mínimo operacional, política de erro de autenticação e circuit breakers. |
| `AiProviderOrchestrator` | Executa rigorosamente `Gemini → Grok` com no máximo 1 tentativa de contingência. |
| `GastronomicAssistantService` | Monta o contexto autorizado, bloqueia chamadas simultâneas duplicadas (`HTTP 429`), gerencia cotas e centraliza toda validação final de negócio e segurança. |

```text
Consumidor (/r/:slug)
  └─ PublicAssistantInteractionController (IP rate limit HMAC 30/min)
       └─ GastronomicAssistantService (sessão anônima, in-flight guard, catálogo elegível, cota diária)
            └─ AiProviderOrchestrator [AiRequestDeadline global: AI_TOTAL_TIMEOUT_MS]
                 ├─ [Prioridade 1] GeminiFlashLiteProvider (timeout efetivo = min(GEMINI_TIMEOUT_MS, restante))
                 │    ├─ Sucesso ─────────────────────────────► Validação centralizada ──► HTTP 200
                 │    ├─ SAFETY_REFUSAL / INVALID_REQUEST ────► NÃO chama Grok ──► Recusa segura
                 │    ├─ AUTH_OR_CONFIG_ERROR (padrão=false) ─► NÃO chama Grok ──► Alerta + Contingência segura
                 │    └─ Falha técnica elegível + remainingMillis >= AI_MIN_FALLBACK_BUDGET_MS
                 │         └─ [Prioridade 2] GrokAiProvider (timeout efetivo = min(GROK_TIMEOUT_MS, restante))
                 │              ├─ Sucesso ───────────────────► Mesma validação centralizada ──► HTTP 200
                 │              └─ Falha ─────────────────────► Contingência segura (AI_UNAVAILABLE)
                 └─ Ambos indisponíveis/desabilitados ────────► Contingência segura (AI_UNAVAILABLE)
```

---

## 2. Deadline global efetivo e timeout individual

- No início de cada solicitação em `AiProviderOrchestrator.recommend(...)`, cria-se um `AiRequestDeadline` absoluto com orçamento `AI_TOTAL_TIMEOUT_MS` (padrão `15000 ms`) e mínimo operacional `AI_MIN_FALLBACK_BUDGET_MS` (padrão `500 ms`).
- O mesmo `AiRequestDeadline` é propagado em `AiProviderRequest` para o Gemini e, em caso de fallback, para o Grok.
- Antes de cada chamada HTTP:
  - Se `deadline.isExpired()` for `true`, a chamada é abortada imediatamente com `FailureType.TIMEOUT` sem abrir conexão.
  - O timeout efetivo é calculado por `deadline.timeoutForProvider(providerTimeoutMs) = min(providerTimeoutMs, remainingMillis())`.
  - Exemplo prático: se o orçamento global for `15000 ms`, o Gemini consumir `12000 ms` e falhar, e o timeout individual do Grok for `8000 ms`, o Grok receberá timeout efetivo de **3000 ms** (nunca `8000 ms`).
  - Se o tempo restante for inferior a `AI_MIN_FALLBACK_BUDGET_MS` (`500 ms`), o fallback ao Grok não é iniciado.

---

## 3. Comportamento de timeout e cancelamento HTTP

- Cada adaptador compartilha uma única instância de `java.net.http.HttpClient` encapsulada por `DeadlineAwareClientHttpRequestFactory` (sem criar pools ou threads ilimitadas por requisição).
- Em cada chamada, `DeadlineAwareClientHttpRequestFactory.executeWithTimeout(effectiveTimeoutMs, ...)` aplica `readTimeout = Duration.ofMillis(effectiveTimeoutMs)` em um `JdkClientHttpRequestFactory` delegado sobre o `HttpClient` compartilhado.
- No Spring Framework 6.2 / Java 21, isso configura tanto o timeout nativo do `java.net.http.HttpRequest` quanto o `CompletableFuture.get(timeout)` com `future.cancel(true)` no expirar do prazo, cancelando efetivamente o stream HTTP/1.1 ou HTTP/2 subjacente e liberando recursos sem deixar tarefas zumbis em execução.
- Tolerância técnica esperada: pequena margem de agendamento de SO/rede ao redor do deadline; nenhum loop de retry é executado após cancelamento ou timeout.

---

## 4. Estratégia atual do adaptador Grok (xAI Responses API)

Auditoria realizada diretamente contra a documentação oficial da xAI (`https://docs.x.ai/`, `https://docs.x.ai/docs/models` e `https://docs.x.ai/docs/guides/structured-outputs`):

- **API adotada:** **Responses API** (`POST {baseUrl}/v1/responses`), interface oficialmente recomendada pela xAI para novas integrações (substituindo o endpoint legado `/v1/chat/completions`).
- **Autenticação:** cabeçalho HTTP `Authorization: Bearer <GROK_API_KEY>` (nunca na URL, logs ou respostas).
- **Structured Outputs:** enviados via `text.format` com `type: "json_schema"`, `name: "gastronomic_recommendation"`, `strict: true` e `additionalProperties: false`.
- **Instruções e limites:** `instructions` (instrução de sistema), `input` (prompt delimitado do catálogo), `max_output_tokens` (padrão `500`), `temperature: 0.3`. Sem ferramentas externas nem function calling.
- **Parsing normalizado:** lê `output[].content[]` (`type="output_text"` -> `text`, com fallback para `output_text`), detecta recusas de segurança (`incomplete_details.reason == "content_filter"` ou parte `type="refusal"`) como `SAFETY_REFUSAL` e extrai `usage.input_tokens` / `usage.output_tokens`.

---

## 5. Modelo Grok selecionado e justificativa

- **Modelo padrão configurável (`GROK_MODEL`):** **`grok-4.3`** (100% substituível por variável de ambiente sem recompilação).
- **Justificativa técnica e econômica (referência `docs.x.ai`, 24/11/2026):**
  - `grok-4-fast` / `grok-4-1-fast` foram descontinuados pela xAI e redirecionados à linha geral atual (`grok-4.3`).
  - `grok-3-mini` pertence à geração anterior.
  - `grok-4.7` (flagship) tem custo significativamente superior (\$2,00/1M input e \$6,00/1M output) para uma tarefa de recomendação curta sobre catálogo JSON limitado.
  - `grok-4.3` é o tier ativo intermediário/econômico da xAI (contexto 1M, suporte nativo à Responses API, `store: false` e Structured Outputs `json_schema` com \$1,25/1M input e \$2,50/1M output na data de referência), adequado para atuar exclusivamente como contingência secundária do Gemini Flash-Lite.

---

## 6. Política de privacidade e não persistência (D-11)

- **No provedor Grok (xAI):** a Responses API armazena contexto no servidor por padrão; por isso o adaptador envia explicitamente **`"store": false`** em toda requisição, garantindo operação estritamente stateless e aderência à Decisão **D-11**.
- **No provedor Gemini (Google):** endpoint `generateContent` stateless, sem histórico persistido no provedor.
- **No Boraê (PostgreSQL, frontend e logs):** nenhuma conversa, prompt ou resposta completa é gravada em banco, `localStorage`, `sessionStorage` ou logs. `SecurityAuditLogger` redige `bearer`, `token`, `secret`, `api_key`, `x-goog-api-key`, `password` e `BORA_ASSISTANT_SESSION`.

---

## 7. Métricas e cálculo de custos por provedor

Todas as métricas reaproveitam a tabela existente `assistant_daily_metrics` (`metric_key VARCHAR(40)`, retenção de 180 dias, sem necessidade de nova migration):

| Métrica | Significado |
| --- | --- |
| `PROVIDER_GEMINI_SUCCESS` / `PROVIDER_GROK_SUCCESS` | Interações respondidas com sucesso por cada provedor. |
| `PROVIDER_FALLBACK_TO_GROK` | Quantidade de interações atendidas via contingência Grok. |
| `PROVIDER_SAFETY_REFUSALS` | Recusas de segurança (sem acionar fallback). |
| `PROVIDER_GEMINI_INPUT_TOKENS` / `PROVIDER_GEMINI_OUTPUT_TOKENS` | Tokens de entrada e saída contabilizados no Gemini. |
| `PROVIDER_GROK_INPUT_TOKENS` / `PROVIDER_GROK_OUTPUT_TOKENS` | Tokens de entrada e saída contabilizados no Grok. |
| `PROVIDER_GEMINI_COST_MICRO_USD` / `PROVIDER_GROK_COST_MICRO_USD` | Custo estimado em micro-USD (`10^-6 USD`) usando exclusivamente a tarifa do respectivo provedor. |
| `PROVIDER_GEMINI_COST_UNAVAILABLE` / `PROVIDER_GROK_COST_UNAVAILABLE` / `ESTIMATED_COST_UNAVAILABLE` | Marcadores registrados quando falta tarifa configurada ou metadados confiáveis de tokens. |
| `INPUT_TOKENS` / `OUTPUT_TOKENS` / `ESTIMATED_COST_MICRO_USD` | Totais agregados diários do restaurante. |

**Regras de cálculo:**
1. Cada tentativa em `AiProviderResponse.attempts()` (ou `AiProviderException.getAttemptUsages()`, incluindo tokens consumidos antes de um JSON malformado ou recusa) é contabilizada separadamente no seu provedor.
2. Em um fallback onde o Gemini consumiu tokens antes de falhar e o Grok respondeu, **ambos** os consumos são somados usando a tarifa individual de cada provedor.
3. **Nunca** se substitui uma tarifa ausente pela tarifa de outro provedor e **nunca** se grava custo `0` quando a tarifa não está configurada ou não há metadados de tokens: os contadores de tokens disponíveis são preservados e `*_COST_UNAVAILABLE` é incrementado.

---

## 8. Tarifas configuráveis e fontes oficiais

As tarifas **não são presumidas como permanentes** e vêm configuradas como `-1` (indisponível) por padrão até parametrização operacional explícita:

- **Unidade:** dólares americanos (**USD**) por **1.000.000 (1M) de tokens**; persistência interna em **micro-USD** (`1 micro-USD = US$ 0,000001`).
- **Propriedades / variáveis:**
  - `GEMINI_INPUT_PRICE_PER_MILLION_USD` (`app.assistant.gemini.input-price-per-million-usd`)
  - `GEMINI_OUTPUT_PRICE_PER_MILLION_USD` (`app.assistant.gemini.output-price-per-million-usd`)
  - `GROK_INPUT_PRICE_PER_MILLION_USD` (`app.assistant.grok.input-price-per-million-usd`)
  - `GROK_OUTPUT_PRICE_PER_MILLION_USD` (`app.assistant.grok.output-price-per-million-usd`)
- **Referências oficiais (data de consulta: 24/11/2026 — confirmar novamente antes da ativação em produção):**
  - Google Gemini API Pricing: `https://ai.google.dev/gemini-api/docs/pricing` (referência `gemini-2.5-flash-lite`: \$0,10/1M input, \$0,40/1M output).
  - xAI Models & Pricing: `https://docs.x.ai/docs/models` (referência `grok-4.3` contexto padrão < 200k: \$1,25/1M input, \$2,50/1M output; endpoint regional US aplica multiplicador 1,1x).
- **Como atualizar:** alterar exclusivamente as variáveis de ambiente correspondentes no serviço `borae-api` (sem alteração de código) sempre que o fornecedor ajustar preços ou quando o modelo configurado mudar.

---

## 9. Política de fallback em erros de autenticação e falhas

| Situação do Gemini | `FailureType` | Comportamento padrão | Circuit Breaker |
| --- | --- | --- | --- |
| Resposta válida | — | Retorna resposta validada do Gemini (Grok não é chamado) | Fecha (`CLOSED`) |
| Timeout | `TIMEOUT` | Tenta Grok (limitado ao orçamento restante) | Incrementa |
| Erro HTTP 5xx | `HTTP_5XX` | Tenta Grok (limitado ao orçamento restante) | Incrementa |
| HTTP 429 | `RATE_LIMITED` | Tenta Grok (limitado ao orçamento restante) | Incrementa |
| Indisponibilidade / rede / circuit aberto | `SERVICE_UNAVAILABLE` | Tenta Grok (limitado ao orçamento restante) | Incrementa |
| Resposta vazia ou JSON inválido | `MALFORMED_RESPONSE` | Registra falha técnica/tokens e tenta Grok | Incrementa |
| HTTP 401 / 403 / 400 / 404 (autenticação ou configuração) | `AUTH_OR_CONFIG_ERROR` | **Não chama Grok por padrão** (`AI_FALLBACK_ON_AUTH_OR_CONFIG_ERROR=false`). Emite alerta `ERROR` sanitizado e retorna `AI_UNAVAILABLE` sem detalhes internos. | Incrementa |
| Recusa de segurança (`SAFETY`, `PROHIBITED_CONTENT`, `content_filter`, `refusal`) | `SAFETY_REFUSAL` | **Nunca chama Grok.** Retorna recusa cordial. | Não abre |
| Solicitação inválida do usuário | `INVALID_USER_REQUEST` | **Nunca chama Grok.** | Não abre |
| Produto inexistente/indisponível/outro restaurante | Regra de negócio | **Nunca chama Grok.** Backend descarta IDs inválidos. | Não abre |

---

## 10. Circuit breakers independentes e proteção contra duplicidade

- Gemini e Grok possuem instâncias independentes de `AiProviderCircuitBreaker` (`AI_CIRCUIT_BREAKER_FAILURE_THRESHOLD=3`, `AI_CIRCUIT_BREAKER_OPEN_DURATION_MS=30000`).
- Falhas técnicas no Gemini abrem apenas o circuito do Gemini, permitindo contingência direta ao Grok sem desperdiçar latência; após `30s`, entra em `HALF_OPEN` para sondar a recuperação do Gemini.
- `GastronomicAssistantService` impede chamadas simultâneas duplicadas para o mesmo par `quotaKey + restaurantId` (`HTTP 429`).

---

## 11. Matriz de variáveis futuras (sem credenciais)

| Grupo | Variável | Padrão | Finalidade |
| --- | --- | --- | --- |
| Orquestração | `AI_PRIMARY_PROVIDER` | `gemini` | Provedor principal (deve permanecer `gemini`). |
| Orquestração | `AI_FALLBACK_PROVIDER` | `grok` | Provedor secundário de contingência. |
| Orquestração | `AI_FALLBACK_ENABLED` | `true` | Habilita contingência automática Gemini → Grok. |
| Orquestração | `AI_FALLBACK_ON_AUTH_OR_CONFIG_ERROR` | **`false`** | Desabilitado por padrão para não mascarar erro de credencial/configuração. |
| Orquestração | `AI_TOTAL_TIMEOUT_MS` | `15000` | Deadline global absoluto por solicitação (ms). |
| Orquestração | `AI_MIN_FALLBACK_BUDGET_MS` | `500` | Orçamento mínimo restante (ms) para iniciar fallback ao Grok. |
| Orquestração | `AI_CIRCUIT_BREAKER_FAILURE_THRESHOLD` | `3` | Falhas consecutivas para abrir circuito. |
| Orquestração | `AI_CIRCUIT_BREAKER_OPEN_DURATION_MS` | `30000` | Janela de abertura do circuito (ms). |
| Gemini | `GEMINI_ENABLED` | `false` | Liga/desliga Gemini (kill switch). |
| Gemini | `GEMINI_DATA_PROCESSING_APPROVED` | `false` | Trava de aprovação de termos do Google AI. |
| Gemini | `GEMINI_API_KEY` | *(vazio)* | Secret exclusivo do backend. |
| Gemini | `GEMINI_MODEL` | `gemini-2.5-flash-lite` | Modelo Gemini configurável. |
| Gemini | `GEMINI_TIMEOUT_MS` / `GEMINI_TIMEOUT_SECONDS` | `0` / `12` | Timeout individual do Gemini (`GEMINI_TIMEOUT_MS` prevalece quando > 0). |
| Gemini | `GEMINI_MAX_OUTPUT_TOKENS` | `500` | Limite de saída em tokens. |
| Gemini | `GEMINI_INPUT_PRICE_PER_MILLION_USD` / `GEMINI_OUTPUT_PRICE_PER_MILLION_USD` | `-1` / `-1` | Tarifas USD/1M tokens (negativo = indisponível). |
| Grok | `GROK_ENABLED` | `false` | Liga/desliga Grok (kill switch). |
| Grok | `GROK_DATA_PROCESSING_APPROVED` | `false` | Trava de aprovação de termos da xAI. |
| Grok | `GROK_API_KEY` | *(vazio)* | Secret exclusivo do backend. |
| Grok | `GROK_MODEL` | `grok-4.3` | Modelo Grok configurável. |
| Grok | `GROK_BASE_URL` | `https://api.x.ai` | Endpoint base oficial configurável da xAI. |
| Grok | `GROK_TIMEOUT_MS` | `8000` | Timeout individual do Grok (limitado ao deadline global). |
| Grok | `GROK_MAX_OUTPUT_TOKENS` | `500` | Limite de saída em tokens. |
| Grok | `GROK_INPUT_PRICE_PER_MILLION_USD` / `GROK_OUTPUT_PRICE_PER_MILLION_USD` | `-1` / `-1` | Tarifas USD/1M tokens (negativo = indisponível). |

---

## 12. Procedimento futuro de homologação real e kill switch

1. **Staging:** confirmar termos/tier e preços vigentes na data da ativação; configurar segredos e tarifas no cofre de variáveis de staging (nunca no Git); validar smoke test normal (Gemini), contingência controlada (Gemini desabilitado -> Grok Responses API com `store=false`), métricas por provedor (`PROVIDER_*`), latência dentro de `AI_TOTAL_TIMEOUT_MS` e redação de logs.
2. **Produção:** promover apenas após aprovação do staging; monitorar `PROVIDER_ERRORS`, `PROVIDER_FALLBACK_TO_GROK`, `PROVIDER_SAFETY_REFUSALS`, `*_COST_MICRO_USD` e `ESTIMATED_COST_UNAVAILABLE`.
3. **Kill switch emergencial:** `GEMINI_ENABLED=false` (contingência imediata via Grok), `GROK_ENABLED=false` ou `AI_FALLBACK_ENABLED=false` (somente Gemini), ou ambos `false` (cardápio 100% operacional com resposta `AI_UNAVAILABLE` sem consumo de cota).
4. **Terceiro provedor futuro:** implementar `AiProvider` com `AiRequestDeadline`, `store=false` / não persistência, `FailureType` tipado e tarifa independente em `AssistantMetricsService`.
