# Especificação Técnica e Arquitetural — Editor Inteligente de Cardápios em Tempo Real

**Status:** especificação definitiva proposta para implementação (ainda **não implementada** no código).  
**Projeto:** Boraê  
**Responsável pelo escopo frontend (Angular):** Renan Cristiano  
**Responsável pelo backend (Spring Boot):** Gabriel  
**Branch de especificação:** `docs/menu-visual-editor-ai-spec` (base `main`)  
**PR de referência:** #30 (`docs/3D_DISH_CAPTURE_WORKFLOW.md`), #27 (`feat/3d-ar-experiences`), #28 (`feat/frontend-3d-ar`), #22 (`catalog`), #29 (`feat/ai-gastronomic-assistant` — referência de infraestrutura IA em andamento)  
**Data:** 18/11/2026

> **Regra de status:** tudo o que estiver marcado como **Implementado** reflete código confirmado na branch `main`. Tudo o que trata do Editor Visual em Tempo Real, Branding, Banners, Múltiplos Cardápios compartilhados, Assistente B2B Gemini/Grok e Geração de Banners está **Planejado / Proposto** nesta especificação e **não deve ser tratado como disponível** até conclusão de backend, frontend e testes.

---

## 1. Relatório Técnico da Arquitetura Atual e Lacunas

### 1.1 Arquitetura atual identificada

#### Frontend Angular (`frontend/src/app/`)
- **Shell e Navegação (`dashboard/modules.ts`, `dashboard/dashboard.routes.ts`, `dashboard/shell/dashboard-shell.component.*`):**
  - `RestaurantContext` gerencia restaurante selecionado, papel (`OWNER`, `MANAGER`, `STAFF`) e flags de conveniência (`canEditProfile`, `canManageCatalog`, `canChangeAvailability`).
  - **Meu Restaurante** (`/app/restaurante`, `RestaurantPage`) contém 3 abas: `Perfil` (`ProfileFormPage`), `Horários` (`BusinessHoursPage`) e `Imagens` (`MediaPage` — upload de `LOGO` e `PHOTO`).
  - **Cardápios** (`/app/cardapios`, `MenusPage`) exibe apenas uma grade simples de cards (`DRAFT`, `PUBLISHED`, `ARCHIVED`).
  - **Editor de Cardápio** (`/app/cardapios/:menuId`, `MenuEditorPage`) possui apenas duas colunas estáticas (Categorias à esquerda, lista de Produtos à direita). A única prévia disponível é um modal isolado de um único produto (`bo-menu-product-card`).
  - **Editor de Produto** (`/app/cardapios/:menuId/produtos/:productId`, `ProductEditorPage`) abre em rota separada (saindo do editor do cardápio) e contém o componente `ProductModel3dComponent` (`bo-product-model-3d`) para gestão do modelo 3D/AR.
  - **Página Pública e QR Code** (`/app/qr-code`, `PublicationPage`; rota pública `/r/:slug`, `PublicMenuPage`) renderiza um layout fixo usando tokens CSS estáticos (`public-menu.page.css`) e o componente compartilhado `MenuProductCardComponent`.
- **Modo Demonstração (`demo/demo-api.interceptor.ts`, `demo/demo-catalog.ts`):** intercepta chamadas para `API_URL` quando `environment.demo === true`.

#### Backend Spring Boot (`backend/src/main/java/com/borae/`)
- **Mídia (`identity/RestaurantMedia*`, `ImageKitRestaurantObjectStorage`, `RestaurantImageProcessor`, `V5`, `V8`):**
  - `RestaurantMediaType` aceita `LOGO`, `PHOTO` e `PRODUCT_IMAGE`.
  - `RestaurantImageProcessor` valida formato real (JPEG, PNG, WebP), corrige orientação EXIF, redimensiona até 2.000 px, recomprime e remove metadados antes de enviar ao **ImageKit** (`RestaurantObjectStorage`).
  - Limites atuais: `LOGO` até 5 MB (1 por restaurante via índice único `uq_restaurant_media_single_logo`), `PHOTO` até 10 MB (limite 100 por restaurante), `PRODUCT_IMAGE` até 10 MB (1:1 via `menu_product_image`).
- **Catálogo (`catalog/Menu*`, `MenuCategory*`, `MenuProduct*`, `V6`, `V7`):**
  - `Menu` (`restaurant_menu`): `id`, `restaurant_id`, `name` (120), `description` (500), `status` (`DRAFT`, `PUBLISHED`, `ARCHIVED`), `display_order`.
  - `MenuCategory` (`menu_category`): pertence estritamente a **um** `Menu` (`menu_id`), com `name`, `description`, `active`, `display_order`.
  - `MenuProduct` (`menu_product`): pertence estritamente a **uma** `MenuCategory` (`category_id`), com `name`, `description`, `price_in_cents`, `lifecycle_status` (`ACTIVE`/`ARCHIVED`), `availability` (`AVAILABLE`/`UNAVAILABLE`), `display_order`, `ingredients` (`ElementCollection`), `addons` (`ElementCollection`), `image` (`@OneToOne` via `menu_product_image`) e `Product3DModel` (via `menu_product_3d_model`, `V9`).
- **Consulta Pública (`PublicRestaurantMenuController`, `PublicRestaurantMenuService`, `PublicRestaurantMenuResponse`):**
  - `GET /api/v1/public/restaurants/{slug}/menu` retorna dados básicos do restaurante (`id`, `name`, `slug`, `description`, `phone`, `whatsapp`, `instagram`, `website`, `category`) e `menus` (`PUBLISHED`), `categories` (`active=true`), `products` (`ACTIVE`, com `imageUrl` e `model3d` se `PUBLISHED`).
- **IA em andamento na PR #29 (`origin/feat/ai-gastronomic-assistant`):**
  - Possui `GeminiFlashLiteProvider` usando `RestClient` para `https://generativelanguage.googleapis.com/v1beta/interactions`, `AssistantQuotaService` (PostgreSQL atômico), `AssistantIpRateLimitService` e `AssistantMetricsService`. Contudo, é voltada exclusivamente ao **consumidor final no cardápio público** (sem fallback Grok, sem geração de cardápio/branding B2B, sem geração de banners).

### 1.2 O que pode ser reaproveitado diretamente

1. **Infraestrutura de Storage e Processamento de Imagem:** `RestaurantObjectStorage`, `ImageKitRestaurantObjectStorage` e `RestaurantImageProcessor` (apenas ajustando limites/dimensões específicas para banner quando necessário).
2. **Entidades e Regras de Catálogo/Segurança:** `Menu`, `MenuCategory`, `MenuProduct`, `Product3DModel`, validação multi-tenant e autorização por `RestaurantMembership` (`OWNER`/`MANAGER`/`STAFF`).
3. **Componentes Compartilhados Angular:** `MenuProductCardComponent`, `Model3dViewerComponent`, `ProductModel3dComponent`, `BrandLogoComponent`, `IconComponent`, `money.ts` (`formatCents`, `parseBrlToCents`), `PanelDialogComponent`, `ConfirmDialogComponent`, `ToastService`, `RestaurantContext`.
4. **Padrão de Cliente HTTP Stateless para Gemini:** `RestClient` com timeouts explícitos, `ObjectMapper` com schema JSON validado e flags de ativação/aprovação (`GEMINI_ENABLED`, `GEMINI_DATA_PROCESSING_APPROVED`) observados na PR #29.

### 1.3 Problemas, gargalos e conflitos de especificação encontrados

