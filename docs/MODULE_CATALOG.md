# Especificação de módulo — Catálogo e cardápio digital

**Status:** planejamento aprovado para iniciar implementação incremental  
**Prioridade:** próximo módulo central do produto após a fundação de restaurante/perfil  
**Escopo deste documento:** orientar backend, frontend, banco de dados, experiência pública, testes e integração  
**Importante:** esta especificação não significa que entidades, endpoints ou telas do catálogo já existam. Os contratos finais devem ser confirmados no código, nos testes e no OpenAPI de cada entrega.

## 1. Objetivo do módulo

Construir a base de catálogo do Boraê para que cada restaurante consiga cadastrar, organizar, manter e publicar seus cardápios sem depender do time técnico. O consumidor deverá conseguir abrir uma página pública responsiva, escolher um cardápio e consultar categorias, produtos, preços, ingredientes, adicionais e disponibilidade, sem precisar criar conta.

O catálogo precisa ser uma fonte confiável de dados para os próximos módulos — assistente gastronômico, descoberta, experiência 3D/AR e fidelidade — sem antecipar ou implementar esses módulos agora.

### Resultados esperados

- O restaurante consegue criar mais de um cardápio e manter vários publicados simultaneamente.
- Cada cardápio tem categorias e produtos próprios, com ordenação controlada pelo restaurante.
- O restaurante consegue editar preços, descrições, ingredientes, adicionais, imagens e disponibilidade.
- O consumidor vê somente conteúdo que deve ser público, com preço e disponibilidade atuais.
- A experiência funciona bem em celular, inclusive ao acessar por QR Code.
- Dados de um restaurante nunca podem ser lidos ou alterados por outro restaurante através de troca de IDs.

## 2. Decisões de produto já aprovadas — não reabrir

As decisões abaixo são regras de produto aprovadas. A implementação deve respeitá-las; dúvidas de schema, endpoint ou experiência devem ser resolvidas sem alterar o comportamento de negócio.

### 2.1 Múltiplos cardápios por restaurante — D-02

- Um restaurante pode possuir vários cardápios, por exemplo **Almoço**, **Jantar**, **Bebidas** ou **Sobremesas**.
- Cada cardápio tem suas próprias categorias e produtos.
- Mais de um cardápio pode permanecer publicado ao mesmo tempo.
- A página pública do restaurante apresenta os cardápios publicados para o consumidor escolher.
- No MVP não existe alternância automática de cardápio por horário.
- O QR Code principal direciona à página pública do restaurante, não a um cardápio individual.

### 2.2 Publicação por cardápio — D-03

Estados do cardápio:
- `DRAFT`: em preparação, não visível publicamente.
- `PUBLISHED`: visível na página pública.
- `ARCHIVED`: arquivado, não visível publicamente.

Regras:
- Um cardápio novo começa em `DRAFT`.
- O restaurante cadastra categorias e produtos, revisa e publica o cardápio.
- A publicação valida a consistência mínima necessária.
- A publicação é controlada pelo cardápio, não por produto nem globalmente pelo restaurante.
- Não existe publicação individual de produto no MVP.
- Em um cardápio publicado, alterações salvas entram no ar imediatamente; não há uma segunda aprovação/publicação por edição.
- O restaurante pode despublicar ou arquivar o cardápio sem apagar os dados.
- Produtos arquivados não aparecem publicamente.
- Produtos indisponíveis podem continuar visíveis, claramente identificados como indisponíveis.

### 2.3 Ingredientes — D-04

- Ingredientes são itens livres cadastrados individualmente pelo restaurante e associados ao produto.
- O restaurante pode adicionar, editar, remover e ordenar os ingredientes.
- Exemplo: pão, carne, queijo, cebola e bacon.
- Não existe neste escopo um catálogo padronizado de alergênicos nem declaração estruturada de contaminação cruzada.
- A interface não deve inventar garantias sobre alergênicos com base apenas na lista de ingredientes.

### 2.4 Adicionais e instruções — D-05

