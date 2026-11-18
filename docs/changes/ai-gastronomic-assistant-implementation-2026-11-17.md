# Plano de implementação — Assistente gastronômico

**Data:** 17/11/2026  
**Status:** sessão anônima, cotas, endpoint público de interação, rate limit por IP pseudonimizado, métricas agregadas e adaptador Gemini implementados inicialmente; CI do commit final em validação; frontend Angular pendente.  
**Escopo:** primeira entrega do assistente no cardápio público, seguindo `MODULE_AI_ASSISTANT.md`, D-10 e D-11.

## 1. Objetivo do MVP

Permitir que um consumidor, sem precisar criar conta, peça recomendações em linguagem natural dentro do cardápio público de um restaurante. O assistente consulta somente o catálogo publicado daquele restaurante e devolve recomendações estruturadas vinculadas a produtos reais.

O assistente é uma camada de descoberta. Não cria pedidos, não adiciona itens ao carrinho, não altera catálogo, não concede descontos e não substitui a navegação tradicional.

## 2. Regras de negócio obrigatórias

1. **Escopo do restaurante:** o slug da rota pública determina o restaurante. Nunca aceitar `restaurantId` enviado pelo navegador como autoridade.
2. **Catálogo elegível:** considerar somente restaurante ativo/publicado, cardápios publicados, categorias ativas e produtos não arquivados com disponibilidade `AVAILABLE`.
3. **Fonte da verdade:** nome, preço em centavos, ingredientes, imagem e disponibilidade são carregados novamente do banco pelo backend. O modelo não define esses valores.
4. **Recomendações:** cada recomendação precisa apontar para um `productId` pertencente ao catálogo elegível do restaurante. IDs desconhecidos, duplicados, arquivados, indisponíveis ou de outro restaurante são descartados.
5. **Preço:** usar sempre `priceInCents` do catálogo; nunca aceitar preço produzido pelo modelo.
6. **Ingredientes:** explicações sobre composição usam somente a lista cadastrada pelo restaurante. Não inferir receita, modo de preparo, ingredientes ocultos ou ausência de alergênicos/contaminação cruzada.
7. **Alergias e segurança alimentar:** nunca garantir que um prato é seguro para uma alergia ou restrição clínica. Orientar o consumidor a confirmar diretamente com o estabelecimento.
8. **Sem resultado:** informar que não encontrou opção compatível e sugerir ajustar orçamento/categoria/preferências. Não preencher a resposta com produtos inventados.
9. **Falha do provedor:** timeout, erro HTTP, indisponibilidade ou JSON inválido não podem bloquear o cardápio. Retornar estado de fallback para a busca e os filtros convencionais.
10. **Sem histórico persistido:** não gravar perguntas, prompts, respostas completas, histórico de conversa, IP ou identificadores pessoais em eventos analíticos.
11. **Sem ação transacional:** o modelo não cria pedidos, reserva, compra, cupom, pagamento ou alteração de disponibilidade.
12. **Conteúdo não confiável:** mensagem do consumidor, descrição e ingredientes são dados, nunca instruções para o modelo. Aplicar defesa contra prompt injection e limitar a resposta ao contrato esperado.

## 3. Provedor e limites aprovados

- Provedor: Google Gemini.
- Modelo: Gemini Flash-Lite; identificador configurável (`GEMINI_MODEL`), com padrão inicial `gemini-3.5-flash-lite` conforme a lista de modelos compatíveis com Interactions API.
- Integração exclusivamente no backend Java/Spring Boot.
- Chave em variável/secret do backend (proposta: `GEMINI_API_KEY`); nunca no Angular, no bundle, no Git ou em logs.
- Limite por interação: contexto do catálogo limitado a **4.000 caracteres** e mensagem a 500 caracteres; saída limitada a **500 tokens**. A contagem exata de tokens de entrada ainda precisa ser validada antes de produção.
- Cotas simultâneas: até **20 interações por consumidor/dia** e **100 por restaurante/dia**.
- Renovação diária à meia-noite no fuso `America/Sao_Paulo`.
- Atingir qualquer uma das cotas bloqueia novas interações de IA no escopo correspondente até a renovação. O cardápio permanece utilizável.
- Registrar tokens (quando fornecidos), latência, erros e custo estimado por restaurante.
- Antes de produção, validar versão estável, faturamento, limites, termos de uso e tratamento de dados do Google.

