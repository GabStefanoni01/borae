# Modelo de domínio e dados

Este documento descreve o modelo conceitual alvo. **Entidades marcadas como propostas não significam que já existam no banco.** O schema físico deve ser criado por migrações Flyway incrementais e revisado antes de cada módulo.

## 1. Convenções

- Identificadores UUID.
- Datas persistidas em UTC; API usa ISO-8601 com timezone.
- Valores monetários no MVP: armazenar em unidades mínimas inteiras (centavos de BRL). Não usar `float` ou `double` para dinheiro; cálculos monetários são feitos no backend.
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

**Decisão de produto D-07 aprovada:** os papéis são atribuídos por vínculo e têm as seguintes responsabilidades:
- `OWNER`: controle total, incluindo perfil, catálogo, publicação, disponibilidade, equipe, papéis, propriedade e cobrança; também pedidos quando esse módulo existir.
- `MANAGER`: administra perfil, catálogo, publicação, disponibilidade, equipe e convites; pode operar pedidos quando esse módulo existir. Não altera papéis, propriedade ou cobrança.
- `STAFF`: acesso operacional ao painel, disponibilidade de produtos e pedidos (visualização/atualização de status quando o módulo existir). Não administra perfil, catálogo, publicação, equipe, propriedade ou cobrança.

Um usuário pode ter papéis diferentes em restaurantes diferentes. A autorização deve ser validada no backend em toda operação privada. Permissões futuras de pedidos não significam inclusão de pedidos no MVP.


### RestaurantInvitation
Convite para integrar equipe.
- id
- restaurant_id
- email
- role (MANAGER | STAFF)
- token_hash
- status (PENDING | ACCEPTED | REVOKED | EXPIRED)
- expires_at (7 dias após criação ou reenvio)
- accepted_at
- revoked_at
- created_by
- created_at
- updated_at

**Decisão D-08 aprovada:** OWNER pode convidar MANAGER ou STAFF; MANAGER pode convidar somente STAFF. O convite é vinculado ao e-mail e só pode ser aceito por uma conta autenticada com o mesmo endereço. Convites pendentes podem ser revogados por OWNER/MANAGER. Convites pendentes ou expirados podem ser reenviados, renovando a validade por 7 dias. Convites aceitos são de uso único. O token original não é persistido: somente seu hash é armazenado, e o token nunca deve aparecer em logs.

O limite numérico de convites simultâneos e a política de entrega de e-mail permanecem em aberto.

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
- price_in_cents (inteiro, centavos)
- currency (BRL no MVP)
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

### ProductAddon
- id
- product_id
- name
- price_delta_in_cents (inteiro, centavos)
- active
- display_order

**Decisão de produto D-05 aprovada:** adicionais são cadastrados pelo restaurante com preço/acréscimo explícito e exibidos ao consumidor no cardápio. Remoção de ingredientes não é uma opção cadastrada no produto: é uma instrução/observação informada pelo cliente no momento do pedido e apresentada ao garçom. Ponto da carne também poderá ser informado como instrução no pedido, quando aplicável. Não há tamanhos ou combos nesta etapa.

### MediaAsset
- id
- restaurant_id
- storage_provider
- storage_key
- media_type
- mime_type
- file_size
- checksum
- status
- created_by
- timestamps

**Decisão de produto D-09 aprovada:** arquivos ficam em serviço externo de object storage; PostgreSQL guarda metadados e referências, nunca conteúdo binário. Cloudflare R2 é o provedor definido para armazenar imagens e modelos 3D no MVP, utilizando a modalidade gratuita dentro das cotas disponíveis. A entrega poderá usar domínio/CDN da Cloudflare conforme configuração. Formatos de imagem: JPG/JPEG, PNG e WebP. Formatos 3D: GLB e glTF. A arquitetura deve suportar CDN, variantes otimizadas e carregamento sob demanda. Backend valida autorização, MIME real, tamanho, checksum e vínculo com o restaurante. Limites numéricos de upload permanecem pendentes e o consumo deve ser monitorado para evitar cobranças.

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
- earning_rule_type (PROPORTIONAL_TO_SPEND | FIXED_PER_PURCHASE)
- earning_rule_parameters (parâmetros versionados da regra escolhida pelo restaurante)
- earning_rule_version
- terms_version
- earning_rule_effective_at / version history
- histórico imutável das versões, com parâmetros e período de vigência
- points_validity_period (configuração definida pelo restaurante)
- program_closure_redemption_deadline / minimum 60 days
- starts_at / ends_at