| # | Problema / Achado | Impacto | Resolução definida nesta especificação |
|---|---|---|---|
| **P1** | **Conflito com decisão D-03 (`OPEN_DECISIONS.md`):** D-03 define que alterações em um cardápio `PUBLISHED` entram no ar imediatamente ao salvar, sem prévia/rascunho separado. | Impossibilita "visualizar resultado antes de publicar", "diferenciar rascunho vs publicado" e "autosave contínuo" sem quebrar o cardápio ao vivo para clientes. | Introduzir **Draft Staging para configuração visual/editorial e rascunho de layout**, além de modelo claro de **Publicação Explícita de Alterações**, exigindo **adendo formal à D-03** (ver Seção 10). |
| **P2** | **Página Pública não recebe Logo nem Mídia do Restaurante:** `PublicRestaurantMenuResponse` não inclui `logoUrl`, `bannerUrl`, nem configurações visuais (cores, tipografia, layout). `PublicMenuPage` hoje exibe apenas iniciais do nome. | Sem expor logo, banner e tema no DTO público, a prévia do editor em tempo real e o cardápio público real divergiriam. | Estender `PublicRestaurantMenuResponse` com `logoUrl`, `branding` e `banner` publicados por cardápio/restaurante. |
| **P3** | **Acoplamento estrito `Menu -> Category -> Product` (`V6`/`V7`):** cada produto pertence a uma única categoria de um único cardápio. | Para ter o mesmo prato no "Cardápio Principal" e no "Cardápio Executivo" ou "Delivery", o restaurante teria que duplicar produto, foto e modelo 3D. | Especificar evolução híbrida segura: **Biblioteca de Mídia compartilhada** + **Biblioteca de Produtos / Vínculo N:N ou Importação com sincronização opcional**, preservando `Product3DModel` único por produto base (Seção 3.4). |
| **P4** | **Ausência de fallback de IA e de geração de banners:** D-10 prevê apenas Gemini Flash-Lite para texto do consumidor e proíbe persistência de conversa. | O editor exige resiliência (Gemini + fallback xAI Grok), contexto contínuo durante a sessão de edição e capacidade distinta para imagem de banner. | Definir gateway multi-provedor `MenuAiTextGateway` (Gemini primário -> Grok fallback), `MenuBannerImageGateway` separado e **adendo formal à D-10/D-11** para escopo B2B autenticado (Seção 5). |
| **P5** | **Navegação fragmentada:** Imagens em `Meu Restaurante`, cardápios em `/app/cardapios`, produto em rota separada `/produtos/:id`, publicação em `/app/qr-code`. | Quebra o fluxo de criação visual contínua e obriga trocas de tela. | Consolidar tudo em **Meu Cardápio** (`/app/cardapios`), redirecionando `/app/restaurante/imagens` com compatibilidade e mantendo acesso direto à Experiência 3D (Seção 3.1 e Seção 8). |
| **P6** | **Sem controle de concorrência (`@Version`):** `Menu` não possui lock otimista em `V6`. | Autosave de dois gerentes simultâneos sobrescreveria alterações silenciosamente. | Adicionar `version` (`@Version` / `ETag` / `expectedVersion`) nas entidades de rascunho e cardápio. |

---

## 2. Arquitetura Alvo do Novo Módulo "Meu Cardápio"

### 2.1 Visão geral em camadas

