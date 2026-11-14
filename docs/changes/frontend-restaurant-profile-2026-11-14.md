# Registro de Alterações: Perfil do Restaurante no Painel (Frontend)

## Identificação
- **Projeto:** Boraê
- **Entrega:** Interface Angular do perfil do restaurante — dados, horários e imagens
- **Responsável:** Renan Cristiano
- **Data:** 14/11/2026
- **Branch:** `feat/restaurant-profile-frontend` (a partir de `main`, após o PR #18)
- **Backend consumido:** branch `feat/restaurant-profile-api` (PR em draft)

**Modificações feitas por Renan Cristiano**

---

## 1. Objetivo
Implementar no painel a seção **Meu restaurante** para OWNER e MANAGER, consumindo a API de perfil do restaurante: dados públicos, horários de funcionamento e imagens (logo e fotos).

## 2. Estrutura
"Meu restaurante" passou de "Parcial" para **Disponível** e ganhou três abas:

| Rota | Aba | Endpoints |
|---|---|---|
| `/app/restaurante` | Perfil | `GET` / `PATCH /api/v1/restaurants/{id}` |
| `/app/restaurante/horarios` | Horários | `GET` / `PUT /api/v1/restaurants/{id}/business-hours` |
| `/app/restaurante/imagens` | Imagens | `GET` / `POST` (multipart) / `DELETE /api/v1/restaurants/{id}/media[/{mediaId}]` |

O cabeçalho mostra nome, slug, papel e o **status de publicação** (Rascunho, Publicado ou Suspenso), somente leitura — não há endpoint de publicação nesta versão. O campo interno `active` não é exibido como publicação.

## 3. Perfil
- Campos: nome público, endereço público (slug), descrição, categoria (sugestões + texto livre), telefone, WhatsApp, Instagram, site, endereço, latitude e longitude.
- Limites de tamanho iguais aos do DTO (`UpdateRestaurantProfileRequest`).
- Salvar só fica ativo quando há alteração; "Descartar" restaura o perfil carregado.
- Envio: todos os campos de texto são enviados (string vazia limpa o campo na API); latitude/longitude só são enviadas quando as duas estão preenchidas.
- **409** (slug em uso) aparece no próprio campo; demais erros na barra de ações.
- "Usar minha localização" (geolocalização do navegador, com permissão) e link "Ver no mapa" (OpenStreetMap).
- Após salvar, o novo nome/slug é refletido no seletor de restaurante e no cabeçalho.

## 4. Horários
- Grade de segunda a domingo; cada dia pode ser aberto/fechado.
- Vários intervalos por dia (almoço e jantar), adicionar/remover intervalo e "Copiar para os dias abertos".
- O `PUT` envia a grade completa; dias fechados são omitidos (a API os considera fechados).
- Avisos imediatos para campos vazios, fechamento antes da abertura e sobreposição — apenas antecipam os erros 400 da API, que continua sendo a autoridade.
- Horários recebidos como `HH:mm` ou `HH:mm:ss` são normalizados para o campo de horário.

## 5. Imagens
- **Logo:** prévia, trocar (a API substitui a anterior) e remover.
- **Fotos:** grade com contador (até 100), envio múltiplo com barra de progresso por arquivo, exclusão com confirmação.
- Antes do envio, o navegador confere formato (JPEG, PNG, WebP) e tamanho (logo 5 MB, foto 10 MB) para evitar uploads que seriam recusados. O servidor valida de novo o conteúdo real, redimensiona e envia ao ImageKit.
- A prévia usa o campo **`url`** devolvido pela API (o texto do PR do backend cita `publicUrl`, mas o DTO `RestaurantMediaResponse` usa `url`).
- Nenhuma credencial do ImageKit no frontend.

## 6. Permissões
- OWNER e MANAGER: acesso completo às três abas.
- STAFF: estado "Perfil disponível para proprietário e gerentes" (o backend também responde 403).

## 7. Erros
O backend usa `server.error.include-message: never`, então as respostas de erro não trazem texto. As mensagens são definidas por status e contexto em `profileErrorMessage` (0, 401, 403, 404, 409, 413, 400 e 5xx).

## 8. Modo demonstração
`npm run start:demo` simula os novos endpoints com as mesmas regras (slug normalizado e único, lat/lng em conjunto, validação da grade, limites e tipos de imagem, uma logo por restaurante). A Cantina Bella Vista vem com perfil e horários preenchidos. Imagens enviadas no modo demonstração ficam apenas na memória da aba.

## 9. Arquivos
**Criados**
- `frontend/src/app/dashboard/profile/restaurant-profile.service.ts` — contratos e chamadas HTTP
- `frontend/src/app/dashboard/profile/profile.store.ts` — estado do perfil compartilhado entre cabeçalho e aba
- `frontend/src/app/dashboard/profile/profile-form.page.ts`, `business-hours.page.ts`, `media.page.ts`
- `docs/changes/frontend-restaurant-profile-2026-11-14.md`

**Modificados**
- `dashboard/pages/restaurant.page.ts` (layout com abas), `dashboard/dashboard.routes.ts` (rotas filhas), `dashboard/modules.ts` (módulo disponível), `dashboard/restaurant-context.service.ts` (`canEditProfile`, `updateRestaurantInfo`)
- `demo/demo-data.ts` e `demo/demo-api.interceptor.ts` (simulação)

## 10. Testes
No modo demonstração (desktop 1366px e celular 375px, temas claro e escuro):
- Perfil: carregamento, conflito de slug (409 no campo), salvamento com normalização do slug e atualização do seletor, bloqueio de latitude sem longitude, descartar alterações.
- Horários: abrir dia, adicionar intervalo, aviso de sobreposição bloqueando o salvar, correção e salvamento.
- Imagens: envio de logo e fotos, recusa de PDF antes do envio, troca de logo, exclusão com confirmação.
- Builds de produção e demonstração sem avisos.

**Não testado:** integração real com o backend publicado (a branch `feat/restaurant-profile-api` ainda não está em `main`/Render) e upload real no ImageKit.

## 11. Limitações e pendências
- A API não permite **remover** coordenadas já salvas (no `PATCH`, latitude/longitude ausentes mantêm os valores). A interface avisa e bloqueia essa tentativa. Sugestão ao backend: aceitar um indicador explícito de remoção.
- Horários que passam da meia-noite precisam ser divididos entre dois dias (a API exige fechamento posterior à abertura no mesmo dia).
- Publicação do perfil (Rascunho → Publicado) aguarda endpoint.
- Validar ponta a ponta após o merge do backend e o deploy no Render.