## 4. Decisão aprovada — sessão anônima assinada (17/11/2026)

A identidade anônima foi aprovada pelo responsável pelo produto. O backend emite cookie `BORA_ASSISTANT_SESSION`, com 256 bits aleatórios, assinatura HMAC-SHA256, HttpOnly, validade de 30 dias e atributos SameSite/Secure configuráveis. O endpoint `POST /api/v1/public/assistant/sessions` reutiliza cookie válido e emite um novo quando necessário.

A cota de 20 é tecnicamente por sessão de navegador, não uma garantia por pessoa física. Apagar cookies ou trocar de navegador/dispositivo pode gerar nova sessão; não usar fingerprinting. IP será somente camada antiabuso separada, nunca identidade. O banco recebe apenas chave HMAC opaca, nunca cookie ou ID bruto. Em produção, configurar segredo dedicado de alta entropia e cookie Secure.

A migration V10 e `AssistantQuotaService` implementam cotas de 20 por sessão/dia e 100 por restaurante/dia com upsert atômico e transação única. Testes PostgreSQL cobrem consumo, limite de sessão e rollback quando o limite do restaurante é atingido. Teste de concorrência paralela ainda precisa ser adicionado.

## 5. Backend — desenho de implementação

Manter o monólito modular existente. Criar o domínio `com.borae.assistant`; não criar microserviço Python/FastAPI nesta entrega.

### Componentes

- `PublicAssistantSessionController` e `PublicAssistantInteractionController`: bootstrap da sessão e endpoint público de interação.
- `AssistantInteractionRequest` / `AssistantInteractionResponse`: contrato HTTP com validação de entrada.
- `GastronomicAssistantService`: resolve o restaurante pelo slug, carrega catálogo elegível, aplica cotas e revalida a saída.
- `AssistantCatalogContextService`: monta contexto mínimo do restaurante e dos produtos publicados/disponíveis.
- `AiProvider`: interface interna independente do SDK/provedor.
- `GeminiFlashLiteProvider`: adaptador REST stateless para a API Interactions do Gemini Flash-Lite, `store=false`, `response_format` JSON Schema efetivamente enviado, timeout, limite de tokens de saída e captura de uso de tokens; ativação exige `GEMINI_ENABLED=true`, chave e aprovação explícita de tratamento de dados.
- `AssistantIpRateLimitService`: limite distribuído de 30 solicitações/minuto por IP, com HMAC no banco, sem persistir endereço bruto e `Retry-After` em 429.
- `AssistantMetricsService`: agregados diários por restaurante para interações, fallback, sem resultado, erros do provedor, latência, tokens, custo estimado, recomendações por produto e cliques; retenção de 180 dias.
- `AssistantRecommendationClickService`: valida sessão e disponibilidade atual do produto antes de registrar clique agregado.
- `AssistantQuotaService`: consumo diário atômico por restaurante e sessão anônima, com janela `America/Sao_Paulo`.
- `AssistantMetricsService`: grava somente métricas/eventos agregados permitidos por D-11.
- `AssistantRecommendationValidator`: valida IDs, remove duplicidades e recarrega dados autoritativos do catálogo.
- `AssistantExceptionHandler`: respostas estáveis para limite atingido, timeout e indisponibilidade.

### Endpoints implementados/propostos

| Método | Endpoint | Acesso | Responsabilidade |
|---|---|---|---|
| POST | `/api/v1/public/assistant/sessions` | Público | Emitir/reutilizar cookie de sessão anônima para cotas. **Implementado.** |
| POST | `/api/v1/public/restaurants/{slug}/assistant/interactions` | Público + sessão | Enviar intenção/filtros e receber resposta e recomendações verificadas. **Implementação inicial concluída.** |
| POST | `/api/v1/public/restaurants/{slug}/assistant/recommendations/{productId}/click` | Público + sessão | Registrar clique agregado em recomendação, sem persistir identidade ou texto. |

