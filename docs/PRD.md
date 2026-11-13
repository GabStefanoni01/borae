# PRD — Product Requirements Document

**Produto:** Boraê  
**Status:** especificação funcional em evolução  
**Público deste documento:** produto, design, frontend, backend e colaboradores.

## 1. Resumo

Boraê é uma plataforma web gastronômica que conecta restaurantes e consumidores. O núcleo da experiência é um cardápio digital acessado por QR Code, com informações confiáveis e atualizadas do estabelecimento. Sobre essa base serão construídos descoberta, experiências 3D/AR, recomendações assistidas por IA e fidelidade.

A plataforma deve funcionar primeiro como web responsiva/PWA, sem exigir instalação para consultar um cardápio.

## 2. Problema e oportunidade

Restaurantes frequentemente mantêm cardápios, informações e relacionamento com clientes em canais fragmentados. Consumidores precisam entender rapidamente o que o restaurante oferece, comparar opções e descobrir pratos adequados ao momento. Boraê pretende centralizar essa experiência e oferecer ferramentas que ajudem o restaurante a apresentar seus produtos e fidelizar clientes.

Essas hipóteses devem ser validadas com restaurantes e consumidores reais antes de assumir métricas ou resultados comerciais.

## 3. Objetivos do produto

1. Permitir que um restaurante mantenha seu perfil e cardápio digital.
2. Permitir que o consumidor consulte o cardápio com poucos passos.
3. Garantir que preços, ingredientes, opções e disponibilidade exibidos correspondam aos dados publicados pelo restaurante.
4. Criar uma base de dados estruturada que habilite IA, 3D/AR e fidelidade sem duplicar regras.
5. Permitir que um usuário participe de vários restaurantes com permissões isoladas.
6. Construir descoberta e fidelidade individual gradualmente, após validar o núcleo.

### Não objetivos do MVP inicial
- Processar pagamentos ou pedidos completos.
- Substituir sistemas POS/ERP dos restaurantes.
- Operar carteira financeira ou crédito.
- Criar aplicativo nativo obrigatório.
- Implementar microserviços antes de necessidade comprovada.

## 4. Personas e necessidades

### Consumidor
Quer consultar o menu rapidamente, entender pratos, preços, ingredientes e opções, e futuramente receber sugestões relevantes. Pode acessar anonimamente por QR Code; conta será necessária apenas para recursos que exijam identidade, como histórico ou fidelidade.

### Proprietário (OWNER)
Tem controle total do restaurante: perfil, cardápios, catálogo, publicação, disponibilidade, equipe, papéis, propriedade e cobrança. Também poderá operar pedidos quando esse módulo existir.

### Gerente (MANAGER)
Administra perfil, cardápios, catálogo, publicação, disponibilidade, equipe e convites; também poderá operar pedidos quando esse módulo existir. Não altera papéis, propriedade ou cobrança.

### Colaborador (STAFF)
Tem acesso operacional ao painel, podendo alterar disponibilidade de produtos e, quando o módulo existir, visualizar pedidos e atualizar seus status. Não administra perfil, catálogo, publicação, equipe ou cobrança.

### Administrador Boraê
Precisa moderar e dar suporte à plataforma, com acesso global estritamente auditado. Esse perfil não faz parte do escopo implementado atual.

## 5. Princípios do produto

- Mobile-first e acessível.
- O consumidor deve conseguir consultar o cardápio sem criar conta.
- O restaurante é a fonte de verdade dos dados de seus produtos.
- Toda informação pública deve ser explicitamente publicada pelo restaurante.
- Funcionalidades indisponíveis não devem aparecer como se estivessem operacionais.
- IA sugere; backend valida.
- Segurança multi-tenant é requisito transversal.
- Recursos premium não podem degradar o fluxo básico do cardápio.

## 6. Jornada principal do restaurante

