# Modelo de domínio e dados

Este documento descreve o modelo conceitual alvo. **Entidades marcadas como propostas não significam que já existam no banco.** O schema físico deve ser criado por migrações Flyway incrementais e revisado antes de cada módulo.

## 1. Convenções

- Identificadores UUID.
- Datas persistidas em UTC; API usa ISO-8601 com timezone.
- Valores monetários: armazenar em decimal com precisão definida ou em unidades mínimas inteiras; escolher uma única estratégia e documentá-la antes do módulo de catálogo. Não usar ponto flutuante binário para dinheiro.
- Valores de enum persistidos com nomes estáveis.
- Toda tabela pertencente a restaurante deve carregar referência explícita ao restaurante ou derivá-la por relação segura.
- Constraints de banco devem reforçar invariantes críticas.
- Não apagar registros financeiros/ledger; corrigir por lançamento compensatório.

## 2. Entidades existentes (confirmadas pela implementação documentada)

### UserAccount
Representa uma conta autenticável.
- id
- fullName
- email (único, normalizado)
- passwordHash
- active
- timestamps conforme entidade atual

### Restaurant
Representa estabelecimento.
- id
- name
- slug
- description (conforme implementação)
- estado e timestamps conforme código atual

### RestaurantMembership
Vínculo entre usuário e restaurante.
- id
- user_id
- restaurant_id
- role: OWNER | MANAGER | STAFF
- active

Regra: um usuário pode ter vínculos com vários restaurantes. A política de unicidade do vínculo deve impedir duplicação lógica do mesmo usuário no mesmo restaurante; reativação deve reaproveitar o vínculo conforme implementação atual.

### RestaurantInvitation
Convite para integrar equipe.
- id
- restaurant_id
- email
- role
- token_hash
- expires_at
- accepted_at
- created_by
- created_at

O token original é exibido na criação e o hash é persistido. Não registrar o token em logs.

## 3. Entidades propostas — perfil e catálogo

### RestaurantProfile (ou extensão de Restaurant)
Campos candidatos:
- public_name
- description
- slug
- logo_media_id
- cover_media_id
- phone
- website_url
- address fields (street, number, complement, neighborhood, city, state, postal_code, country)
- latitude/longitude opcionais
- timezone
- publication_status
- created_at / updated_at

**Decisão de produto D-01 aprovada:** o perfil inclui nome do restaurante, nome público/fantasia, descrição opcional, logo e capa opcionais, telefone/WhatsApp, Instagram e site opcionais, endereço obrigatório para estabelecimentos físicos, localização no mapa, horário por dia da semana, categoria gastronômica obrigatória e slug gerado automaticamente/editável com unicidade.

Status de publicação do restaurante: `DRAFT | PUBLISHED | SUSPENDED`. Campos opcionais não bloqueiam o início do cadastro nem a montagem do cardápio. A publicação pública deve validar os campos obrigatórios.

**Decisão técnica ainda necessária:** escolher entre estender `Restaurant` ou criar tabela `RestaurantProfile`. A decisão de produto não determina essa estrutura física; evitar duplicação e preservar compatibilidade com os dados existentes.

### Menu
- id
- restaurant_id
- name
- description opcional
- status: DRAFT | PUBLISHED | ARCHIVED
- published_at
- timestamps

**Decisão de produto D-02 aprovada:** cada restaurante pode ter múltiplos cardápios simultaneamente (ex.: Almoço, Jantar e Bebidas). Cada cardápio possui categorias e produtos próprios, e mais de um pode estar publicado ao mesmo tempo. A página pública do restaurante lista os cardápios para escolha do consumidor. No MVP, não haverá alternância automática por horário; o QR Code principal abre a página do restaurante.

Relacionamento: `Restaurant 1:N Menu`. Esta decisão de produto está aprovada; detalhes de schema físico e contratos devem seguir as convenções técnicas do projeto.

### MenuCategory
- id
- menu_id
- name
- description opcional
- display_order
- active
- timestamps

### Product
- id
- restaurant_id
- category_id
- name
- description
- price
- currency
- image_media_id opcional
- ingredientes associados ao produto por uma coleção de itens individuais, com nome e ordem de exibição.
- lifecycle_status (ACTIVE | ARCHIVED; não representa publicação pública)
- availability_status (AVAILABLE | UNAVAILABLE)
- display_order
- timestamps

**Decisão de produto D-03 aprovada:** o estado de publicação pertence ao `Menu` (`DRAFT | PUBLISHED | ARCHIVED`). Não há publicação individual de produto no MVP. Produtos elegíveis e categorias ativas ficam públicos quando o cardápio é publicado. Em cardápio publicado, alterações salvas entram no ar imediatamente. Disponibilidade operacional permanece separada da publicação.

Produto deve pertencer a uma categoria do mesmo restaurante. Validar essa consistência no backend e, se viável, no banco.

### ProductIngredient
- id
- product_id
- name
- display_order

Cada ingrediente é cadastrado livremente pelo restaurante e pertence a um único produto. Exemplo: pão, carne, queijo, cebola e bacon em um hambúrguer. O restaurante pode adicionar, editar, remover e ordenar os ingredientes. Não há catálogo padronizado de alergênicos nem declaração específica de contaminação cruzada neste escopo.

### ProductOptionGroup
- id
- product_id
- name
- min_selections
- max_selections
- required
- display_order

### ProductOption
- id
- option_group_id
- name
- price_delta
- active
- display_order