- O restaurante pode cadastrar adicionais opcionais, como bacon, cheddar ou ovo.
- Cada adicional tem nome, preço/acréscimo explícito e estado ativo/inativo.
- O preço do adicional deve ser apresentado ao consumidor antes da seleção.
- Remoção de ingredientes (por exemplo, “sem cebola”) não é uma opção pré-cadastrada do produto: será uma instrução/observação no momento do pedido, em módulo futuro.
- Ponto da carne, quando aplicável, também será uma instrução futura no pedido.
- Não haverá tamanhos ou combos nesta etapa.
- Não implementar pedidos ou checkout junto com o catálogo.

### 2.5 Preço — D-06

- A moeda do MVP é BRL.
- Valores são armazenados e trafegados como inteiros em centavos: `priceInCents` e `priceDeltaInCents`.
- Não usar `float` ou `double` para valores monetários.
- O backend é a autoridade para validação e cálculos.
- Preços não podem ser negativos.
- O frontend apenas formata centavos para reais na apresentação.

### 2.6 Permissões — D-07

A autorização é por vínculo ativo do usuário com o restaurante e deve ser validada no backend em todas as operações privadas.

| Ação | OWNER | MANAGER | STAFF |
|---|---:|---:|---:|
| Consultar painel e catálogo administrativo | Sim | Sim | Conforme acesso operacional definido no painel |
| Criar/editar cardápios, categorias e produtos | Sim | Sim | Não |
| Alterar preços e ingredientes | Sim | Sim | Não |
| Publicar, despublicar e arquivar cardápios | Sim | Sim | Não |
| Alterar disponibilidade de produtos | Sim | Sim | Sim |
| Consultar cardápio público | Público | Público | Público |

STAFF pode alterar somente a disponibilidade operacional dos produtos, não o conteúdo, preço ou publicação. A interface pode ocultar ações não permitidas, mas isso nunca substitui a autorização no servidor.

## 3. Escopo funcional

### 3.1 Gestão de cardápios

O painel do restaurante deve permitir:

- Listar cardápios do restaurante.
- Criar cardápio informando nome e, opcionalmente, descrição.
- Abrir um cardápio para administrar categorias e produtos.
- Editar nome e descrição.
- Consultar estado (`DRAFT`, `PUBLISHED`, `ARCHIVED`).
- Publicar um rascunho após validação.
- Despublicar um cardápio publicado, retornando-o a `DRAFT`.
- Arquivar sem apagar o conteúdo.
- Reordenar cardápios quando a experiência de listagem exigir.
- Visualizar uma prévia do que o consumidor verá.

Não criar agendamento de publicação nem alternância automática por horário no MVP.

### 3.2 Categorias

Cada categoria pertence a exatamente um cardápio.

Campos previstos:
- Nome obrigatório.
- Descrição opcional.
- Ordem de exibição.
- Estado ativo/inativo.
- Identificador UUID.
- Datas de criação e atualização.

Ações:
- Listar categorias do cardápio.
- Criar, editar e renomear.
- Ativar/desativar.
- Reordenar por lista explícita de IDs.
- Consultar quantidade de produtos associados.

Regras e cuidados:
- Não permitir nomes duplicados no mesmo cardápio após normalização definida na implementação.
- Categorias inativas não aparecem na página pública.
- O comportamento de produtos associados a uma categoria desativada deve ser consistente: eles deixam de ser exibidos publicamente enquanto a categoria estiver inativa, sem apagar o produto.
- Não excluir categoria que ainda tenha produtos sem definir e validar uma operação explícita de realocação ou arquivamento. Preferir desativação como primeira opção.
- Validar no backend que a categoria pertence ao cardápio e ao restaurante informado.

### 3.3 Produtos

Cada produto pertence a uma categoria, e por consequência a um cardápio e restaurante.

Campos previstos:
- Nome obrigatório.
- Descrição opcional.
- Preço em centavos (BRL), obrigatório e não negativo.
- Categoria obrigatória.
- Imagem opcional.
- Ingredientes em lista ordenada de itens livres.
- Adicionais opcionais com preço em centavos.
- Estado de ciclo de vida: ativo ou arquivado.
- Disponibilidade operacional: `AVAILABLE` ou `UNAVAILABLE`.
- Ordem de exibição.
- Datas de criação e atualização.

