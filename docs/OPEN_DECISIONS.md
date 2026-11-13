# Registro de decisões pendentes

Este arquivo impede que decisões não tomadas sejam inventadas durante a implementação. Cada item deve ser resolvido pelo responsável pelo produto antes do módulo correspondente começar.

| ID | Tema | Decisão necessária | Bloqueia |
| --- | --- | --- | --- |
| D-01 | Perfil restaurante | **APROVADA** — estrutura de perfil definida; ver decisão registrada abaixo | Concluído |
| D-02 | Menus | **APROVADA** — múltiplos cardápios simultâneos por restaurante; ver decisão registrada abaixo | Concluído |
| D-03 | Publicação | **APROVADA** — publicação por cardápio, alterações publicadas entram no ar ao salvar; ver decisão registrada abaixo | Concluído |
| D-04 | Ingredientes | **APROVADA** — lista livre de ingredientes associados ao produto; ver decisão registrada abaixo | Concluído |
| D-05 | Opções | **APROVADA** — adicionais pagos e remoção de ingredientes; sem tamanhos ou ponto da carne no cadastro | Concluído |
| D-06 | Preço | **APROVADA** — BRL, valores inteiros em centavos e cálculos no backend | Concluído |
| D-07 | Permissões | **APROVADA** — matriz de permissões para OWNER, MANAGER e STAFF | Concluído |
| D-08 | Convites | **APROVADA** — convite por e-mail, expiração de 7 dias, revogação e reenvio | Concluído |
| D-09 | Mídia | **APROVADA** — object storage, Cloudflare R2 como provedor definido, formatos e CDN | Concluído |
| D-10 | IA | **APROVADA** — Gemini API no plano gratuito inicialmente; integração desacoplada e controles de consumo | Concluído |
| D-11 | IA | **APROVADA** — não persistir conversas; armazenar somente métricas agregadas por restaurante | Concluído |
| D-12 | Fidelidade/Club | **PARCIALMENTE APROVADA** — programas separados; acúmulo local configurável e Club a 1 ponto/R$ 1. Gatilho e expiração ainda pendentes | Fidelidade/Club |
| D-13 | Fidelidade | **PARCIALMENTE APROVADA** — confirmação manual após compra concluída por garçom ou gerente; estornos e controles restantes pendentes | Fidelidade |
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

### D-03 — Publicação por cardápio

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:** a publicação será controlada no nível do cardápio, não individualmente por produto nem globalmente por restaurante.

- Todo cardápio novo começa como `DRAFT`.
- O restaurante organiza categorias e produtos e pode revisar uma prévia antes de publicar.
- A ação de publicar valida a consistência mínima e muda o cardápio para `PUBLISHED`.
- Ao publicar um cardápio, seus produtos elegíveis e categorias ativas ficam visíveis na página pública.
- Não haverá botão de publicar/despublicar por produto no MVP.
- Em cardápios já publicados, alterações salvas entram em vigor imediatamente, sem etapa adicional de aprovação/publicação.
- Um produto pode permanecer visível e ser marcado como indisponível; disponibilidade operacional não é estado de publicação.
- O cardápio pode ser despublicado ou arquivado sem apagar seus dados.

**Motivação:** manter a gestão simples para o restaurante, permitir revisão do conjunto antes da primeira publicação e evitar um fluxo de publicação excessivamente granular. Atualizações operacionais precisam refletir rapidamente no cardápio público.

**Impactos:** estado de publicação pertence a `Menu`; produtos não precisam de estado próprio de publicação no MVP. Atualizar PRD, modelo de domínio, API, fluxo de telas e módulo de catálogo. Esta decisão não implica que a funcionalidade já esteja implementada.

### D-04 — Ingredientes dos produtos

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:** cada produto poderá ter uma lista de ingredientes cadastrados livremente pelo restaurante. Cada ingrediente será um item individual associado ao produto e poderá ser incluído, editado, removido e ordenado para exibição.