```
┌─────────────────────────────────────────────────────────────────────────┐
│ FRONTEND ANGULAR (/app/cardapios) — Responsável: Renan Cristiano        │
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌───────────────┐ │
│  │ Workspace Lateral    │  │ Stage / Prévia Real  │  │ Assistente IA │ │
│  │ (Abas Contextuais)   │  │ (Fiel ao /r/:slug)   │  │ (Drawer/Painel│ │
│  │                      │  │                      │  │  Contextual)  │ │
│  │ • Estrutura & Pratos │◄─┼─► Signal Store       │◄─┼─► Preview     │ │
│  │ • Identidade & Cores │  │   + Undo/Redo Stack  │  │   de Sugestões│ │
│  │ • Banner & Mídia     │  │   + Autosave Queue   │  │   (Diff/Aceite│ │
│  │ • Cardápios          │  │   + Viewport Switch  │  │    Parcial)   │ │
│  └──────────┬───────────┘  └──────────┬───────────┘  └───────┬───────┘ │
└─────────────┼─────────────────────────┼──────────────────────┼─────────┘
              │ HTTPS REST + JWT        │                      │
              ▼                         ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ BACKEND SPRING BOOT (/api/v1) — Responsável: Gabriel                    │
│                                                                         │
│  • Segurança: JWT + Membership + Papel (OWNER/MANAGER vs STAFF)         │
│  • MenuWorkspaceService: Draft vs Published snapshots + Lock Otimista   │
│  • RestaurantMediaService + ImageKit: Logo, Banner, Galeria, Produtos   │
│  • CatalogService: Cardápios, Categorias, Produtos, Biblioteca          │
│  • MenuAiAssistantService:                                              │
│      ├── MenuAiTextGateway (Gemini Primário ──► xAI Grok Fallback)      │
│      ├── MenuBannerImageGateway (Provedor Imagem ──► ImageKit)          │
│      └── Quota & Rate Limit Service (B2B por restaurante)               │
│  • Product3DModelService (PR #27 / PR #30 — preservado integralmente)   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Princípio de fidelidade entre Prévia e Cardápio Público

A prévia em tempo real no editor **não deve ser um mockup separado**. Deve existir um **componente de renderização único compartilhado** (por exemplo `MenuRendererComponent` / `<bo-menu-canvas>`), utilizado tanto:
1. No **Editor Visual** (`MenuEditorWorkspacePage`), recebendo um `MenuRenderModel` em memória (combinando rascunho atual + eventual proposta temporária da IA em preview).
2. Na **Página Pública** (`/r/:slug`, `PublicMenuPage`), recebendo o `PublicRestaurantMenuResponse` oficial retornado pelo backend.

Isso garante por construção que cores, tipografia, banner, posicionamento de imagem, layout de categorias, layout de cards, badges de indisponibilidade e botões **"Ver em 3D"** tenham exatamente a mesma aparência antes e depois da publicação.

---

## 3. Reorganização da Navegação e Funcionalidades Frontend

### 3.1 Reorganização de "Meu Restaurante" → "Meu Cardápio"

| Área Atual | Novo Destino / Organização | Compatibilidade |
|---|---|---|
| Sidebar: `Cardápios` (`/app/cardapios`) | Renomear rótulo na sidebar para **`Meu cardápio`** (rota `/app/cardapios` preservada). | Mantém URLs existentes. |
| `Meu restaurante` → aba `Imagens` (`/app/restaurante/imagens`) | Incorporada à aba **`Banner & Mídia`** dentro de `Meu cardápio` (centralizando Logo, Banner, Fotos do restaurante e Imagens de produtos em uma única Biblioteca de Mídia). | Rota `/app/restaurante/imagens` passa a redirecionar para `/app/cardapios/midia` (ou exibe aviso com botão direto), evitando links quebrados. |
| `Meu restaurante` (`/app/restaurante`) | Permanece focado em **Perfil** (nome, slug, categoria, descrição, endereço, contatos) e **Horários**. | Sem quebra no `ProfileFormPage` ou `BusinessHoursPage`. |
| `Página pública e QR` (`/app/qr-code`) | Mantém acesso direto na sidebar **e** ganha botão rápido **"Publicar e QR Code"** no topo do Workspace de `Meu cardápio`. | Preserva `PublicationPage`. |
| `Experiências 3D` / `Assistente IA` (`modules.ts`) | `Experiências 3D` permanece integrada em cada produto dentro do editor (e futura captura guiada da PR #30); `Assistente IA` passa a ter entrada direta no painel do editor para OWNER/MANAGER. | Não mistura captura 3D com geração de cardápio. |

### 3.2 Estrutura do Workspace "Meu Cardápio" (`/app/cardapios` e `/app/cardapios/:menuId`)

#### Visão Geral / Seletor de Cardápios (`/app/cardapios`)
- Ao abrir `Meu cardápio`, se o restaurante já possuir cardápios, pode abrir diretamente o cardápio principal/último editado no workspace com seletor rápido de cardápio no topo (dropdown/tabs: *Cardápio Principal*, *Bebidas*, *Sobremesas*, *Executivo*, *Delivery*, *Sazonal*), além de botão **"Gerenciar todos os cardápios"** e **"Novo cardápio"**.
- Exibe distintivos claros por cardápio:
  - `Rascunho` (`DRAFT`) — nunca publicado.
  - `Publicado` (`PUBLISHED`) — ao vivo e sem pendências.
  - `Publicado (com rascunho pendente)` (`PUBLISHED` com `hasUnpublishedChanges = true`) — há edições salvas em rascunho aguardando publicação explícita.
  - `Arquivado` (`ARCHIVED`) — somente leitura.

#### Barra Superior do Editor (Top Studio Bar)
- **Seletor de cardápio ativo** + criação rápida de novo cardápio.
- **Indicador de Autosave:** `Salvo às HH:mm` | `Salvando alterações...` | `Alterações locais não sincronizadas` | `Erro ao salvar (Tentar novamente)`.
- **Controles de Histórico:** `Desfazer (Ctrl+Z)` e `Refazer (Ctrl+Shift+Z)`.
- **Seletor de Proporção / Viewport da Prévia:**
  - `Celular` (`375px` — padrão mobile-first para QR Code)
  - `Tablet` (`768px`)
  - `Desktop` (`100%` / `1200px`)
- **Indicador de Modo / Rascunho vs Publicado:** alternador rápido *"Visualizando: Rascunho atual"* vs *"Comparar com versão publicada"*.
- **Ação Principal:** `Publicar alterações` (ou `Publicar cardápio` se estiver em `DRAFT`), sempre sob ação explícita de `OWNER` ou `MANAGER`.

#### Painel Lateral Esquerdo (4 Abas Contextuais Simples)
1. **Conteúdo (Categorias & Produtos):**
   - Criar/renomear/ativar/desativar/reordenar categorias.
   - Criar/editar/reordenar/arquivar produtos e alternar disponibilidade rápida (`AVAILABLE`/`UNAVAILABLE` — único controle acessível a `STAFF`).
   - Ao clicar em um produto, abre **Inspetor de Produto** em gaveta lateral ou sub-painel sem sair da prévia (reflete nome, preço, descrição, foto, ingredientes e adicionais em tempo real na prévia!).
   - Dentro do Inspetor de Produto há uma aba/seção dedicada **"Experiência 3D"** preservando o `ProductModel3dComponent` (e futuro fluxo guiado da PR #30).
   - Ação **"Importar itens de outro cardápio / Biblioteca"** para evitar retrabalho em múltiplos cardápios.
2. **Identidade & Layout (Branding):**
   - **Presets inteligentes** (ex.: *Bistrô Sofisticado*, *Hamburgueria Urbana*, *Café Aconchegante*, *Pizzaria Tradicional*) + personalização manual completa.
   - **Cores:** cor primária (destaques/botões), cor secundária, fundo da página, fundo dos cards, cor do texto principal e secundário — com **alerta automático de contraste insuficiente** (WCAG AA) para evitar textos ilegíveis.
   - **Tipografia:** pares curados (ex.: *Plus Jakarta Sans + Inter*, *Playfair Display + Inter*, *Fraunces + DM Sans*, *Space Grotesk + Inter*) e escala de tamanho (`Compacto`, `Padrão`, `Confortável`).
   - **Disposição & Layout:**
     - Estilo de navegação de categorias: `Abas horizontais fixas`, `Lista contínua com âncoras` ou `Seções expansíveis (Acordeão)`.
     - Disposição dos produtos: `Lista editorial` (foto lateral), `Cards em grade` (2 colunas desktop / 1 mobile), `Destaque fotográfico` (card grande) ou `Compacto sem foto dominante`.
     - Posição da imagem no card: `Esquerda`, `Direita`, `Topo`.
   - **Escopo da identidade:** opção *"Aplicar como identidade padrão do restaurante"* (para consistência entre todos os cardápios) ou *"Personalizar apenas este cardápio"*.
3. **Banner & Mídia (Unificando `MediaPage`):**
   - **Gerenciamento do Banner do Cardápio:**
     - Upload direto (JPEG, PNG, WebP até 10 MB) ou escolha da Biblioteca de Mídia ou solicitação ao Assistente IA.
     - **Ajuste de enquadramento (Focal Point & Zoom):** arrastar/selecionar ponto focal X/Y (`0%–100%`), zoom (`1.0x–1.8x`), proporção automática adaptativa (`16:9` desktop / `16:10` mobile) e altura (`Compacto`, `Médio`, `Destaque`).
     - **Sobreposição (Overlay) e Textos:** controle de opacidade do escurecimento/gradiente (`0%–80%`) para legibilidade, toggle de exibição de título, subtítulo e selo sobre o banner, com prévia instantânea.
     - **Guia de qualidade inline:** *"Recomendado: imagem horizontal com pelo menos 1200×480 px (proporção ~2.5:1), formato JPEG, PNG ou WebP até 10 MB. Evite textos embutidos na foto."*
   - **Biblioteca de Mídia do Restaurante:**
     - Logo do restaurante (1 por restaurante, até 5 MB — migrado de `MediaPage`).
     - Galeria de fotos do restaurante e pratos (até 100 fotos — migrado de `MediaPage`), permitindo **reutilizar qualquer foto da biblioteca como imagem de produto ou banner** sem fazer novo upload!
4. **Assistente IA (Painel Direito ou Contextual):**
   - Interface de chat contextual + botões de ação rápida (*"Sugerir estrutura de cardápio"*, *"Gerar identidade visual"*, *"Melhorar descrições"*, *"Criar proposta de banner"*).
   - Mostra claramente o contexto atual detectado (nome do restaurante, categoria gastronômica, cardápio aberto, quantidade de itens).
   - **Fluxo obrigatório de controle manual:** toda resposta da IA gera um cartão de **Proposta Estruturada (`AiProposal`)** com botão **`Pré-visualizar na tela`** (aplica temporariamente sobre a prévia com barra laranja *"Modo Pré-visualização de Sugestão IA"*), checkboxes para **`Aceitar parcialmente`** (ex.: aceitar só a paleta de cores, ou só 3 dos 5 pratos sugeridos, ou só a nova descrição), **`Aceitar tudo`**, **`Editar antes de salvar`** ou **`Descartar`**.

### 3.3 Comportamento para papel `STAFF`
- Seguindo D-07 (`OPEN_DECISIONS.md` e `MODULE_CATALOG.md`): `STAFF` continua acessando o cardápio em modo **Somente Consulta + Toggle de Disponibilidade** (`AVAILABLE` / `UNAVAILABLE`). As abas de Branding, Banner, Assistente IA e ações de criação/edição/publicação ficam ocultas no Angular e bloqueadas com `403 Forbidden` no backend.

### 3.4 Estratégia para Múltiplos Cardápios sem Duplicação

Considerando o schema atual (`V6` e `V7`: `restaurant_menu -> menu_category -> menu_product`), propõe-se uma estratégia em **duas camadas**, compatível com o código atual:

1. **Camada de Mídia Compartilhada (Imediata):**
   - Permitir que `menu_product_image` e `menu_banner` referenciem qualquer `RestaurantMedia` pertencente ao mesmo `restaurant_id` (seleção pela Biblioteca de Mídia), eliminando re-uploads no ImageKit.
   - Nota técnica: ajustar a exclusão de produto/imagem para só deletar o arquivo no ImageKit se nenhuma outra referência (`menu_product_image`, `menu_visual_config`) apontar para aquele `media_id` (reference counting / query de uso).
2. **Camada de Catálogo Reutilizável (Evolução de Modelo de Dados):**
   - **Opção Recomendada (Catálogo Base do Restaurante + Vínculo por Cardápio):**
     - Criar conceito/tabela `catalog_item` (ou promover o produto a entidade do restaurante `restaurant_product` com `image`, `Product3DModel`, `ingredients`, `addons` padrão) e tabela associativa `menu_category_item` (`id`, `category_id`, `catalog_item_id`, `display_order`, `price_override_in_cents` opcional, `availability_override` opcional).
     - **Vantagem:** Um hambúrguer com foto e modelo 3D gerado na PR #30 pode aparecer no *Cardápio Principal*, no *Cardápio Executivo* e no *Cardápio de Delivery* sem duplicar o modelo 3D nem recadastrar ingredientes. Se o restaurante alterar o nome ou a foto base, reflete onde estiver vinculado (ou permite override específico por cardápio).
   - **Alternativa Incremental de Curto Prazo (se quiserem evitar refatoração imediata de `V7`):**
     - Manter `Menu -> MenuCategory -> MenuProduct` e oferecer endpoint de **Importar/Clonar itens de outro cardápio** (`POST .../menus/{menuId}/import`) reutilizando a mesma `RestaurantMedia` (`media_id`) e opcionalmente vinculando `source_product_id` para sincronização futura.
   - **Regras de ativação e disponibilidade entre múltiplos cardápios:**
     - Cada `Menu` mantém status independente (`DRAFT`, `PUBLISHED`, `ARCHIVED`) e `display_order` na página pública (D-02).
     - `isDefault`: opcionalmente um cardápio publicado pode ser marcado como cardápio padrão inicial ao abrir `/r/:slug`.
     - O QR Code principal continua apontando para `/r/:slug` (regra D-02), exibindo todos os cardápios `PUBLISHED`.

---

## 4. Personalização Visual, Autosave, Draft/Publish e Undo/Redo

### 4.1 Modelo de Documento Visual (`MenuVisualConfig`)

Toda a personalização visual é representada por um documento JSON versionado e validado por schema no backend (`jsonb` no PostgreSQL), separado do conteúdo transacional de preços/produtos:

```json
{
  "schemaVersion": 1,
  "inheritRestaurantTheme": true,
  "theme": {
    "presetId": "warm-bistro",
    "colors": {
      "primary": "#FF7200",
      "secondary": "#191919",
      "background": "#FFFFFF",
      "surface": "#FFF9F2",
      "textPrimary": "#191919",
      "textSecondary": "#666666"
    },
    "typography": {
      "headingFont": "Plus Jakarta Sans",
      "bodyFont": "Inter",
      "scale": "DEFAULT"
    }
  },
  "layout": {
    "categoryStyle": "TABS",
    "productLayout": "EDITORIAL_LIST",
    "imagePosition": "RIGHT",
    "showUnavailableProducts": true,
    "showProductDescriptions": true,
    "showIngredientsPreview": false
  },
  "banner": {
    "enabled": true,
    "mediaId": "uuid-da-restaurant-media-ou-null",
    "focalPointX": 50,
    "focalPointY": 50,
    "zoom": 1.0,
    "heightPreset": "MEDIUM",
    "overlayOpacity": 45,
    "showTextOverlay": true,
    "overlayTitle": "Cardápio Principal",
    "overlaySubtitle": "Sabores preparados na hora",
    "overlayBadge": "Feito na casa"
  }
}
```

**Regras de validação no Backend:**
- Cores devem ser hex válidos (`^#[0-9A-Fa-f]{6}$`).
- Fontes devem pertencer a uma **allowlist fixa** de fontes seguras/carregadas pelo app (`Plus Jakarta Sans`, `Inter`, `Playfair Display`, `Fraunces`, `DM Sans`, `Space Grotesk`, `Manrope`). Nunca aceitar URLs de CSS externo arbitrárias (prevenção de XSS/CSS injection).
- `focalPointX` e `focalPointY` entre `0` e `100`; `zoom` entre `1.0` e `2.0`; `overlayOpacity` entre `0` e `85`.
- `mediaId` (quando informado) deve existir em `restaurant_media` e pertencer ao mesmo `restaurant_id`!
- Textos do overlay sanitizados (sem HTML).

