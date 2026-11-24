# Arquitetura Multi-Provider de IA e Fallback (`Gemini → Grok`)

**Status:** estrutura técnica implementada e validada com provedores simulados (Sessão 04).  
**Ativação operacional:** pendente de homologação futura com credenciais reais, validação de endpoints/modelos oficiais e aprovação dos termos de processamento de dados de cada fornecedor.  
**Prioridade obrigatória:** `PRIORIDADE 1 — GEMINI` → `PRIORIDADE 2 — GROK (xAI)` → `Contingência segura do backend`.

---

## 1. Arquitetura de provedores

O módulo `com.borae.assistant` mantém o domínio desacoplado de SDKs e protocolos proprietários através de um conjunto pequeno e explícito de abstrações:

| Componente | Responsabilidade |
| --- | --- |
| `AiProvider` | Interface comum (`id()`, `isAvailable()`, `recommend(AiProviderRequest)`). |
| `AiProviderId` | Enum determinístico de provedores suportados (`GEMINI`, `GROK`). |
| `AiProviderRequest` | Contrato único de entrada contendo instruções de sistema, mensagem do consumidor, catálogo público delimitado (JSON <= 4.000 chars) e limite de tokens de saída. |
| `AiProviderResponse` | Contrato normalizado de saída (`provider`, `answer`, `recommendations[]`, `inputTokens`, `outputTokens`, `usedFallbackProvider`, `primaryFailureCode`). |
| `AiProviderFailure` / `AiProviderException` | Classificação tipada de falhas técnicas, operacionais e recusas de segurança, sempre com mensagens sanitizadas. |
| `GeminiFlashLiteProvider` | Adaptador do provedor principal (**Prioridade 1**), usando Google Gemini REST `POST /v1beta/models/{model}:generateContent` com cabeçalho `x-goog-api-key`. |
| `GrokAiProvider` | Adaptador do provedor secundário de contingência (**Prioridade 2**), preparado para xAI REST `POST {baseUrl}/v1/chat/completions` com cabeçalho `Authorization: Bearer`. |
| `AiProviderCircuitBreaker` | Circuit breaker leve e independente por provedor (`CLOSED` → `OPEN` → `HALF_OPEN`). |
| `AiProviderConfiguration` | Centraliza prioridades, políticas de fallback, orçamento total de tempo e limites dos circuit breakers. |
| `AiProviderOrchestrator` | Executa rigorosamente a ordem `Gemini → Grok` com no máximo 1 tentativa de contingência. |
| `GastronomicAssistantService` | Monta o contexto autorizado do restaurante, impede chamadas simultâneas duplicadas, controla cotas e executa toda validação final de negócio, catálogo, preços, alergias e prompt injection. |

---

## 2. Ordem de prioridade (`Gemini → Grok`)

```text
Consumidor (/r/:slug)
  └─ PublicAssistantInteractionController (IP rate limit HMAC 30/min)
       └─ GastronomicAssistantService (sessão anônima, in-flight guard, catálogo elegível, cota diária)
            └─ AiProviderOrchestrator
                 ├─ [Prioridade 1] GeminiFlashLiteProvider (circuit breaker independente)
                 │    ├─ Sucesso ─────────────────────────► Validação centralizada no backend ──► Resposta 200
                 │    ├─ SAFETY_REFUSAL / INVALID_REQUEST ─► NÃO chama Grok ──► Recusa segura (sem fallback)
                 │    └─ Falha técnica elegível + orçamento de tempo restante
                 │         └─ [Prioridade 2] GrokAiProvider (máx. 1 tentativa, circuit breaker independente)
                 │              ├─ Sucesso ───────────────► Mesma validação centralizada ──► Resposta 200
                 │              └─ Falha ─────────────────► Contingência segura (`fallback: true`, `AI_UNAVAILABLE`)
                 └─ Ambos indisponíveis/desabilitados ────► Contingência segura (`fallback: true`, `AI_UNAVAILABLE`)
```

Regras invioláveis desta arquitetura:

- **O Grok nunca é chamado antes do Gemini.** `AiProviderConfiguration` rejeita na inicialização qualquer configuração que tente promover `grok` como provedor principal.
- **Sem balanceamento aleatório, sem split por custo e sem chamadas paralelas** entre provedores.
- **No máximo 1 tentativa de contingência ao Grok** por solicitação.

---

## 3. Fluxo completo de fallback