Exemplo para um hambúrguer: pão, carne, queijo, cebola, bacon, alface e tomate.

Não haverá, neste escopo, catálogo padronizado de alergênicos, declaração específica de contaminação cruzada ou campos técnicos de segurança alimentar. O objetivo é apresentar de forma simples a composição informada pelo estabelecimento.

**Motivação:** facilitar o cadastro e ajudar o consumidor a entender o que compõe cada prato, sem impor ao restaurante um formulário técnico ou complexo.

**Impactos:** representar ingredientes como itens associados ao produto; atualizar modelo de domínio, PRD, módulo de catálogo, fluxo de telas e contexto do assistente gastronômico. Esta decisão não implica que a funcionalidade já esteja implementada.

### D-05 — Adicionais e remoção de ingredientes

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:** o cadastro do produto terá adicionais opcionais, como bacon, cheddar ou ovo. Cada adicional terá seu preço/acréscimo definido pelo restaurante e exibido claramente ao consumidor no cardápio, antes de fazer o pedido.

A remoção de ingredientes não será cadastrada como opção comercial nem como configuração individual no produto. Quando fizer o pedido, o cliente poderá solicitar observações como “sem cebola” ou “sem tomate”; essa instrução deverá ser apresentada de forma clara para o garçom responsável pelo atendimento/preparo.

Não haverá cadastro de ponto da carne nem de tamanhos/variações nesta etapa. O ponto da carne, quando aplicável, poderá ser informado como instrução no momento do pedido. Combos e regras avançadas de seleção ficam fora desta decisão.

As escolhas são destinadas à futura personalização do pedido; a aprovação desta estrutura não significa que pedidos online façam parte do MVP atual.

**Motivação:** oferecer personalização útil sem exigir que o restaurante configure combinações e variações desnecessárias.

**Impactos:** modelar adicionais com seus preços e prever observações/instruções de pedido para comunicação ao garçom; atualizar PRD, modelo de domínio, API, fluxo de telas e módulo de catálogo. Esta decisão não implica que a funcionalidade já esteja implementada.

### D-12 — Estrutura dos programas e regra de acúmulo

**Status:** Parcialmente aprovada pelo responsável pelo produto em 13/11/2026.

**Decisões aprovadas:**
- Existirão dois programas independentes: a fidelidade própria de cada restaurante e o Boraê Club, compartilhado entre restaurantes parceiros.
- Os saldos, regras, recompensas e ledgers dos dois programas serão separados. O consumidor poderá participar de ambos; pontos de um programa não serão convertidos ou transferidos automaticamente para o outro.
- Na fidelidade individual, cada restaurante poderá escolher entre dois modelos de acúmulo: pontos proporcionais ao valor gasto ou quantidade fixa de pontos por compra.
- No Boraê Club, a regra será uniforme para todos os parceiros: **1 ponto por R$ 1,00 gasto**. Exemplo: uma compra elegível de R$ 50,00 gera 50 pontos Club.
- O Boraê Club será opcional para restaurantes parceiros e terá uma carteira/saldo compartilhado do consumidor, distinto dos saldos locais.

**Experiência pretendida:** durante o atendimento/fechamento da compra, o garçom poderá perguntar se o consumidor já participa do Boraê Club ou se deseja conhecer/aderir. A identificação e adesão devem ocorrer com participação e consentimento explícitos do consumidor; não haverá inscrição automática. O método de identificação (telefone, QR Code ou outro) e o fluxo detalhado ainda serão definidos.

**Ainda pendente nesta decisão:**
- Momento exato de concessão dos pontos e mecanismo confiável de confirmação da compra.
- Tratamento de cancelamentos, devoluções e estornos.
- Expiração dos pontos e ordem de consumo.
- Configuração detalhada dos parâmetros do programa local (incluindo valor fixo por compra ou taxa por real).
- Papéis autorizados e limites para registro/ajuste manual.

