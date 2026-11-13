# Diretrizes e contratos planejados da API

Este documento é um guia de desenho para módulos futuros, não uma declaração de que os endpoints já existem. Os contratos finais devem ser aprovados e publicados no OpenAPI antes da implementação.

## 1. Convenções gerais

- Base: `/api/v1`.
- JSON em UTF-8.
- Datas em ISO-8601 com timezone.
- UUIDs em formato string.
- Paginação futura: `page`, `size`, `sort`, com limites máximos no servidor.
- Erros padronizados (preferencialmente Problem Details/RFC 9457): type, title, status, detail e campos de validação.
- Não retornar entidades JPA.
- Não confiar em restaurantId, userId, role ou ownerId como prova de autorização.
- Toda operação privada deriva usuário do JWT, valida membership ativo no restaurante solicitado e aplica a matriz D-07 no backend. OWNER tem controle total; MANAGER administra perfil, catálogo, publicação, disponibilidade, equipe e convites, sem alterar papéis, propriedade ou cobrança; STAFF tem acesso operacional ao painel e à disponibilidade de produtos, além de pedidos quando o módulo existir. O mesmo usuário pode ter papéis diferentes por restaurante. Autorização não pode depender apenas de controles visuais no frontend.

## Convites de equipe — regras D-08

- Criar convite vinculado ao e-mail e papel autorizado: OWNER pode convidar MANAGER/STAFF; MANAGER somente STAFF.
- Aceitar convite exige usuário autenticado com e-mail correspondente ao convite.
- Validade de 7 dias a partir da criação ou reenvio.
- Permitir revogação de convites pendentes por OWNER/MANAGER.
- Permitir reenvio de convites pendentes ou expirados, rotacionando o token e renovando a validade por 7 dias.
- Aceitação deve ser atômica e de uso único; convite aceito, revogado ou expirado não pode ser aceito.
- Persistir somente hash do token; nunca registrar o token em logs.
- Validar papel de quem convida e membership ativo no restaurante em toda operação.

O limite numérico de convites simultâneos e a política de entrega de e-mail ainda não foram definidos. Os endpoints e payloads finais serão especificados antes da implementação.

## 2. Recursos já existentes

Consultar [API.md](API.md) para a lista de endpoints confirmados. Não alterar contratos existentes sem compatibilidade ou migração coordenada do frontend.

## 3. Perfil do restaurante — proposta

- `GET /api/v1/restaurants/{restaurantId}`
- `PATCH /api/v1/restaurants/{restaurantId}`
- `GET /api/v1/public/restaurants/{slug}`

O GET público deve retornar somente campos publicados e seguros. O PATCH exige o papel autorizado pela matriz D-07: OWNER ou MANAGER para edição de perfil.

## 4. Catálogo — proposta

- `GET /api/v1/restaurants/{restaurantId}/menus`
- `POST /api/v1/restaurants/{restaurantId}/menus`
- `PATCH /api/v1/restaurants/{restaurantId}/menus/{menuId}`
- `GET/POST /api/v1/restaurants/{restaurantId}/menus/{menuId}/categories`
- `PATCH/DELETE /api/v1/restaurants/{restaurantId}/categories/{categoryId}`
- `GET/POST /api/v1/restaurants/{restaurantId}/products`
- `GET/PATCH/DELETE /api/v1/restaurants/{restaurantId}/products/{productId}`
- `POST /api/v1/restaurants/{restaurantId}/products/{productId}/publish`
- `POST /api/v1/restaurants/{restaurantId}/products/{productId}/availability`
- `GET /api/v1/public/restaurants/{slug}/menu`

Todos os IDs relacionados devem ser validados contra o mesmo restaurante. Operações de ordenação devem receber uma lista explícita de IDs e validar que todos pertencem ao conjunto autorizado.

### Exemplo conceitual de produto (não é contrato fechado)
```json
{
  "name": "Nome do prato",
  "description": "Descrição informativa",
  "priceInCents": 3990,
  "currency": "BRL",
  "addons": [
    { "name": "Bacon extra", "priceDeltaInCents": 500 }
  ],
  "categoryId": "uuid",
  "ingredients": ["ingrediente A", "ingrediente B"],
  "availability": "AVAILABLE"
}
```