**Decisões D-12, D-14 e D-15:** no MVP, cada restaurante terá seu próprio programa e será responsável pelas recompensas. A validade é configurada por restaurante. Alterações de regra só se aplicam a pontos acumulados após sua vigência; créditos anteriores preservam a regra aplicável quando foram gerados. O encerramento do programa garante no mínimo 60 dias para resgate. Não há transferência entre restaurantes nem Boraê Club compartilhado no MVP.

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
- expires_at (para créditos sujeitos à validade)
- earning_rule_version (versão que originou o crédito)
- earning_rule_snapshot (parâmetros aplicáveis ao crédito, se necessário para auditoria/reprodução)
- reverses_entry_id / source_reference (quando aplicável)

Saldo deve ser derivado do ledger ou mantido como projeção reconciliável. Não editar saldo diretamente sem lançamento correspondente. Estornos devem gerar lançamentos compensatórios e preservar a referência ao crédito original; nunca apagar o histórico. Se os pontos já tiverem sido utilizados, registrar uma pendência de análise sem criar saldo negativo.

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
- earned_rule_version (snapshot das condições aplicáveis, quando necessário)
- idempotency_key
- created_at
- redeemed_at

## 6. Boraê Club — conceito futuro, fora do MVP

O Boraê Club compartilhado entre restaurantes parceiros não será implementado no MVP, conforme D-14. As entidades de carteira compartilhada, adesão de parceiros, ledger Club, resgate cruzado e liquidação ficam fora do schema inicial. Caso o produto seja retomado no futuro, seu modelo financeiro, responsabilidades, contratos e regras de operação deverão ser aprovados antes da modelagem e implementação.

A fidelidade individual de cada restaurante permanece isolada: contas, pontos, recompensas e lançamentos pertencem ao programa daquele estabelecimento. Não existe transferência ou conversão entre restaurantes.

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
- Product 1:N ProductIngredient e Product 1:N ProductAddon.
- Restaurant 1:N MediaAsset.
- Product 0..1:N ProductModel3D conforme histórico/versionamento decidido.
- Restaurant 1:N RestaurantLoyaltyProgram.
- LoyaltyProgram 1:N LoyaltyAccount e Reward.
- LoyaltyAccount 1:N LoyaltyLedgerEntry e RewardRedemption.
- Estruturas de ClubWallet, ClubLedgerEntry e ClubPartner somente poderão ser adicionadas em uma fase futura, após nova decisão de produto e financeira.

## 9. Regras de integridade obrigatórias

1. Não aceitar referência a categoria/produto/mídia de outro restaurante.
2. Slugs públicos devem ser únicos.
3. Preços de produtos e adicionais são armazenados como inteiros em centavos, não podem ser negativos e usam BRL no MVP. Cálculos monetários são feitos no backend, sem `float` ou `double`.
4. Produto indisponível ou não publicado não pode ser recomendado como opção disponível.
5. Convites expiram e são de uso único.
6. Vínculos inativos não concedem acesso.
7. Toda movimentação de pontos deve possuir referência, idempotência e trilha auditável.
8. Resgate não pode gerar saldo negativo; validar atomicamente.
9. Não excluir lançamentos de ledger; lançar reversão.
10. Alterações de regra não podem alterar retroativamente os pontos já acumulados nem retirar recompensas já conquistadas; cada crédito preserva a versão e os parâmetros aplicáveis.
11. Estorno de compra com pontos já resgatados gera pendência para análise, sem saldo negativo.
12. Dados pessoais e preferências precisam de política de acesso e retenção.

## 10. Decisões que precisam preceder o schema físico

- Campos finais e estados do perfil do restaurante.
- Um ou vários menus por restaurante.
- Regras de variações e adicionais.
- Regras de variações e adicionais.
- Demais decisões de modelagem ainda pendentes.
- Identidade de consumidor e login social, se houver.
- Política de retenção de sessões de IA.
- Gatilho de crédito, expiração e estorno dos programas de fidelidade; a estrutura e as fórmulas-base de acúmulo foram parcialmente aprovadas em D-12.
- Modelo financeiro, contratos e liquidação do Boraê Club, caso o programa compartilhado seja retomado no futuro.
- Política de reviews e moderação.
