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
| D-12 | Fidelidade | **APROVADA** — modelos, cálculo, base de pontuação e estornos definidos | Concluído |
| D-13 | Consumidor/fidelidade | **APROVADA** — conta global, verificação, consentimento, confirmação e idempotência definidos | Concluído |
| D-14 | Escopo da fidelidade | **APROVADA** — MVP terá somente fidelidade individual por restaurante; Boraê Club compartilhado fica para uma fase futura | Concluído |
| D-15 | Fidelidade | **APROVADA** — validade, FEFO, expiração, avisos e estornos definidos | Concluído |
| D-16 | Descoberta | **APROVADA** — localização, busca, filtros, avaliações, favoritos e páginas de marca/unidade definidos | Concluído |
| D-17 | Consumidor | **APROVADA** — cadastro, autenticação, segurança, perfil e notificações definidos | Concluído |
| D-18 | Monetização | **APROVADA** — assinatura fixa, plano único, teste, cobrança, cancelamento e indicação definidos | Concluído* |
| D-19 | Administração | **APROVADA** — papéis globais, permissões, auditoria e manutenção definidos | Concluído |
| D-20 | Pedidos/integrações | **APROVADA** — sem pedidos/POS no lançamento; API autenticada, webhooks e canais definidos | Concluído |

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

### D-12 — Estrutura e cálculo do programa de fidelidade

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Decisões aprovadas:**
- Cada restaurante mantém um programa de fidelidade individual, com carteira, saldo, regras, recompensas e histórico independentes.
- O restaurante escolhe entre acúmulo proporcional ao valor gasto ou quantidade fixa de pontos por compra.
- No modelo proporcional, o restaurante pode configurar uma taxa por valor ou faixas de pontuação. Mesmo quando configurado por faixa, o cálculo é proporcional ao valor efetivamente pago dentro da faixa aplicável.
- O cálculo proporcional admite frações; o resultado final é arredondado para baixo (ex.: 37,9 resulta em 37 pontos).
- A base de cálculo é o valor efetivamente pago após descontos e cupons. Taxas de entrega, serviço e outros encargos não geram pontos.
- Os parâmetros do programa devem ser versionados. Alterações só afetam compras/pontos gerados após a vigência da nova regra.
- O consumidor pode ter uma conta global e carteiras independentes em vários restaurantes. Não há transferência ou conversão de pontos entre restaurantes.

**Estorno e compensação:**
- Cancelamento/estorno remove os pontos ainda disponíveis originados pela compra.
- Se parte dos pontos já tiver sido utilizada, o sistema registra o saldo pendente de compensação, sem criar saldo negativo.
- Os próximos acúmulos compensam primeiro essa pendência; somente o excedente é adicionado ao saldo disponível.
- O estorno pode ser automático quando uma integração confiável enviar o evento ou manual por funcionário autorizado. A operação deve ser idempotente e auditável.

**Observação:** o valor mínimo de compra do modelo de pontos fixos já foi definido anteriormente pelo responsável pelo produto; não alterar nem inventar esse valor nesta consolidação. Se a especificação numérica não estiver disponível no repositório, recuperá-la antes de codificar a validação.

### D-13 — Consumidor, confirmação da compra e prevenção de duplicidade

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

**Conta e identificação:**
- Existe uma conta global por consumidor, identificada pelo número de telefone verificado por SMS.
- A mesma conta pode possuir carteiras de fidelidade independentes em vários restaurantes.
- O cadastro exige nome, telefone verificado, e-mail e senha.
- O acesso usa telefone + senha. SMS é obrigatório na verificação inicial do telefone e na recuperação/troca de número, não em todo login.
- A adesão à fidelidade exige consentimento explícito. Autorização para ofertas e promoções é separada e opcional.
- Para trocar o telefone, validar o novo número por SMS; preservar conta, carteiras e histórico, sem exigir validação do número antigo.
- Recuperação de senha por SMS ou link de e-mail, à escolha do consumidor; os mecanismos devem ter validade limitada, uso único e limites contra abuso.
- Apenas uma sessão/dispositivo conectado por vez; novo login encerra a sessão anterior.
- Após cinco tentativas consecutivas de senha incorreta, bloquear temporariamente o acesso por 15 minutos.
- Contas não expiram por inatividade.
- O consumidor pode solicitar exclusão; dados pessoais devem ser anonimizados quando aplicável, preservando o que for necessário por obrigação legal e auditoria.
- Notificações de segurança abrangem novos logins, alterações de senha, telefone e e-mail e atividades suspeitas.