D-06 aprovada: o MVP utiliza BRL e representa valores monetários em centavos como inteiros (`priceInCents`, `priceDeltaInCents`). O backend realiza os cálculos; não utilizar `float` ou `double`. A API retorna valores em centavos e a interface os formata para exibição em reais. D-04 e D-05 estão aprovadas: ingredientes são itens livres; adicionais são cadastrados com preço explícito e exibido ao consumidor. Remoções de ingredientes e ponto da carne, quando aplicável, são observações informadas no momento do pedido e apresentadas ao garçom, não opções pré-cadastradas no produto. Não há tamanhos ou combos nesta etapa.

## 5. Mídia — proposta

- `POST /api/v1/restaurants/{restaurantId}/media/upload-intents`
- `POST /api/v1/restaurants/{restaurantId}/media/{mediaId}/complete`
- `DELETE /api/v1/restaurants/{restaurantId}/media/{mediaId}`

D-09 aprovada: usar serviço externo de object storage; PostgreSQL guarda somente metadados e referências. Cloudflare R2 é o provedor definido para imagens e modelos 3D no MVP, utilizando a modalidade gratuita dentro das cotas disponíveis. Formatos aceitos: JPG/JPEG, PNG, WebP, GLB e glTF. Preferir upload direto por URL assinada quando suportado. Backend valida membership/permissão, MIME real (não confiar no Content-Type declarado), tamanho, checksum, status e vínculo da mídia ao restaurante. Não aceitar URL arbitrária do cliente como mídia confiável. Entrega deve permitir CDN, variantes otimizadas de imagem e carregamento sob demanda. Limites numéricos de upload permanecem pendentes; monitorar o consumo para evitar cobranças.

## 6. IA — proposta

D-10 aprovada: Google Gemini é o provedor inicial, usando a Gemini API no plano gratuito inicialmente e sem habilitar faturamento para o projeto sem nova aprovação. A integração ocorre exclusivamente no backend por meio de uma abstração interna, sem acoplar regras de negócio ao SDK. Credenciais ficam em segredos/variáveis de ambiente do servidor. O backend monta o contexto mínimo a partir do catálogo publicado; o modelo não acessa banco de dados diretamente. Aplicar rate limits por sessão/IP e controles de consumo por restaurante. As cotas variam por modelo e podem mudar; não enviar informações pessoais ou sensíveis ao plano gratuito. Modelo/versão e cotas numéricas permanecem pendentes.

D-11 aprovada: não persistir conversas, prompts nem respostas completas. Armazenar somente métricas/eventos agregados por restaurante, sem conteúdo textual ou identificadores pessoais: interações, produtos recomendados/frequência, cliques/taxa de clique, interações sem resultado, latência, erros/timeouts/respostas inválidas/fallback e chamadas/tokens quando disponíveis. O painel deve respeitar permissões e isolamento multi-tenant. Cliques não comprovam compra; métricas de vendas dependem de integração futura com pedidos.

- `POST /api/v1/public/restaurants/{slug}/recommendations`

Entrada conceitual:
```json
{
  "message": "Quero algo leve até 40 reais",
  "filters": {
    "maxPrice": 40
  }
}
```

Saída deve conter referências a produtos existentes:
```json
{
  "recommendations": [
    {
      "productId": "uuid",
      "reason": "Explicação baseada nos dados cadastrados",
      "catalogVersion": "versão ou timestamp consultado"
    }
  ],
  "notice": "Confirme alergênicos e risco de contaminação cruzada com o restaurante."
}
```

O backend deve revalidar IDs, publicação, preço e disponibilidade antes de responder. Não expor chain-of-thought, prompt interno ou credenciais do provedor. Não persistir conteúdo da conversa; métricas analíticas devem ser agregadas e desprovidas do texto das mensagens.


## 7. Fidelidade — proposta

Recursos candidatos:
- `GET /api/v1/restaurants/{restaurantId}/loyalty/program`
- `PUT /api/v1/restaurants/{restaurantId}/loyalty/program`
- `GET /api/v1/me/loyalty-accounts`
- `GET /api/v1/me/loyalty-accounts/{accountId}/ledger`
- `POST /api/v1/restaurants/{restaurantId}/loyalty/transactions`
- `POST /api/v1/me/reward-redemptions`

