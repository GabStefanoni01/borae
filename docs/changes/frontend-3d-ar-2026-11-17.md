# Registro de Alterações: Experiências 3D e Realidade Aumentada (Frontend)

**Modificações feitas por Renan Cristiano**
**Data da publicação: 17/11/2026**

- **Projeto:** Boraê
- **Entrega:** interface Angular dos modelos 3D de produto, com prévia no painel, visualização 3D pública e AR progressiva
- **Backend consumido:** PR #27, branch `feat/3d-ar-experiences` (não alterada). Referências: `docs/IMPLEMENTATION_3D_AR.md` e `docs/MODULE_3D_AR.md` daquela branch
- **Branch:** `feat/frontend-3d-ar`, criada a partir de `main`; PR independente para `main`, a ser integrada depois da PR #27

---

## 1. Objetivo
Permitir que OWNER e MANAGER associem um modelo 3D (GLB) a um produto existente, informem as dimensões físicas, revisem a prévia e publiquem. O consumidor pode girar o prato em 3D no cardápio público e, em aparelhos compatíveis, vê-lo no ambiente em realidade aumentada. A foto do produto continua como fallback em qualquer falha.

## 2. Contratos consumidos (conferidos no código da PR #27)
Base: `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d`

| Método | Uso no frontend |
|---|---|
| `GET /` | Carrega o modelo. **404 = produto sem modelo**: a tela mostra "Nenhum modelo 3D adicionado.", não um erro |
| `POST /` (multipart `file`, `widthMm`, `heightMm`, `depthMm`) | Enviar ou substituir, com progresso |
| `PATCH /publication` `{ "published": boolean }` | Publicar e despublicar |
| `DELETE /` | Remover |

- Resposta (`Product3DModelResponse`): `id`, `productId`, `status` (`READY`/`PUBLISHED`), `modelUrl`, `contentType`, `fileSize`, `widthMm`, `heightMm`, `depthMm`, `createdAt`, `updatedAt`.
- No cardápio público, `PublicProduct.model3d` (`modelUrl`, `contentType` e as dimensões) só vem quando o modelo está publicado e o produto é elegível.
- Todas as operações exigem OWNER ou MANAGER, inclusive a consulta. STAFF não vê a seção, porque o formulário de produto já é restrito a esses papéis.

## 3. Painel — seção "Experiência 3D" no formulário de produto
- Fica dentro de `/app/cardapios/:menuId/produtos/:productId`, sem cadastro paralelo. Em produto novo, orienta a salvar primeiro, porque os endpoints exigem o `productId`.
- Mostra o estado: carregando, sem modelo, erro com "Tentar novamente", pronto (não publicado) ou publicado. Exibe tamanho do arquivo, dimensões e data.
- **Envio e substituição:** largura, altura e profundidade em milímetros (inteiros de 1 a 5000; o teto só orienta a digitação) e escolha do `.glb`. Antes de enviar, o navegador confere a extensão, o tamanho (até 10 MB) e o cabeçalho GLB (`glTF`, versão 2); a validação completa continua no backend. Há barra de progresso e a mensagem "Validando o modelo no servidor..." depois de 100%. A substituição pede confirmação e avisa quando o modelo já está publicado.
- **Publicar/despublicar** e **remover** (com confirmação). Um 404 nessas ações recoloca a tela no estado "sem modelo".
- **Prévia:** "Visualizar em 3D" abre o visualizador num painel, com a foto do produto como alternativa.
- Para ajustar só as dimensões é preciso reenviar o arquivo, porque a API não tem endpoint só de dimensões. A tela informa isso.
- Upload, publicação e remoção têm efeito imediato e são independentes de "Salvar produto": uma falha no 3D não impede salvar os outros dados.
- Ao trocar de restaurante ou de produto, todo o estado local é descartado e o modelo é consultado de novo.
- Mensagens por status: 400 (GLB inválido, recursos externos, dimensões), 403, 404, 409 (cardápio ou produto arquivado), 413 (10 MB) e 5xx.

## 4. Visualizador 3D (`shared/model-3d-viewer.component.ts`)
- Usa `@google/model-viewer` 4.3.1 (web component), com `three` 0.183.2 fixado como dependência explícita.
- **Carregamento sob demanda:** a biblioteca vira um pedaço separado (`model-viewer`, cerca de 1 MB, 243 kB comprimido), baixado só quando alguém abre o 3D. O carregamento inicial do app ficou praticamente igual (90,7 kB → 91,1 kB comprimido). O carregador está no token `MODEL_VIEWER_LOADER`, para os testes não baixarem a biblioteca.
- Câmera com rotação e zoom por toque, mouse e teclado; texto alternativo; instrução de uso; progresso de carregamento; giro automático desligado com `prefers-reduced-motion`.
- **Escala (calibração):** com as três dimensões informadas, o modelo é ajustado por um fator uniforme, a mediana das proporções entre as medidas físicas (mm → m) e a caixa do GLB (x = largura, y = altura, z = profundidade). Diferenças abaixo de 1% são ignoradas. O AR usa `ar-scale="fixed"`. A tela avisa que o tamanho percebido pode variar por aparelho e ambiente.
- **AR progressiva:** `ar-modes="webxr scene-viewer quick-look"` (Android via WebXR/Scene Viewer; iPhone via Quick Look, com USDZ gerado a partir do GLB pelo próprio model-viewer). O botão "Ver no seu ambiente" fica no slot `ar-button`, que a biblioteca só mostra quando o aparelho suporta AR (conferido no desktop: botão oculto).
- **Fallback:** se a biblioteca ou o modelo falhar, mostra a foto do produto com aviso e emite `failed`.
- `loading="eager"`: como o visualizador só aparece por ação do usuário, o modelo começa a carregar na hora.

