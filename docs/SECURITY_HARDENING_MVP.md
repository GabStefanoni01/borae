# Auditoria e Fortalecimento de Segurança do Backend — MVP (Sessões 01 e 01.1)

Este documento registra a auditoria de segurança do backend do Boraê, a reutilização e adaptação técnica dos componentes do **Neves Shield (Neves Scholar)**, as correções dos bloqueadores da revisão técnica (Sessão 01.1), a cobertura de testes e os riscos residuais para homologação comercial e produção.

---

## 1. Relatório de Auditoria do Estado Inicial e Bloqueadores da Revisão (Sessão 01.1)

| ID | Vulnerabilidade / Risco Encontrado | Arquivo / Localização | Impacto Potencial | Severidade | Correção Implementada |
|---|---|---|---|---|---|
| **SEC-01** | Valor padrão conhecido de `JWT_SECRET` (`MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=`) | `application.yml` e `JwtService.java` | Falsificação arbitrária de tokens JWT em ambientes sem `JWT_SECRET` explícito. | **CRÍTICA** | Removido o segredo padrão de `application.yml`. `JwtService` exige `JWT_SECRET` explícito, valida Base64 (>= 32 bytes), verifica entropia mínima (>= 8 bytes distintos) e bloqueia chaves conhecidas/previsíveis. |
| **SEC-REV-01** | Validação condicional de issuer JWT (`tokenIssuer != null`) aceitava tokens sem a claim `iss` | `JwtService.java` | Aceitação de tokens JWT emitidos sem a claim obrigatória `iss=borae-api`. | **ALTA (Bloqueador)** | `JwtService.extractSubject` utiliza `.requireIssuer(issuer)` diretamente no parser JJWT, rejeita qualquer algoritmo diferente de `HS256` e exige obrigatoriamente `iss`, `exp` e `sub` não vazios. |
| **SEC-REV-02** | Confiança por padrão em cabeçalhos encaminhados (`X-Forwarded-For`, `X-Real-IP`, `CF-Connecting-IP`) e leitura do primeiro salto à esquerda | `ClientIpResolver.java` e `application.yml` | Falsificação de IP (IP spoofing) por clientes externos para contornar rate limiting, bloqueio de WAF, auditoria e detecção de anomalias. | **ALTA (Bloqueador)** | `ClientIpResolver` só confia em cabeçalhos encaminhados quando `request.getRemoteAddr()` pertence explicitamente a `security.trusted-proxies` (padrão vazio/fail-closed). Quando há proxy confiável, percorre `X-Forwarded-For` da direita para a esquerda, valida literais IPv4/IPv6 sem consulta DNS e rejeita cadeias malformadas ou acima do limite. |
| **SEC-REV-03** | Bloqueio de 5 falhas consecutivas de login não integrado ao fluxo real de autenticação e `isLoginTemporarilyLocked` retornando `false` | `AuthService.java`, `AuthController.java`, `RateLimitService.java` | Tentativas repetidas de senha incorreta só eram barradas pelo limite volumétrico de IP, sem bloqueio efetivo de 5 falhas consecutivas por conta/origem. | **ALTA (Bloqueador)** | Integrados `assertLoginAttemptAllowed`, `recordLoginFailure` e `clearLoginFailures` ao fluxo real de `AuthService.login`, usando chaves derivadas por `HMAC-SHA256` (nunca e-mail puro), resposta HTTP `429` com `Retry-After` via `ApiExceptionHandler`, reset em login válido, expiração automática em 15 minutos e comportamento idêntico para e-mails existentes e inexistentes. |
| **SEC-02** | Ausência de validação fail-closed de configurações críticas na inicialização de produção | `application.yml` e `ProductionSecurityConfigValidator.java` | Inicialização silenciosa em produção com senha padrão de banco (`change-me`), CORS permissivo ou links de QR Code apontando para `localhost`. | **ALTA** | `ProductionSecurityConfigValidator` impede a inicialização nos perfis `prod`/`production` caso qualquer variável crítica esteja ausente ou insegura. `SecurityConfig` rejeita wildcard (`*`) em CORS em todos os perfis. |
| **SEC-03** | Ausência de rate limiting distribuído e inspeção contra varredura automatizada (WAF) | Todos os endpoints `/api/v1/**` | Força bruta, spam de cadastros/convites, abuso de upload no ImageKit e varredura automatizada (`sqlmap`, `nikto`, etc.). | **ALTA** | Implementados `BoraeSecurityShieldFilter`, `RateLimitService`, `PostgresRateLimitStorage` (tabela `security_rate_limit` na migration `V10`), `ResilientRateLimitStorage` (com throttling de logs), `WafRequestInspector` e `SecurityAnomalyMonitor`. |
| **SEC-04** | Ausência de tratamento global padronizado de exceções (`@RestControllerAdvice`) e de canal dedicado de logs de segurança | `ApiExceptionHandler.java` e `SecurityAuditLogger.java` | Respostas de erro inconsistentes e ausência de trilha padronizada de auditoria para falhas 401/403/429. | **ALTA** | `ApiExceptionHandler` sanitiza `DataAccessException` e exceções inesperadas sem expor SQL/stack traces e `SecurityAuditLogger` redige tokens e senhas automaticamente. |
| **SEC-06** | Cabeçalhos HTTP de segurança incompletos, `img-src` com wildcard `https:` e HSTS com `includeSubDomains` fixo | `BoraeSecurityShieldFilter.java` e `SecurityConfig.java` | Permissão excessiva de imagens externas na CSP e risco de impacto em subdomínios não-HTTPS com `includeSubDomains` fixo. | **MÉDIA** | CSP restrita a `'self' data: blob: https://ik.imagekit.io`, HSTS configurável (`max-age=31536000` sem `includeSubDomains` por padrão) e `X-Forwarded-Proto` aceito somente de proxy confiável. |
| **SEC-07** | Validação de uploads de imagem e GLB e exclusão remota pós-commit sem log de órfãos em `RestaurantMediaService` e `MenuProductImageService` | `RestaurantImageProcessor.java`, `GlbFileValidator.java`, `RestaurantMediaService.java`, `MenuProductImageService.java` | Risco de `data:text/html` em GLB, magic bytes falsificados e falha silenciosa na exclusão remota do ImageKit após commit. | **MÉDIA** | Validação prévia de magic bytes, limite de `12.000 px` por eixo, `data:` URIs em GLB restritos a `PNG`/`JPEG`/`WebP`, limpeza em rollback e retry + log auditável `REMOTE_STORAGE_ORPHAN_CLEANUP_REQUIRED` caso a exclusão remota pós-commit falhe. |

