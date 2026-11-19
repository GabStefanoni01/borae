# Homologação Ponta a Ponta do Fluxo Principal — Sessão 02 (19/11/2026)

**Branch:** `quality/core-flow-homologation` (a partir de `main` em `3326bff`)

## 1. Diagnóstico e Correções Realizadas

### Backend
- **Publicação D-01 (`RestaurantService.java`):** `POST /api/v1/restaurants/{restaurantId}/publish` agora valida obrigatoriamente `name`, `slug`, `category` e `address` antes de mudar o restaurante para `PUBLISHED` (retornando `400 Bad Request` com os campos faltantes), e `PATCH /api/v1/restaurants/{restaurantId}` impede esvaziar `category` ou `address` enquanto o restaurante estiver `PUBLISHED`.
- **Isolamento de mídia (`RestaurantMediaService.java` e `RestaurantMediaRepository.java`):** `GET /api/v1/restaurants/{restaurantId}/media` filtra exclusivamente `LOGO` e `PHOTO`, impedindo que registros `PRODUCT_IMAGE` vazem na galeria do restaurante.
- **Imutabilidade de produto arquivado (`MenuProductImageService.java`):** `DELETE /products/{productId}/image` agora rejeita produtos `ARCHIVED` com `409 Conflict`, alinhado ao `POST /image`.
- **Resolução pública de slug (`PublicRestaurantMenuService.java`):** normalização `trim().toLowerCase(Locale.ROOT)` na consulta `GET /api/v1/public/restaurants/{slug}/menu`.

### Frontend
- **Tratamento de HTTP `429` e mensagens seguras da API:** atualizados `auth.service.ts`, `restaurant-context.service.ts`, `restaurant-profile.service.ts`, `catalog-errors.ts` e `public-menu.page.ts` para tratar `429 Too Many Requests` (bloqueio de login e rate limiting da Sessão 01) e exibir mensagens seguras do `ApiExceptionHandler` (como campos obrigatórios D-01 na publicação).
- **Checklist D-01 e orientação de publicação:** `publication.page.ts` exibe aviso dos campos obrigatórios ausentes (`Categoria gastronômica`, `Endereço`) com link direto para `/app/restaurante`; `restaurant.page.ts` corrigiu a mensagem desatualizada que dizia que a publicação ainda não estava disponível no painel; corrigida quebra de linha de links inline em `.d-note` e `.c-pub-notes`.
- **Sessão expirada e logout limpo (`auth.service.ts`):** detecção local de expiração do JWT (`exp`) no `localStorage` para evitar redirecionamentos/401 em loop e limpeza de `borae.dashboard.restaurant` no `logout()`.
- **Troca de restaurante (`restaurant-switcher.component.ts`):** redireciona para `/app/cardapios` ao trocar de restaurante estando em `/app/cardapios/:menuId...`, evitando tela `404`. Corrigido acesso lazy a `ROLE_LABEL` / `PUBLICATION_LABEL` em `restaurant-switcher.component.ts`, `accept-invitation.page.ts` e `restaurant.page.ts`.
- **Visão geral (`overview.page.ts`):** adicionados atalhos rápidos para `Cardápios` e `Página pública e QR Code` e ocultado link de edição do perfil para `STAFF`.
- **Ambiente local (`environment.local.ts` e `angular.json`):** adicionada configuração `local` (`http://localhost:8080`) para homologação E2E local sem alterar o build de produção (`https://borae.onrender.com`).

## 2. Cobertura de Testes Adicionada
- **Backend (`CoreFlowPostgresIntegrationTest.java` + `RestaurantServiceTest.java`):** 110 testes (incluindo PostgreSQL 16 real com decodificação binária do QR Code PNG via ZXing, isolamento multi-tenant, D-01 e D-03).
- **Frontend (`auth.service.spec.ts`, `team-and-profile.spec.ts`, `publication.page.spec.ts`, `catalog-errors.spec.ts`):** 90 testes (13 arquivos) + `npm run build`.
- **E2E e Responsividade no Navegador:** fluxo completo validado ponta a ponta em PostgreSQL 16 + Spring Boot + Angular (`http://127.0.0.1:4200`) em Desktop (`1280x800`), Tablet (`768x1024`) e Mobile (`375x812`) sem overflow horizontal.