Ações:
- Listar produtos por cardápio/categoria.
- Criar e editar.
- Arquivar e, se permitido, reativar.
- Alterar disponibilidade.
- Reordenar.
- Gerenciar ingredientes (adicionar, editar, remover e ordenar).
- Gerenciar adicionais (criar, editar preço/nome, ativar/desativar e ordenar).
- Vincular ou trocar imagem.
- Consultar prévia pública do produto.

Regras:
- Não permitir preço negativo.
- Produto arquivado não aparece publicamente.
- Produto indisponível pode permanecer visível com rótulo claro.
- Não confundir arquivamento com indisponibilidade.
- Não aceitar que um produto seja associado a categoria de outro restaurante ou cardápio.
- A disponibilidade pode ser alterada por OWNER, MANAGER e STAFF; demais campos apenas por OWNER/MANAGER.
- Não implementar tamanhos, combos, variações complexas, estoque ou integração POS/ERP nesta fase.

### 3.4 Ingredientes

- Cada ingrediente é um item textual associado a um único produto.
- A ordem cadastrada deve ser preservada na exibição.
- Nome obrigatório após remoção de espaços em branco.
- Não duplicar itens equivalentes no mesmo produto após normalização definida na implementação.
- Não tratar ingredientes como alérgenos certificados.
- A edição do produto deve permitir administrar a lista de forma simples, sem exigir cadastro global de ingredientes.

### 3.5 Adicionais

- Cada adicional pertence a um produto.
- Campos: nome, acréscimo de preço em centavos, estado ativo/inativo e ordem.
- Nome obrigatório; preço não negativo.
- Adicionais inativos não aparecem como opções públicas.
- Exibir nome e preço adicional de forma inequívoca.
- Não implementar seleção/compra do adicional ou cálculo de pedido nesta etapa; apenas cadastrar e apresentar as opções.

### 3.6 Imagens de produtos

O catálogo precisa permitir associar uma imagem ao produto. O arquivo binário não deve ser salvo no PostgreSQL.

Estado atual relevante:
- O projeto já possui fluxo de mídia de restaurante com ImageKit, abstração `RestaurantObjectStorage`, metadados no PostgreSQL e endpoints de logo/fotos.
- O endpoint atual de mídia de restaurante aceita `LOGO` e `PHOTO`; ainda não existe associação específica de mídia a produto.
- A implementação do catálogo deve avaliar a reutilização da abstração e do provedor ImageKit existentes, estendendo o domínio de mídia de forma compatível. Não introduzir Cloudflare R2 nem criar um segundo mecanismo de upload sem decisão explícita.
- Credenciais do ImageKit permanecem exclusivamente no backend.

Requisitos:
- JPEG, PNG e WebP.
- Validar conteúdo real, extensão, MIME, tamanho e limites no backend; validação no navegador é apenas antecipação de erro.
- Reutilizar o processamento de imagem já existente quando tecnicamente adequado (normalização de orientação, redimensionamento, compressão e remoção de metadados quando possível).
- Persistir no banco apenas referência, URL pública e metadados necessários.
- Impedir associação de imagem pertencente a outro restaurante.
- Ao trocar/remover imagem, evitar arquivos órfãos e manter consistência entre banco e provedor.
- Definir limites específicos para imagens de produto durante a implementação, sem assumir que o limite de 100 fotos de galeria seja automaticamente a política correta para o catálogo.
- Entregar imagem com carregamento responsivo/lazy loading na página pública.

### 3.7 Página pública do restaurante e cardápio

A página pública deve ser acessível pelo slug do restaurante e não exigir login.

Estrutura funcional:
1. Identidade pública do restaurante: nome, descrição, logo/capa quando disponíveis, categoria, endereço/contato e horário conforme os dados públicos já existentes.
2. Lista dos cardápios publicados.
3. Seleção de um cardápio.
4. Navegação por categorias.
5. Lista de produtos elegíveis.
6. Detalhe do produto com imagem, descrição, preço, ingredientes e adicionais ativos.
7. Indicação visível de produto indisponível.