**Confirmação e crédito:**
- Pontos só ficam disponíveis após confirmação efetiva do pagamento da compra.
- Quando houver integração confiável disponível, a confirmação pode chegar automaticamente; no fluxo sem integração, funcionário autorizado confirma manualmente.
- Garçons e gerentes autorizados podem confirmar a compra no fechamento e registrar o valor efetivamente pago.
- O consumidor não pode gerar pontos para si próprio.
- A confirmação registra restaurante, funcionário responsável e referência única da compra.

**Idempotência:** a referência/identificador único da compra é a proteção principal contra crédito duplicado. Análise complementar de restaurante, consumidor, valor, horário e demais atributos da compra pode identificar possíveis duplicidades ou inconsistências.

### D-14 — Escopo da fidelidade no MVP

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

O MVP terá exclusivamente programas de fidelidade individuais, administrados e financiados por cada restaurante. Pontos, carteiras e recompensas não circulam entre estabelecimentos. O Boraê Club compartilhado permanece como visão de longo prazo, fora do MVP; qualquer retomada exigirá nova decisão sobre financiamento, compensação, liquidação, responsabilidades e regras legais/operacionais.

### D-15 — Validade, estornos, expiração e encerramento da fidelidade

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

- Cada restaurante define a validade dos pontos de seu próprio programa.
- Cada crédito/lote mantém sua data de expiração e a versão da regra que o originou.
- O consumo segue FEFO (*first expired, first out*): utilizar primeiro os pontos que vencem antes. Em caso de mesma data de vencimento, consumir primeiro o crédito mais antigo.
- A expiração é automática: retirar o saldo disponível e registrar evento no histórico ligado ao crédito original, com quantidade e data.
- Durante os 14 dias anteriores ao vencimento, o consumidor recebe pelo menos uma notificação por dia sobre os pontos próximos de expirar.
- As notificações usam push e central interna do app. Cada aviso diário agrupa os lotes próximos do vencimento, preservando restaurante, quantidade e data de vencimento de cada lote.
- O horário de envio deve considerar o comportamento do consumidor e seu fuso, evitando períodos de descanso; usar horário padrão seguro quando não houver dados suficientes.
- Notificações transacionais de expiração não podem ser desativadas por preferências de marketing. Promoções podem ser desativadas separadamente.
- Se o push estiver desativado no aparelho, manter os avisos na central interna; não enviar SMS como canal alternativo.
- Alterações de regras só afetam pontos acumulados após sua data/hora de vigência. Pontos anteriores mantêm a versão e as condições originais; recompensas já conquistadas não podem ser retiradas.
- Estornos preservam o lançamento original e criam lançamentos compensatórios. Pontos já utilizados geram pendência de compensação, sem saldo negativo; os próximos acúmulos compensam a pendência primeiro.
- Ao encerrar voluntariamente o programa, comunicar os consumidores e garantir pelo menos 60 dias para resgate. O restaurante pode oferecer prazo maior.
- Em fechamento repentino/imprevisível, comunicar os consumidores assim que possível. O Boraê não assume automaticamente o custo das recompensas nem permite resgate em outro restaurante.

### D-16 — Descoberta de restaurantes

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

