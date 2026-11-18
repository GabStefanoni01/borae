# Plano de implementação — Assistente gastronômico

**Data:** 17/11/2026  
**Status:** contrato técnico detalhado; implementação funcional depende da decisão de identidade de sessão descrita na seção 4.  
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
- Modelo: Gemini Flash-Lite, com identificador estável exato configurado após validação da versão disponível no momento da integração.
- Integração exclusivamente no backend Java/Spring Boot.
- Chave em variável/secret do backend (proposta: `GEMINI_API_KEY`); nunca no Angular, no bundle, no Git ou em logs.
- Limite por interação: até **2.000 tokens de entrada** e **500 tokens de saída**.
- Cotas simultâneas: até **20 interações por consumidor/dia** e **100 por restaurante/dia**.
- Renovação diária à meia-noite no fuso `America/Sao_Paulo`.
- Atingir qualquer uma das cotas bloqueia novas interações de IA no escopo correspondente até a renovação. O cardápio permanece utilizável.
- Registrar tokens (quando fornecidos), latência, erros e custo estimado por restaurante.
- Antes de produção, validar versão estável, faturamento, limites, termos de uso e tratamento de dados do Google.

## 4. Bloqueio técnico que precisa de decisão antes da cota por consumidor

O PRD permite consulta anônima ao cardápio e não há hoje uma identidade/sessão de consumidor implementada no backend. A conta existente (`UserAccount`) atende autenticação do painel/restaurante e não deve ser reaproveitada como identidade de consumidor.

A regra de 20 interações por consumidor/dia está aprovada, mas falta definir como identificar um consumidor anônimo de forma confiável. Não adotar silenciosamente IP como identidade (NAT pode agrupar várias pessoas) nem um UUID livre do navegador (pode ser trocado para contornar a cota).

**Alternativa técnica recomendada para aprovação:** endpoint de bootstrap emite cookie de sessão anônima assinado pelo backend, com identificador aleatório opaco, HttpOnly, Secure em produção, SameSite apropriado e validade curta renovável. A cota usa HMAC/hash do identificador no escopo diário, sem persistir o identificador bruto. Aplicar também rate limit de abuso por IP, sem usar IP como identidade de consumidor. O contador técnico de cota deve expirar após a janela diária; ele não é histórico analítico nem histórico de conversa.

Até a aprovação dessa estratégia, não declarar a cota individual implementada nem liberar o endpoint de IA em produção. A cota de 100 por restaurante também deve ser aplicada atomicamente para evitar ultrapassagem por chamadas concorrentes.

## 5. Backend — desenho de implementação

Manter o monólito modular existente. Criar o domínio `com.borae.assistant`; não criar microserviço Python/FastAPI nesta entrega.

### Componentes

- `PublicAssistantController`: endpoints públicos e DTOs; controller fino.
- `GastronomicAssistantService`: orquestra o caso de uso, valida o restaurante, carrega catálogo elegível, aplica cotas e revalida a saída.
- `AssistantCatalogContextService`: monta contexto mínimo do restaurante e dos produtos publicados/disponíveis.
- `AiProvider`: interface interna independente do SDK/provedor.
- `GeminiFlashLiteProvider`: implementação HTTP do provedor, com timeout, limite de tokens e parsing de saída estruturada.
- `AssistantQuotaService`: consumo diário atômico por restaurante e sessão anônima, com janela `America/Sao_Paulo`.
- `AssistantMetricsService`: grava somente métricas/eventos agregados permitidos por D-11.
- `AssistantRecommendationValidator`: valida IDs, remove duplicidades e recarrega dados autoritativos do catálogo.
- `AssistantExceptionHandler`: respostas estáveis para limite atingido, timeout e indisponibilidade.

### Endpoints propostos

| Método | Endpoint | Acesso | Responsabilidade |
|---|---|---|---|
| POST | `/api/v1/public/assistant/sessions` | Público | Emitir/renovar cookie de sessão anônima para cotas, após aprovação da seção 4. |
| POST | `/api/v1/public/restaurants/{slug}/assistant/interactions` | Público + sessão | Enviar intenção/filtros e receber resposta e recomendações verificadas. |
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

- Timeout de conexão e resposta configuráveis; retry no máximo uma vez apenas para falhas transitórias seguras, sem duplicar contadores.
- Contar a interação conforme a regra aprovada e definir claramente se falha técnica consome cota; proposta: falha anterior à chamada do provedor não consome; chamada efetivamente enviada consome para impedir abuso/custo.
- Rate limit adicional por IP para abuso, sem substituir as cotas de negócio.
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

Renan (`@renancristianobn-del`) revisa o contrato e lidera a implementação Angular após o backend publicar DTOs/OpenAPI estáveis. O frontend não deve antecipar um contrato diferente do backend. A integração final valida CORS, cookie de sessão com credenciais, SameSite/Secure e ambiente de homologação.

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
- Testes de integração com PostgreSQL real para constraints e concorrência.

### Frontend
- Painel responsivo, acessível e não bloqueante.
- Envio de pergunta, filtros e apresentação dos cards.
- Estados de loading, vazio, cota, timeout e fallback.
- Reuso do catálogo e abertura de detalhe do produto.
- Não persistir conversa e não expor chave.
- Testes de serviço e componente; `npm run build`.

### Definition of Done
- [ ] Decisão da identidade/sessão anônima aprovada.
- [ ] Gemini Flash-Lite e condições operacionais validadas.
- [ ] Migration e cotas atômicas implementadas.
- [ ] Backend, OpenAPI e testes aprovados.
- [ ] Frontend Angular integrado por contrato.
- [ ] Renan revisou e validou a integração.
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