## 5. Cardápio público (`/r/:slug`)
- Selo "3D" na lista para produtos com modelo publicado; o rótulo acessível inclui "disponível em 3D".
- No detalhe, o botão "Ver em 3D" troca a foto pelo visualizador, e "Ver foto" volta. Nada de 3D é baixado na lista nem ao abrir o detalhe.
- Produto sem `model3d` continua exatamente como antes.

## 6. Modo demonstração
- Simula os quatro endpoints com as regras da PR #27: somente OWNER/MANAGER, 404 sem modelo, `.glb` com cabeçalho válido, 10 MB, dimensões positivas, 409 para produto arquivado, e a substituição preserva o estado de publicação.
- O "Spaghetti alla carbonara" da Cantina Bella Vista já vem com um modelo **publicado**: um prato de 28 cm gerado em memória como GLB 2.0 válido (`demo/demo-glb.ts`), sem arquivos externos.
- Nada disso entra no build de produção (conferido por busca no `dist`).

## 7. Testes
`npm test`: **11 arquivos, 80 testes aprovados** (24 novos).

| Arquivo | Cobertura nova |
|---|---|
| `shared/model-3d-viewer.component.spec.ts` | Carregamento sob demanda, atributos, AR opcional, calibração aplicada, fallback por erro do modelo e da biblioteca, `calibrationFactor` |
| `dashboard/catalog/product-model-3d.component.spec.ts` | Produto novo, 404 sem modelo, erro e nova tentativa, publicar/despublicar, envio com dimensões, confirmação de substituição, validação de extensão, cabeçalho e dimensões, progresso, 400 do servidor, 10 MB, remoção, prévia com foto, troca de restaurante |
| `dashboard/catalog/catalog.service.spec.ts` | URLs, métodos e multipart dos endpoints `/model-3d` |
| `public-menu/public-menu.page.spec.ts` | Selo 3D, visualizador só sob demanda, AR, fallback para a foto, produto sem modelo |

Testado manualmente no navegador (modo demonstração): carregamento real do GLB de exemplo (`loaded`, dimensões 280 × 68 × 280 mm), botão de AR oculto no desktop, despublicar e publicar, recusa de `.obj` e de GLB falso, substituição com confirmação e novas medidas, remoção, e a biblioteca baixada só depois do clique.

`npm run build` (produção) e o build `demo` terminam sem erros nem avisos.

## 8. Pendências e limitações
- **Não testado contra a API real nem com o ImageKit**: o backend da PR #27 não está publicado e a homologação de GLB no ImageKit está pendente (pré-requisito do próprio backend).
- **AR não testado em celular real.** Conferido só que o botão fica oculto sem suporte. Validar em Android (Chrome) e iPhone (Safari/Quick Look) antes de liberar em produção. Não há promessa de compatibilidade universal.
- Ajustar só as dimensões exige reenviar o arquivo (falta endpoint no backend).
- O limite de 10 MB é provisório no backend; se mudar, atualizar `MODEL_3D_LIMITS`.
- Se o ImageKit servir o GLB em outro domínio, o CDN precisa permitir CORS para `https://borae-web.onrender.com`, porque o model-viewer baixa o arquivo via `fetch`. Validar na homologação.

## 9. Arquivos
**Criados:** `shared/model-3d-viewer.component.ts` (+ spec), `dashboard/catalog/product-model-3d.component.ts` (+ spec), `demo/demo-glb.ts`, `docs/changes/frontend-3d-ar-2026-11-17.md`.

**Modificados:**
- `dashboard/catalog/catalog.models.ts`, `catalog.service.ts`, `catalog-errors.ts`, `product-editor.page.ts`
- `dashboard/styles/catalog-product.css`
- `shared/menu-product-card.component.ts`
- `public-menu/public-menu.service.ts`, `public-menu.page.ts`, `public-menu.extras.css`
- `styles.css`, `demo/demo-catalog.ts`
- specs existentes: `catalog.service`, `product-editor.page`, `public-menu.page`
- `package.json` e `package-lock.json`: `@google/model-viewer` 4.3.1 e `three` 0.183.2

## 10. Próximas etapas
1. Merge da PR #27 e homologação do ImageKit (upload, MIME `model/gltf-binary`, CORS e cache).
2. Validação ponta a ponta com a API real e com modelos reais de pratos.
3. Teste de AR em celulares Android e iPhone reais.
4. Decidir com o backend o limite final de tamanho e um endpoint para ajustar só as dimensões.