1. Criar conta ou entrar.
2. Criar restaurante e tornar-se OWNER.
3. Completar perfil público.
4. Configurar equipe e convites.
5. Criar um ou mais cardápios (ex.: Almoço, Jantar e Bebidas), cada um com suas categorias e produtos.
6. Revisar prévia pública.
7. Publicar os cardápios desejados; múltiplos podem ficar publicados simultaneamente.
8. Gerar QR Code principal, que direciona à página pública do restaurante.
9. Atualizar preços e disponibilidade conforme operação.

**Decisão de produto D-02 aprovada (13/11/2026):** múltiplos cardápios por restaurante; a página pública permite ao consumidor escolher o cardápio. Não haverá alternância automática por horário no MVP.

**Critério de sucesso:** um restaurante consegue publicar e manter um cardápio completo sem intervenção técnica do time Boraê.

## 7. Jornada principal do consumidor

1. Escanear QR Code ou encontrar restaurante na descoberta.
2. Abrir página pública do estabelecimento.
3. Navegar por categorias.
4. Abrir detalhes de um produto.
5. Consultar preço, descrição, ingredientes, opções e disponibilidade.
6. Opcionalmente visualizar modelo 3D/AR quando disponível.
7. Futuramente pedir recomendações ou participar de benefícios.

**Critério de sucesso:** consumidor entende o que pode pedir e quanto custa sem precisar criar conta nem contatar o restaurante para informações básicas.

## 8. Requisitos funcionais por módulo

### RF-01 — Conta e sessão
- Cadastro, autenticação, encerramento de sessão e consulta da conta atual.
- Dados de identidade e autorização definidos no servidor.
- Não permitir acesso a rotas privadas sem sessão válida.

### Matriz de permissões — D-07 aprovada

| Ação | OWNER | MANAGER | STAFF |
|---|---|---|---|
| Visualizar painel | Sim | Sim | Sim |
| Editar perfil do restaurante | Sim | Sim | Não |
| Criar/editar cardápios, categorias, produtos e preços | Sim | Sim | Não |
| Publicar/arquivar cardápios | Sim | Sim | Não |
| Alterar disponibilidade de produtos | Sim | Sim | Sim |
| Visualizar pedidos e atualizar status (quando o módulo existir) | Sim | Sim | Sim |
| Gerenciar equipe e convites | Sim | Sim | Não |
| Alterar papéis da equipe | Sim | Não | Não |
| Excluir ou transferir restaurante | Sim | Não | Não |
| Gerenciar assinatura e cobrança | Sim | Não | Não |

Os papéis são atribuídos por vínculo com cada restaurante. O mesmo usuário pode ter papéis diferentes em restaurantes distintos. A autorização deve ser aplicada no backend em todas as operações privadas. As permissões de pedidos são uma definição para o módulo futuro e não incluem pedidos no MVP atual.

### RF-02 — Restaurante e equipe
- Criar restaurante e vínculo OWNER atomicamente.
- Listar restaurantes do usuário.
- OWNER pode convidar MANAGER ou STAFF; MANAGER pode convidar somente STAFF.
- Vincular cada convite a um endereço de e-mail específico; aceitar somente com conta autenticada usando o mesmo e-mail.
- Fazer o convite expirar em 7 dias.
- Permitir que OWNER e MANAGER revoguem convites pendentes.
- Permitir reenvio de convites pendentes ou expirados, renovando a validade por 7 dias.
- Garantir que convites aceitos sejam de uso único.
- Listar equipe e alterar/remover vínculos conforme autorização.
- O limite de convites simultâneos e a política de entrega de e-mail ainda precisam ser definidos.

### RF-03 — Perfil do restaurante
- Editar nome do restaurante e nome público/fantasia.
- Permitir descrição, logo, capa, telefone/WhatsApp, Instagram e site como campos opcionais.
- Exigir endereço para estabelecimentos físicos e suportar localização no mapa.
- Configurar horário de funcionamento por dia da semana.
- Exigir categoria gastronômica.
- Gerar slug automaticamente e permitir edição quando único e válido.
- Suportar status `DRAFT`, `PUBLISHED` e `SUSPENDED`.
- Não bloquear o início do cadastro nem a montagem do cardápio pela ausência de campos opcionais.
- Validar os campos obrigatórios antes da publicação pública.
- Registrar quem alterou dados relevantes.

