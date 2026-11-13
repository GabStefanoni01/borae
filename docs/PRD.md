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

### Proprietário/gestor (OWNER)
Quer cadastrar o restaurante, manter dados e cardápio, convidar equipe e controlar quem pode administrar o estabelecimento.

### Gerente (MANAGER)
Precisa consultar e futuramente operar áreas delegadas pelo proprietário. O conjunto exato de permissões de escrita ainda deve ser aprovado.

### Colaborador (STAFF)
Precisa executar tarefas operacionais autorizadas, sem acesso administrativo amplo. Permissões específicas ainda devem ser definidas por módulo.

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

### RF-02 — Restaurante e equipe
- Criar restaurante e vínculo OWNER atomicamente.
- Listar restaurantes do usuário.
- Convidar MANAGER/STAFF por token seguro.
- Aceitar convite apenas com conta autenticada cujo e-mail corresponda.
- Listar equipe e alterar/remover vínculos conforme autorização.
- Convite por e-mail automatizado é evolução futura; fluxo atual compartilha token manualmente.

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
- Permitir cadastrar livremente vários ingredientes por produto, editar, remover e ordenar sua exibição.
- Exibir os ingredientes informados pelo restaurante nos detalhes do produto.
- Suportar variações e adicionais com regras de seleção.
- Controlar publicação no nível do cardápio: novos cardápios começam como `DRAFT`; publicar torna públicos os produtos elegíveis e categorias ativas.
- Não oferecer publicação individual por produto no MVP.
- Em cardápios publicados, alterações salvas entram em vigor imediatamente.
- Separar disponibilidade (`AVAILABLE` / `UNAVAILABLE`) do estado de publicação do cardápio.
- Impedir que produtos arquivados apareçam no menu público.
- Manter preço em unidade monetária definida pelo sistema (BRL no lançamento inicial, sujeito à confirmação comercial).

**Decisão de produto aprovada (D-03, 13/11/2026).**

**Decisão de produto aprovada (D-04, 13/11/2026):** ingredientes são itens individuais cadastrados livremente pelo restaurante e associados ao produto. Não haverá cadastro específico de alergênicos ou contaminação cruzada neste escopo.

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