- Usar a localização atual como referência padrão, permitindo pesquisar ou selecionar manualmente outra região.
- Ordenar por relevância, considerando distância, avaliações, popularidade e completude/qualidade das informações. Personalização por histórico fica para evolução futura.
- Oferecer filtros por categoria, distância, faixa de preço, avaliação, horário de funcionamento, benefícios de fidelidade e modalidade de atendimento (entrega/retirada).
- A página do restaurante apresenta fotos, descrição, endereço/mapa, horários, cardápios e preços, avaliações, fidelidade e modalidades de atendimento.
- Modelos 3D dos pratos são opcionais e aparecem somente quando disponíveis.
- Somente consumidores com compra confirmada podem avaliar. A avaliação inclui nota geral de 1 a 5, comentário opcional e critérios de atendimento, comida e ambiente.
- O restaurante pode responder publicamente, mas não pode editar nem apagar avaliações dos consumidores. A moderação de denúncias é responsabilidade do Boraê.
- Favoritos e listas personalizadas são suportados. Compartilhamento ocorre por links; não haverá mensagens internas, rede social própria nem listas colaborativas.
- Restaurantes fechados aparecem abaixo dos abertos, com indicação de fechado e próxima abertura quando conhecida.
- Cadastros incompletos podem aparecer com os dados disponíveis; o sistema não deve inventar informações.
- Redes com várias unidades terão uma página principal da marca e páginas individuais por unidade. A marca terá nota geral baseada nas unidades; cada unidade terá suas próprias avaliações e nota.

### D-17 — Conta e perfil do consumidor

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

- Login por telefone + senha; SMS para verificação inicial e recuperação/troca de telefone, não em todo acesso.
- Cadastro obrigatório: nome, telefone verificado, e-mail e senha.
- Não haverá foto de perfil; usar identidade visual padrão.
- Nome e e-mail podem ser editados diretamente. Telefone e senha exigem validação de segurança; novo telefone deve ser verificado por SMS.
- Exclusão mediante solicitação, com anonimização de dados pessoais quando aplicável e retenção do que for exigido por lei/auditoria.
- Recuperação de senha por SMS ou link de e-mail, à escolha do consumidor, com validade limitada, uso único e proteção contra abuso.
- Apenas um dispositivo/sessão ativa por vez; novo login encerra a sessão anterior.
- Cinco tentativas consecutivas incorretas geram bloqueio temporário por 15 minutos.
- Contas não expiram por inatividade.
- Alertas de segurança para novos logins, alterações de senha, telefone e e-mail e atividades suspeitas.

### D-18 — Monetização e cobrança dos restaurantes

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026. Valores numéricos de preço e percentuais de desconto ainda não foram definidos.

- Modelo SaaS por assinatura fixa; não há comissão por compra/transação.
- Haverá um único plano, com as mesmas funcionalidades e o mesmo preço-base para todos os restaurantes.
- Novos restaurantes têm 30 dias de teste gratuito.
- Ao final do teste, o restaurante precisa cadastrar uma forma de pagamento e confirmar a assinatura para continuar; não iniciar cobrança sem essa confirmação.
- Períodos disponíveis: mensal, trimestral e anual, com descontos progressivos para compromissos mais longos.
- Cancelamento permitido a qualquer momento, sem multa; o acesso continua até o fim do período já pago.
- Falha de cobrança gera novas tentativas e notificações, com 7 dias de tolerância para regularização. Após esse prazo, suspender o acesso se o pagamento continuar pendente.
- Após suspensão por inadimplência, manter dados do restaurante e de seus clientes por 90 dias; depois, poderão ser excluídos, respeitadas obrigações legais de retenção.
- Não há taxa de implantação ou configuração inicial.
- O preço-base é único e definido pelo Boraê, igual para todos. Reajustes podem atingir todos os restaurantes, mas só entram em vigor na próxima renovação do período contratado e exigem aviso prévio de pelo menos 30 dias.
- Troca entre modalidades mensal, trimestral e anual é permitida; a nova modalidade entra em vigor somente na próxima renovação.
- Programa de indicação: restaurante que indicar outro estabelecimento que efetivamente assine recebe créditos para abater futuras mensalidades.
- Créditos de indicação podem quitar uma mensalidade integralmente, mas não podem ser sacados nem transferidos para outro restaurante. Expiram em 12 meses.
- Após cancelamento, créditos ficam indisponíveis e só poderão voltar a ser utilizados se o restaurante reativar a assinatura, respeitada a validade.
- Não haverá multa ou juros por atraso.
- Formas de pagamento aprovadas: cartão de crédito e Pix.
- Emitir nota fiscal automaticamente a cada cobrança.

