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
6. Construir descoberta e benefícios compartilhados gradualmente, após validar o núcleo.

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

### RF-09 — Fidelidade do restaurante
- Configurar regras de ganho e recompensa.
- Registrar eventos de crédito, débito, expiração e estorno.
- Consultar saldo e histórico.
- Proteger contra eventos duplicados e concorrência.

### RF-10 — Boraê Club
- Permitir adesão de restaurantes parceiros.
- Manter saldo e ledger separados dos programas individuais.
- Registrar origem, destino, custo, resgate e liquidação.
- Exigir regras comerciais e financeiras aprovadas antes do desenvolvimento.

### RF-11 — Descoberta
- Listar estabelecimentos publicados e ativos.
- Filtrar por localização e categoria.
- Abrir perfil e cardápio público.
- Favoritos, avaliações e ranking ficam fora do primeiro recorte.

### RF-12 — Administração da plataforma
- Gerir contas, restaurantes, planos, campanhas e incidentes.
- Exigir papéis administrativos próprios e auditoria.
- Nunca reutilizar OWNER como administrador global.

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