---

## 2. Impacto e Transição da Validação Obrigatória de Issuer JWT (`SEC-REV-01`)

- Todos os tokens emitidos pelo `JwtService.generateToken()` incluem `iss: "borae-api"`, `sub`, `iat` e `exp` (15 minutos por padrão), assinados com `HS256`.
- O `JwtService.extractSubject()` agora exige `requireIssuer("borae-api")` diretamente no parser JJWT. Qualquer token sem a claim `iss` é imediatamente rejeitado com `JwtException` (`401 Unauthorized`).
- **Estratégia de transição:** como os tokens emitidos antes da Sessão 01 possuíam validade de 15 minutos (`900.000 ms`) e não continham `iss`, qualquer token legado em trânsito expira naturalmente em no máximo 15 minutos após o deploy. No frontend Angular, o `auth.interceptor.ts` já trata respostas `401` limpando a sessão local e solicitando novo login, tornando a transição segura sem aceitar tokens sem `iss`.

---

## 3. Resolução Segura de IP e Cadeia de Proxies (`SEC-REV-02`)

1. **Padrão Fail-Closed:** `security.trusted-proxies` (`SECURITY_TRUSTED_PROXIES`) inicia vazio (`""`). Enquanto nenhum proxy confiável estiver configurado, `ClientIpResolver` ignora integralmente `X-Forwarded-For`, `X-Real-IP` e `CF-Connecting-IP` e utiliza `request.getRemoteAddr()`.
2. **Validação sem DNS Lookup:** endereços IPv4 e IPv6 são convertidos por parser próprio de literais (`parseIpLiteralToBytes`), nunca acionando resolução DNS em valores vindos de cabeçalhos HTTP.
3. **Travessia da Direita para a Esquerda:** quando `request.getRemoteAddr()` pertence a um proxy ou bloco CIDR listado em `SECURITY_TRUSTED_PROXIES`, a cadeia `X-Forwarded-For` (limitada a 512 caracteres e 16 saltos, com todos os saltos validados) é percorrida da direita para a esquerda, selecionando o primeiro endereço que **não** pertence à lista de proxies confiáveis. Isso impede que um cliente externo falsifique seu IP injetando endereços à esquerda do cabeçalho.

---

## 4. Arquitetura de Rate Limiting, Resiliência e Comportamento em Falha (Seção 6)