### Contrato de interação proposto

Request:
- `message`: obrigatório, texto simples, tamanho máximo técnico a definir no DTO; não aceitar HTML como instrução.
- `menuId`: opcional; se informado, precisa pertencer ao restaurante e estar publicado.
- `filters.maxPriceInCents`: opcional, inteiro não negativo.
- `filters.categoryIds`: opcional; validar que todas as categorias pertencem a cardápios publicados do restaurante.
- `filters.excludedIngredients`: opcional, lista textual normalizada; é preferência de busca, não declaração de alergênicos.
- Não aceitar `restaurantId`, preço de produto, disponibilidade, role ou identidade de usuário como fonte de autoridade.

Response:
- `answer`: texto curto do assistente.
- `recommendations[]`: `productId`, `name`, `priceInCents`, `availability`, `imageUrl`, `ingredients[]` e `reason`; campos autoritativos vêm do banco.
- `quota`: `remainingConsumer`, `remainingRestaurant` e `resetsAt`, sem revelar identificador de sessão.
- `fallback`: booleano e código seguro para a interface decidir exibir busca/filtros convencionais.
- Resposta sem recomendação válida deve conter lista vazia.

### Contrato de saída do modelo

O modelo deve retornar somente estrutura JSON contendo IDs candidatos e justificativas curtas. O backend descarta qualquer campo não esperado, valida os IDs contra a lista permitida enviada no contexto, remove duplicados e reconstrói os cards a partir do banco. Não usar tool calling com acesso ao banco, execução de código ou ferramentas administrativas.

### Persistência e métricas

Adicionar migration Flyway para:
- Contadores diários atômicos de cota, com chave composta por escopo/tipo/dia e contador; armazenar apenas hash/HMAC do identificador de sessão, nunca cookie ou ID bruto.
- Eventos analíticos agregados por restaurante e dia: total de interações, IDs de produtos recomendados/frequência, cliques, sem resultado, latência, timeout/erro/fallback e tokens/custo quando disponíveis.
- Constraints e índices para impedir duplicidade do contador diário e permitir atualização concorrente segura.

Não persistir texto da mensagem, prompt, resposta, conteúdo integral enviado ao provedor, IP ou histórico conversacional. A retenção dos agregados deve seguir política documentada e não incluir dados pessoais.

### Resiliência e segurança

- Timeout de conexão e resposta configuráveis; retry ainda não implementado. Falhas do provedor retornam fallback; a tentativa consome cota quando o provedor estava habilitado e a chamada foi iniciada.
- O adaptador permanece desabilitado por padrão. Só habilitar com `GEMINI_ENABLED=true`, `GEMINI_API_KEY` e `GEMINI_DATA_PROCESSING_APPROVED=true`, após validar plano/faturamento e termos de tratamento de dados do Google. O fato de não armazenarmos histórico no Boraê não elimina o tratamento feito pelo provedor.
- Contar a interação conforme a regra aprovada e definir claramente se falha técnica consome cota; proposta: falha anterior à chamada do provedor não consome; chamada efetivamente enviada consome para impedir abuso/custo.
- Rate limit adicional implementado: 30 requisições/minuto por IP com HMAC e bucket PostgreSQL distribuído. Por padrão usa `request.getRemoteAddr()` e ignora `X-Forwarded-For`; `ASSISTANT_TRUST_FORWARDED_HEADERS=true` só deve ser usado se o proxy confiável sobrescrever esse header. IP não substitui a cota da sessão.
- Respostas de erro não devem expor prompt, stack trace, chave ou mensagem bruta do provedor.
- Não registrar corpo HTTP de entrada/saída nem conteúdo enviado ao Gemini.
- Não confiar em nomes, descrições ou ingredientes como instruções. Escapar/delimitar contexto e instruir o modelo a ignorar comandos contidos nos dados.

## 6. Frontend — escopo para Renan

A interface será implementada no Angular existente, integrada à página pública do cardápio e sem introduzir uma segunda aplicação.

### Experiência