D-12 aprovada: cada restaurante escolhe acúmulo proporcional ao valor gasto ou pontos fixos por compra. No modelo proporcional, pode configurar taxa ou faixas; dentro da faixa o cálculo é proporcional ao valor efetivamente pago e o resultado é arredondado para baixo. A base exclui taxas de entrega, serviço e encargos e considera o valor após descontos/cupons. Parâmetros são versionados e não reescrevem lançamentos anteriores.

Crédito/resgate deve exigir chave de idempotência e autorização específica. O consumidor não pode criar créditos para si mesmo. O evento que gera pontos precisa ser validado por origem confiável.

**D-13 aprovada:** pontos só ficam disponíveis após confirmação efetiva do pagamento. Quando houver integração confiável, a confirmação poderá ser automática; sem integração, garçom ou gerente autorizado confirma manualmente no fechamento, informando o valor efetivamente pago. A operação registra restaurante, funcionário e identificador único da compra. O consumidor é identificado por telefone verificado via SMS; a conta global exige nome, telefone, e-mail e senha, com carteiras independentes por restaurante. O identificador único da compra é a proteção principal contra duplicidade, complementada por análise de restaurante, consumidor, valor e horário.

**D-15 aprovada — regras operacionais:** cada restaurante configura a validade dos pontos. Créditos preservam versão e parâmetros da regra; alterações só afetam pontos futuros e não retiram recompensas já conquistadas. Consumo por FEFO: vence primeiro, consome primeiro; empate por crédito mais antigo. Expiração automática gera lançamento vinculado ao crédito original. Enviar ao menos um aviso diário nos 14 dias anteriores ao vencimento, por push e central interna, agrupando lotes sem perder restaurante, quantidade e data. Horário inteligente por comportamento/fuso, com fallback seguro. Avisos transacionais não podem ser desligados por preferências de marketing; sem push, manter na central interna e não usar SMS. Estornos geram compensação; se pontos já foram usados, registrar pendência sem saldo negativo e compensar nos próximos acúmulos. Padrões suspeitos geram alertas ao gerente, sem confirmação extra do consumidor nem bloqueio automático no MVP. Encerramento voluntário exige comunicação e ao menos 60 dias para resgate; fechamento repentino exige comunicação assim que possível, sem transferência automática de custos ao Boraê ou resgate em outro restaurante.

## 8. Boraê Club — evolução futura, fora do MVP

D-14 aprovada: o MVP não terá carteira compartilhada, adesão de parceiros, saldo Club, resgate cruzado, compensação ou liquidação entre restaurantes. A API inicial deve contemplar somente os recursos da fidelidade individual de cada restaurante, com isolamento por `restaurantId`.

Se o Boraê Club for retomado futuramente, seus endpoints e contratos dependerão de decisões específicas sobre financiamento, valor do ponto, responsabilidade por recompensas, compensação, liquidação, estornos, fraude e obrigações legais/contábeis. Não criar endpoints Club nem movimentar pontos compartilhados nesta etapa.

## 9. Descoberta — proposta

- `GET /api/v1/public/restaurants` com filtros por categoria, localização, distância, faixa de preço, avaliação, horário, benefícios de fidelidade e modalidade de atendimento, além de paginação.
- `GET /api/v1/public/restaurants/{slug}` para perfil público de unidade.
- `GET /api/v1/public/brands/{slug}` para página principal da marca/rede.
- `GET /api/v1/public/restaurants/{slug}/menu`.
- Resultados ordenados por relevância considerando distância, avaliações, popularidade e completude; personalização por histórico fica para fase futura.
- Páginas de marca e unidade mantêm identidade e avaliações próprias. Somente avaliações vinculadas a compras confirmadas podem ser publicadas.
- Não expor endereço privado, contatos internos, membros ou indicadores do painel. Dados ausentes não devem ser inventados.


## 10. Autorização e erros

Testar pelo menos:
- Sem token: 401.
- Token inválido/expirado: 401.
- Usuário sem vínculo: 403 ou 404 conforme política de não enumeração definida.
- Membership inativo: acesso negado.
- Papel insuficiente: 403.
- Recurso inexistente: 404.
- Payload inválido: 400 com campos identificados.
- Conflito de slug/duplicidade: 409.

A decisão entre 403 e 404 para evitar enumeração deve ser uniforme por recurso e documentada.

