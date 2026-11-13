# Registro de decisões pendentes

Este arquivo impede que decisões não tomadas sejam inventadas durante a implementação. Cada item deve ser resolvido pelo responsável pelo produto antes do módulo correspondente começar.

| ID | Tema | Decisão necessária | Bloqueia |
| --- | --- | --- | --- |
| D-01 | Perfil restaurante | **APROVADA** — estrutura de perfil definida; ver decisão registrada abaixo | Concluído |
| D-02 | Menus | Um menu ou múltiplos menus por restaurante (ex.: almoço/jantar) | Catálogo |
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

## Como resolver uma decisão

Para cada ID:
1. Registrar a decisão escolhida e alternativas descartadas.
2. Informar motivação e impacto no usuário/negócio.
3. Atualizar PRD, modelo de dados, API e roadmap afetados.
4. Definir data/responsável pela aprovação.
5. Só então liberar implementação.

## Estado atual

Nenhum item desta tabela deve ser considerado aprovado implicitamente. Ausência de decisão significa que o desenvolvimento do recurso correspondente deve aguardar alinhamento.