1. Adicionar CTA discreto “Me ajuda a escolher?” na página pública do restaurante.
2. Abrir painel de conversa responsivo (drawer/modal em desktop e painel de tela no mobile), sem bloquear navegação do cardápio.
3. Apresentar perguntas sugeridas: “Quero algo até R$ 40”, “Prefiro opções vegetarianas” e “O que combina com…?” — sem prometer classificação vegetariana/alergênica se os dados não sustentarem.
4. Permitir texto livre e filtros estruturados de orçamento/categoria.
5. Renderizar recomendações reutilizando o card de produto existente, com preço/imagem/ingredientes recebidos do backend e justificativa curta.
6. Clique no card abre o detalhe do produto no fluxo atual; não cria pedido.
7. Exibir estados: inicial, enviando, resposta, sem resultados, limite diário, indisponibilidade do provedor, sessão expirada e erro recuperável.
8. Se IA falhar ou a cota acabar, manter filtros e navegação convencionais disponíveis.
9. Não persistir conversa em localStorage/sessionStorage; estado fica em memória e é descartado ao fechar/recarregar.
10. Acessibilidade: foco controlado ao abrir/fechar, botão de fechar acessível, mensagens anunciadas por região live, navegação por teclado, contraste e labels.
11. Não incluir chave do Gemini, lógica de cotas ou regra de disponibilidade no frontend.

### Componentes/serviços sugeridos

- `PublicAssistantPanelComponent`: apresentação e estados da conversa.
- `PublicAssistantService`: sessão e chamadas HTTP.
- `AssistantMessageComponent`: mensagens sem persistência.
- `AssistantRecommendationListComponent`: cards reutilizados do catálogo.
- `AssistantPromptSuggestionsComponent`: sugestões iniciais.
- Tipos TypeScript derivados do contrato OpenAPI, sem duplicar regra de negócio.

### Responsabilidade de integração

Renan (`@renancristianobn-del`) é responsável exclusivamente pela implementação Angular: painel, serviço HTTP, estados, acessibilidade e integração com o cardápio público. Ele não é reviewer desta PR. O frontend deve aguardar DTOs/OpenAPI estáveis; a integração final valida CORS, cookie de sessão com credenciais, SameSite/Secure e ambiente de homologação.

## 7. Testes e critérios de aceite

### Backend
- Restaurante inexistente, inativo ou não publicado.
- Cardápios/categorias/produtos não publicados ou arquivados nunca aparecem.
- Produto indisponível nunca é apresentado como disponível.
- ID inventado, duplicado ou de outro restaurante é descartado.
- Preço e ingredientes retornados são sempre do banco.
- Prompt injection em mensagem/descrição não altera escopo nem regras.
- JSON inválido/saída fora do schema gera fallback seguro.
- Timeout/5xx do Gemini não bloqueia cardápio.
- Cotas de consumidor e restaurante aplicadas simultaneamente, atomicamente e em concorrência.
- Renovação à meia-noite de `America/Sao_Paulo`, incluindo mudança de dia e horário de verão conforme regras da zona.
- Nenhuma pergunta, resposta, IP ou identidade bruta persiste nos eventos analíticos.
- Testes de integração com PostgreSQL real para consumo, limite e rollback; teste de concorrência paralela adicionado, aguardando CI.

### Frontend
- Painel responsivo, acessível e não bloqueante.
- Envio de pergunta, filtros e apresentação dos cards.
- Estados de loading, vazio, cota, timeout e fallback.
- Reuso do catálogo e abertura de detalhe do produto.
- Não persistir conversa e não expor chave.
- Testes de serviço e componente; `npm run build`.

### Definition of Done
- [x] Decisão da identidade/sessão anônima aprovada.
- [ ] Gemini Flash-Lite e condições operacionais validadas.
- [x] Migration e cotas atômicas implementadas (testes de concorrência ainda pendentes).
- [ ] Backend, OpenAPI e testes aprovados.
- [ ] Frontend Angular integrado por contrato.
- [ ] Renan implementou e validou a integração Angular.
- [ ] CI backend/frontend verde.
- [ ] Homologação ponta a ponta realizada sem persistência de conversas.
- [ ] Documentação de API, arquitetura e módulo atualizada.
- [ ] PR aprovada por revisão humana; sem merge automático.

## 8. Fora do escopo desta primeira entrega