| Categoria | Regra / Escopo | Armazenamento Primário | Comportamento em Falha do PostgreSQL |
|---|---|---|---|
| **Login** (`POST /api/v1/auth/login`) | 5 falhas consecutivas por conta/origem (15 min de bloqueio) + 20 req/15 min volumétrico por IP | **PostgreSQL compartilhado** (`security_rate_limit`) com chaves `HMAC-SHA256` | `ResilientRateLimitStorage` degrada automaticamente para `InMemoryRateLimitStorage` (mantendo o bloqueio de 5 falhas por instância), emite alerta `[BORAE SECURITY] rate_limit_fallback` com throttling de 60s e registra `rate_limit_recovered` ao restabelecer. |
| **Cadastro** (`POST /api/v1/auth/register`) | 10 requisições / 60 min por IP | **PostgreSQL compartilhado** (`security_rate_limit`) | Degrada para `InMemoryRateLimitStorage` por instância com alerta auditado, mantendo proteção local sem derrubar o cadastro. |
| **Convites** (`POST .../invitations` e `/accept`) | Envio: 10/h por Admin e 30/dia por Restaurante (D-08 / 11.2); Aceite: 15/15 min por Usuário | **PostgreSQL compartilhado** (`restaurant_invitation` + `security_rate_limit`) | No aceite, degrada para memória local por instância; no envio, a contagem transacional em `restaurant_invitation` depende do banco principal da própria transação de criação do convite. |
| **Uploads** (logo, fotos, imagens de produto e GLB 3D) | 20 uploads / 10 min por Usuário autenticado (`HMAC-SHA256`) | **PostgreSQL compartilhado** (`security_rate_limit`) | Degrada para memória local por instância com alerta auditado, protegendo o consumo do ImageKit mesmo durante instabilidade no contador compartilhado. |
| **Assistente Gastronômico** | 20 interações / 24h por Consumidor (D-10 / 11.1) | **PostgreSQL compartilhado** (`security_rate_limit`) | Degrada para memória local por instância com alerta auditado. |
| **Administração** (criação/publicação de restaurante, equipe, QR Code) | 30 requisições / 1 min por Usuário autenticado (`HMAC-SHA256`) | **PostgreSQL compartilhado** (`security_rate_limit`) | Degrada para memória local por instância com alerta auditado. |
| **Leitura Pública** (`GET /api/v1/public/**`) e **Pico de Rajada** (`burst-peak`) | 120 req/min por IP (público) e 100 req/10s por IP (pico global) | **Memória local por instância** (`InMemoryRateLimitStorage`) | Não depende do PostgreSQL: evita escrita em banco a cada consulta pública de cardápio e continua operando mesmo se o banco estiver indisponível. |

### Limpeza e Desempenho da Tabela `security_rate_limit`
- A tabela `security_rate_limit` possui chave primária em `bucket_key` e índice em `reset_at` (`idx_security_rate_limit_reset_at`).
- A limpeza oportunista ocorre no máximo uma vez a cada 10 minutos por instância e executa remoção limitada em lote (`LIMIT 500` para registros expirados há mais de 15 minutos), evitando locks longos ou impacto na latência do login.

---

## 5. Cabeçalhos HTTP, CSP, HSTS e Compatibilidade com Frontend / 3D / AR (Seção 7)

- **`Content-Security-Policy`:**
  `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https://ik.imagekit.io; font-src 'self' data:; connect-src 'self' https://ik.imagekit.io; media-src 'self' blob: https://ik.imagekit.io; worker-src 'self' blob:; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`
  - Não utiliza `'unsafe-eval'` nem wildcard `https:` aberto.
  - Preserva `https://ik.imagekit.io`, `data:` e `blob:` necessários para carregamento de imagens, texturas e buffers binários GLB no visualizador 3D.
- **`Strict-Transport-Security` (HSTS):**
  - Valor padrão em HTTPS: `max-age=31536000` (1 ano), **sem** `includeSubDomains` por padrão (`SECURITY_HSTS_INCLUDE_SUBDOMAINS=false`), evitando impacto em subdomínios não verificados.
  - Só é emitido quando `request.isSecure() == true`, quando `SECURITY_HSTS_ENABLED=true`, ou quando `X-Forwarded-Proto: https` é enviado por um proxy explicitamente listado em `SECURITY_TRUSTED_PROXIES`.

---

## 6. Uploads, ImageKit e Recuperação de Inconsistências Pós-Commit (Seção 8)

- **Validação de Conteúdo:**
  - `RestaurantImageProcessor` valida magic bytes reais (`JPEG`: `FF D8 FF`, `PNG`: `89 50 4E 47 0D 0A 1A 0A`, `WebP`: `RIFF....WEBP`) antes da leitura por `ImageIO`, limita cada eixo a `12.000 px` e `40 MP` totais, e regrava o bitmap eliminando metadados EXIF e payloads embutidos.
  - `GlbFileValidator` valida o cabeçalho binário GLB 2.0 (`glTF`, versão 2, comprimento exato), rejeita chunks desconhecidos, proíbe `buffers[].uri` externos e restringe `images[].uri` exclusivamente a `data:image/png`, `data:image/jpeg`, `data:image/jpg` e `data:image/webp`.
- **Consistência Transacional e Recuperação de Órfãos no ImageKit:**
  - Em `RestaurantMediaService`, `MenuProductImageService` e `Product3DModelService`, um hook `afterCompletion(STATUS_ROLLED_BACK)` remove do ImageKit qualquer arquivo recém-enviado caso a transação do PostgreSQL sofra rollback.
  - A remoção de arquivos substituídos ou excluídos ocorre somente em `afterCommit()`, com retentativa imediata em caso de falha transitória.
  - Caso a exclusão remota no ImageKit falhe após a retentativa pós-commit, a aplicação registra o marcador estruturado `REMOTE_STORAGE_ORPHAN_CLEANUP_REQUIRED` com a fase e o `fileId` nos logs para reconciliação operacional, sem reverter a transação já confirmada no banco e sem tratar silenciosamente a falha como concluída.