1. O controlador público aplica proteção de borda (`BoraeSecurityShieldFilter`) e rate limit distribuído por IP pseudonimizado (`AssistantIpRateLimitService`).
2. `GastronomicAssistantService` valida o cookie assinado `BORA_ASSISTANT_SESSION`, carrega o restaurante publicado pelo `slug` normalizado e bloqueia chamadas simultâneas duplicadas da mesma sessão/restaurante (`HTTP 429`).
3. O serviço filtra somente cardápios `PUBLISHED`, categorias ativas e produtos `ACTIVE` + `AVAILABLE`, aplicando filtros de `menuId`, categoria, preço máximo e ingredientes excluídos. Se nenhum produto for elegível, responde honestamente sem chamar IA e sem consumir cota.
4. Se nenhum provedor estiver habilitado/configurado, retorna `fallback: true` (`AI_UNAVAILABLE`) sem consumir cota.
5. A cota diária (20/sessão/dia e 100/restaurante/dia em `America/Sao_Paulo`) é consumida atomicamente no PostgreSQL antes da chamada externa.
6. `AiProviderOrchestrator` chama o Gemini (**Prioridade 1**) com `AiProviderRequest`.
7. Se o Gemini falhar por motivo técnico elegível e houver orçamento de tempo restante, o orquestrador executa **uma única** chamada de contingência ao Grok (**Prioridade 2**) com exatamente o mesmo `AiProviderRequest`.
8. A resposta normalizada (seja do Gemini ou do Grok) passa pela mesma validação centralizada no backend (pertencimento ao restaurante, disponibilidade, preço oficial em centavos, sanitização contra preços fabricados/prompt injection e cautela conservadora para alergias).
9. Se ambos os provedores falharem, o usuário recebe a resposta segura de contingência (`fallback: true`, `fallbackCode: "AI_UNAVAILABLE"`) e o cardápio tradicional continua 100% utilizável.

---

## 4. Contrato compartilhado de requisição e resposta

### Entrada (`AiProviderRequest`)
- `systemInstruction`: instrução de sistema fixa definindo regras de fidelidade ao catálogo, anti prompt-injection, proibição de mencionar preços em texto livre, limite de 4 recomendações e postura conservadora sobre alergias.
- `userMessage`: mensagem do consumidor (máx. 500 caracteres), marcada explicitamente como dado não confiável.
- `catalogJson`: JSON delimitado (máx. 4.000 caracteres) contendo `restaurant`, `category` e lista de produtos elegíveis (`productId`, `name`, `description`, `priceInCents`, `menu`, `category`, `ingredients`, `addons` ativos).
- `maxOutputTokens`: teto de tokens de saída (padrão `500`, limitado entre `64` e `1000`).

### Saída normalizada (`AiProviderResponse`)
- `provider`: `GEMINI` ou `GROK`.
- `answer`: texto curto em português do Brasil (posteriormente sanitizado pelo backend).
- `recommendations`: lista de até 4 candidatos `{ productId: UUID, reason: String }` (todos validados contra o mapa de produtos elegíveis do restaurante).
- `inputTokens` / `outputTokens`: contadores de uso reportados pelo provedor para métricas agregadas.
- `usedFallbackProvider` / `primaryFailureCode`: metadados internos para observabilidade agregada (nunca expostos com detalhes internos ao cliente).

---

## 5. Classificação de falhas e política de fallback

| Situação do Gemini | Classificação (`FailureType`) | Ação do Orquestrador | Abre Circuit Breaker? |
| --- | --- | --- | --- |
| Resposta válida | — | Retorna resposta validada do Gemini (Grok não é chamado) | Não (reseta para `CLOSED`) |
| Timeout de rede/leitura | `TIMEOUT` | Tenta Grok (se houver tempo restante no orçamento) | Sim |
| Erro HTTP 5xx | `HTTP_5XX` | Tenta Grok | Sim |
| Limite de requisições / HTTP 429 | `RATE_LIMITED` | Tenta Grok (respeitando limite total de tempo) | Sim |
| Indisponibilidade / erro de conexão / desabilitado / circuit aberto | `SERVICE_UNAVAILABLE` | Tenta Grok | Sim |
| Resposta vazia, JSON inválido ou fora do schema | `MALFORMED_RESPONSE` | Registra falha técnica e tenta Grok | Sim |
| Erro HTTP 401/403/400/404 (autenticação/configuração) | `AUTH_OR_CONFIG_ERROR` | Emite alerta operacional (`ERROR` sanitizado) e tenta Grok se `AI_FALLBACK_ON_AUTH_OR_CONFIG_ERROR=true` | Sim |
| Recusa de segurança (`SAFETY`, `PROHIBITED_CONTENT`, `content_filter`) | `SAFETY_REFUSAL` | **Não chama Grok.** Retorna recusa segura e registra `PROVIDER_SAFETY_REFUSALS` | Não |
| Solicitação inválida do usuário | `INVALID_USER_REQUEST` | **Não chama Grok.** | Não |
| Produto inexistente/indisponível/de outro restaurante ou sem resultados | Regra de negócio (pós-provedor) | **Não chama Grok.** Backend descarta IDs inválidos e responde honestamente | Não |