Regras de exposição:
- Retornar somente cardápios `PUBLISHED`.
- Retornar somente categorias ativas.
- Não retornar produtos arquivados.
- Produtos indisponíveis podem ser retornados com estado explícito.
- Não expor IDs internos de usuário, dados da equipe, notas administrativas ou campos privados.
- Validar status de publicação do restaurante; restaurante suspenso ou não publicado não pode expor o cardápio.
- Não exigir cadastro, login ou localização do consumidor para consultar.
- Não exibir ações de pedido, pagamento, carrinho ou checkout.

### 3.8 QR Code

- O QR Code principal do restaurante deve apontar para a URL pública do restaurante, não para um menu específico.
- A página pública lista os cardápios publicados para escolha.
- O QR Code é uma forma de abrir a mesma URL pública; não criar um segundo fluxo de consulta.
- Geração/download do QR pode ser entregue junto da página pública ou em incremento posterior, mas não deve bloquear o CRUD inicial do catálogo.
- QR Code por mesa não faz parte do escopo atual.

## 4. Modelo de domínio proposto

Entidades conceituais:

- `Menu`: pertence a `Restaurant`; contém nome, descrição, status, ordem, timestamps e informação de publicação.
- `MenuCategory`: pertence a `Menu`; contém nome, descrição, ordem, estado ativo e timestamps.
- `Product`: pertence a uma categoria; contém nome, descrição, preço em centavos, referência de imagem, estado de ciclo de vida, disponibilidade, ordem e timestamps.
- `ProductIngredient`: pertence a `Product`; contém nome e ordem.
- `ProductAddon`: pertence a `Product`; contém nome, acréscimo em centavos, estado ativo e ordem.
- Referência de mídia: associação segura entre produto e arquivo existente no provedor, com metadados mantidos no PostgreSQL.

Relacionamentos:
- Restaurant 1:N Menu.
- Menu 1:N MenuCategory.
- MenuCategory 1:N Product.
- Product 1:N ProductIngredient.
- Product 1:N ProductAddon.
- Product 0..1:N referência de imagem conforme decisão física final; no MVP, uma imagem principal por produto é suficiente.

Invariantes:
- Todos os recursos devem ter escopo de restaurante verificável.
- Categoria deve pertencer ao menu esperado.
- Produto deve pertencer à categoria esperada.
- Ingredientes e adicionais devem pertencer ao produto esperado.
- Reordenação recebe IDs explícitos e rejeita IDs ausentes, repetidos ou fora do escopo.
- Constraints no banco devem reforçar unicidade e integridade sempre que possível.
- Migrações Flyway incrementais; nunca alterar migrations já aplicadas.

O schema físico, nomes finais de colunas, estratégia de exclusão e índices serão definidos na implementação após inspeção do código atual.

## 5. API — recursos candidatos

Os caminhos abaixo são uma proposta de organização REST. Os contratos finais devem ser confirmados no código e no Swagger; não assumir que já existem.

### Administração autenticada

