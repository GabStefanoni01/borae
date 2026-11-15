# Registro de Alterações: Catálogo, Cardápio Digital e Página Pública (Frontend)

**Modificações feitas por Renan Cristiano**
**Data da publicação: 15/11/2026**

- **Projeto:** Boraê
- **Entrega:** Interface Angular do módulo de Catálogo e Cardápio Digital — administração, publicação, QR Code e página pública
- **Backend consumido:** [PR #22](https://github.com/GabStefanoni01/borae/pull/22) — branch `docs/catalog-menu-module-plan` (não alterada)
- **Branch:** `feat/frontend-catalog-menu`, criada a partir de `main`; PR independente para `main`, a ser integrada depois da PR #22

---

## 1. Objetivo
Permitir que OWNER e MANAGER organizem cardápios, categorias e produtos (com ingredientes, adicionais e imagem) no painel, publiquem o perfil do restaurante e baixem o QR Code principal, e que qualquer consumidor consulte o cardápio publicado por uma página pública, sem login. STAFF consulta o catálogo e altera somente a disponibilidade dos produtos.

## 2. Escopo
Incluído: cardápios, categorias, produtos, ingredientes, adicionais, imagem do produto, disponibilidade, arquivamento/restauração, ordenação, publicação do restaurante, QR Code, página pública por slug, permissões visuais, estados de interface, simulação no modo demonstração e testes.

Fora do escopo (não implementado): pedidos, carrinho, checkout, pagamentos, estoque, reservas, PDV/ERP, QR por mesa, seleção de adicionais, descoberta, avaliações, favoritos, IA, fidelidade, Boraê Club e 3D/AR. Nenhum arquivo de `backend/` foi alterado.

## 3. Arquitetura implementada
- Tudo dentro do shell existente do painel (`/app`), reaproveitando `RestaurantContext` (restaurante selecionado e papel), `authInterceptor` (JWT), `AuthService`, `authGuard`, tema claro/escuro e componentes `.d-*`.
- Chamadas HTTP centralizadas em serviços tipados; os componentes não montam URLs.
- Nenhuma atualização otimista: a tela muda só depois da resposta do servidor (inclusive o switch de disponibilidade e a ordenação).
- O backend é a autoridade de permissões, estados, validação e isolamento. O frontend apenas antecipa erros comuns (nome vazio, preço inválido, repetição, formato e tamanho de imagem) e trata as respostas da API.
- Página pública fora do painel (`/r/:slug`), sem `authGuard`, sem JWT e com visual próprio mobile-first.

## 4. Componentes criados
| Arquivo | Função |
|---|---|
| `dashboard/catalog/menus.page.ts` | Listagem "Seus cardápios", criação, edição, publicar/despublicar/arquivar, arquivados |
| `dashboard/catalog/menu-form.component.ts` | Formulário de cardápio (criar/editar) |
| `dashboard/catalog/menu-editor.page.ts` | Editor do cardápio: estado, categorias, produtos por categoria, disponibilidade, ordenação, arquivamento e prévia |
| `dashboard/catalog/product-editor.page.ts` | Formulário de produto: dados, preço, categoria, ingredientes, adicionais e imagem |
| `dashboard/catalog/publication.page.ts` | Publicação do perfil do restaurante e QR Code |
| `dashboard/ui/feedback.ts` | `ToastService`/`bo-toasts` (avisos com `aria-live`) e `bo-panel-dialog` (formulários em `<dialog>`) |
| `shared/menu-product-card.component.ts` | Cartão de produto como o consumidor vê (página pública e prévia do painel) |
| `public-menu/public-menu.page.ts` | Cardápio digital público |

## 5. Services criados
- `CatalogService` (`dashboard/catalog/catalog.service.ts`): cardápios, categorias, produtos, disponibilidade, ordenação e imagem.
- `PublicMenuService` (`public-menu/public-menu.service.ts`): consulta pública por slug.
- `RestaurantProfileService` (existente) ganhou `publish`, `unpublish` e `downloadQrCode`.
- Utilitários: `shared/money.ts` (BRL ↔ centavos) e `dashboard/catalog/catalog-errors.ts` (mensagens por status e ação).

## 6. DTOs utilizados (conferidos no código da PR #22)
| Frontend | Backend |
|---|---|
| `Menu`, `MenuCreateRequest`, `MenuUpdateRequest` | `MenuResponse`, `CreateMenuRequest`, `UpdateMenuRequest` |
| `Category`, `CategoryCreateRequest`, `CategoryUpdateRequest` | `MenuCategoryResponse`, `CreateMenuCategoryRequest`, `UpdateMenuCategoryRequest` |
| `Product`, `ProductSaveRequest` (create/update), `Addon` | `MenuProductResponse` (+ `AddonResponse`), `Create/UpdateMenuProductRequest`, `ProductAddonRequest` |
| `ReorderProductsRequest` | `ReorderMenuProductsRequest` |
| `{ availability }` | `ChangeProductAvailabilityRequest` |
| `PublicRestaurantMenu`, `PublicMenu`, `PublicCategory`, `PublicProduct`, `PublicAddon` | `PublicRestaurantMenuResponse` e registros internos |
| `PublicationStatus`, `RestaurantProfile` | `RestaurantPublicationStatus`, `RestaurantProfileResponse` |

Observações de contrato:
- `POST /menus` e `POST /categories` exigem `displayOrder`; o frontend envia a próxima posição. `PUT /menus` aceita `displayOrder` nulo (mantém).
- `PUT /categories/{id}` exige `name`, `displayOrder` e `active`; ativar/desativar envia todos os campos.
- `PUT /categories/order` recebe a **lista** de IDs; `PUT /products/order` recebe `{ categoryId, productIds }` com **todos** os produtos da categoria (inclusive arquivados).
- A imagem do produto vem em `imageUrl`; o upload é multipart com o campo `file`.

## 7. Rotas administrativas
| Rota | Tela |
|---|---|
| `/app/cardapios` | Seus cardápios |
| `/app/cardapios/:menuId` | Editor do cardápio (`?categoria=` abre uma categoria) |
| `/app/cardapios/:menuId/produtos/novo` | Adicionar produto (`?categoria=` pré-seleciona) |
| `/app/cardapios/:menuId/produtos/:productId` | Editar produto |
| `/app/qr-code` | Página pública e QR Code |

`/app/categorias` e `/app/produtos` redirecionam para `/app/cardapios`: categorias e produtos pertencem a cada cardápio. Os itens planejados "Categorias" e "Produtos" saíram do menu lateral; "Cardápios" e "Página pública e QR" passaram a **Disponível**. O formulário de produto tem `canDeactivate` para confirmar o descarte de alterações.

## 8. Rota pública
`/r/:slug`, registrada antes do curinga em `app.routes.ts`, sem `authGuard`. O prefixo `/r/` coincide com o padrão `PUBLIC_RESTAURANT_URL_PREFIX=http://localhost:4200/r/` do backend, que é quem gera o QR Code. O `authInterceptor` não anexa JWT às rotas `/api/v1/public/` e um 401 nelas não encerra a sessão.

## 9. Cardápios
- Lista com nome, descrição, estado (Rascunho, Publicado, Arquivado, com cor e texto) e quantidade de categorias, obtida por `GET /categories` de cada cardápio, já que a API não envia a contagem.
- Esqueleto durante o carregamento; o erro de carregamento mostra "Não foi possível carregar seus cardápios." com "Tentar novamente" e **não** vira lista vazia.
- Estado vazio: "Seu primeiro cardápio começa aqui." com "Criar primeiro cardápio".
- Criar e editar em painel modal, com nome obrigatório e descrição opcional. Fechar com alterações pede confirmação.
- Publicar, despublicar (volta para rascunho) e arquivar (com confirmação). O 409 da publicação explica a regra: precisa haver uma categoria ativa com produto não arquivado.
- Cardápios arquivados ficam numa seção recolhida, só para consulta. A API não tem endpoint de desarquivar.

## 10. Categorias
- Lista numerada na ordem de exibição, com quantidade de produtos ativos e o selo "Inativa".
- Criar e editar em painel modal ("Dê um nome à categoria para agrupar produtos semelhantes."). O 409 mostra "A categoria já existe neste cardápio. Escolha outro nome."
- Ativar e desativar (sem exclusão destrutiva).
- Reordenar com botões "mover para cima/para baixo", acessíveis por teclado. Envia a lista completa e aplica a ordem devolvida. Em caso de erro, recarrega a ordem do servidor.

## 11. Produtos
- Produtos da categoria selecionada com miniatura, nome, descrição, preço em BRL, selos "Indisponível" e "Arquivado" (aparências diferentes), busca e "Mostrar arquivados".
- OWNER/MANAGER: adicionar, editar, arquivar (com confirmação), restaurar, reordenar, alterar disponibilidade e prévia.
- STAFF: consulta, prévia e switch de disponibilidade. Não vê editar, ordenar, arquivar, restaurar nem criar.
- Produto arquivado não abre para edição (o backend responde 409); o formulário explica e manda restaurar.
- Preço: digitação livre ("25,9", "25.90", "1.234,56"), formatado ao sair do campo e convertido para centavos inteiros com aritmética de strings (sem `float`). Valores negativos ou inválidos são recusados.

## 12. Ingredientes
Seção "Ingredientes" com o texto de apoio pedido. Permite adicionar (Enter ou botão), editar no próprio campo, remover, reordenar e salvar a lista vazia. A ordem informada é preservada, há aviso amigável para repetição e limite de 40. Não existe catálogo global nem selo de alergênicos. No público, aparecem como "Ingredientes informados pelo restaurante."

## 13. Adicionais
Seção "Adicionais" com nome, acréscimo em BRL (`priceDeltaInCents`), ativo/inativo e ordem. Cada linha mostra a prévia "Cheddar — + R$ 3,00". Limite de 30, aviso para repetição. Não há soma ao preço, seleção ou carrinho. Adicionais inativos não aparecem na prévia nem na página pública (a API pública já os omite).

## 14. Upload e remoção de imagens
- Somente pelos endpoints do backend (`POST/DELETE .../products/{id}/image`); nenhuma credencial do ImageKit no Angular.
- Pré-validação de formato (JPEG, PNG, WebP) e tamanho (10 MB). Progresso "Enviando imagem... N%". Sucesso: "Imagem atualizada com sucesso."; erro: "Não foi possível enviar a imagem. Confira o formato e o tamanho do arquivo." (413 mostra o limite).
- Produto existente: trocar ou remover tem efeito imediato. Produto novo: a imagem escolhida é enviada logo depois da criação. Se o envio falhar, o produto fica salvo e a tela abre a edição dele para nova tentativa.
- Depois da remoção, a imagem some da tela. Um 404 ("sem imagem") também limpa a prévia.

## 15. Publicação do restaurante
Card em `/app/qr-code` com o estado atual e os textos do handoff: "Seu restaurante ainda não está público." com "Publicar restaurante", e "Seu restaurante está publicado." com "Despublicar restaurante" (com confirmação) e "Ver página pública". Se o restaurante estiver suspenso, nenhuma ação é oferecida; o 409 é tratado e a tela recarrega o estado real. A tela explica que publicar o restaurante não publica cardápios.

## 16. QR Code
"QR Code do seu restaurante": prévia do PNG retornado por `GET /restaurants/{id}/qrcode` e botão "Baixar QR Code", que salva `qrcode-<nome-sanitizado>.png`. O QR não é gerado localmente e nenhum endereço é montado no frontend. Erro com "Tentar novamente". Quando o restaurante não está publicado, um aviso explica que o código abrirá a página indisponível. Somente OWNER e MANAGER.

## 17. Controle visual de permissões
`RestaurantContext` ganhou `canManageCatalog` (OWNER e MANAGER) e `canChangeAvailability`, derivados do papel real do vínculo (`GET /users/me/restaurants`). Esconder botões é só experiência: qualquer 403 mostra "Você não tem permissão para realizar esta ação." e nada é simulado.

## 18. Tratamento de erros
O backend não envia o texto dos erros (`server.error.include-message: never`), então `catalogErrorMessage(error, ação)` escolhe a mensagem por status e contexto: 0 (conexão), 400 (validação, ordem desatualizada, imagem), 401 (fluxo global de sessão expirada), 403, 404 ("Este conteúdo não está mais disponível."), 409 (regra específica da ação), 413 (tamanho) e 5xx. Stack traces e mensagens internas nunca são exibidos.

## 19. Responsividade
- Editor em duas colunas com *container query*: vira uma coluna quando o espaço útil fica abaixo de 900px, e as linhas de produto reorganizam as ações abaixo de 560px.
- Formulário de produto em duas colunas no desktop e uma no celular, com a barra de ações fixa no rodapé.
- Página pública mobile-first: abas de cardápios e categorias com rolagem horizontal, categorias fixas no topo, detalhe do produto em painel inferior no celular e modal no desktop. Conferida a 375px sem rolagem horizontal.

## 20. Acessibilidade
Labels em todos os campos (inclusive linhas de ingredientes e adicionais), `aria-label` descritivo em botões só com ícone, foco visível laranja, diálogos nativos (`<dialog>`, foco preso, Esc), avisos com `aria-live`, switch com `role="switch"`, estados com texto além da cor, produto indisponível anunciado no rótulo e `prefers-reduced-motion` respeitado em esqueletos, avisos e rolagem.

## 21. Testes
Executor: Vitest 3.2.4 + jsdom 26.1.0, pelo builder `@angular/build:unit-test`, que ainda é experimental no Angular 20. Comando: `npm test`.

| Arquivo | Cobertura |
|---|---|
| `shared/money.spec.ts` | Conversão de centavos, formatos aceitos/recusados, ida e volta |
| `dashboard/catalog/catalog.service.spec.ts` | Métodos, URLs e payloads de todos os endpoints (ordenação com lista completa, multipart) |
| `dashboard/catalog/catalog-errors.spec.ts` | 403, 409 por ação, 400, 404, 413, conexão, 5xx sem detalhes técnicos |
| `auth.interceptor.spec.ts` | Sem JWT na rota pública; 401 público não encerra sessão |
| `dashboard/catalog/menus.page.spec.ts` | Listar, vazio, erro, criar, editar, publicar, despublicar, arquivar, 409, OWNER/MANAGER/STAFF |
| `dashboard/catalog/menu-editor.page.spec.ts` | Categorias (criar, duplicidade, ativar/desativar, reordenar e restaurar ordem), disponibilidade sem otimismo, reordenar produtos, arquivar/restaurar, prévia, STAFF, cardápio arquivado |
| `dashboard/catalog/product-editor.page.spec.ts` | Criar com centavos, ingredientes e adicionais; validações; editar e 400; arquivado; imagem (enviar, substituir, erro de API, remover, formato, tamanho, progresso); STAFF |
| `dashboard/catalog/publication.page.spec.ts` | Publicar, despublicar com confirmação, suspenso, QR PNG e nome do arquivo, erro do QR, STAFF |
| `public-menu/public-menu.page.spec.ts` | Restaurante publicado, vários cardápios, categorias, indisponível, detalhe com adicionais, 404, sem cardápios, falha de conexão |

**Resultado:** 9 arquivos, **56 testes aprovados**.

Nota técnica: no pacote de testes, algumas constantes de módulo ainda não estavam inicializadas quando os componentes eram construídos. Por isso as telas leem `CATALOG_LIMITS` e os rótulos de estado por *getters*, o que funciona igual no app e nos testes.

Também foram verificados manualmente no navegador, em modo demonstração: todos os fluxos acima como OWNER e STAFF, página pública a 375px e 1024px, e console sem erros.

## 22. Resultado do build
- `npm run build` (produção): sem erros e sem avisos de budget.
- Build `demo`: sem erros.
- O build de produção não contém dados do modo demonstração (verificado por busca no `dist`).

## 23. Pendências de integração
- **Não testado contra o backend real:** a PR #22 não está na `main` nem publicada, e o ambiente local não tem Java 21, PostgreSQL ou Docker. A validação foi feita com testes automatizados e com o simulador do modo demonstração, que reproduz as regras dos services da PR #22.
- Upload real no ImageKit não testado.
- Logo e capa do restaurante na página pública: `PublicRestaurantMenuResponse` não traz essas imagens, então a página mostra as iniciais do restaurante.
- Endereço e horários não aparecem na página pública porque o DTO público não os inclui.

## 24. Configurações necessárias no backend
- `PUBLIC_RESTAURANT_URL_PREFIX` no ambiente publicado: `https://borae-web.onrender.com/r/` (ou o domínio oficial + `/r/`). Sem isso, o QR Code aponta para `localhost`.
- `CORS_ALLOWED_ORIGINS` incluindo a origem do frontend publicado (`https://borae-web.onrender.com`).
- No Static Site do Render, manter a regra de rewrite `/*` → `/index.html`, para que `/r/:slug` abra direto pelo QR Code.
- Sugestões para versões futuras (não implementadas): logo/capa e endereço no DTO público; contagem de categorias e produtos em `MenuResponse`; endpoint para desarquivar cardápio.

## 25. Arquivos criados
- `frontend/src/app/dashboard/catalog/`: `catalog.models.ts`, `catalog.service.ts`, `catalog-errors.ts`, `menu-form.component.ts`, `menus.page.ts`, `menu-editor.page.ts`, `product-editor.page.ts`, `publication.page.ts` e os `*.spec.ts` correspondentes
- `frontend/src/app/dashboard/ui/feedback.ts`
- `frontend/src/app/dashboard/styles/catalog.css`, `catalog-editor.css`, `catalog-product.css`
- `frontend/src/app/public-menu/`: `public-menu.page.ts`, `public-menu.service.ts`, `public-menu.page.css`, `public-menu.extras.css`, `public-menu.page.spec.ts`
- `frontend/src/app/shared/money.ts`, `money.spec.ts`, `menu-product-card.component.ts`
- `frontend/src/app/auth.interceptor.spec.ts`
- `frontend/src/app/demo/demo-catalog.ts`
- `frontend/src/testing/test-setup.ts`, `catalog-fixtures.ts`
- `frontend/tsconfig.spec.json`
- `docs/changes/frontend-catalog-menu-2026-11-15.md`

## 26. Arquivos modificados
- `frontend/src/app/app.routes.ts` (rota `/r/:slug`)
- `frontend/src/app/auth.interceptor.ts` (rotas públicas sem JWT)
- `frontend/src/app/dashboard/dashboard.routes.ts`, `modules.ts`, `restaurant-context.service.ts`
- `frontend/src/app/dashboard/shell/dashboard-shell.component.ts/.html` (avisos e estilos do catálogo)
- `frontend/src/app/dashboard/profile/restaurant-profile.service.ts` (publish, unpublish, QR Code)
- `frontend/src/app/demo/demo-api.interceptor.ts`, `demo-data.ts` (simulação; Cantina publicada)
- `frontend/src/styles.css` (cartão de produto `.mp-*`)
- `frontend/angular.json`, `package.json`, `package-lock.json`: alvo `test` e as dependências de desenvolvimento `vitest` e `jsdom`, sem dependências de execução novas

## 27. Branch
`feat/frontend-catalog-menu`, a partir de `main` (base e destino confirmados com o responsável). Não há commits na branch da PR #22.

## 28. Commits
1. `test(frontend): set up Vitest unit test runner`
2. `feat(frontend): add catalog management, restaurant publication and QR code`
3. `feat(frontend): add public restaurant menu`
4. `feat(frontend): simulate catalog endpoints in demo mode`
5. `test(frontend): cover catalog workflows, permissions and public menu`
6. `docs(frontend): document catalog implementation`

## 29. Pull Request
PR em rascunho de `feat/frontend-catalog-menu` para `main`, título `feat(frontend): implement catalog management and public digital menu`. **Sem merge automático**: integrar depois da PR #22 e da validação conjunta.

## 30. Próximas etapas
1. Fazer o merge da PR #22 e publicar o backend com `PUBLIC_RESTAURANT_URL_PREFIX` e CORS configurados.
2. Validar de ponta a ponta contra a API real, incluindo upload no ImageKit e a leitura do QR Code por celular.
3. Decidir com o backend sobre logo, capa e endereço no DTO público.
4. Avaliar pré-renderização (SSG) de `/r/:slug` para SEO e compartilhamento em redes sociais.
