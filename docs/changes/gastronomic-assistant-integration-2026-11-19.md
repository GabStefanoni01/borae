# Integração e Homologação — Assistente Gastronômico (Sessão 03)

**Data:** 19/11/2026  
**Branch:** `feature/gastronomic-assistant-integration`  
**Base:** `origin/main` (`3326bff`)

## 1. Resumo da entrega

Esta entrega consolida o backend do Assistente Gastronômico originado na PR #29 (`origin/feat/ai-gastronomic-assistant`), corrige conflitos arquiteturais e de segurança identificados na auditoria em relação à `main` (Sessão 01), atualiza o adaptador Gemini Flash-Lite para o endpoint oficial estável e integra a experiência conversacional completa ao cardápio público (`/r/:slug`) no frontend Angular.

## 2. Correções e evoluções no Backend (`backend/`)

- **Migrations Flyway compatíveis com PostgreSQL 16 / Neon:** renumeradas para `V11__create_assistant_daily_quotas.sql`, `V12__create_assistant_ip_rate_limit.sql` e `V13__create_assistant_daily_metrics.sql`, preservando intacta `V10__create_security_rate_limit_and_indexes.sql`.
- **Segurança & Sessão 01:**
  - `AssistantIpRateLimitService` passou a delegar a resolução de IP ao `ClientIpResolver` (fail-closed com `security.trusted-proxies`, sem leitura ingênua do primeiro IP de `X-Forwarded-For` e sem DNS lookup).
  - `SecurityConfig` habilitou `allowCredentials(true)` mantendo estritamente a validação de origens explícitas sem wildcard, viabilizando o cookie `HttpOnly` `BORA_ASSISTANT_SESSION` cross-origin.
  - `RateLimitService` separou `/api/v1/public/restaurants/{slug}/assistant/**` para uma regra de borda (`public-assistant-edge`, 60 req/min por IP) para não bloquear redes Wi-Fi compartilhadas de restaurantes em 20/dia por IP, mantendo `AssistantIpRateLimitService` (30 req/min) e `AssistantQuotaService` (20/sessão/dia + 100/restaurante/dia) como camada autoritativa com respostas estruturadas `429`.
  - `BoraeSecurityShieldFilter` aplica `Cache-Control: private, no-store, max-age=0, must-revalidate` nos endpoints públicos do assistente.
  - `AssistantExceptionHandler` recebeu `@Order(Ordered.HIGHEST_PRECEDENCE)` para garantir precedência sobre `ApiExceptionHandler` e entregar `code`, `scope`, `resetsAt`, `retryAfterSeconds` e cabeçalho `Retry-After`.
  - `@EnableScheduling` adicionado em `BoraeApplication` para ativar a purga agendada de buckets de IP (`purgeExpiredBuckets`) e métricas antigas (`purgeExpiredMetrics`).
- **Gemini Flash-Lite & Inteligência Gastronômica:**
  - `GeminiFlashLiteProvider` atualizado para o endpoint REST oficial `POST /v1beta/models/{model}:generateContent` com modelo padrão configurável `gemini-2.5-flash-lite` (substituindo o identificador inexistente `gemini-3.5-flash-lite`), chave estritamente via header `x-goog-api-key` e `generationConfig` aplicando `maxOutputTokens` (padrão 500), `temperature: 0.3`, `responseMimeType: "application/json"` e `responseSchema`.
  - `GastronomicAssistantService` passou a enviar nome/categoria do restaurante, menu, categoria, descrição, ingredientes e adicionais ativos no contexto limitado a 4.000 caracteres; preserva respostas naturais sanitizadas do modelo enquanto bloqueia alegações inseguras de alérgenos, preços fabricados em prosa e vazamentos de prompt; inclui aviso conservador obrigatório de confirmação com o restaurante sempre que a pergunta ou filtros envolvam alergias/intolerâncias/restrições.

## 3. Integração no Frontend (`frontend/`)

- `PublicAssistantService`: bootstrap de sessão anônima (`POST /api/v1/public/assistant/sessions`), interações (`POST /api/v1/public/restaurants/{slug}/assistant/interactions`) com renovação transparente em caso de `401` e rastreamento de cliques (`POST .../click`), sempre com `withCredentials: true`, sem JWT e sem exposição de chaves de IA.
- `PublicAssistantDrawerComponent`: botão flutuante não bloqueante, painel responsivo (bottom sheet no mobile, painel lateral no desktop/tablet), saudação com nome do restaurante, sugestões rápidas, filtros opcionais (orçamento máximo e ingredientes a evitar), histórico em memória apenas durante a sessão (D-11), indicador de carregamento, prevenção de envio duplicado, tratamento de `fallback`/`429`/rede e cards clicáveis que abrem o modal nativo de detalhes do produto (`MenuProductCardComponent`, incluindo adicionais e 3D).
- Integrado em `PublicMenuPage` (exibido apenas quando há produtos disponíveis no cardápio publicado) e suportado no modo demonstração (`demo-catalog.ts`).

## 4. Variáveis de ambiente necessárias para ativação em produção (Render)

| Variável | Recomendação em produção |
| --- | --- |
| `ASSISTANT_SESSION_SECRET` | Segredo dedicado de alta entropia (>= 32 bytes) |
| `ASSISTANT_RATE_LIMIT_SECRET` | Segredo dedicado de alta entropia (>= 32 bytes) |
| `ASSISTANT_SESSION_COOKIE_SECURE` | `true` |
| `ASSISTANT_SESSION_COOKIE_SAME_SITE` | `None` (se frontend e API estiverem em subdomínios distintos do Render) ou `Lax` (same-site) |
| `GEMINI_API_KEY` | Secret do Google AI Studio / Gemini API (exclusivamente no backend) |
| `GEMINI_MODEL` | `gemini-2.5-flash-lite` (ou versão estável equivalente configurada) |
| `GEMINI_ENABLED` | `true` |
| `GEMINI_DATA_PROCESSING_APPROVED` | `true` (após confirmação dos termos/tier do Google AI) |
| `GEMINI_TIMEOUT_SECONDS` | `12` |
| `GEMINI_MAX_OUTPUT_TOKENS` | `500` |
| `CORS_ALLOWED_ORIGINS` | Origens HTTPS explícitas do frontend (ex.: `https://borae-web.onrender.com`) |