---

## 6. Timeouts, retries, proteção contra duplicidade e circuit breakers

- **Timeout individual por provedor:** `GEMINI_TIMEOUT_MS` (fallback compatível com `GEMINI_TIMEOUT_SECONDS`, padrão `12s`; recomendação futura com fallback ativo: `7000`–`8000 ms`) e `GROK_TIMEOUT_MS` (padrão `8000 ms`).
- **Tempo total máximo da solicitação:** `AI_TOTAL_TIMEOUT_MS` (padrão `15000 ms`). Se o tempo consumido na chamada primária deixar menos de `500 ms` disponíveis, o fallback ao Grok é pulado imediatamente para não prender o consumidor.
- **Política de retry:** sem loops de retry em cada provedor; no máximo **1 tentativa no Gemini + 1 tentativa de contingência no Grok**.
- **Circuit breaker independente por provedor:**
  - Padrão: `AI_CIRCUIT_BREAKER_FAILURE_THRESHOLD=3` falhas técnicas consecutivas abre o circuito por `AI_CIRCUIT_BREAKER_OPEN_DURATION_MS=30000` ms.
  - Quando o circuito do Gemini está `OPEN`, o orquestrador evita latência desperdiçada e aciona o Grok diretamente (se disponível e com circuito próprio fechado).
  - Após a janela de abertura, entra em `HALF_OPEN` permitindo uma única sondagem ao Gemini; sucesso fecha imediatamente o circuito e restaura o Gemini como provedor ativo.
  - Recusas de segurança (`SAFETY_REFUSAL`) nunca abrem o circuit breaker.
- **Proteção contra chamadas simultâneas duplicadas:** `GastronomicAssistantService` controla chaves em voo por `quotaKey + restaurantId`, rejeitando chamadas concorrentes sobrepostas da mesma sessão com `HTTP 429`.

---

## 7. Validação centralizada de conteúdo

Independentemente de o provedor responder via Gemini ou Grok, o backend sempre impõe:

1. **Multi-tenancy e catálogo real:** somente `productId` pertencente ao conjunto elegível (`PUBLISHED` + `ACTIVE` + `AVAILABLE`) do `slug` consultado.
2. **Fonte única de verdade:** `name`, `priceInCents`, `availability`, `imageUrl` e `ingredients` vêm exclusivamente do PostgreSQL.
3. **Bloqueio de preços fabricados e vazamento de prompt:** `answer` e `reason` passam por sanitização que rejeita valores monetários em prosa (`R$ ...`) e termos de vazamento (`system prompt`, `api_key`, `x-goog-api-key`, `gemini_api_key`, `grok_api_key`).
4. **Cautela para alergias e restrições alimentares:** bloqueio de garantias inseguras (*"100% livre de glúten"*, *"zero contaminação cruzada"*) e inclusão obrigatória de aviso orientando confirmação direta com o restaurante.

---

## 8. Segurança e privacidade (D-11)

- Nenhuma chave de API no frontend, bundle Angular, repositório Git, respostas HTTP ou logs.
- Gemini recebe chave apenas via header `x-goog-api-key`; Grok recebe chave apenas via header `Authorization: Bearer`.
- `SecurityAuditLogger` redige `bearer`, `token`, `secret`, `api_key`, `x-goog-api-key`, `password` e `BORA_ASSISTANT_SESSION`.
- Nenhuma conversa, prompt ou resposta completa é persistida no PostgreSQL ou em logs; apenas métricas agregadas diárias por restaurante (`assistant_daily_metrics`, retenção de 180 dias): `INTERACTIONS`, `FALLBACKS`, `NO_RESULTS`, `PROVIDER_ERRORS`, `PROVIDER_GEMINI_SUCCESS`, `PROVIDER_GROK_SUCCESS`, `PROVIDER_FALLBACK_TO_GROK`, `PROVIDER_SAFETY_REFUSALS`, tokens e custo estimado.
- Sessões anônimas (`BORA_ASSISTANT_SESSION`), segredos independentes de produção (`ASSISTANT_SESSION_SECRET`, `ASSISTANT_RATE_LIMIT_SECRET`), CORS explícito, `ClientIpResolver` e cotas permanecem inalterados.