## 11. Compatibilidade

- Alterações aditivas preferidas.
- Renomear/remover campo exige estratégia de depreciação.
- Versionar breaking changes.
- Atualizar OpenAPI, frontend, testes e docs no mesmo PR.


## 12. API externa, credenciais e integrações — D-20

A API será pública para integração por terceiros autorizados, mas **não será anônima**. Toda integração externa deve usar chave de API com permissões/escopos explícitos. A autorização deve ser aplicada por recurso e restaurante; chaves devem poder ser revogadas e não podem aparecer em logs ou respostas.

Requisitos:
- Rate limiting por chave e, quando apropriado, por IP/tenant.
- Documentação pública Swagger/OpenAPI, distinguindo endpoints públicos de operações autenticadas.
- Webhooks configuráveis por evento, com autenticação/assinatura e reenvio automático após falha.
- Registro de tentativas, status e política de retry com backoff e limite configurável; evitar duplicação no consumidor por meio de identificador único do evento.
- Ambiente Sandbox isolado de dados e operações reais.
- Não criar integração com PDV/ERP no lançamento. A existência da API e dos webhooks prepara integrações futuras, não autoriza integração POS antecipada.

## 13. Conta do consumidor, assinatura e administração — contratos futuros

### Consumidor
- Conta global por telefone verificado via SMS, com nome, e-mail e senha.
- Login por telefone + senha; recuperação por SMS ou link de e-mail.
- Uma sessão ativa por vez; novo login encerra a anterior.
- Cinco tentativas incorretas consecutivas: bloqueio temporário de 15 minutos.
- Troca de telefone exige SMS no novo número; exclusão sob solicitação com anonimização quando aplicável e retenção legal/auditável.
- Consentimento de fidelidade explícito e separado do consentimento opcional de marketing.

### Assinatura
- Um plano SaaS, preço-base único, sem comissão por transação.
- Trial de 30 dias; ativação paga somente após forma de pagamento cadastrada e confirmação expressa.
- Ciclos mensal, trimestral e anual com descontos progressivos.
- Cancelamento a qualquer momento, acesso até o fim do período pago; troca de ciclo na próxima renovação.
- Falha de cobrança: tentativas e avisos, sete dias de tolerância e suspensão posterior.
- Cartão e Pix; nota fiscal automática a cada cobrança.
- Valor nominal e percentuais de desconto ainda não definidos.

### Administração global
- Administrador principal e equipe de suporte limitada, com papéis separados dos papéis dos restaurantes.
- Suporte acessa dados técnicos, pode suspender temporariamente restaurantes e não acessa dados pessoais, histórico de compras ou dados financeiros.
- Todas as ações administrativas são auditadas.
- Operações de administração global exigem autorização específica e justificativa/confirmacão em exclusões.


## 12. Contratos adicionais aprovados para implementação

### Integrações por API Key
- Cada integração recebe uma chave individual e identificável.
- A autorização é definida por escopos/permissões associados à chave e aplicada no backend.
- Aplicar rate limit por chave; auditar criação, uso relevante, rotação e revogação.
- Permitir revogação imediata. Nunca retornar o segredo em consultas posteriores nem armazená-lo em texto puro.
- Nenhuma chave concede acesso direto ao banco de dados.

### Pagamentos e assinaturas (Asaas)
Prever endpoints internos para criar/consultar/cancelar a assinatura do restaurante e receber eventos do Asaas. A confirmação de estado deve ser baseada em eventos autenticados e reconciliação, com idempotência. Não assumir que redirecionamento do checkout comprova pagamento.

### Webhooks e eventos
Eventos recebidos devem ser autenticados, deduplicados e processados de forma idempotente. Falhas de processamento entram em fila com sete tentativas internas progressivas (imediata, 1 min, 5 min, 15 min, 1 h, 6 h e 24 h), mantendo registro e reprocessamento manual após esgotamento. A retentativa de entrega externa do Asaas continua sob controle do provedor.

### Administração global
A aplicação administrativa separada consumirá endpoints administrativos internos, com autenticação/sessão própria e permissões globais específicas. Não expor endpoints administrativos ao escopo de API Keys de parceiros e não permitir acesso direto ao banco.

Esses contratos são especificações futuras; não representam endpoints já implementados. Consulte [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md).