**Motivação:** permitir que cada restaurante mantenha relacionamento próprio com seus clientes e, ao mesmo tempo, criar um incentivo de circulação entre estabelecimentos da rede Boraê, com uma regra de acúmulo Club simples e consistente.

**Impactos:** manter entidades, saldos e ledgers separados; atualizar PRD, modelo de domínio, módulo de fidelidade e contratos de API. A aprovação não significa que pedidos, pagamentos, integração POS, crédito automático ou liquidação financeira estejam implementados ou definidos.

### D-13 — Confirmação da compra e autorização para conceder pontos

**Status:** Parcialmente aprovada pelo responsável pelo produto em 13/11/2026.

**Decisões aprovadas:**
- Os pontos só poderão ser concedidos depois que a compra for efetivamente concluída e confirmada; abrir o cardápio, fazer um pedido ou iniciar o atendimento não gera pontos.
- No fluxo inicial, a confirmação será manual por meio de uma ação no sistema durante o fechamento da conta, sem depender de integração com POS ou processamento de pagamento pelo Boraê.
- Garçons e gerentes poderão confirmar a compra e registrar o valor efetivamente pago.
- A confirmação deverá identificar o funcionário responsável e o restaurante, mantendo registro auditável.
- O consumidor não poderá criar créditos de pontos para si mesmo.

**Ainda pendente:**
- **Identificação aprovada:** utilizar o número de telefone cadastrado pelo consumidor para localizar sua conta e associar a compra. O fluxo de verificação/validação do telefone ainda será definido.
- Definir o tratamento de cancelamentos, devoluções, estornos e correções após o crédito.
- Definir limites e controles antifraude para confirmações e ajustes manuais.
- Definir os detalhes da referência única/idempotência para impedir crédito duplicado.
- Definir o momento exato em que o crédito ficará disponível após a confirmação.

**Motivação:** vincular a pontuação a uma compra concluída, evitando créditos por simples pedidos ou consultas ao cardápio, sem exigir integração externa no primeiro fluxo.

**Impactos:** prever confirmação manual autorizada e auditável no fluxo de fechamento. Esta decisão não aprova pedidos online, pagamentos pelo Boraê nem integração POS.

## Como resolver uma decisão

Para cada ID:
1. Registrar a decisão escolhida e alternativas descartadas.
2. Informar motivação e impacto no usuário/negócio.
3. Atualizar PRD, modelo de dados, API e roadmap afetados.
4. Definir data/responsável pela aprovação.
5. Só então liberar implementação.

## Estado atual

- **D-01 — Perfil do restaurante:** aprovada e registrada nesta documentação.
- **D-02 — Múltiplos cardápios:** aprovada e registrada nesta documentação.
- **D-03 — Publicação por cardápio:** aprovada e registrada nesta documentação.
### D-06 — Preços e valores monetários

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:**
- O Boraê utilizará somente real brasileiro (BRL) no MVP.
- Valores terão precisão de duas casas decimais (centavos).
- Preços de produtos e adicionais serão armazenados em centavos como números inteiros.
- O backend será responsável pelos cálculos monetários; não utilizar `float` ou `double`.
- Preços de produtos e adicionais não poderão ser negativos.
- O preço dos adicionais será somado ao preço do produto conforme a seleção feita no pedido.

**Exemplo:** produto de R$ 25,00 + bacon extra de R$ 5,00 + cheddar extra de R$ 3,00 = R$ 33,00.

**Impactos:** padronizar o modelo de domínio e os contratos da API com valores em centavos (por exemplo, `priceInCents` e `priceDeltaInCents`). Exibição ao consumidor deve formatar os valores como BRL. Esta decisão não implica que pedidos ou pagamentos já façam parte do MVP.

### D-07 — Papéis e permissões da equipe

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:** o Boraê terá três papéis por vínculo com restaurante:

- **OWNER:** controle total do restaurante. Pode visualizar o painel; editar o perfil; criar e administrar cardápios, categorias, produtos, preços e publicação; alterar disponibilidade; visualizar e atualizar pedidos quando o módulo existir; gerenciar equipe, convites e papéis; excluir ou transferir a propriedade do restaurante; gerenciar assinatura e cobrança.
- **MANAGER:** pode visualizar o painel; editar o perfil; criar e administrar cardápios, categorias, produtos e preços; publicar e arquivar cardápios; alterar disponibilidade; visualizar e atualizar pedidos quando o módulo existir; gerenciar equipe e convites. Não pode alterar papéis, excluir ou transferir o restaurante, nem gerenciar assinatura e cobrança.
- **STAFF:** acesso operacional. Pode visualizar o painel; alterar disponibilidade dos produtos; visualizar pedidos e atualizar seus status quando o módulo existir. Não pode editar perfil, cardápios, categorias, produtos ou preços, publicar cardápios, gerenciar equipe ou convites, alterar papéis, excluir/transferir o restaurante ou gerenciar cobrança.

As permissões são vinculadas ao membership de cada usuário em cada restaurante. Um mesmo usuário pode ter papéis diferentes em restaurantes diferentes. O backend deve validar autorização em toda operação privada; ocultar ações no frontend não substitui essa validação.

**Escopo futuro:** permissões de pedidos ficam definidas desde já, mas não significam que pedidos estejam incluídos no MVP atual.

**Impactos:** atualizar modelo de domínio, PRD e contratos/regras de autorização da API. Esta decisão não implica que todas as funcionalidades estejam implementadas.

### D-08 — Convites para equipe

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:**
- OWNER pode convidar usuários como MANAGER ou STAFF.
- MANAGER pode convidar usuários somente como STAFF.
- Cada convite é vinculado a um endereço de e-mail específico.
- O convidado precisa entrar ou criar uma conta usando o mesmo e-mail do convite.
- O convite expira após 7 dias.
- OWNER e MANAGER podem revogar convites pendentes.
- Convites pendentes podem ser reenviados; o reenvio renova a validade por mais 7 dias.
- Convites expirados podem ser reenviados, também com nova validade de 7 dias.
- Convites aceitos são de uso único e não podem ser aceitos novamente.

O limite numérico de convites simultâneos por restaurante não foi definido nesta decisão e permanece em aberto. A política de envio/entrega de e-mail e proteção contra abuso também deverá ser detalhada antes da implementação.

**Impactos:** atualizar modelo de domínio, PRD e API para suportar status, expiração, revogação e reenvio. O token deve ser armazenado somente como hash, nunca registrado em logs. Esta decisão não implica que o fluxo esteja implementado.

### D-09 — Armazenamento de imagens e modelos 3D

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:**
- Arquivos de mídia serão armazenados em serviço externo de object storage; PostgreSQL guarda somente metadados e referências, nunca o binário.
- Cloudflare R2 será o provedor de object storage para imagens e modelos 3D no MVP, utilizando a modalidade gratuita dentro das cotas disponíveis. A entrega pública poderá usar domínio/CDN da Cloudflare conforme configuração.
- Formatos de imagem aceitos: JPG/JPEG, PNG e WebP.
- Modelos 3D aceitos: GLB e glTF, com validação de integridade e dependências.
- A arquitetura deve permitir CDN, otimização/variantes de imagem e carregamento sob demanda.
- Backend valida autorização, MIME real, tamanho, checksum e associação da mídia ao restaurante antes de concluir o upload.
- Arquivos binários não serão armazenados no PostgreSQL.

**Ainda pendente:** limites numéricos de tamanho para imagens e modelos 3D e política de dimensões/resolução. As cotas e condições do plano gratuito devem ser conferidas na implantação e periodicamente, pois podem mudar.

**Impactos:** atualizar modelo de domínio, PRD, API e especificações de catálogo/3D. Esta decisão não implica que upload e processamento estejam implementados.

