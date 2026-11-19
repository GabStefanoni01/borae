# Auditoria e Fortalecimento de Segurança do Backend — MVP (Sessão 01)

Este documento registra a auditoria de segurança do backend do Boraê, a reutilização e adaptação técnica dos componentes do **Neves Shield (Neves Scholar)**, as correções implementadas, a cobertura de testes e os riscos residuais para homologação comercial e produção.

---

## 1. Relatório de Auditoria do Estado Inicial

| ID | Vulnerabilidade / Risco Encontrado | Arquivo / Localização | Impacto Potencial | Severidade | Correção Implementada |
|---|---|---|---|---|---|
| **SEC-01** | Valor padrão conhecido de `JWT_SECRET` (`MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=`, correspondente a `"0123456789abcdef0123456789abcdef"`) | `backend/src/main/resources/application.yml` (linha 30) e `JwtService.java` | Falsificação arbitrária de tokens JWT em qualquer ambiente onde `JWT_SECRET` não fosse sobrescrito, permitindo tomada completa de contas e restaurantes. | **CRÍTICA** | Removido o segredo padrão de `application.yml`. `JwtService` exige `JWT_SECRET` explícito, valida Base64 (>= 32 bytes), verifica entropia mínima (>= 8 bytes distintos) e bloqueia chaves conhecidas/previsíveis. |
| **SEC-02** | Ausência de validação fail-closed de configurações críticas na inicialização de produção (`DB_PASSWORD: change-me`, `DB_URL` local, `CORS_ALLOWED_ORIGINS` com `localhost` ou `*`, `PUBLIC_RESTAURANT_URL_PREFIX` sem HTTPS) | `backend/src/main/resources/application.yml` e `SecurityConfig.java` | Inicialização silenciosa em produção com senha padrão de banco, CORS permissivo ou links de QR Code apontando para `localhost`. | **ALTA** | Criado `ProductionSecurityConfigValidator` que impede a inicialização nos perfis `prod`/`production` caso qualquer variável crítica esteja ausente ou insegura. `SecurityConfig` rejeita wildcard (`*`) em CORS em todos os perfis. |
| **SEC-03** | Ausência de rate limiting, proteção contra brute force e inspeção contra varredura automatizada (WAF) | Todos os endpoints `/api/v1/**` | Ataques de força bruta em `/api/v1/auth/login`, spam de cadastros/convites, abuso de upload no ImageKit e varredura automatizada (`sqlmap`, `nikto`, etc.). | **ALTA** | Implementados `BoraeSecurityShieldFilter`, `RateLimitService`, `PostgresRateLimitStorage` (tabela `security_rate_limit` na migration `V10`), `ResilientRateLimitStorage`, `WafRequestInspector` e `SecurityAnomalyMonitor`, além de limites de convites em `RestaurantTeamService` (10/h por admin e 30/dia por restaurante). |
| **SEC-04** | Ausência de tratamento global padronizado de exceções (`@RestControllerAdvice`) e de canal dedicado de logs de segurança | `backend/src/main/java/com/borae/config/` | Respostas de erro inconsistentes em falhas de validação/multipart/banco e ausência de trilha padronizada de auditoria para falhas de autenticação (401), autorização (403) e rate limit (429). | **ALTA** | Criados `ApiExceptionHandler` (sanitização integral de `DataAccessException` e exceções inesperadas, retorno padronizado sem stack traces/SQL e `413 Payload Too Large` para uploads acima do limite) e `SecurityAuditLogger` com redação automática de tokens e senhas. |
| **SEC-05** | Validação de JWT sem verificação de emissor (`iss`), sem exigência explícita de claim `exp` e sem limite de tamanho do token | `JwtService.java` e `JwtAuthenticationFilter.java` | Aceitação de tokens sem data de expiração (caso assinados pela mesma chave) ou sem identificação de emissor, além de descarte silencioso de falhas sem registro de auditoria. | **MÉDIA** | `JwtService` inclui e valida o emissor (`borae-api`), exige `exp` e `sub` não vazios, limita o tamanho do token (4096 chars) e `JwtAuthenticationFilter` registra falhas no `SecurityAuditLogger` sem expor o token. |
| **SEC-06** | Cabeçalhos HTTP de segurança incompletos nas respostas da API | `SecurityConfig.java` | Ausência de `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy`, `X-Permitted-Cross-Domain-Policies` e `Strict-Transport-Security` explícitos. | **MÉDIA** | `BoraeSecurityShieldFilter` aplica `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 0`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`, `Content-Security-Policy` compatível com ImageKit/3D e `Strict-Transport-Security` em HTTPS. |
| **SEC-07** | Validação de uploads de imagem e GLB vulnerável a magic bytes falsificados, dimensões extremas por eixo, `data:` URIs não-raster em GLB e exclusão não transacional na galeria do restaurante | `RestaurantImageProcessor.java`, `GlbFileValidator.java`, `Product3DModelService.java`, `RestaurantMediaService.java` | Processamento de formatos não permitidos antes da checagem do `ImageReader`, risco de `data:text/html` ou `data:image/svg+xml` embutido em `.glb`, e violação de FK (HTTP 500) ao tentar excluir `PRODUCT_IMAGE` pelo endpoint de galeria. | **MÉDIA** | Validação prévia de magic bytes (JPEG, PNG, WebP), limite de dimensão máxima por eixo (`12.000 px`), restrição de `data:` URIs em GLB apenas para `image/png`, `image/jpeg` e `image/webp`, rejeição de chunks desconhecidos, bloqueio de `PRODUCT_IMAGE` no endpoint de galeria e exclusão remota agendada `afterCommit`. |
| **SEC-08** | Aceite de convite sem verificar se o restaurante está ativo e diferença de tempo de resposta (timing side-channel) em login com e-mail inexistente | `RestaurantTeamService.java`, `RestaurantQrCodeService.java`, `AuthService.java` | Possibilidade de aceitar convite de restaurante desativado e enumeração de e-mails cadastrados por medição de latência do BCrypt no login. | **BAIXA** | `RestaurantTeamService.acceptInvitation` e `RestaurantQrCodeService` validam restaurante ativo; `AuthService.login` executa verificação BCrypt dummy quando o e-mail não existe. |

---

## 2. Reutilização e Adaptação do Neves Shield (Neves Scholar)

A biblioteca de referência `neves-shield` (extraída da plataforma Neves Scholar) foi integralmente analisada e adaptada para a arquitetura Java 21 + Spring Boot 3.5 + PostgreSQL do Boraê:

| Componente Neves Shield | Destino no Boraê | Decisão | Justificativa Técnica |
|---|---|---|---|
| `sql/limites_de_uso.sql` e `src/limite-compartilhado/postgres.ts` | `V10__create_security_rate_limit_and_indexes.sql` e `PostgresRateLimitStorage.java` | **Reutilizado e Adaptado** | Utiliza UPSERT atômico no PostgreSQL (`INSERT ... ON CONFLICT (bucket_key) DO UPDATE ... RETURNING request_count, reset_at`) para compartilhar contadores de rate limit entre múltiplas instâncias no Render sem introduzir custo de Redis externo no MVP. |
| `src/limite-compartilhado/armazenamento.ts` (`comReservaLocal`, `criarArmazenamentoEmMemoria`) | `ResilientRateLimitStorage.java` e `InMemoryRateLimitStorage.java` | **Reutilizado e Adaptado** | Evita falha silenciosa caso o banco sofra instabilidade: degrada para contagem local em memória e registra alerta no canal `[BORAE SECURITY]`. |
| `src/limite-compartilhado/limite.ts` (`sujeitoDoLimite`, `chaveDoLimite`) | `RateLimitService.java` | **Reutilizado e Adaptado** | Separa chaves autenticadas (`usuario:<hash>`) — derivadas exclusivamente do JWT validado no servidor — de chaves anônimas (`anon:<ip>`), evitando bloquear usuários legítimos que compartilham o mesmo IP (Wi-Fi de restaurante ou CGNAT 4G). |
| `src/limite-por-ip.ts` (política `so-a-rota`, detecção de `pico` e bloqueio progressivo `2^reincidencias`) | `RateLimitService.java` | **Reutilizado e Adaptado** | Aplica HTTP `429` escopado por rota (`so-a-rota`) para limites operacionais e reserva o bloqueio global progressivo do IP (dobrando até 32x) para ataques de WAF e scanners. |
| `src/waf.ts` (`formasDeInspecao`, `PADROES_NA_URL`, `AGENTES_DE_VARREDURA`) | `WafRequestInspector.java` | **Reutilizado e Adaptado** | Expressões regulares com repetição limitada (protegidas contra ReDoS) e decodificação de `%XX` duplo, `\uXXXX`, entidades HTML e comentários SQL `/**/` para bloquear `sqlmap`, `nikto`, `nmap`, `nuclei`, SQLi, XSS, Command Injection, Path Traversal e Header Injection. Ignora payloads `multipart/form-data` e binários para evitar falsos positivos em imagens e modelos `.glb`. |
| `src/cabecalhos.ts` e `src/csp.ts` | `BoraeSecurityShieldFilter.java` | **Reutilizado e Adaptado** | Aplica `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 0` (padrão OWASP atual), `Referrer-Policy`, `Permissions-Policy`, `Cache-Control: private, no-store` em rotas privadas, `Strict-Transport-Security` em HTTPS e `Content-Security-Policy` adaptada para permitir `https://ik.imagekit.io`, `blob:` e `data:` necessários ao visualizador 3D/AR. |
| `src/ip.ts` (`ipDoPedido`) | `ClientIpResolver.java` | **Reutilizado e Adaptado** | Lê o primeiro IP válido dos cabeçalhos confiáveis configurados (`X-Forwarded-For`, `X-Real-IP`, `CF-Connecting-IP`), removendo `x-vercel-forwarded-for` por não ser aplicável fora da Vercel. |
| `src/anomalias.ts` (`criarMonitorDeAnomalias`, `sinaisDeAutomacao`) | `SecurityAnomalyMonitor.java` | **Reutilizado e Adaptado** | Monitora varredura de rotas (>= 30 rotas/5 min), insistência após recusas (>= 20 recusas/5 min) e sinais de automação com limite de memória (`5.000` IPs), emitindo alertas sem bloquear IPs compartilhados. |
| `src/registro.ts` e `src/porta.ts` | `SecurityAuditLogger.java` e `SecurityConfig.java` | **Reutilizado e Adaptado** | Canal de auditoria `[BORAE SECURITY]` sempre ativo em nível `WARN`/`ERROR`, registrando metadados do evento e mascarando qualquer padrão de token ou senha. |
| `src/robo.ts` (Vercel BotID / Turnstile / hCaptcha) | — | **Descartado** | Exige integração com serviços externos e alteração de fluxo no frontend Angular, vedados pelas restrições da Sessão 01. |
| `src/selo.ts` e `src/cifra.ts` | — | **Descartado** | No Boraê, preços (em centavos), disponibilidade e permissões são resolvidos integralmente no servidor e no PostgreSQL, sem tráfego de estado calculado confiável pelo cliente. |