| Método | Caminho candidato | Ação |
|---|---|---|
| GET | `/api/v1/restaurants/{restaurantId}/menus` | Listar cardápios |
| POST | `/api/v1/restaurants/{restaurantId}/menus` | Criar cardápio |
| GET | `/api/v1/restaurants/{restaurantId}/menus/{menuId}` | Consultar cardápio |
| PATCH | `/api/v1/restaurants/{restaurantId}/menus/{menuId}` | Editar cardápio |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/publish` | Publicar |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/unpublish` | Despublicar |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/archive` | Arquivar |
| GET/POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/categories` | Listar/criar categorias |
| PATCH | `/api/v1/restaurants/{restaurantId}/categories/{categoryId}` | Editar categoria |
| DELETE ou POST | `/api/v1/restaurants/{restaurantId}/categories/{categoryId}/...` | Desativar/remover conforme regra definida |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/categories/order` | Reordenar categorias |
| GET/POST | `/api/v1/restaurants/{restaurantId}/products` | Listar/criar produtos |
| GET/PATCH | `/api/v1/restaurants/{restaurantId}/products/{productId}` | Consultar/editar produto |
| DELETE ou POST | `/api/v1/restaurants/{restaurantId}/products/{productId}/archive` | Arquivar produto |
| POST | `/api/v1/restaurants/{restaurantId}/products/{productId}/availability` | Alterar disponibilidade |
| PUT | `/api/v1/restaurants/{restaurantId}/products/{productId}/ingredients` | Substituir lista de ingredientes |
| PUT | `/api/v1/restaurants/{restaurantId}/products/{productId}/addons` | Substituir lista de adicionais |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/order` | Reordenar produtos |

Os endpoints de ingredientes/adicionais podem ser modelados como sub-recursos CRUD em vez de substituição integral, desde que a experiência de edição, validação e concorrência fique clara. Não duplicar regras em endpoints incompatíveis.

### Consulta pública

| Método | Caminho candidato | Ação |
|---|---|---|
| GET | `/api/v1/public/restaurants/{slug}/menu` | Página pública e lista de cardápios publicados |
| GET | `/api/v1/public/restaurants/{slug}/menus/{menuSlugOrId}` | Consultar um cardápio publicado, se necessário |

A consulta pública não exige JWT. A API deve validar publicação do restaurante/cardápio e retornar somente campos explicitamente públicos.

### Contrato e erros

- Usar DTOs de request/response; não retornar entidades JPA.
- Valores monetários em centavos inteiros.
- Datas em ISO-8601.
- IDs UUID.
- Erros coerentes com os padrões já adotados no backend.
- Documentar códigos 400, 401, 403, 404, 409 e 413 quando aplicáveis.
- Atualizar OpenAPI/Swagger e `docs/API.md` quando os endpoints forem realmente implementados.
- Não aceitar `restaurantId`, papel ou identidade enviados no corpo como prova de autorização.

## 6. Experiência do painel administrativo

A gestão deve integrar-se ao dashboard atual, respeitando os padrões Angular já existentes.

### Navegação sugerida

- **Cardápios:** visão geral, criar cardápio, status e ações.
- **Editor do cardápio:** categorias e produtos.
- **Editor de produto:** dados básicos, preço, imagem, ingredientes e adicionais.
- **Prévia:** simulação da experiência pública.
- **Disponibilidade:** ação rápida disponível também para STAFF, sem liberar edição do produto.

### Comportamentos essenciais

- Exibir estado vazio com CTA para criar o primeiro cardápio.
- Distinguir rascunho, publicado e arquivado visualmente.
- Alertar quando tentar sair com alterações não salvas.
- Evitar perda de formulário após erro de rede.
- Mostrar feedback de salvamento e erros de validação por campo quando possível.
- Não permitir ações administrativas para STAFF; a disponibilidade deve continuar acessível.
- Usar confirmação para arquivamento quando houver impacto visível.
- Permitir edição em telas pequenas, sem depender de hover.
- Prévia pública deve refletir exatamente as regras de exposição da API pública.

## 7. Ordem de implementação sugerida

Entregar em incrementos pequenos, cada um em branch e PR própria, sem desenvolver todo o módulo em uma única mudança.

### Etapa 0 — Inspeção e alinhamento técnico
- Inspecionar código real de backend, frontend, autenticação, membership, mídia/ImageKit e migrations.
- Confirmar convenções de pacotes, DTOs, tratamento de erros e testes.
- Comparar contratos planejados com o código atual.
- Registrar apenas decisões técnicas necessárias para iniciar; não reabrir D-02 a D-07.

**Saída:** desenho técnico curto e lista de arquivos/entidades a criar ou estender.