### 4.2 Separação Rascunho (`DRAFT`) vs Publicado (`PUBLISHED`) e Versionamento

Para atender ao requisito *"O restaurante deverá visualizar o resultado antes de publicar"* e *"Diferenciar claramente alterações em rascunho das alterações já publicadas"*:

1. Cada `Menu` (e opcionalmente o `RestaurantVisualTheme` padrão do restaurante) passa a manter:
   - `draft_visual_config` (JSONB atualizado pelo **autosave** durante a edição).
   - `published_visual_config` (JSONB congelado, exibido na API pública `/api/v1/public/restaurants/{slug}/menu`).
   - `draft_version` (BIGINT para lock otimista — `If-Match` / `expectedVersion`).
   - `published_at` e `has_unpublished_visual_changes` (derivado comparando hashes ou `draft_updated_at > published_at`).
2. **Snapshots / Histórico de versões (`menu_visual_revision`):**
   - A cada publicação (e antes de aplicar grandes lotes de IA ou restauração), o backend grava uma revisão imutável (retenção sugerida: últimas 20 revisões por cardápio) permitindo **"Restaurar versão anterior"** com um clique.
3. **Publicação Explícita:**
   - Clicar em **`Publicar alterações`** (`POST /api/v1/restaurants/{restaurantId}/menus/{menuId}/publish` ou `.../publish-changes`) copia `draft_visual_config` → `published_visual_config` e torna o cardápio `PUBLISHED` de forma atômica.
   - Clicar em **`Descartar rascunho visual`** (`POST .../visual-draft/discard`) restaura `draft_visual_config` para igualar `published_visual_config`.
   - **Sobre alterações de preço/produto em cardápio já publicado:** ver **Seção 10 (Decisão Pendente / Evolução de D-03)** para confirmar com o responsável pelo produto se produtos/preços continuam entrando ao vivo imediatamente (D-03 original) ou se também passarão a suportar rascunho antes de publicar.

### 4.3 Estratégia de Autosave sem Requisições Excessivas e Undo/Redo

- **Buffer local de recuperação:** toda alteração na Store do Editor grava instantaneamente em memória (Signal) e espelha em `sessionStorage`/`IndexedDB` (`borae.editor.recovery.<restaurantId>.<menuId>`) com timestamp e `baseVersion`. Se o usuário fechar a aba, atualizar a página ou perder conexão, ao retornar o editor compara a versão local com a do servidor e oferece **"Recuperar alterações locais não salvas"**.
- **Autosave com Debounce + Coalescing (Angular):**
  - Alterações visuais/textuais disparam um timer de **debounce de 1.500 ms** (com *maxWait* de 6.000 ms se o usuário continuar arrastando sliders continuamente).
  - Apenas o estado consolidado mais recente é enviado em `PUT /api/v1/restaurants/{restaurantId}/menus/{menuId}/visual-draft` com `{ expectedVersion, config }`.
  - Se uma requisição ainda estiver em voo (*in-flight*), novas mudanças aguardam conclusão para enviar um único patch consolidado (evita race conditions e excesso de chamadas).
  - **Tratamento de erro `409 Conflict`:** se outro usuário (`OWNER`/`MANAGER`) salvou uma versão mais nova no servidor, o editor pausa o autosave e exibe modal claro: *"Outro membro da equipe atualizou este cardápio. Recarregar versão do servidor ou sobrescrever com suas alterações?"*.
  - **Tratamento de falha de rede / 5xx:** exibe estado `Erro ao salvar (salvo localmente)` e agenda retry com backoff (3s, 6s, 15s) + botão manual `Tentar salvar agora`.
- **Undo / Redo (Desfazer e Refazer):**
  - Pilha de histórico na Store Angular (limite de 50 passos em memória) agrupando digitação rápida / arraste de cor em transações lógicas (~500 ms).
  - Ao acionar `Desfazer` ou `Refazer`, a prévia atualiza instantaneamente e agenda um autosave normal para persistir o estado restaurado no rascunho.

---

## 5. Arquitetura do Assistente de IA (Gemini + Fallback Grok + Banners)

### 5.1 Princípios de Segurança e Privacidade

1. **Zero credenciais no frontend:** chaves de Gemini, Grok e gerador de imagem ficam exclusivamente em variáveis de ambiente do backend Spring Boot.
2. **Backend como único orquestrador e validador:** toda resposta da IA é JSON estruturado validado por schema estrito no Java. Preços, IDs ou entidades inventadas são rejeitadas/marcadas.
3. **Sem alucinação factual:**
   - Para criação de cardápios/produtos, a IA pode sugerir nomes comerciais, descrições apetitosas e categorias, mas **não deve inventar preços ou ingredientes declarados como fatos**. Quando não informados pelo usuário, retorna `suggestedPriceInCents: null` e `requiresMerchantInput: ["priceInCents", "ingredients"]`, forçando o restaurante a preencher ou confirmar antes de salvar no catálogo.