### D-19 — Administração global da plataforma

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

- A administração global terá um administrador principal e uma equipe de suporte com permissões limitadas. Não confundir papéis globais com OWNER/MANAGER/STAFF de cada restaurante.
- O administrador principal pode criar outros administradores; somente o administrador principal original pode excluir outros administradores.
- Suporte acessa apenas dados técnicos; não acessa dados pessoais de consumidores, histórico de compras nem dados financeiros dos restaurantes.
- Suporte pode suspender restaurantes temporariamente, mas não reativá-los nem aplicar suspensão permanente.
- Todas as ações administrativas devem gerar registros de auditoria.
- O painel global apresenta indicadores de restaurantes, consumidores, compras e receita. Dados financeiros não ficam disponíveis à equipe de suporte.
- O administrador pode bloquear consumidores temporária ou permanentemente.
- Não haverá painel próprio de denúncias/moderação neste escopo; a responsabilidade de moderação do Boraê definida em D-16 permanece.
- Administrador não edita informações dos restaurantes, mas pode excluí-los da plataforma. Exclusão exige confirmação e justificativa obrigatórias.
- Manutenções programadas devem ser comunicadas previamente aos usuários.
- Administradores e equipe técnica autorizada podem ativar o modo de manutenção.

### D-20 — Pedidos, integrações e canais de acesso

**Status:** Aprovada pelo responsável pelo produto em 13/11/2026.

- Não haverá pedidos online no lançamento. O primeiro escopo público contempla descoberta, páginas de restaurantes, cardápios e fidelidade.
- Não haverá integração com sistemas de gestão/PDV/ERP no lançamento; as operações iniciais serão próprias e os registros poderão ser manuais.
- Haverá API pública para integrações futuras, mas ela será protegida por chaves de API e permissões. “Pública” significa documentada e disponível para integrações autorizadas, não anônima ou sem autenticação.
- A API terá rate limiting e documentação pública Swagger/OpenAPI.
- Haverá webhooks para eventos do Boraê, com tentativas automáticas de reenvio em caso de falha.
- Haverá ambiente Sandbox para testar integrações sem afetar dados reais.
- Restaurantes usarão painel web responsivo; não haverá aplicativo nativo para restaurantes no lançamento.
- Consumidores usarão web responsiva; não haverá aplicativos nativos Android/iOS no lançamento.
- Notificações por e-mail serão suportadas.
- Haverá chat de suporte dentro da plataforma, inicialmente com IA e possibilidade de encaminhamento para atendimento humano.

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
- **D-14 — Escopo da fidelidade no MVP:** aprovada; somente programas individuais por restaurante. Boraê Club compartilhado fica para uma fase futura.
- **D-15 — Regras operacionais da fidelidade:** parcialmente aprovada; validade, estornos, antifraude, encerramento e alteração não retroativa das regras (D-15.8) definidos. Ordem de consumo dos pontos permanece pendente.
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

- **D-12 — Fidelidade individual:** parcialmente aprovada; modelos de acúmulo definidos, parâmetros exatos e ordem de consumo dos pontos pendentes.
- **D-13 — Confirmação da compra:** parcialmente aprovada; confirmação manual por funcionário e identificação por telefone definidas. Verificação do telefone, idempotência detalhada e momento de disponibilidade do crédito permanecem pendentes.
- **D-14 — Escopo da fidelidade:** aprovada; somente programas individuais por restaurante no MVP.
- **D-15 — Regras operacionais da fidelidade:** parcialmente aprovada; políticas registradas acima, com ordem de consumo dos pontos pendente.
- **D-16 a D-20:** permanecem pendentes; não presumir aprovação nem iniciar implementação de regras de negócio correspondentes sem alinhamento.

Ausência de decisão significa que o desenvolvimento do recurso correspondente deve aguardar alinhamento.
