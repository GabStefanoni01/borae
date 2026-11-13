# Registro de decisões e pendências restantes

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
| D-09 | Mídia | **APROVADA** — Cloudflare R2, limites de mídia e otimização definidos; ver decisões comerciais e operacionais | Concluído |
| D-10 | IA | **APROVADA** — Gemini Flash-Lite, cotas por consumidor/restaurante e controles de consumo; ver decisões comerciais e operacionais | Concluído |
| D-11 | IA | **APROVADA** — não persistir conversas; armazenar somente métricas agregadas por restaurante | Concluído |
| D-12 | Fidelidade | **APROVADA** — modelos, cálculo, base de pontuação e estornos definidos | Concluído |
| D-13 | Consumidor/fidelidade | **APROVADA** — conta global, verificação, consentimento, confirmação e idempotência definidos | Concluído |
| D-14 | Escopo da fidelidade | **APROVADA** — MVP terá somente fidelidade individual por restaurante; Boraê Club compartilhado fica para uma fase futura | Concluído |
| D-15 | Fidelidade | **APROVADA** — validade, FEFO, expiração, avisos e estornos definidos | Concluído |
| D-16 | Descoberta | **APROVADA** — localização, busca, filtros, avaliações, favoritos e páginas de marca/unidade definidos | Concluído |
| D-17 | Consumidor | **APROVADA** — cadastro, autenticação, segurança, perfil e notificações definidos | Concluído |
| D-18 | Monetização | **APROVADA** — assinatura fixa, preços, descontos, Asaas e emissão fiscal condicionada definidos; ver decisões comerciais e operacionais | Concluído |
| D-19 | Administração | **APROVADA** — papéis globais, permissões, auditoria e manutenção definidos | Concluído |
| D-20 | Pedidos/integrações | **APROVADA** — sem pedidos/POS no lançamento; API autenticada, webhooks e canais definidos | Concluído |

## Decisões comerciais e operacionais complementares

As escolhas de preço, provedores, cotas, armazenamento, monitoramento e políticas operacionais aprovadas em 13/11/2026 estão detalhadas em [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md). Esses parâmetros complementam D-08, D-09, D-10, D-18 e D-20; não substituem as regras de domínio abaixo.

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

- **D-01 a D-11:** aprovadas e registradas nesta documentação.
- **D-12 a D-17:** aprovadas e registradas nesta documentação.
- **D-18:** modelo comercial, preços, descontos e regras operacionais aprovados; ver [decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md).
- **D-19 e D-20:** aprovadas e registradas nesta documentação.
- **Boraê Club:** visão futura preservada; fora do MVP.

Uma decisão de produto aprovada define o comportamento esperado, mas não significa que a funcionalidade já esteja implementada. Itens técnicos e parâmetros comerciais explicitamente pendentes devem ser resolvidos antes da implementação correspondente.


## Parâmetros aprovados e pendências restantes

Os preços e descontos de D-18 foram definidos: R$ 49,90 mensal, 10% de desconto trimestral e 20% anual. Provedores e políticas operacionais aprovados estão em [DECISIONS_COMMERCIAL_AND_OPERATIONS.md](DECISIONS_COMMERCIAL_AND_OPERATIONS.md).

Permanecem parametrizáveis antes da implementação/produção: limite de tokens por interação, limites numéricos de convites e API Keys, retenção operacional de logs/métricas, limiares de alertas, versão estável exata do Gemini Flash-Lite, parâmetros de otimização de mídia, configuração fiscal do Asaas e schema físico/migrações. Não tratar esses parâmetros como decisões de produto em aberto nem inventar valores.