### Etapa 1 — Backend: cardápios e categorias
- Criar migrations Flyway e entidades/repositórios.
- CRUD de cardápios.
- Estados DRAFT/PUBLISHED/ARCHIVED e transições permitidas.
- CRUD de categorias, ativação e ordenação.
- Aplicar autorização e isolamento multi-tenant.
- Testes unitários e de integração para transições, duplicidade e ownership.

**Critério de conclusão:** OWNER/MANAGER gerenciam cardápios e categorias; usuários sem permissão recebem 403; dados de outro restaurante nunca são acessíveis.

### Etapa 2 — Backend: produtos, ingredientes e adicionais
- CRUD de produtos.
- Preço em centavos e validações.
- Ingredientes e adicionais.
- Ordenação e arquivamento.
- Disponibilidade separada do estado de ciclo de vida.
- STAFF pode alterar somente disponibilidade.
- Testes de autorização, valores, vínculo entre recursos e operações inválidas.

**Critério de conclusão:** um restaurante consegue montar um catálogo completo pela API, com regras de preço e permissão aplicadas no servidor.

### Etapa 3 — Mídia de produto
- Estender a solução ImageKit já usada pelo perfil, sem duplicar storage.
- Definir associação de mídia com produto e validação de escopo.
- Upload/troca/remoção e tratamento de falhas para evitar órfãos.
- Testar limites, formato real, arquivo inválido e acesso cruzado.

**Critério de conclusão:** imagem é enviada, associada, consultada e removida de forma consistente, sem expor credenciais.

### Etapa 4 — Frontend administrativo
- Listagem e criação de cardápios.
- Gestão de categorias.
- Lista e formulário de produtos.
- Ingredientes, adicionais, imagem e preço.
- Publicar/despublicar/arquivar.
- Ação de disponibilidade para STAFF.
- Estados loading/empty/error/success e responsividade.
- Integração com API real, não apenas demo.

**Critério de conclusão:** OWNER/MANAGER conseguem administrar o catálogo pelo painel e STAFF consegue alterar somente disponibilidade.

### Etapa 5 — API e página pública
- Endpoint público por slug.
- Página pública responsiva do restaurante.
- Seleção entre vários cardápios publicados.
- Navegação por categorias e detalhes de produtos.
- Exibição de preços, ingredientes, adicionais e indisponibilidade.
- QR Code principal apontando para a página do restaurante.
- Cache e invalidação após mudanças relevantes.

**Critério de conclusão:** consumidor abre por link/QR sem login e vê somente dados publicados e atualizados.

### Etapa 6 — Validação integrada e endurecimento
- Testes ponta a ponta em desktop e mobile.
- Testes de isolamento multi-tenant.
- Testes de publicação e conteúdo público.
- Testes de upload e remoção no ImageKit real.
- Verificação de cache, SEO básico, acessibilidade e desempenho.
- Atualização de Swagger, API.md, DOMAIN_MODEL.md, SCREEN_FLOWS.md e ROADMAP.md.
- Deploy controlado e validação no ambiente publicado.

**Critério de conclusão:** todos os critérios de aceite da seção 9 passam; não marcar o módulo inteiro como implementado enquanto houver etapa crítica sem validação.

## 8. Segurança, consistência e desempenho

### Multi-tenancy e autorização
- Toda operação privada deve derivar o usuário do JWT.
- Validar membership ativo para o restaurante da rota.
- Validar papel conforme ação.
- Verificar que menu, categoria, produto, ingrediente, adicional e mídia pertencem ao mesmo restaurante.
- Nunca confiar em IDs recebidos sem confirmar suas relações.
- Não permitir IDOR por troca de UUID.
- Testar explicitamente acesso cruzado entre dois restaurantes.

### Integridade
- Usar transações nas operações que alterem múltiplos recursos relacionados.
- Constraints e índices para unicidade/relacionamentos importantes.
- Reordenação deve validar a lista completa antes de persistir.
- Exclusão/arquivamento não deve deixar referências inválidas.
- Alterações em cardápio publicado entram no ar ao salvar, conforme D-03.
- Garantir que uma publicação não exponha registros parcialmente gravados.