**Decisão de produto aprovada (D-01, 13/11/2026).**

### RF-04 — Catálogo
- Criar e ordenar categorias dentro de um cardápio.
- Criar, editar e arquivar produtos.
- Informar nome, descrição, preço, ingredientes individuais, imagem e disponibilidade.
- Fazer upload de imagens JPG/JPEG, PNG e WebP por serviço externo de mídia, sem armazenar binários no PostgreSQL.
- Permitir cadastrar livremente vários ingredientes por produto, editar, remover e ordenar sua exibição.
- Exibir os ingredientes informados pelo restaurante nos detalhes do produto.
- Permitir cadastrar adicionais opcionais com seus preços/acréscimos, exibidos ao consumidor no cardápio.
- Remoções de ingredientes serão informadas como observações no momento do pedido e apresentadas ao garçom.
- Não incluir tamanhos ou combos nesta etapa; ponto da carne poderá ser informado como instrução no pedido, quando aplicável.
- Controlar publicação no nível do cardápio: novos cardápios começam como `DRAFT`; publicar torna públicos os produtos elegíveis e categorias ativas.
- Não oferecer publicação individual por produto no MVP.
- Em cardápios publicados, alterações salvas entram em vigor imediatamente.
- Separar disponibilidade (`AVAILABLE` / `UNAVAILABLE`) do estado de publicação do cardápio.
- Impedir que produtos arquivados apareçam no menu público.
- Utilizar BRL no MVP e armazenar preços de produtos e adicionais como inteiros em centavos.
- Realizar cálculos monetários no backend, sem `float` ou `double`, e impedir valores negativos.

**Decisão de produto aprovada (D-03, 13/11/2026).**

**Decisão de produto aprovada (D-05, 13/11/2026):** o catálogo permite adicionais com preço visível. Remoções e ponto da carne, quando aplicável, são instruções informadas no momento do pedido e repassadas ao garçom; não são opções pré-cadastradas no produto. Não haverá tamanhos ou combos nesta etapa.

**Decisão de produto aprovada (D-04, 13/11/2026):** ingredientes são itens individuais cadastrados livremente pelo restaurante e associados ao produto. Não haverá cadastro específico de alergênicos ou contaminação cruzada neste escopo.

**Decisão de produto aprovada (D-06, 13/11/2026):** preços em BRL, armazenados em centavos como inteiros; cálculos monetários no backend, sem `float` ou `double`.

**Decisão de produto aprovada (D-09, 13/11/2026):** imagens JPG/JPEG, PNG e WebP e modelos GLB/glTF serão armazenados em object storage externo; PostgreSQL mantém apenas metadados e referências. Cloudflare R2 será utilizado como object storage no MVP, dentro das cotas gratuitas disponíveis. Limites numéricos de upload permanecem pendentes e o consumo deverá ser monitorado.

### RF-05 — Cardápio público
- Carregar dados publicados do restaurante.
- Exibir categorias e produtos disponíveis.
- Exibir indisponibilidade sem apagar o produto, se o restaurante optar por mantê-lo visível.
- Ser responsivo, acessível e rápido.
- Não expor dados privados do painel ou equipe.

### RF-06 — QR Code
- Gerar QR Code que aponta para URL pública estável do restaurante.
- Permitir regenerar/baixar arte quando necessário.
- QR por mesa é uma extensão; não presumir pedidos por mesa no MVP.
- QR Code não deve conter credenciais ou identificadores secretos.

### RF-07 — 3D/AR
- Associar modelo GLB/glTF a produto.
- Validar tipo, tamanho e integridade do arquivo.
- Armazenar modelos e imagens em object storage externo; manter somente metadados e referências no PostgreSQL.
- Suportar CDN, otimização de imagens e carregamento sob demanda.
- Avaliar Cloudflare R2 como provedor definido para mídia, sem fixar o provedor definitivo antes de validar custos e suporte aos formatos 3D.
- Oferecer visualização 3D e AR somente quando suportado.
- Manter fallback visual.