### D-10 — Provedor inicial de inteligência artificial

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:**
- Google Gemini será o provedor inicial de modelos de IA do Boraê, utilizando a Gemini API no plano gratuito inicialmente, sem habilitar faturamento para esse projeto.
- A integração será realizada exclusivamente pelo backend; frontend não chamará o provedor diretamente.
- Credenciais serão mantidas em variáveis/segredos de ambiente no servidor, nunca no repositório nem no cliente.
- A integração ficará atrás de uma camada/contrato interno de abstração, evitando dependência do SDK ou dos formatos proprietários do provedor no domínio do produto e permitindo substituição futura.
- O modelo receberá somente o contexto mínimo necessário, preparado pelo backend a partir do catálogo publicado e do restaurante em contexto.
- O modelo não terá acesso direto ao banco de dados, credenciais, ferramentas administrativas ou dados privados de outros restaurantes.
- Implementar controles de consumo e custo, rate limits por sessão/IP e monitoramento de uso por restaurante.
- Em indisponibilidade, timeout ou resposta inválida, o cardápio continuará funcional e o consumidor poderá usar busca e filtros convencionais como fallback.

**Ainda pendente:** modelo/versão específica do Gemini, cotas e limites exatos de requisições/tokens por modelo e estratégia detalhada de retry/fallback. O uso gratuito tem limites variáveis e pode estar sujeito às políticas de uso de dados do plano; não enviar informações pessoais ou sensíveis. A evolução para plano pago exige nova aprovação. A aprovação do provedor não define esses parâmetros nem implica implementação.

**Motivação:** iniciar com um provedor acessível e manter flexibilidade técnica, segurança, previsibilidade de custos e continuidade da navegação quando a IA falhar.

**Impactos:** atualizar arquitetura do assistente, API e PRD. Esta decisão não implica que a integração já esteja implementada.

### D-11 — Histórico e métricas do assistente

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisão:**
- Não persistir o conteúdo das conversas, perguntas ou respostas completas do consumidor.
- Não criar histórico de conversas no painel do restaurante no MVP.
- Persistir somente eventos e métricas agregadas, vinculados ao restaurante, sem conteúdo textual das mensagens.
- Métricas úteis por restaurante:
  - quantidade de interações com o assistente;
  - produtos recomendados e frequência com que cada produto é sugerido;
  - cliques nas recomendações e taxa de clique;
  - interações sem recomendação adequada/sem resultado;
  - latência/tempo de resposta;
  - erros, timeouts, respostas inválidas e acionamentos de fallback;
  - consumo de tokens e quantidade de chamadas, quando disponibilizados pelo provedor.
- Exibir essas métricas no painel do respectivo restaurante, respeitando as permissões do restaurante e o isolamento multi-tenant.
- Não armazenar prompts, respostas livres, identificadores pessoais ou dados sensíveis nos eventos analíticos.
- Definir retenção mínima necessária para agregados e métricas operacionais na implementação; logs técnicos devem evitar conteúdo de mensagens.

**Motivação:** oferecer ao restaurante indicadores acionáveis sobre interesse dos consumidores e qualidade do assistente, reduzindo armazenamento de dados pessoais e complexidade de privacidade.

**Limites:** cliques indicam interesse, não comprovam compra ou conversão. Métricas de pedidos/vendas só poderão ser adicionadas quando existir integração confiável com o fluxo de pedidos.

**Impactos:** atualizar módulo de IA, PRD, API e painel/analytics. Esta decisão não implica que as métricas já estejam implementadas.

- **D-12 — Fidelidade/Club:** parcialmente aprovada; estrutura dos dois programas e fórmulas de acúmulo definidas. Gatilho de crédito, expiração, estornos e comprovação da compra permanecem pendentes.
- **D-13 a D-20:** permanecem pendentes; não presumir aprovação nem iniciar implementação de regras de negócio correspondentes sem alinhamento.

Ausência de decisão significa que o desenvolvimento do recurso correspondente deve aguardar alinhamento.