---

## 3. Cobertura e Limites de Rate Limiting

| Regra | Escopo | Limite Padrão | Janela | Armazenamento |
|---|---|---:|---|---|
| `auth-login` (`POST /api/v1/auth/login`) | Por IP | 10 requisições | 15 minutos | Compartilhado (PostgreSQL + reserva local) |
| `auth-register` (`POST /api/v1/auth/register`) | Por IP | 10 requisições | 60 minutos | Compartilhado (PostgreSQL + reserva local) |
| `auth-password-recovery` | Por IP | 5 requisições | 60 minutos | Compartilhado (PostgreSQL + reserva local) |
| `invitation-send` (`POST /api/v1/restaurants/{id}/invitations`) | Por Admin e por Restaurante | 10/hora por Admin e 30/dia por Restaurante (D-08 / 11.2) | 1h / 24h | Compartilhado (PostgreSQL) |
| `invitation-accept` (`POST /api/v1/restaurant-invitations/accept`) | Por Usuário autenticado | 15 requisições | 15 minutos | Compartilhado (PostgreSQL + reserva local) |
| `anonymous-session` | Por IP | 20 requisições | 10 minutos | Compartilhado (PostgreSQL + reserva local) |
| `gastronomic-assistant` | Por Consumidor / IP | 20 interações | 24 horas | Compartilhado (PostgreSQL + reserva local) |
| `media-upload` (imagens e modelos 3D) | Por Usuário autenticado | 20 uploads | 10 minutos | Compartilhado (PostgreSQL + reserva local) |
| `admin-sensitive` (criação/publicação de restaurante, gestão de membros, QR Code) | Por Usuário autenticado | 30 requisições | 1 minuto | Compartilhado (PostgreSQL + reserva local) |
| `public-endpoint` (`GET /api/v1/public/**`) | Por IP | 120 requisições | 1 minuto | Memória por instância |
| `api-default` (demais rotas autenticadas) | Por Usuário autenticado | 120 requisições | 1 minuto | Memória por instância |
| `burst-peak` (`pico` global por IP) | Por IP (soma de todas as rotas) | 100 requisições | 10 segundos | Memória por instância |