---

## 9. Matriz de variáveis de ambiente futuras

> **Importante:** nenhuma credencial deve ser versionada ou configurada nesta etapa. Antes da ativação real, confirmar endpoints, identificadores de modelo estáveis, faturamento e termos de dados na documentação oficial do Google AI (Gemini) e da xAI (Grok).

| Variável | Propriedade Spring | Padrão seguro | Finalidade |
| --- | --- | --- | --- |
| `AI_PRIMARY_PROVIDER` | `app.assistant.ai.primary-provider` | `gemini` | Provedor principal (deve permanecer `gemini`). |
| `AI_FALLBACK_PROVIDER` | `app.assistant.ai.fallback-provider` | `grok` | Provedor secundário de contingência. |
| `AI_FALLBACK_ENABLED` | `app.assistant.ai.fallback-enabled` | `true` | Habilita/desabilita contingência automática Gemini → Grok. |
| `AI_FALLBACK_ON_AUTH_OR_CONFIG_ERROR` | `app.assistant.ai.fallback-on-auth-or-config-error` | `true` | Define se erro 401/403/400 do primário pode acionar contingência após alerta operacional. |
| `AI_TOTAL_TIMEOUT_MS` | `app.assistant.ai.total-timeout-ms` | `15000` | Orçamento máximo de tempo total da solicitação (ms). |
| `AI_CIRCUIT_BREAKER_FAILURE_THRESHOLD` | `app.assistant.ai.circuit-breaker.failure-threshold` | `3` | Falhas técnicas consecutivas para abrir o circuito do provedor. |
| `AI_CIRCUIT_BREAKER_OPEN_DURATION_MS` | `app.assistant.ai.circuit-breaker.open-duration-ms` | `30000` | Tempo de abertura do circuito antes da sondagem `HALF_OPEN` (ms). |
| `GEMINI_ENABLED` | `app.assistant.gemini.enabled` | `false` | Habilita/desabilita o adaptador Gemini. |
| `GEMINI_DATA_PROCESSING_APPROVED` | `app.assistant.gemini.data-processing-approved` | `false` | Trava de aprovação formal dos termos/tier do Google AI. |
| `GEMINI_API_KEY` | `app.assistant.gemini.api-key` | *(vazio)* | Credencial privada do Google Gemini (somente backend). |
| `GEMINI_MODEL` | `app.assistant.gemini.model` | `gemini-2.5-flash-lite` | Identificador configurável do modelo Gemini Flash-Lite. |
| `GEMINI_TIMEOUT_MS` | `app.assistant.gemini.timeout-ms` | `0` *(usa `GEMINI_TIMEOUT_SECONDS`)* | Timeout individual do Gemini em ms (sobrepõe `GEMINI_TIMEOUT_SECONDS` quando > 0). |
| `GEMINI_TIMEOUT_SECONDS` | `app.assistant.gemini.timeout-seconds` | `12` | Timeout legado em segundos (compatibilidade mantida). |
| `GEMINI_MAX_OUTPUT_TOKENS` | `app.assistant.gemini.max-output-tokens` | `500` | Limite de tokens de saída do Gemini. |
| `GROK_ENABLED` | `app.assistant.grok.enabled` | `false` | Habilita/desabilita o adaptador Grok (contingência). |
| `GROK_DATA_PROCESSING_APPROVED` | `app.assistant.grok.data-processing-approved` | `false` | Trava de aprovação formal dos termos/tier da xAI. |
| `GROK_API_KEY` | `app.assistant.grok.api-key` | *(vazio)* | Credencial privada da xAI (somente backend). |
| `GROK_MODEL` | `app.assistant.grok.model` | `grok-3-mini` | Identificador configurável do modelo Grok (validar versão estável oficial antes de ativar). |
| `GROK_BASE_URL` | `app.assistant.grok.base-url` | `https://api.x.ai` | URL base oficial configurável da API da xAI. |
| `GROK_TIMEOUT_MS` | `app.assistant.grok.timeout-ms` | `8000` | Timeout individual do Grok em ms. |
| `GROK_MAX_OUTPUT_TOKENS` | `app.assistant.grok.max-output-tokens` | `500` | Limite de tokens de saída do Grok. |