**Decisão pendente:** definir se opções representam tamanhos, escolhas mutuamente exclusivas, adicionais ou todos esses casos. O modelo final deve expressar regras de seleção sem ambiguidades.

### MediaAsset
- id
- restaurant_id
- storage_key
- media_type
- mime_type
- file_size
- checksum
- status
- created_by
- timestamps

Arquivos ficam em object storage. Banco guarda metadados e referência, não conteúdo binário.

## 4. Entidades propostas — IA e experiência

### ProductModel3D
- id
- restaurant_id
- product_id
- media_asset_id
- scale/calibration metadata
- status
- reviewed_at

### RecommendationSession
- id
- restaurant_id
- anonymous/session reference ou consumer_id opcional
- preferences/context (minimizado)
- created_at

### RecommendationItem
- session_id
- product_id
- explanation
- rank
- snapshot de contexto mínimo necessário

Não persistir conversas ou dados sensíveis sem política de privacidade, finalidade e retenção definidas.

## 5. Entidades propostas — consumidor e fidelidade

### ConsumerAccount
Pode ser a própria UserAccount com perfis/roles adicionais; evitar criar identidade duplicada. Decidir antes de implementar.

### RestaurantLoyaltyProgram
- id
- restaurant_id
- status
- earning_rule_version
- terms_version
- starts_at / ends_at

### LoyaltyAccount
- id
- program_id
- consumer_id
- status

### LoyaltyLedgerEntry
- id
- loyalty_account_id
- entry_type (CREDIT | DEBIT | REVERSAL | EXPIRATION | ADJUSTMENT)
- points_delta
- source_reference
- idempotency_key
- created_at
- created_by/system_actor

Saldo deve ser derivado do ledger ou mantido como projeção reconciliável. Não editar saldo diretamente sem lançamento correspondente.

### LoyaltyReward
- id
- program_id
- name
- description
- points_cost
- active
- terms

### RewardRedemption
- id
- reward_id
- loyalty_account_id
- points_spent
- status
- idempotency_key
- created_at
- redeemed_at

## 6. Entidades propostas — Boraê Club

### ClubMembership
- id
- consumer_id
- status
- terms_version
- joined_at

### ClubPartner
- id
- restaurant_id
- status
- agreement_reference
- joined_at
- left_at

### ClubWallet
- id
- consumer_id
- currency/points unit
- status

### ClubLedgerEntry
- id
- wallet_id
- entry_type
- points_delta
- partner_restaurant_id
- source_reference
- idempotency_key
- created_at

### ClubRedemption / PartnerSettlement
Registrar resgate e obrigação do parceiro, custo, status, estorno e liquidação. O modelo contábil/financeiro depende de definição comercial e jurídica; não implementar a partir apenas destes nomes.

**Invariante:** ledger do Club e ledger de fidelidade individual são separados e nunca transferidos implicitamente.

## 7. Entidades propostas — descoberta e monetização

- RestaurantLocation: localização pública e coordenadas, se separada do perfil.
- RestaurantCategory / CuisineType: taxonomia de busca.
- Favorite: relação consumidor-restaurante.
- Review: somente após definir moderação, política de abuso e critérios de publicação.
- SubscriptionPlan, RestaurantSubscription, BillingEvent: plano e cobrança B2B.
- PlatformAdminRole / AuditEvent: administração global e trilha de auditoria.

## 8. Relacionamentos conceituais

- UserAccount 1:N RestaurantMembership N:1 Restaurant.
- Restaurant 1:N RestaurantInvitation.
- Restaurant 1:N Menu.
- Menu 1:N MenuCategory.
- MenuCategory 1:N Product.
- Product 1:N ProductIngredient e Product 1:N ProductOptionGroup 1:N ProductOption.
- Restaurant 1:N MediaAsset.
- Product 0..1:N ProductModel3D conforme histórico/versionamento decidido.
- Restaurant 1:N RestaurantLoyaltyProgram.
- LoyaltyProgram 1:N LoyaltyAccount e Reward.
- LoyaltyAccount 1:N LoyaltyLedgerEntry e RewardRedemption.
- Consumer 1:1 ClubWallet; ClubWallet 1:N ClubLedgerEntry.
- Restaurant 1:0..1 ClubPartner (ou histórico N:1 se reentrada for permitida).

## 9. Regras de integridade obrigatórias

1. Não aceitar referência a categoria/produto/mídia de outro restaurante.
2. Slugs públicos devem ser únicos.
3. Preços e deltas não podem ser negativos, salvo regra explícita aprovada.
4. Produto indisponível ou não publicado não pode ser recomendado como opção disponível.
5. Convites expiram e são de uso único.
6. Vínculos inativos não concedem acesso.
7. Toda movimentação de pontos deve possuir referência, idempotência e trilha auditável.
8. Resgate não pode gerar saldo negativo; validar atomicamente.
9. Não excluir lançamentos de ledger; lançar reversão.
10. Dados pessoais e preferências precisam de política de acesso e retenção.

## 10. Decisões que precisam preceder o schema físico

- Campos finais e estados do perfil do restaurante.
- Um ou vários menus por restaurante.
- Regras de variações e adicionais.
- Regras de variações e adicionais.
- Estratégia de moeda e precisão.
- Identidade de consumidor e login social, se houver.
- Política de retenção de sessões de IA.
- Fórmulas de pontos e expiração.
- Regras contábeis, contratos e liquidação do Club.
- Política de reviews e moderação.
