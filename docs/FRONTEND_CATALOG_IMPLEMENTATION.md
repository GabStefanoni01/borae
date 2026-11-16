# Guia de implementação frontend — Catálogo e Cardápio Digital

**Responsável principal:** Renan (Angular)  
**PR de referência:** [#22 — Catálogo e Cardápio Digital](https://github.com/GabStefanoni01/borae/pull/22)  
**Branch de trabalho:** `docs/catalog-menu-module-plan`  
**Stack existente:** Angular 20 + TypeScript.

Este documento transforma a especificação de produto em tarefas concretas para o frontend. A implementação deve acontecer na branch da PR #22, mantendo o backend e o frontend integrados na mesma entrega. Não abrir uma PR paralela para este módulo.

## 1. Objetivo da entrega

Permitir que um restaurante gerencie seus cardápios pelo painel Boraê e que qualquer consumidor consulte o cardápio publicado por uma página pública responsiva, sem precisar entrar ou criar conta.

O trabalho deve consumir a API real do backend. Não entregar telas usando somente dados mockados ou o modo demo.

## 2. Antes de começar

1. Atualizar a branch `docs/catalog-menu-module-plan` e ler este guia, `MODULE_CATALOG.md`, `SCREEN_FLOWS.md` e `API.md`.
2. Inspecionar a estrutura atual em `frontend/src/app`, principalmente rotas, dashboard, componentes compartilhados, autenticação, interceptors, tema e estilos.
3. Reutilizar os padrões visuais e técnicos já existentes. Não criar um segundo sistema de autenticação, cliente HTTP ou layout.
4. Alinhar os contratos com o backend/Swagger da branch. Alguns endpoints descritos no planejamento ainda serão implementados; não presumir que já estejam disponíveis.
5. Desenvolver em incrementos pequenos, fazer commits na branch da PR #22 e manter a integração com o backend coordenada.

## 3. Área administrativa — telas e funcionalidades

### 3.1 Entrada e listagem de cardápios

Criar uma área **Cardápios** dentro do dashboard do restaurante selecionado.

A tela deve:
- Listar os cardápios daquele restaurante, sem misturar dados de outros restaurantes.
- Mostrar nome, descrição quando houver e status: Rascunho, Publicado ou Arquivado.
- Oferecer ação para criar cardápio.
- Permitir abrir/editar um cardápio e acessar sua gestão de categorias e produtos.
- Disponibilizar ações compatíveis com o status: publicar, despublicar e arquivar.
- Exibir estado vazio com explicação e CTA para criar o primeiro cardápio.
- Ter loading, erro com opção de tentar novamente e feedback de sucesso.

Um restaurante pode manter vários cardápios publicados ao mesmo tempo. Não implementar troca automática por horário.

### 3.2 Criar e editar cardápio

Formulário com:
- Nome obrigatório.
- Descrição opcional.

Comportamentos:
- Novo cardápio começa como `DRAFT`.
- Salvar e voltar à listagem/editor.
- Alertar antes de descartar alterações não salvas.
- Publicar somente após o backend validar as condições necessárias.
- Despublicar retorna o cardápio para `DRAFT`.
- Arquivar preserva os dados e exige confirmação clara.
- Edições salvas em cardápio publicado entram no ar imediatamente; não criar uma etapa extra de aprovação por edição.

### 3.3 Editor de categorias

Dentro do cardápio, permitir:
- Listar categorias e sua ordem.
- Criar categoria com nome obrigatório e descrição opcional.
- Editar/renomear.
- Ativar e desativar.
- Reordenar por ação explícita (drag-and-drop pode ser usado se já houver padrão/biblioteca adequada; sempre oferecer alternativa acessível).
- Mostrar quantidade de produtos associados, quando a API fornecer.

Categoria inativa não aparece no cardápio público. Não oferecer exclusão destrutiva de categoria com produtos associados; priorizar desativação.

### 3.4 Lista e formulário de produtos

O editor deve permitir visualizar produtos por categoria, buscar e filtrar quando fizer sentido. Cada item deve apresentar nome, preço, status e disponibilidade.

Campos do produto:
- Nome obrigatório.
- Descrição opcional.
- Preço em BRL, obrigatório e não negativo.
- Categoria pertencente ao cardápio atual.
- Imagem opcional.
- Ingredientes em lista ordenável.
- Adicionais opcionais.
- Estado de ciclo de vida (ativo/arquivado).
- Disponibilidade (disponível/indisponível).
- Ordem de exibição.

Ações:
- Criar e editar produto.
- Arquivar e reativar quando permitido pela API.
- Alterar disponibilidade.
- Reordenar produtos.
- Abrir prévia do produto.

**Preço:** a API recebe e devolve inteiros em centavos (`priceInCents`, `priceDeltaInCents`). O frontend deve converter/formatar para BRL na interface, sem usar ponto flutuante como contrato de persistência. Exibir, por exemplo, `R$ 25,90`.

### 3.5 Ingredientes

No formulário do produto:
- Adicionar ingrediente textual.
- Editar e remover um ingrediente.
- Reordenar a lista.
- Permitir salvar a lista vazia.
- Exibir os ingredientes na prévia pública.

Ingredientes são texto livre. Não criar catálogo global, seletor de alergênicos ou afirmação de que um prato é livre de contaminação cruzada.

### 3.6 Adicionais

No formulário do produto:
- Adicionar adicional com nome e acréscimo de preço.
- Editar nome e preço.
- Ativar/desativar.
- Reordenar.

Mostrar claramente o preço adicional, por exemplo, “Cheddar + R$ 3,00”. Adicionais inativos não aparecem como opções na apresentação pública. Nesta entrega, apenas cadastramos e exibimos os adicionais; não existe seleção/compra, carrinho ou cálculo de pedido.

### 3.7 Imagem do produto

- Permitir selecionar e enviar imagem.
- Mostrar prévia, estado de upload e erro.
- Permitir trocar/remover imagem.
- Aceitar apenas os formatos definidos pelo backend (JPEG, PNG e WebP).
- Exibir validações amigáveis de tamanho/formato, sem substituir a validação do servidor.
- Não colocar credenciais do ImageKit no Angular; upload deve passar pelo backend.
- Reutilizar o contrato de mídia que será implementado no backend, sem inventar endpoint.

### 3.8 Disponibilidade e papéis

A interface deve respeitar os papéis retornados pelo contexto de usuário/membership:
- `OWNER` e `MANAGER`: gerenciam cardápios, categorias, produtos, preços, ingredientes, adicionais, imagens e publicação.
- `STAFF`: pode alterar somente a disponibilidade operacional do produto.

Para STAFF, ocultar/desabilitar ações de edição de conteúdo, preço e publicação e manter acessível a ação rápida de disponibilidade. Isso é apenas UX: a autorização definitiva é do backend. Nunca confiar somente em guards ou esconder botões.

## 4. Página pública do restaurante

A página pública será acessada por slug e não exige login.

Implementar:
1. Cabeçalho com identidade pública do restaurante (nome, descrição e logo/capa quando disponíveis).
2. Lista/seleção dos cardápios publicados.
3. Navegação pelas categorias ativas do cardápio selecionado.
4. Lista de produtos elegíveis.
5. Detalhe do produto com imagem, descrição, preço, ingredientes e adicionais ativos com seus acréscimos.
6. Identificação visual clara de produto indisponível.

Regras de exibição:
- Não mostrar cardápios `DRAFT` ou `ARCHIVED`.
- Não mostrar categorias inativas.
- Não mostrar produtos arquivados.
- Produto indisponível pode continuar visível, mas deve estar claramente marcado como indisponível.
- Não expor informações internas, dados da equipe ou campos administrativos.
- Se o restaurante não estiver publicado ou estiver suspenso, respeitar a resposta da API e não exibir o catálogo.
- Consumidor consulta sem login.
- A página deve funcionar bem em celular e desktop, com imagens responsivas e lazy loading quando apropriado.

O QR Code principal do restaurante aponta para a página pública do restaurante. Não criar QR por mesa nem QR específico por cardápio nesta entrega.

## 5. Rotas e organização

Definir as rotas seguindo o padrão atual do projeto, sem impor nomes de arquivos antes de inspecionar a estrutura existente. Como referência funcional:
- Área autenticada: listagem de cardápios do restaurante selecionado.
- Área autenticada: editor de um cardápio (categorias e produtos).
- Área autenticada: criação/edição de produto.
- Área pública: página do restaurante por slug.

Separar responsabilidades em componentes e serviços Angular coerentes com a arquitetura atual. Centralizar chamadas HTTP do catálogo em serviços tipados; não espalhar URLs e transformação de payloads pelos componentes. Reutilizar interceptor, sessão, seleção de restaurante, guards e componentes compartilhados existentes.

## 6. Estados e qualidade obrigatórios

Todas as telas com dados remotos precisam cobrir:
- Loading sem duplicar submissões.
- Estado vazio com orientação clara.
- Erros de validação por campo quando possível.
- Erro de rede recuperável com opção de tentar novamente.
- Sucesso com feedback acessível.
- 401/sessão expirada conforme fluxo global existente.
- 403/sem permissão.
- 404/recurso inexistente ou removido.
- Proteção contra perda acidental de alterações não salvas.

Requisitos gerais:
- Layout responsivo; administração utilizável em telas pequenas.
- Acessibilidade por teclado, labels e foco visível.
- Não depender de hover.
- Não usar mocks no fluxo integrado.
- Não duplicar lógica de negócio que pertence ao backend.
- Não inventar estados ou contratos não definidos na API.

## 7. Sequência sugerida para o frontend

1. Inspecionar estrutura e preparar rotas/serviços tipados.
2. Integrar listagem, criação e edição de cardápios e categorias aos endpoints disponíveis.
3. Integrar produtos, ingredientes e adicionais assim que os contratos do backend estiverem prontos.
4. Integrar upload de imagem após definição do endpoint de mídia de produto.
5. Implementar a experiência pública quando o endpoint público estiver disponível.
6. Fazer validação integrada com backend e testes responsivos.

O frontend pode avançar em paralelo com o backend usando contratos alinhados, mas deve marcar claramente o que depende de endpoints ainda não entregues. Não publicar uma tela como funcional se estiver sustentada apenas por dados fictícios.

## 8. Fora do escopo

Não implementar nesta PR:
- Pedidos, carrinho, checkout, pagamento ou seleção efetiva de adicionais.
- Estoque, integração com PDV/ERP ou reservas.
- Tamanhos, combos, promoções ou preços por horário.
- Alternância automática de cardápio por horário.
- QR Code por mesa.
- Descoberta global, avaliações, favoritos, IA, fidelidade, Boraê Club ou 3D/AR.
- Cadastro/gestão de alergênicos certificados.

## 9. Critérios de aceite do frontend

- [ ] OWNER/MANAGER conseguem listar, criar e editar cardápios.
- [ ] OWNER/MANAGER conseguem publicar, despublicar e arquivar cardápios.
- [ ] Categorias podem ser criadas, editadas, ordenadas e desativadas.
- [ ] Produtos podem ser criados/editados/arquivados e reordenados.
- [ ] Ingredientes e adicionais podem ser administrados e ordenados.
- [ ] Preços são exibidos em BRL e trafegados como centavos inteiros.
- [ ] Upload/troca/remoção de imagem funciona pelo backend.
- [ ] STAFF consegue alterar disponibilidade, mas não conteúdo/preço/publicação.
- [ ] Página pública abre por slug sem autenticação.
- [ ] Página pública mostra somente dados elegíveis e indica indisponibilidade.
- [ ] Layout público é mobile-first e o painel é responsivo.
- [ ] Loading, vazio, erro, sucesso e sem permissão foram tratados.
- [ ] Integração utiliza API real e contratos documentados.
- [ ] Build e testes do frontend passam junto com a CI da PR #22.