---

## 10. Processo de ativação em staging

1. Confirmar aprovação comercial/jurídica dos termos de processamento de dados do Google AI e (se desejado para contingência) da xAI.
2. Confirmar o identificador exato do modelo estável na documentação oficial de cada fornecedor.
3. Configurar no serviço de **staging** (nunca em arquivos versionados):
   - Segredos independentes (`ASSISTANT_SESSION_SECRET`, `ASSISTANT_RATE_LIMIT_SECRET`), `ASSISTANT_SESSION_COOKIE_SECURE=true` e `CORS_ALLOWED_ORIGINS`.
   - `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_TIMEOUT_MS=7000`, `GEMINI_ENABLED=true`, `GEMINI_DATA_PROCESSING_APPROVED=true`.
   - Opcionalmente para validar contingência: `GROK_API_KEY`, `GROK_BASE_URL=https://api.x.ai`, `GROK_MODEL`, `GROK_TIMEOUT_MS=7000`, `GROK_ENABLED=true`, `GROK_DATA_PROCESSING_APPROVED=true`, `AI_FALLBACK_ENABLED=true`, `AI_TOTAL_TIMEOUT_MS=15000`.
4. Executar smoke tests controlados em staging:
   - Cenário normal: confirmar `PROVIDER_GEMINI_SUCCESS` incrementando e `PROVIDER_GROK_SUCCESS` em zero.
   - Simulação de contingência (ex.: `GEMINI_ENABLED=false` temporariamente em staging): confirmar resposta validada via Grok e incremento em `PROVIDER_GROK_SUCCESS` + `PROVIDER_FALLBACK_TO_GROK`.
   - Verificar latência, consumo de tokens, sanitização de preços/alergias e ausência de chaves/conversas em logs e tabelas.

---

## 11. Processo de ativação em produção

1. Concluir e aprovar todos os smoke tests em staging.
2. Configurar as variáveis aprovadas no cofre de secrets do Render (serviço `borae-api`), mantendo `GEMINI_ENABLED=true` como principal e `GROK_ENABLED` conforme aprovação operacional.
3. Monitorar nas primeiras 24h: taxa de erro (`PROVIDER_ERRORS`), acionamentos de fallback (`PROVIDER_FALLBACK_TO_GROK`), latência (`LATENCY_TOTAL_MS / LATENCY_SAMPLES`), recusas de segurança (`PROVIDER_SAFETY_REFUSALS`) e custo estimado (`ESTIMATED_COST_MICRO_USD`).

---

## 12. Como desabilitar um provedor emergencialmente (Kill Switch)

- **Desabilitar somente o Gemini (forçando contingência via Grok temporariamente):** definir `GEMINI_ENABLED=false` no ambiente e reiniciar o serviço.
- **Desabilitar somente o Grok (mantendo apenas Gemini + fallback estático seguro):** definir `GROK_ENABLED=false` ou `AI_FALLBACK_ENABLED=false` e reiniciar.
- **Desabilitar toda IA mantendo o cardápio público 100% funcional:** definir `GEMINI_ENABLED=false` e `GROK_ENABLED=false`. O endpoint responderá `fallback: true` (`AI_UNAVAILABLE`) sem consumir cota do consumidor ou do restaurante.

---

## 13. Como adicionar um terceiro provedor futuramente

1. Adicionar o novo identificador em `AiProviderId`.
2. Criar uma nova classe `@Component` implementando `AiProvider`:
   - Reaproveitar `AiProviderRequest` (mesmo `systemInstruction` e `buildUserPrompt()`).
   - Mapear a saída para `AiProviderResponse` e classificar erros exclusivamente via `AiProviderFailure.FailureType` (incluindo mapeamento correto de `SAFETY_REFUSAL`).
   - Enviar credencial apenas via cabeçalho HTTP e manter flag `enabled=false` + `data-processing-approved=false` por padrão.
3. Registrar o provedor na cadeia ordenada do `AiProviderOrchestrator` com seu próprio `AiProviderCircuitBreaker`, preservando o orçamento total `AI_TOTAL_TIMEOUT_MS` e sem alterar nenhuma regra de negócio de `GastronomicAssistantService`.