### RF-08 — Assistente gastronômico
- Receber intenção, orçamento, preferências e restrições.
- Consultar apenas catálogo publicado e disponível no restaurante selecionado.
- Retornar recomendações vinculadas a IDs reais de produtos.
- Basear explicações sobre a composição somente nos ingredientes cadastrados; não inferir ingredientes que não foram informados.


**Decisão D-10 aprovada (13/11/2026):** Google Gemini será o provedor inicial, integrado exclusivamente pelo backend e isolado por uma abstração interna para permitir substituição futura. Credenciais ficam no servidor; o modelo não acessa diretamente o banco. Haverá controles de consumo/custo, rate limits por sessão/IP e fallback para busca/filtros convencionais. Modelo/versão e cotas numéricas permanecem pendentes; a Gemini API será usada no plano gratuito inicialmente, sem habilitar faturamento sem nova aprovação. O plano gratuito tem limites variáveis e não deve receber informações pessoais ou sensíveis.

**Decisão D-11 aprovada (13/11/2026):** não persistir histórico, prompts ou respostas completas do assistente. Armazenar somente métricas agregadas por restaurante: interações, produtos recomendados/frequência, cliques/taxa de clique, interações sem resultado, latência, erros/timeouts/respostas inválidas/fallback e chamadas/tokens quando disponíveis. Não registrar conteúdo textual nem identificadores pessoais nos eventos analíticos. Exibir métricas apenas a usuários autorizados do respectivo restaurante. Cliques não comprovam compras; métricas de vendas dependem de integração futura com pedidos.
### RF-09 — Fidelidade do restaurante
- Cada restaurante poderá manter um programa de fidelidade próprio, com saldo e recompensas independentes.
- O restaurante poderá escolher entre acúmulo proporcional ao valor gasto ou quantidade fixa de pontos por compra.
- Registrar eventos de crédito, débito, expiração e estorno.
- Consultar saldo e histórico.
- Proteger contra eventos duplicados e concorrência.

**D-12 parcialmente aprovada:** cada restaurante escolhe entre acúmulo proporcional ao valor gasto ou quantidade fixa de pontos por compra. Os parâmetros exatos de cada modelo e a ordem de consumo dos pontos ainda serão definidos.

### RF-10 — Fidelidade individual por restaurante
- Cada restaurante poderá operar seu próprio programa de fidelidade, com contas, saldos, regras, recompensas e histórico independentes.
- O restaurante define as recompensas oferecidas e assume o custo dos benefícios do próprio programa.
- Os pontos só podem ser acumulados e utilizados no programa do mesmo restaurante; não há transferência, conversão ou resgate em outro estabelecimento.
- O restaurante poderá escolher entre acúmulo proporcional ao valor gasto ou quantidade fixa de pontos por compra, com parâmetros a definir.
- Registrar créditos, débitos, expirações, estornos e ajustes auditáveis; proteger contra duplicidade e concorrência.

**D-12 parcialmente aprovada:** estrutura e alternativas de acúmulo da fidelidade individual definidas; parâmetros exatos e ordem de consumo dos pontos continuam pendentes.

**D-13 parcialmente aprovada:** no fluxo inicial, a compra concluída será confirmada manualmente no fechamento da conta por garçom ou gerente. O registro deverá identificar funcionário e restaurante e ser auditável. O consumidor será localizado pelo telefone cadastrado. Verificação do telefone, idempotência detalhada e momento de disponibilidade dos pontos ainda serão definidos.

**D-14 aprovada:** o MVP contempla somente fidelidade individual por restaurante. O Boraê Club compartilhado fica para uma fase futura e não terá carteira, pontos ou resgates entre parceiros nesta etapa.