### Consulta pública
- DTOs públicos com allowlist explícita de campos.
- Não expor dados internos de equipe, auditoria ou configuração.
- Não revelar conteúdo de rascunhos/arquivados.
- Cache com invalidação ou versionamento quando nome, preço, disponibilidade, imagem, categoria ou publicação mudarem.
- Não confiar em cache para autorização de endpoints privados.

### Observabilidade
- Registrar eventos administrativos relevantes (criação, edição, publicação, arquivamento e alteração de disponibilidade) sem registrar segredos.
- Métricas úteis: consultas públicas, erros, latência, falhas de mídia e operações administrativas.
- Não armazenar dados pessoais desnecessários.

## 9. Critérios de aceite do módulo

- [ ] OWNER cria, edita, publica, despublica e arquiva cardápios.
- [ ] MANAGER executa as mesmas operações de catálogo permitidas pela matriz D-07.
- [ ] STAFF não cria nem edita conteúdo ou preço; altera somente disponibilidade.
- [ ] Um restaurante consegue manter vários cardápios publicados simultaneamente.
- [ ] Cada cardápio possui categorias e produtos próprios.
- [ ] Categorias podem ser ordenadas e desativadas.
- [ ] Produtos podem ser criados, editados e arquivados.
- [ ] Ingredientes são itens livres, individuais e ordenáveis.
- [ ] Adicionais têm preço explícito em centavos e podem ser ativados/desativados.
- [ ] Preços negativos e valores monetários em ponto flutuante são rejeitados/não utilizados.
- [ ] Produto indisponível é distinguido de produto arquivado.
- [ ] Imagens de produto são armazenadas no ImageKit por meio do backend, com referência no PostgreSQL.
- [ ] Usuário de outro restaurante não consegue consultar ou alterar recursos privados por troca de IDs.
- [ ] Cardápio DRAFT ou ARCHIVED não aparece publicamente.
- [ ] Restaurante não publicado/suspenso não expõe cardápio público.
- [ ] Página pública lista somente cardápios publicados, categorias ativas e produtos não arquivados.
- [ ] Produto indisponível continua identificável como indisponível.
- [ ] Consumidor consulta sem login.
- [ ] QR Code principal leva à página do restaurante.
- [ ] Frontend contempla loading, erro, vazio, sucesso e responsividade.
- [ ] Testes automatizados e validação ponta a ponta passam.
- [ ] Swagger e documentação refletem somente endpoints efetivamente implementados.

## 10. Fora do escopo deste módulo

Não implementar nesta entrega:
- Pedidos, carrinho, checkout ou pagamento.
- Integração com PDV/ERP ou controle de estoque.
- Reserva de mesas.
- Tamanhos, combos ou composição avançada de produtos.
- Promoções, cupons ou precificação por horário.
- Alternância automática de cardápios conforme horário.
- Publicação individual de produtos.
- QR Code por mesa.
- Descoberta global, avaliações e favoritos.
- Assistente gastronômico/IA.
- Modelos 3D/AR (o catálogo deve apenas preservar caminho de extensão futuro).
- Fidelidade ou Boraê Club.
- Aplicativos nativos.

## 11. Dependências e próximos módulos

O catálogo é a fonte de dados para:
- **Assistente gastronômico:** recomendar somente produtos reais, publicados e disponíveis, respeitando as cotas e regras do módulo de IA.
- **3D/AR:** associar modelo a produto em etapa posterior; não bloquear catálogo por isso.
- **Descoberta:** apresentar restaurantes e cardápios públicos.
- **Fidelidade:** usar dados de compras confirmadas quando esse módulo existir; catálogo não gera pontos.
- **Pedidos:** fora do MVP atual e sujeito a decisão/planejamento próprio.

## 12. Documentos relacionados

- [PRD](PRD.md)
- [Modelo de domínio](DOMAIN_MODEL.md)
- [Desenho da API](API_DESIGN.md)
- [Fluxos de tela](SCREEN_FLOWS.md)
- [Roadmap](ROADMAP.md)
- [Decisões aprovadas e pendências](OPEN_DECISIONS.md)
- [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md)