- Pedidos, carrinho, checkout, pagamento ou reserva.
- Alteração do catálogo pelo modelo.
- Geração de receitas, promoções, cupons ou preços.
- Diagnóstico alimentar, garantia de ausência de alergênicos ou orientação médica.
- Histórico persistente, memória de conversas ou perfilamento de consumidor.
- Treinamento/fine-tuning de modelo.
- Microserviço Python/FastAPI, agentes autônomos ou tool calling com acesso irrestrito.
- Painel analítico completo; apenas instrumentação agregada para futura visualização.


## 9. Decisão aprovada — sessão anônima assinada e cotas (17/11/2026)

A estratégia de sessão anônima por cookie assinada pelo backend foi aprovada pelo responsável pelo produto.

### Implementação iniciada

- Cookie `BORA_ASSISTANT_SESSION`, HttpOnly, SameSite configurável e Secure configurável por ambiente.
- Valor opaco com 256 bits aleatórios, payload com expiração e assinatura HMAC-SHA256.
- Validação da assinatura em tempo constante e rejeição de cookies expirados/malformados.
- Validade de 30 dias; chamada de bootstrap reutiliza cookie válido em vez de emitir outro.
- Cookie limitado ao caminho `/api/v1/public`, compartilhado pelos endpoints públicos do assistente.
- Endpoint `POST /api/v1/public/assistant/sessions` retorna 204 e define o cookie quando necessário.
- CORS passa a aceitar credenciais somente nas origens explicitamente configuradas em `CORS_ALLOWED_ORIGINS`.
- Identificador bruto não deve ser persistido ou registrado. O serviço expõe somente uma chave HMAC derivada para uso futuro na cota diária.
- Segredo dedicado configurável por `ASSISTANT_SESSION_SECRET`; fallback para `JWT_SECRET` apenas para desenvolvimento/compatibilidade. Produção deve configurar segredo dedicado de alta entropia.
- Testes unitários adicionados para atributos do cookie, emissão, derivação opaca da chave, adulteração, entradas inválidas e isolamento entre sessões.

### Limite de identidade anônima

Sem cadastro ou outro mecanismo de identidade forte, o limite de 20 interações é tecnicamente uma cota por **sessão de navegador**, não uma garantia de uma pessoa física única. O consumidor pode apagar cookies ou trocar de navegador/dispositivo. Não usar fingerprinting para tentar contornar essa limitação. O rate limit por IP será uma camada antiabuso separada e não substitui a cota da sessão.

### Configuração operacional

- Produção: definir `ASSISTANT_SESSION_SECRET` e `ASSISTANT_SESSION_COOKIE_SECURE=true`.
- Se frontend e API estiverem em sites diferentes, configurar `ASSISTANT_SESSION_COOKIE_SAME_SITE=None` junto com `ASSISTANT_SESSION_COOKIE_SECURE=true`; manter CORS com origens explícitas, nunca curingas.
- Para frontend/API same-site, manter `SameSite=Lax`.
- A cota diária ainda precisa ser implementada com contador atômico e chave HMAC; o bootstrap de sessão, sozinho, não implementa as cotas.


### Cotas diárias — primeira implementação

- Migration `V10__create_assistant_daily_quotas.sql` cria tabela de contadores diários com chave única por data, escopo e chave do escopo.
- `AssistantQuotaService` aplica a cota de sessão (20/dia) e restaurante (100/dia) na mesma transação.
- Cada incremento é feito por PostgreSQL `INSERT ... ON CONFLICT ... DO UPDATE ... WHERE consumed < limit RETURNING`, garantindo concorrência atômica sem ultrapassar o limite.
- Se qualquer um dos dois escopos estiver esgotado, a exceção provoca rollback do incremento do outro escopo.
- A data é calculada no backend com `America/Sao_Paulo`; a resposta inclui saldos restantes e próximo reset.
- A tabela armazena apenas a chave HMAC da sessão e o UUID do restaurante, nunca o cookie ou o identificador aleatório original.
- Ainda faltam testes de integração PostgreSQL para concorrência/rollback, endpoint de interação, rate limit por IP e integração com Gemini.
