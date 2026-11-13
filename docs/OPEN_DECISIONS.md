# Registro de decisões pendentes

Este arquivo impede que decisões não tomadas sejam inventadas durante a implementação. Cada item deve ser resolvido pelo responsável pelo produto antes do módulo correspondente começar.

| ID | Tema | Decisão necessária | Bloqueia |
| --- | --- | --- | --- |
| D-01 | Perfil restaurante | **APROVADA** — estrutura de perfil definida; ver decisão registrada abaixo | Concluído |
| D-02 | Menus | **APROVADA** — múltiplos cardápios simultâneos por restaurante; ver decisão registrada abaixo | Concluído |
| D-03 | Publicação | Publicação imediata ou rascunho + publicar; escopo produto/menu | Catálogo |
| D-04 | Ingredientes | Estrutura de ingredientes, observações e alergênicos | Catálogo/IA |
| D-05 | Opções | Variações, adicionais, combos, limites e acréscimos | Catálogo |
| D-06 | Preço | Estratégia de armazenamento monetário, moeda e arredondamento | Catálogo |
| D-07 | Permissões | Matriz completa de ações para OWNER, MANAGER e STAFF | Painel |
| D-08 | Convites | Revogação, reenvio, envio por e-mail e limite de convites | Equipe |
| D-09 | Mídia | Provedor de object storage, limites, formatos e CDN | Imagens/3D |
| D-10 | IA | Provedor/modelo, orçamento, retenção, rate limits e fallback | Assistente |
| D-11 | IA | Se e como persistir histórico e consentimento | Assistente |
| D-12 | Fidelidade | Fórmula de pontos, gatilho de crédito e expiração | Fidelidade |
| D-13 | Fidelidade | Prova de compra, estorno e papéis autorizados | Fidelidade |
| D-14 | Club | Financiamento, valor do ponto, compensação e liquidação | Boraê Club |
| D-15 | Club | Termos, fraude, estorno, expiração e saída de parceiro | Boraê Club |
| D-16 | Descoberta | Taxonomia, geolocalização, raio e ordenação | Descoberta |
| D-17 | Consumidor | Conta única, dados de perfil e login social | Fidelidade/Club |
| D-18 | Monetização | Planos, limites, cobrança, trial e cancelamento | SaaS |
| D-19 | Administração | Papéis globais, auditoria e suporte | Admin |
| D-20 | Pedidos | Se haverá pedido online, integração POS e responsabilidade de pagamento | Pedidos |

## Decisões aprovadas

### D-01 — Perfil do restaurante

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:** o perfil terá os seguintes campos e comportamentos:

- **Nome do restaurante:** obrigatório.
- **Nome fantasia/nome público:** usado na apresentação pública do estabelecimento.
- **Descrição:** opcional.
- **Logo e imagem de capa:** opcionais.
- **Telefone/WhatsApp, Instagram e site:** opcionais.
- **Endereço:** obrigatório para estabelecimentos físicos.
- **Localização no mapa:** suportada; coordenadas associadas ao endereço quando disponíveis.
- **Horário de funcionamento:** configurável por dia da semana.
- **Categoria gastronômica:** obrigatória.
- **Slug público:** gerado automaticamente e editável, desde que permaneça único e válido.
- **Status de publicação do restaurante:** `DRAFT`, `PUBLISHED` ou `SUSPENDED`.

O cadastro não deve exigir o preenchimento de todos os campos opcionais para que o proprietário comece a montar o cardápio. A publicação pública deve respeitar os campos obrigatórios e as validações definidas pelo produto.

**Motivação:** reduzir a fricção do onboarding sem abrir mão das informações essenciais para identificar e localizar estabelecimentos físicos. O perfil público será a identidade digital do restaurante e o ponto de entrada para seu cardápio.

**Impactos:** atualizar PRD, modelo de domínio, fluxo de telas e contrato de API do perfil. Não implica que a funcionalidade já esteja implementada.

### D-02 — Múltiplos cardápios por restaurante

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:** cada restaurante poderá criar e manter múltiplos cardápios, por exemplo Almoço, Jantar e Bebidas. Cada cardápio terá suas próprias categorias e produtos. Mais de um cardápio poderá permanecer publicado simultaneamente.

A página pública do restaurante apresentará os cardápios disponíveis para escolha do consumidor. No MVP, não haverá alternância automática por horário. O QR Code principal direcionará para a página pública do restaurante, e não diretamente para um cardápio específico.

**Motivação:** atender restaurantes com ofertas diferentes por período ou tipo de consumo, sem obrigar o estabelecimento a misturar todos os itens em um único catálogo.

**Impactos:** o relacionamento de Menu com Restaurant será um-para-muitos; atualizar modelo de domínio, PRD, fluxo de telas, API e módulo de catálogo. Esta decisão não implica que a funcionalidade já esteja implementada.

## Como resolver uma decisão

Para cada ID:
1. Registrar a decisão escolhida e alternativas descartadas.
2. Informar motivação e impacto no usuário/negócio.
3. Atualizar PRD, modelo de dados, API e roadmap afetados.
4. Definir data/responsável pela aprovação.
5. Só então liberar implementação.

## Estado atual

- **D-01 — Perfil do restaurante:** aprovada e registrada nesta documentação.
- **D-02 a D-20:** permanecem pendentes; não presumir aprovação nem iniciar implementação de regras de negócio correspondentes sem alinhamento.

Ausência de decisão significa que o desenvolvimento do recurso correspondente deve aguardar alinhamento.