**D-15 parcialmente aprovada:** cada restaurante define a validade dos pontos. Estornos são registrados automaticamente por funcionário autorizado; se os pontos já tiverem sido utilizados, registra-se uma pendência sem saldo negativo. A proteção antifraude usa auditoria e alertas ao gerente, sem confirmação extra do consumidor nem bloqueio automático no MVP. No encerramento do programa, clientes terão no mínimo 60 dias para resgate; o restaurante pode oferecer mais. Fechamento repentino exige comunicação assim que possível, sem transferência de custos ao Boraê ou resgate em outro restaurante. A alteração de regras (D-15.8) só se aplica a pontos acumulados após sua data/hora de vigência; pontos existentes continuam vinculados à versão e aos parâmetros anteriores, e recompensas já conquistadas não podem ser retiradas. A ordem de consumo dos pontos permanece pendente.

### RF-11 — Descoberta de restaurantes
- Usar localização atual por padrão, com busca/seleção manual de outra região.
- Ordenar por relevância considerando distância, avaliações, popularidade e completude das informações.
- Filtrar por categoria, distância, faixa de preço, avaliação, horário, benefícios de fidelidade e entrega/retirada.
- Exibir página completa do restaurante com fotos, descrição, endereço/mapa, horários, cardápios/preços, avaliações, fidelidade e modalidades de atendimento.
- Modelos 3D dos pratos são opcionais e só aparecem quando disponíveis.
- Somente consumidores com compra confirmada podem avaliar (nota geral 1–5, comentário opcional e critérios de atendimento, comida e ambiente).
- Restaurante pode responder publicamente, mas não editar/apagar avaliações. Moderação de denúncias é responsabilidade do Boraê.
- Favoritos e listas personalizadas; compartilhamento por links, sem mensagens internas, rede social própria ou listas colaborativas.
- Restaurantes fechados aparecem abaixo dos abertos, com próxima abertura quando conhecida.
- Redes têm página principal da marca e páginas por unidade; marca e unidades têm avaliações/notas próprias.
- Cadastros incompletos podem aparecer com dados disponíveis, sem inventar conteúdo.

### RF-12 — Administração global da plataforma
- Separar papéis globais de OWNER/MANAGER/STAFF dos restaurantes.
- Ter administrador principal e equipe de suporte limitada.
- Suporte acessa somente dados técnicos; não acessa dados pessoais, histórico de compras ou dados financeiros dos restaurantes.
- Suporte pode suspender restaurantes temporariamente.
- Todas as ações administrativas geram auditoria.
- Painel global de indicadores: restaurantes, consumidores, compras e receita; dados financeiros não ficam disponíveis ao suporte.
- Administrador pode bloquear consumidores temporária ou permanentemente.
- Administrador não edita informações de restaurantes, mas pode excluí-los com confirmação e justificativa obrigatórias.
- Não haverá painel dedicado de denúncias/moderação neste escopo.
- Manutenções programadas devem ser avisadas previamente; administradores e equipe técnica autorizada podem ativar o modo de manutenção.

### RF-13 — Conta e segurança do consumidor
- Cadastro com nome, telefone verificado por SMS, e-mail e senha.
- Login por telefone + senha; SMS não é exigido a cada login.
- Sem foto de perfil; identidade visual padrão.
- Nome/e-mail editáveis; alterações de telefone e senha exigem validação de segurança.
- Recuperação por SMS ou link de e-mail, com validade limitada e uso único.
- Uma sessão/dispositivo ativo por vez; novo login encerra a sessão anterior.
- Bloquear por 15 minutos após cinco tentativas consecutivas incorretas.
- Não expirar contas por inatividade.
- Alertar sobre novos logins, alterações de credenciais/contato e atividade suspeita.
- Exclusão sob solicitação, com anonimização quando aplicável e retenção legal/auditável.