---

## 4. Variáveis Obrigatórias e Validação de Produção

No perfil `prod` ou `production`, `ProductionSecurityConfigValidator` valida na inicialização:

- `JWT_SECRET`: obrigatório em todos os ambientes; deve ser Base64 válido com pelo menos 32 bytes (256 bits) decodificados, alta entropia (>= 8 bytes distintos) e diferente de qualquer valor padrão conhecido.
- `DB_URL`: obrigatório; deve iniciar com `jdbc:postgresql://` e não pode apontar para `localhost` ou `127.0.0.1` em produção.
- `DB_USERNAME`: obrigatório e não vazio.
- `DB_PASSWORD`: obrigatório, mínimo de 8 caracteres e diferente de valores padrão (`change-me`, `postgres`, `password`, `admin`, etc.).
- `CORS_ALLOWED_ORIGINS`: obrigatório; lista separada por vírgulas de origens explícitas `https://`, sem wildcard (`*`) e sem `localhost`.
- `PUBLIC_RESTAURANT_URL_PREFIX`: obrigatório; URL absoluta `https://` válida para geração do QR Code público.

---

## 5. Riscos Residuais e Pendências Operacionais

1. **Armazenamento do JWT no Frontend (`localStorage`)**: conforme documentado em `docs/ARCHITECTURE.md` (seção 4), o frontend Angular do MVP persiste o access token no `localStorage`. O tempo de expiração curto (`15 minutos` por padrão) e a `Content-Security-Policy` reduzem a janela de exposição, mas a evolução futura para refresh token com cookie `HttpOnly; Secure; SameSite` e revogação explícita de sessão permanece recomendada antes da expansão comercial em larga escala.
2. **Rate Limiting de Leitura Pública (`public-endpoint`) e Rajada (`burst-peak`)**: para não adicionar uma escrita no PostgreSQL a cada leitura anônima de cardápio (`GET /api/v1/public/restaurants/{slug}/menu`), as regras de leitura pública de alta frequência e detecção de pico de 10 segundos rodam em memória por instância, enquanto todas as rotas sensíveis, de autenticação, upload e mutação utilizam o armazenamento compartilhado no PostgreSQL (`security_rate_limit`). Quando houver escalonamento horizontal com múltiplas instâncias sob alto tráfego ou adoção de Cloudflare WAF/Redis na borda, recomenda-se ativar rate limiting também no proxy/CDN de borda.
3. **Inspeção Profunda de Modelos 3D e Antimalware**: o `GlbFileValidator` valida a estrutura binária GLB 2.0, bloqueia chunks desconhecidos, proíbe referências externas (`buffers` e `images`) e restringe `data:` URIs exclusivamente a imagens raster (`PNG`, `JPEG`, `WebP`). A homologação da entrega de arquivos `.glb` na conta ImageKit de produção e limites de complexidade geométrica (contagem de vértices/polígonos) permanecem como etapas operacionais do módulo 3D/AR.