4. **Proteção contra Prompt Injection:** dados de entrada do usuário e nomes/descrições existentes no catálogo são encapsulados como dados não confiáveis no prompt do sistema.
5. **Rate limiting e Controle de Custos:**
   - Cotas dedicadas para uso administrativo B2B do editor (ex.: configuráveis por restaurante/dia e por usuário/minuto, separadas da cota de consumidor da PR #29).
   - Timeout explícito por chamada (ex.: 15s para texto/estrutura, 30s para geração de imagem), com limite máximo de tokens de entrada/saída.

### 5.2 Arquitetura Multi-Provedor: Google Gemini (Primário) + xAI Grok (Fallback)

No backend Java (`com.borae.menueditor.ai` ou extensão de `com.borae.assistant`):

```java
public interface MenuDesignAiProvider {
    AiProviderId id(); // GEMINI | GROK
    boolean isAvailable();
    AiStructuredCompletion completeStructured(AiStructuredRequest request);
}
```

- **`GeminiMenuDesignProvider` (Prioridade 1):**
  - Utiliza a API oficial Google Gemini (`generativelanguage.googleapis.com`, `RestClient` já adotado na PR #29) com `response_mime_type: "application/json"` e `response_schema`.
  - Modelo configurável via `GEMINI_MODEL` (ex.: `gemini-3.5-flash-lite` ou modelo Gemini Flash configurado pelo time).
- **`GrokMenuDesignProvider` (Prioridade 2 — Fallback):**
  - Utiliza a API oficial da xAI (`https://api.x.ai/v1/chat/completions`, compatível com `Authorization: Bearer ${GROK_API_KEY}`) com `response_format: { type: "json_schema", ... }`.
  - Modelo configurável via `GROK_MODEL` (ex.: `grok-3-mini` ou `grok-2-latest` conforme disponibilidade da conta xAI).
- **`MenuAiOrchestratorService` (Roteador com Fallback Automático):**
  1. Tenta o provedor configurado em `AI_PROVIDER_PRIMARY` (padrão `GEMINI`).
  2. Aciona automaticamente o provedor em `AI_PROVIDER_FALLBACK` (padrão `GROK`) **somente se**:
     - Provedor primário estiver indisponível/desabilitado (`!isAvailable()`), OU
     - Ocorrer erro de rede, timeout, HTTP `429` (rate limit do provedor), HTTP `5xx` (500/502/503/504), ou circuit breaker aberto, OU
     - A resposta retornar JSON malformado / violar o schema estruturado esperado.
  3. **Não** aciona fallback em erros de entrada inválida do cliente (`400`) ou cota interna do restaurante esgotada (`429`).
  4. Registra métricas agregadas (`providerUsed`, `fallbackTriggered`, `fallbackReason`, `latencyMs`, `inputTokens`, `outputTokens`, `estimatedCostUsd`) **sem gravar API keys ou payloads confidenciais em logs**.

### 5.3 Separação: Geração de Texto/Design vs. Geração de Imagens (Banners)

Conforme exigido no escopo: **não confundir geração de texto pelo Gemini/Grok com geração de imagens raster de banners.**

- **Camada 1 — Direção de Arte e Prompt Visual (LLM Texto: Gemini / Grok):**
  - A IA de texto recebe a identidade visual do restaurante (culinária, paleta, tom, título do cardápio) e produz:
    1. Proposta de composição do banner (título curto, subtítulo, posição focal, opacidade do overlay, paleta complementar).
    2. Um `imagePrompt` técnico otimizado (em inglês, descrevendo iluminação, ângulo gastronômico, fundo limpo, ausência de textos dentro da foto) + `aspectRatio` (`21:9` ou `16:9`).
- **Camada 2 — Geração da Imagem Raster (`MenuBannerImageProvider`):**
  - Interface dedicada separada do `MenuDesignAiProvider`, permitindo habilitar geração real de imagens sem acoplar o domínio:
    - Provedor candidato principal: **Google Imagen / Gemini Image Generation** (via `IMAGE_GEN_PROVIDER=GEMINI_IMAGEN`, usando chave/endpoint compatível caso a conta Google AI tenha faturamento/acesso habilitado para geração de imagem) ou outro provedor configurado (`IMAGE_GEN_API_KEY`, `IMAGE_GEN_MODEL`).
  - **Fluxo seguro de entrega e armazenamento:**
    1. Backend chama `MenuBannerImageProvider` -> recebe bytes da imagem gerada.
    2. Backend passa os bytes pelo `RestaurantImageProcessor` (garantindo formato JPEG/WebP seguro, dimensões adequadas e remoção de metadados).
    3. Backend envia para um bucket temporário/pasta `banner-previews/` no **ImageKit** (ou devolve preview autenticada / salva como `RestaurantMedia` do tipo `MENU_BANNER_PREVIEW` com expiração, promovendo para `MENU_BANNER` quando o restaurante aprovar!).
    4. Se nenhum provedor de geração de imagem estiver configurado/disponível (`IMAGE_GEN_ENABLED=false` ou falha), a IA **degrada graciosamente** propondo um **Banner Tipográfico/Estilizado com a paleta da marca** ou sugerindo **fotos já existentes na Biblioteca de Mídia do restaurante** (`RestaurantMedia`), sem quebrar a experiência.

### 5.4 Contexto Contínuo da Assistência em Tempo Real

- Para responder pedidos como *"Deixe meu cardápio mais sofisticado"* ou *"Transforme este cardápio em uma versão executiva"*:
  - O frontend envia na requisição o `menuId`, a `message` do usuário, o `conversationId` opcional (ou histórico recente limitado em sessão no navegador — ex.: últimas 6 mensagens resumidas) e flags de foco (`scope: FULL_MENU | BRANDING | BANNER | PRODUCTS | SINGLE_CATEGORY`).
  - O backend **carrega do banco** o estado atual do restaurante (nome, descrição, categoria gastronômica, logo existente) + o rascunho atual do cardápio (categorias, produtos e `draft_visual_config`), garantindo fonte de verdade segura sem depender de o usuário repetir informações cadastradas.
  - Nota sobre **D-11**: verificar alinhamento se mensagens de sessão do assistente B2B ficam apenas em memória/sessão efêmera do navegador (enviando últimas N trocas no request dentro do limite de tokens) para manter conformidade estrita com *"não persistir conversas no banco"*, persistindo apenas as **propostas aplicadas** no histórico de rascunho!

---

## 6. Contratos Propostos entre Angular e Spring Boot

Base: `/api/v1`. Todos os endpoints privados exigem `Authorization: Bearer <JWT>`, vínculo ativo e papel `OWNER` ou `MANAGER` (exceto leitura e toggle de disponibilidade para `STAFF`, que continuam nas rotas existentes).

### 6.1 Workspace Visual, Rascunho, Publicação e Versionamento

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/workspace` | Membro ativo | Retorna dados do cardápio, `draftVisualConfig`, `publishedVisualConfig`, `draftVersion`, `hasUnpublishedChanges`, `themeMode` (`INHERIT` \| `CUSTOM`) e `logoUrl`. |
| `PUT` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/visual-draft` | OWNER, MANAGER | Salva o rascunho visual (`MenuVisualConfig`) com **autosave**. Exige `expectedVersion` para controle de concorrência (`409` se defasado). |
| `POST` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/visual-draft/discard` | OWNER, MANAGER | Descarta alterações visuais do rascunho, restaurando para o estado publicado atual. |
| `POST` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/publish` | OWNER, MANAGER | *(Existente, evoluído)* Valida consistência mínima, promove `draftVisualConfig` → `publishedVisualConfig`, gera snapshot em `menu_visual_revision` e altera status para `PUBLISHED`. |
| `GET` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/revisions` | OWNER, MANAGER | Lista histórico recente de revisões visuais publicadas/salvas. |
| `POST` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/revisions/{revisionId}/restore` | OWNER, MANAGER | Restaura uma revisão anterior para o **rascunho** atual (exigindo nova publicação se desejar ir ao ar). |
| `PUT` | `/api/v1/restaurants/{restaurantId}/visual-theme` | OWNER, MANAGER | Atualiza o tema visual padrão do restaurante (herdado por cardápios configurados com `inheritRestaurantTheme = true`). |

#### Exemplo de Payload — `PUT .../visual-draft`
Request:
```json
{
  "expectedVersion": 14,
  "config": {
    "schemaVersion": 1,
    "inheritRestaurantTheme": false,
    "theme": {
      "presetId": "editorial-warm",
      "colors": {
        "primary": "#FF7200",
        "secondary": "#191919",
        "background": "#FFFFFF",
        "surface": "#FFF9F2",
        "textPrimary": "#191919",
        "textSecondary": "#666666"
      },
      "typography": {
        "headingFont": "Plus Jakarta Sans",
        "bodyFont": "Inter",
        "scale": "DEFAULT"
      }
    },
    "layout": {
      "categoryStyle": "TABS",
      "productLayout": "EDITORIAL_LIST",
      "imagePosition": "RIGHT",
      "showUnavailableProducts": true,
      "showProductDescriptions": true,
      "showIngredientsPreview": false
    },
    "banner": {
      "enabled": true,
      "mediaId": "6e7a3d10-9c2a-4c3e-9a91-81a8e4921001",
      "focalPointX": 50,
      "focalPointY": 42,
      "zoom": 1.15,
      "heightPreset": "MEDIUM",
      "overlayOpacity": 40,
      "showTextOverlay": true,
      "overlayTitle": "Cardápio Principal",
      "overlaySubtitle": "Preparados artesanalmente todos os dias",
      "overlayBadge": null
    }
  }
}
```
Response (`200 OK`):
```json
{
  "menuId": "uuid",
  "status": "PUBLISHED",
  "draftVersion": 15,
  "hasUnpublishedChanges": true,
  "updatedAt": "2026-11-18T12:40:12Z",
  "draftVisualConfig": { "...": "..." }
}
```

### 6.2 Mídia Unificada, Banner e Reuso de Imagens

Extensão de `RestaurantMediaType` (atualmente `LOGO`, `PHOTO`, `PRODUCT_IMAGE`):
- Adicionar `MENU_BANNER` (e opcionalmente `AI_GENERATED_BANNER_DRAFT` com limpeza automática para previews descartados).
- Endpoints:
  - `GET /api/v1/restaurants/{restaurantId}/media` *(existente)* — adicionar parâmetro opcional `?type=PHOTO|LOGO|MENU_BANNER|PRODUCT_IMAGE` para a Biblioteca de Mídia em **Meu Cardápio**.
  - `POST /api/v1/restaurants/{restaurantId}/media` *(existente)* — passar a aceitar `type=MENU_BANNER` (JPEG/PNG/WebP até 10 MB, processado para até 2.000 px preservando proporção panorâmica).
  - `PUT /api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/image-from-media` *(novo)* — associa uma imagem já existente em `RestaurantMedia` (da Biblioteca) ao produto sem exigir novo upload!

### 6.3 Múltiplos Cardápios e Reaproveitamento sem Duplicação

- Fase 1 (imediata e compatível):
  - `POST /api/v1/restaurants/{restaurantId}/menus/{menuId}/import` — importa categorias/produtos de outro cardápio do mesmo restaurante reutilizando os mesmos `mediaId` das fotos (e opcionalmente `sourceProductId`).
  - `PUT /api/v1/restaurants/{restaurantId}/menus/order` — reordena os cardápios exibidos na página pública.
- Fase 2 (evolução recomendada de domínio para zero duplicação):
  - `GET/POST/PUT /api/v1/restaurants/{restaurantId}/catalog-items` — biblioteca central de itens do restaurante (dona de preço base, descrição, ingredientes, adicionais, foto e `Product3DModel`).
  - `POST /api/v1/restaurants/{restaurantId}/menus/{menuId}/categories/{categoryId}/items` — vincula um `catalogItemId` ao cardápio, com `displayOrder` e `priceOverrideInCents` opcional.

### 6.4 Assistente de IA do Editor de Cardápios (B2B)

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/ai/assist` | OWNER, MANAGER | Envia instrução contextual (criar estrutura, branding, melhorar descrições, adaptar versão executiva) -> retorna `MenuAiProposalResponse` via Gemini (primário) ou Grok (fallback). |
| `POST` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/ai/banner` | OWNER, MANAGER | Solicita geração de banner (prompt + imagem raster quando `IMAGE_GEN_ENABLED=true`, ou composição visual alternativa). Retorna `BannerAiProposalResponse` com URL de prévia no ImageKit (`mediaId` temporário) pronta para aprovação. |
| `POST` | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/ai/proposals/{proposalId}/apply` | OWNER, MANAGER | *(Opcional se a aplicação for via endpoints normais de rascunho/catálogo, ou útil para auditoria de aceitação parcial/total)* — alternativamente, o frontend aplica os trechos aceitos diretamente via `PUT .../visual-draft` e `POST/PUT .../categories|products`, mantendo o **servidor de catálogo como fonte única de verdade**. |
| `GET` | `/api/v1/restaurants/{restaurantId}/ai/quota` | OWNER, MANAGER | Retorna cota restante do dia para o assistente do editor (`limit`, `remaining`, `resetsAt`). |

#### Exemplo de Payload — `POST .../ai/assist`
Request:
```json
{
  "intent": "GENERATE_BRANDING",
  "message": "Crie uma identidade visual acolhedora para hamburgueria artesanal e melhore as descrições da categoria Burgers",
  "targetCategoryId": "uuid-opcional",
  "recentMessages": [
    { "role": "user", "text": "..." },
    { "role": "assistant", "text": "..." }
  ]
}
```
Response (`200 OK` — `MenuAiProposalResponse`):
```json
{
  "proposalId": "uuid",
  "providerUsed": "GEMINI",
  "fallbackUsed": false,
  "assistantMessage": "Preparei uma identidade visual quente em tons de brasa e grafite, além de descrições mais apetitosas para os 3 burgers da categoria.",
  "warnings": [
    "Nenhum preço ou ingrediente novo foi inventado. Confira os textos antes de salvar."
  ],
  "brandingSuggestion": {
    "rationale": "Combina o laranja quente com fundo creme para destacar fotos de hambúrgueres.",
    "theme": {
      "presetId": "artisan-grill",
      "colors": {
        "primary": "#D94E1F",
        "secondary": "#1C1917",
        "background": "#FFFBF7",
        "surface": "#FFFFFF",
        "textPrimary": "#1C1917",
        "textSecondary": "#57534E"
      },
      "typography": {
        "headingFont": "Fraunces",
        "bodyFont": "DM Sans",
        "scale": "DEFAULT"
      }
    }
  },
  "bannerSuggestion": {
    "overlayTitle": "Artesanal na Brasa",
    "overlaySubtitle": "Blends selecionados e pão brioche tostado",
    "overlayOpacity": 45
  },
  "categorySuggestions": [],
  "productUpdates": [
    {
      "targetProductId": "uuid-do-produto-real",
      "currentName": "Burger Clássico",
      "suggestedName": "Clássico da Brasa",
      "suggestedDescription": "Blend suculento grelhado na brasa com queijo derretido e toque especial da casa.",
      "requiresConfirmation": []
    }
  ],
  "newProductSuggestions": [
    {
      "tempId": "tmp-1",
      "targetCategoryId": "uuid-da-categoria",
      "suggestedName": "Smash Duplo Crocante",
      "suggestedDescription": "Sugestão: dois discos prensados na chapa com crosta caramelizada.",
      "suggestedPriceInCents": null,
      "requiresConfirmation": ["priceInCents", "ingredients"]
    }
  ],
  "quota": {
    "dailyLimit": 50,
    "remaining": 48,
    "resetsAt": "2026-11-19T03:00:00Z"
  }
}
```

### 6.5 Atualização do Contrato Público (`GET /api/v1/public/restaurants/{slug}/menu`)

Estender `PublicRestaurantMenuResponse` (mantendo retrocompatibilidade total com campos atuais):
- Em `PublicRestaurantMenuResponse`:
  - Adicionar `logoUrl: String | null` (URL pública da `LOGO` em `restaurant_media`).
  - Adicionar `defaultVisualConfig: PublicVisualConfig | null` (tema padrão publicado do restaurante).
- Em `PublicMenu`:
  - Adicionar `visualConfig: PublicVisualConfig | null` (contendo `publishedVisualConfig` do cardápio com cores, tipografia, layout e `banner` contendo `imageUrl` resolvida da `mediaId`, `focalPointX`, `focalPointY`, `zoom`, `heightPreset`, `overlayOpacity`, textos).
- **Regra:** jamais expor `draftVisualConfig`, revisões, dados de IA ou `providerFileId` no endpoint público!

### 6.6 Estados de Processamento e Códigos HTTP

- `200 OK`: leitura do workspace, autosave de rascunho, publicação, geração/proposta síncrona de IA (dentro do timeout).
- `201 Created`: criação de cardápio, categoria, produto ou upload de mídia/banner.
- `202 Accepted`: reservado caso geração pesada de banner seja processada assincronamente.
- `204 No Content`: exclusão de mídia / descarte sem corpo.
- `400 Bad Request`: hex inválido, fonte fora da allowlist, dimensão/formato de imagem inválido, payload malformado.
- `401 Unauthorized`: token ausente/expirado/inválido.
- `403 Forbidden`: `STAFF` tentando editar visual/catálogo/IA ou usuário sem vínculo ativo no restaurante.
- `404 Not Found`: restaurante, cardápio, mídia ou revisão fora do escopo do restaurante.
- `409 Conflict`: `expectedVersion` defasado (edição concorrente), cardápio arquivado (`ARCHIVED`), ou mídia em uso quando bloqueado por integridade.
- `413 Payload Too Large`: imagem acima de 5 MB (`LOGO`) ou 10 MB (`PHOTO` / `MENU_BANNER`).
- `422 Unprocessable Entity`: tentativa de publicar cardápio sem categoria ativa com produto ativo.
- `429 Too Many Requests`: cota diária de IA ou rate limit excedido (incluir header `Retry-After` / reset).
- `502 / 503 / 504`: falha simultânea do provedor primário (Gemini) e fallback (Grok) ou indisponibilidade do ImageKit — resposta normalizada sem expor stack trace ou chaves.

---

## 7. Variáveis de Ambiente Necessárias

Todas as credenciais sensíveis são exclusivas do **serviço backend Spring Boot** (Render / `.env` local nunca versionado). O frontend Angular **não deve possuir nenhuma dessas chaves**.

| # | Nome da variável | Finalidade | Serviço | Obrigatória / Opcional | Onde configurar | Tratamento quando ausente |
|---|---|---|---|---|---|---|
| 1 | `AI_PROVIDER_PRIMARY` | Define o provedor primário de IA de texto/branding/cardápio (valor padrão: `GEMINI`). | Backend (`borae-api`) | Opcional (default `GEMINI`) | Painel Render (`borae-api`) / `.env` local | Assume `GEMINI`. Se inválido no boot, falha rápida de configuração ou loga erro estruturado e desabilita IA. |
| 2 | `AI_PROVIDER_FALLBACK` | Define o provedor de fallback automático em indisponibilidade/erro do primário (valor padrão: `GROK`; aceita `NONE`). | Backend (`borae-api`) | Opcional (default `GROK`) | Painel Render (`borae-api`) / `.env` local | Assume `GROK`. Se `GROK_API_KEY` estiver ausente, o fallback é ignorado com log de aviso sem impedir uso do primário. |
| 3 | `GEMINI_ENABLED` | Chave mestra (feature flag) para habilitar chamadas ao Google Gemini no backend. | Backend (`borae-api`) | Opcional (default `false`) | Painel Render (`borae-api`) / `.env` local | Quando `false`, `GeminiMenuDesignProvider.isAvailable()` retorna `false` e roteia para fallback (se habilitado) ou informa IA indisponível (`503`). |
| 4 | `GEMINI_DATA_PROCESSING_APPROVED` | Confirmação explícita de conformidade de termos/faturamento Google AI antes de habilitar produção (padrão já existente na PR #29). | Backend (`borae-api`) | Opcional (default `false`) | Painel Render (`borae-api`) | Bloqueia envio de dados ao Gemini mesmo que `GEMINI_API_KEY` esteja presente. |
| 5 | `GEMINI_API_KEY` | Chave privada da API Google Gemini (AI Studio / Vertex). | Backend (`borae-api`) | **Obrigatória** para usar Gemini | Secret Env no Render (`borae-api`) — **nunca no Git ou Angular** | Se vazia/ausente, provedor fica indisponível; o orquestrador tenta `GROK` (se habilitado) ou retorna `503` amigável mantendo editor manual 100% operacional. |
| 6 | `GEMINI_MODEL` | Identificador exato do modelo Gemini utilizado no editor B2B (ex.: `gemini-2.5-flash` ou `gemini-3.5-flash-lite` conforme suporte a JSON schema). | Backend (`borae-api`) | Opcional (default configurável em `application.yml`) | Painel Render (`borae-api`) | Usa default seguro definido em `application.yml`. |
| 7 | `GROK_ENABLED` | Feature flag para habilitar o provedor fallback xAI Grok. | Backend (`borae-api`) | Opcional (default `false`) | Painel Render (`borae-api`) / `.env` local | Quando `false`, não tenta fallback no Grok; se Gemini falhar, retorna `503` amigável orientando edição manual. |
| 8 | `GROK_DATA_PROCESSING_APPROVED` | Confirmação de termos comerciais/LGPD do provedor xAI antes de produção. | Backend (`borae-api`) | Opcional (default `false`) | Painel Render (`borae-api`) | Bloqueia envio de contexto à API da xAI mesmo que `GROK_API_KEY` exista. |
| 9 | `GROK_API_KEY` | Chave privada da API xAI (`https://api.x.ai/v1`). | Backend (`borae-api`) | Obrigatória somente se `GROK_ENABLED=true` | Secret Env no Render (`borae-api`) | Fallback Grok permanece desabilitado; erro estruturado não vaza ao cliente e o editor manual segue funcionando. |
| 10 | `GROK_MODEL` | Identificador do modelo xAI usado no fallback (ex.: `grok-3-mini` / `grok-3-fast`). | Backend (`borae-api`) | Opcional (default em `application.yml`) | Painel Render (`borae-api`) | Usa default configurado em `application.yml`. |
| 11 | `AI_MENU_EDITOR_TIMEOUT_SECONDS` | Timeout HTTP por chamada de geração de cardápio/branding (default `15`). | Backend (`borae-api`) | Opcional (default `15`) | `application.yml` / Render (`borae-api`) | Usa `15` segundos. |
| 12 | `AI_MENU_EDITOR_DAILY_QUOTA_PER_RESTAURANT` | Cota diária de chamadas de assistente B2B por restaurante (renovação `America/Sao_Paulo`). | Backend (`borae-api`) | Opcional (default ex.: `50`) | `application.yml` / Render (`borae-api`) | Usa valor padrão seguro para evitar abuso financeiro. |
| 13 | `IMAGE_GEN_ENABLED` | Habilita geração automática de imagem raster de banner por IA. | Backend (`borae-api`) | Opcional (default `false`) | Painel Render (`borae-api`) | Quando `false`, o assistente de banner gera apenas **propostas de composição/texto sobre fotos existentes ou fundo estilizado** (degradação graciosa). |
| 14 | `IMAGE_GEN_PROVIDER` | Provedor selecionado para geração de imagem de banner (ex.: `GEMINI_IMAGEN` ou `NONE`). | Backend (`borae-api`) | Opcional (default `NONE`) | Painel Render (`borae-api`) | Desabilita geração raster mantendo upload manual e biblioteca de mídia ativos. |
| 15 | `IMAGE_GEN_API_KEY` | Chave de autenticação do serviço de geração de imagem (caso utilize conta/provedor distinto da `GEMINI_API_KEY`). | Backend (`borae-api`) | Obrigatória se `IMAGE_GEN_ENABLED=true` e provedor exigir chave própria | Secret Env no Render (`borae-api`) | Pode herdar `GEMINI_API_KEY` se `IMAGE_GEN_PROVIDER=GEMINI_IMAGEN` usar a mesma conta; se ausente, retorna fallback visual sem quebrar o editor. |
| 16 | `IMAGE_GEN_MODEL` | Identificador do modelo de geração de imagem (ex.: `imagen-3.0-generate-002` ou equivalente vigente). | Backend (`borae-api`) | Opcional (default em `application.yml`) | Painel Render (`borae-api`) | Usa default ou desabilita geração raster se não configurado. |
| 17 | `IMAGEKIT_PRIVATE_KEY` e `IMAGEKIT_URL_ENDPOINT` | *(Já existentes em `application.yml`)* Upload/exclusão e CDN de imagens, banners e modelos 3D. | Backend (`borae-api`) | Obrigatórias em produção para mídia | Secret Env no Render (`borae-api`) | Sem elas, uploads de banner/mídia falham com `503` controlado sem corromper o banco. |

---

## 8. Compatibilidade com a PR #30 (`docs/3D_DISH_CAPTURE_WORKFLOW.md`) e Módulo 3D/AR

A reorganização de **Meu Cardápio** e a introdução da IA de criação visual **não substituem nem alteram** o fluxo da PR #27, PR #28 e PR #30:

1. **Separação de domínios:**
   - A geração automática de **modelos 3D dos pratos** (PR #30: captura guiada por câmera -> orquestração FastAPI/ZeroGPU -> validação `GlbFileValidator` -> `Product3DModel` -> aprovação humana) é um processo assíncrono específico de reconstrução 3D por prato.
   - O **Editor Inteligente de Cardápios** atua sobre layout, branding, banners, organização de catálogo e sugestões textuais/estruturais de cardápio.
2. **Preservação na Nova Navegação Angular:**
   - No novo **Inspetor de Produto** dentro do editor visual em tempo real, deve existir uma guia dedicada **"Experiência 3D"** embutindo diretamente o componente existente `<bo-product-model-3d>` (`ProductModel3dComponent`).
   - A rota `/app/cardapios/:menuId/produtos/:productId` (`ProductEditorPage`) deve continuar funcionando (ou abrir em modo focado) especialmente para suportar o fluxo mobile-first de captura guiada por câmera previsto na PR #30.
   - A rota da prévia em tempo real (`MenuRendererComponent`) deve continuar consumindo `MenuProductCardComponent` e exibindo o selo **`3D`** e a ação **`Ver em 3D`** / **`Ver na minha mesa`** (AR) sempre que o produto possuir modelo `PUBLISHED`.
3. **Preservação do Catálogo e ImageKit:**
   - Produtos criados ou editados via editor visual ou sugestões aprovadas da IA continuam gerando registros reais em `MenuProduct` (`UUID` imutável), garantindo que qualquer modelo `Product3DModel` já associado ao `productId` permaneça intacto.
   - Quando a Fase 2 de **Catálogo Compartilhado** (`CatalogItem`) for implementada para evitar duplicação entre múltiplos cardápios, a associação `menu_product_3d_model` deverá apontar para o item base para que um único modelo 3D sirva a todos os cardápios onde o prato aparecer.

---

## 9. Plano de Implementação em Etapas Executáveis

### Etapa 0 — Alinhamento de Decisões e Contratos
- **Responsáveis:** Produto + Gabriel + Renan.
- **Atividades:** Aprovar adendos às decisões **D-03** (rascunho vs publicação de visual/conteúdo), **D-10/D-11** (provedor fallback Grok, cotas B2B, política de não persistir conversas e geração de banners). Fechar schema JSON de `MenuVisualConfig` e contratos OpenAPI.
- **Critério de aceite:** Documentos de decisão atualizados e contratos validados por ambos.

### Etapa 1 — Backend: Persistência Visual, Mídia e Draft/Publish (Gabriel)
- **Atividades:**
  - Migration Flyway (ex.: `V13` ou sequencial após PR #29): adicionar `draft_visual_config` (JSONB), `published_visual_config` (JSONB), `published_at`, `version` (`@Version`) em `restaurant_menu`; adicionar `default_visual_config` em `restaurant`; criar tabela `menu_visual_revision`; estender `ck_restaurant_media_type` para incluir `MENU_BANNER` e `AI_BANNER_DRAFT`.
  - Endpoints de `/workspace`, `/visual-draft`, `/visual-draft/discard`, `/revisions`, `/visual-theme`, `image-from-media`, `/menus/order`, `/menus/{id}/import`.
  - Atualizar `PublicRestaurantMenuService` / `PublicRestaurantMenuResponse` para incluir `logoUrl`, `defaultVisualConfig` e `visualConfig` publicado (resolvendo `banner.imageUrl` segura).
  - Testes unitários e integração PostgreSQL (Testcontainers): multi-tenancy, lock otimista `409`, validação de hex/allowlist de fontes, RBAC (`OWNER`/`MANAGER` vs `STAFF`), e exposição pública filtrada.
- **Critério de aceite:** API salva rascunhos com lock otimista, mantém versão publicada isolada e entrega tema/logo/banner no endpoint público sem vazar rascunhos.

### Etapa 2 — Frontend: Reorganização de "Meu Cardápio" e Editor Visual em Tempo Real (Renan)
- **Atividades:**
  - Extrair componente compartilhado `MenuRendererComponent` usado por `PublicMenuPage` e pelo novo `MenuWorkspacePage`.
  - Reorganizar `modules.ts` e `dashboard.routes.ts` (`Meu cardápio`, incorporando `MediaPage` em `Banner & Mídia` com redirect de `/app/restaurante/imagens`).
  - Construir `MenuEditorStore` (Signals) com: sincronização instantânea com a prévia, pilha **Undo/Redo**, **Autosave com debounce + coalescing** + tratamento `409`, buffer de recuperação em `sessionStorage`, seletor de viewport (`375px` / `768px` / `1200px`), comparador Rascunho vs Publicado.
  - Implementar painéis: Conteúdo (com `ProductModel3dComponent` preservado), Identidade & Layout (paleta com alerta WCAG, tipografia, disposição) e Banner & Mídia (focal point, zoom, overlay, biblioteca unificada).
  - Atualizar `demo-api.interceptor.ts` e testes unitários/componentes (`vitest`).
- **Critério de aceite:** Restaurante edita tudo com prévia fiel em tempo real, autosave, undo/redo, gerencia banner e biblioteca de imagens dentro de `Meu cardápio`, sem perder o acesso 3D do produto.

### Etapa 3 — Backend: Gateway Multi-Provedor Gemini + Fallback Grok e Banners IA (Gabriel)
- **Atividades:**
  - Criar abstração `MenuDesignAiProvider` com `GeminiMenuDesignProvider` (primário) e `GrokMenuDesignProvider` (fallback) + roteador `MenuAiOrchestratorService` com timeout, circuit breaker, validação estrita de JSON Schema e anti-prompt-injection.
  - Criar `MenuBannerImageProvider` separado para geração de imagem + pipeline `RestaurantImageProcessor` -> ImageKit.
  - Criar cota B2B e rate limit por restaurante + métricas agregadas sem persistência de conversas completas.
  - Endpoints `/ai/assist`, `/ai/banner`, `/ai/quota` com testes simulando falha/timeout/429 do Gemini acionando fallback Grok.
- **Critério de aceite:** Falha do Gemini aciona Grok automaticamente; nenhum segredo vaza ao cliente; preços/ingredientes não inventados; falhas totais degradam com `503` sem afetar edição manual.

### Etapa 4 — Frontend: Interface Conversacional de IA e Aprovação Granular (Renan)
- **Atividades:**
  - Construir `MenuAiAssistantPanelComponent` conectado a `/ai/assist`, `/ai/banner` e `/ai/quota`.
  - Implementar modo **Pré-visualização de Sugestão IA** sobre o `MenuRendererComponent` + aprovação granular (por tema, banner, categoria ou produto) + edição manual imediata após aceite + alerta visível quando itens requerem confirmação de preço/ingredientes.
  - Testes de componente cobrindo aceite total, aceite parcial, descarte, cota esgotada (`429`) e fallback/erro (`503`).
- **Critério de aceite:** Nenhuma sugestão da IA altera rascunho ou publicação sem aprovação explícita; itens parciais funcionam e podem ser desfeitos.

### Etapa 5 — Evolução Sem Duplicação (Catálogo Compartilhado) e Validação E2E
- **Atividades:**
  - Avaliar após validação dos primeiros restaurantes a migração incremental para `CatalogItem` compartilhado entre cardápios (mantendo compatibilidade com `Product3DModel`).
  - Testes E2E cobrindo: autosave + refresh recovery, publicação explícita, consulta `/r/:slug`, fluxo 3D intacto e responsividade mobile/desktop.

---

## 10. Riscos e Decisões Pendentes para Aprovação Humana

1. **Adendo à D-03 (Edições em Cardápio Publicado):**
   - *Risco:* A D-03 atual diz que salvar em cardápio `PUBLISHED` entra no ar na hora. O editor visual exige prévia e publicação explícita de rascunho.
   - *Recomendação:* Separar `draftVisualConfig` / `publishedVisualConfig` com publicação explícita para identidade/layout/banner, e definir se alterações de preço/produto em cardápio publicado continuam imediatas (para agilidade operacional de preço/disponibilidade) ou também entram em fila de publicação.
2. **Adendo à D-10 / D-11 (Provedor Grok e Geração de Imagem):**
   - *Risco:* D-10 aprova somente *Gemini Flash-Lite*. Usar **xAI Grok** como fallback e um serviço de **geração de imagens de banner** exige aprovação explícita de termos de uso, LGPD/privacidade e orçamento pelo responsável pelo produto.
   - *Recomendação:* Manter `GROK_ENABLED=false` e `IMAGE_GEN_ENABLED=false` por padrão até aprovação comercial/legal das contas.
3. **Limites e Custos de Geração de Banner:**
   - Definir cota específica para geração de imagens (custo por imagem é significativamente superior ao de texto) e política de expiração de previews gerados e não aprovados no ImageKit (para evitar acúmulo de arquivos órfãos).
4. **Caminho para Múltiplos Cardápios Sem Duplicação:**
   - Confirmar se a Entrega 1 usará **Importação/Clonagem com Biblioteca de Mídia compartilhada** (mais rápido para integrar) ou já migrará `V7` para **Catálogo Base (`catalog_item` + `menu_category_item`)** desde o início.
5. **Conflito pré-existente de case-sensitivity no Git (`docs/ROADMAP.md` vs `docs/roadmap.md`):**
   - Em sistemas Windows, os dois arquivos colidem no checkout. Recomenda-se consolidar para um único arquivo (`docs/ROADMAP.md`) em PR de limpeza dedicada.