### RF-14 — Monetização e assinatura
- SaaS por assinatura fixa, sem comissão por transação.
- Plano único com mesmas funcionalidades e preço-base para todos os restaurantes.
- Teste gratuito de 30 dias; após o teste, exigir cadastro de forma de pagamento e confirmação expressa da assinatura.
- Períodos mensal, trimestral e anual, com descontos progressivos.
- Cancelamento a qualquer momento, sem multa, com acesso até o fim do período pago.
- Cobrança falha: tentativas adicionais e avisos; tolerância de sete dias antes de suspender acesso.
- Manter dados por 90 dias após suspensão por inadimplência; exclusão posterior sujeita a obrigações legais.
- Sem taxa de implantação; reajustes aplicados na próxima renovação, com aviso de 30 dias.
- Mudança de periodicidade vale na próxima renovação.
- Créditos de indicação podem abater mensalidades, não são sacáveis nem transferíveis, expiram em 12 meses e, após cancelamento, só voltam a ser utilizáveis se a assinatura for reativada.
- Sem multa ou juros por atraso; pagamentos por cartão de crédito e Pix; nota fiscal automática por cobrança.
- Preço nominal e percentuais de desconto ainda precisam de definição comercial.

### RF-15 — Integrações e API externa
- API pública para integrações autorizadas, protegida por chaves e permissões.
- Rate limiting obrigatório.
- Documentação pública Swagger/OpenAPI.
- Webhooks para eventos, com reenvio automático em caso de falha.
- Ambiente Sandbox para testes.
- Não oferecer endpoints anônimos para operações privadas nem confundir API pública com API sem autenticação.

### RF-16 — Canais e suporte
- Painel do restaurante web responsivo; sem aplicativo nativo para restaurantes no lançamento.
- Experiência do consumidor web responsiva; sem apps nativos Android/iOS no lançamento.
- Notificações por e-mail.
- Chat de suporte dentro da plataforma, com IA e encaminhamento para atendimento humano.

### RF-17 — Limites do lançamento: pedidos e POS
- Não haverá pedidos online no lançamento.
- Não haverá integração com PDV/ERP no lançamento.
- Operações iniciais serão próprias e registros poderão ser manuais.
- Estruturas de catálogo e adicionais não devem ser interpretadas como autorização para processar pedidos ou pagamentos.

## 9. Requisitos não funcionais

- **Segurança:** autenticação, autorização por recurso, isolamento entre restaurantes, validação de entrada e gestão segura de segredos.
- **Privacidade:** coletar apenas dados necessários; definir retenção e exclusão antes de lançar recursos de perfil/histórico.
- **Disponibilidade:** falhas de IA, mídia ou AR não podem impedir a consulta básica do cardápio.
- **Desempenho:** priorizar carregamento mobile, imagens otimizadas, paginação quando necessário e consultas indexadas.
- **Acessibilidade:** navegação por teclado, contraste, textos alternativos e semântica adequada.
- **Observabilidade:** logs sem segredos, métricas de erro/latência e rastreabilidade de operações críticas.
- **Manutenibilidade:** monólito modular, migrações versionadas, testes e documentação atualizada.
- **Compatibilidade:** experiência base deve funcionar em navegadores móveis modernos; AR é melhoria progressiva.

## 10. Métricas a validar

Não assumir metas numéricas antes de obter baseline. Instrumentar:
- Restaurantes que completam onboarding.
- Tempo até primeiro cardápio publicado.
- Percentual de cardápios com produtos e preços completos.
- Acessos e interações com cardápios.
- Retorno de restaurantes ao painel.
- Uso e conversão de recomendações, fidelidade e Club após lançamento.
- Erros, latência e abandono por etapa.

## 11. Critérios gerais de aceite

Uma funcionalidade só pode ser liberada quando:
- O fluxo principal e os estados alternativos estão definidos.
- As regras de negócio e permissões estão testadas no backend.
- Casos de acesso cruzado entre restaurantes são testados.
- Frontend apresenta carregamento, erro, vazio e sucesso.
- A API está documentada.
- A CI está verde.
- A documentação foi atualizada.
- O responsável pelo produto validou o comportamento.
