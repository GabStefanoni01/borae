# Registro de Alterações: Refinamento de Marca, Ícones, Animações e Login do Boraê

## Identificação
- **Projeto:** Boraê
- **Entrega:** Refinamento visual do frontend (marca, ícones, animações e página de login)
- **Responsável:** Renan Cristiano
- **Data:** 13/11/2026
- **Branch:** `feat/frontend-brand-refresh` (a partir de `feat/borae-landing-page`)

**Modificações feitas por Renan Cristiano**

---

## 1. Resumo
- Logo da marca passou a ser vetorial (SVG) e encaixada no header, drawer, rodapé, login e painel — sem o fundo creme do PNG quadrado.
- Todos os emojis usados como ícones foram removidos e substituídos por uma biblioteca própria de ícones em Angular (`<bo-icon>`).
- Animações leves e responsivas: entrada ao rolar a página, hero animado, flutuação suave, micro-interações em cards e botões, barra de progresso de leitura no header.
- Página de login refeita com a identidade da marca (laranja Boraê), substituindo o tema verde-limão anterior.
- Correções de bugs encontrados durante a revisão (fonte de títulos, drawer mobile e estouro horizontal no celular).

Nenhuma dependência nova foi adicionada. Nenhum arquivo de backend foi alterado.

---

## 2. Novos componentes compartilhados (`frontend/src/app/shared/`)
| Arquivo | O que faz |
|---|---|
| `icon.component.ts` | `<bo-icon name="..." [size]="20" />` — 40 ícones vetoriais de traço (24×24), cor via `currentColor`, acessíveis (decorativos por padrão, `label` para leitor de tela), opção `spin` para carregamento. |
| `brand-logo.component.ts` | `<bo-brand-logo />` — logotipo vetorial com o símbolo da cloche laranja + "Boraê" em Plus Jakarta Sans. Variantes `horizontal`, `stacked`, `mark`; tom `light`/`dark` para fundos claros e escuros; tamanho configurável. |
| `reveal.directive.ts` | `boReveal` / `[boReveal]="120"` — animação de entrada ao rolar (IntersectionObserver), com atraso para efeito cascata. Sem JS o conteúdo continua visível; respeita `prefers-reduced-motion`. |

---

## 3. Emojis substituídos
| Local | Antes | Depois (`bo-icon`) |
|---|---|---|
| Hero | ✨ 🍽️ 📱 👨‍🍳 ✓ → | sparkles, cloche, qr-code, chef-hat, check, arrow-right |
| Proposta | 🏪 🍽️ ✓ → | store, utensils, check (SVG), arrow-right |
| Experiência | 📜 🧊 🧠 ⭐ | book-open, cube, lightbulb, star |
| Restaurantes | ✨ 📸 📱 🤝 🧊 🎁 → | sparkles, camera, smartphone, link, cube, gift, arrow-right |
| Consumidores | 🥑 → | leaf, arrow-right |
| Como funciona | 🔍 📖 🤝 | search, book-open, link |
| Ecossistema | 🏢 👥 ✨ 🤖 🎯 🌟 ℹ️ | building, users, smartphone, sparkles, target, star, info |
| Propósito | 🧡 | heart |
| FAQ | ＋ | plus |
| CTA final | ✓ → | check-circle, arrow-right |
| Header (drawer) | ✕ | x |
| Login | ✳ ✓ ↗ | ícones de campo (mail, lock, user), eye/eye-off, check, arrow-right, log-out, loader |

---

## 4. Animações (leves e responsivas)
- **Header:** entrada suave, reduz de 80px para 68px ao rolar, sublinhado animado nos links, barra de progresso de leitura em degradê laranja.
- **Hero:** conteúdo entra em cascata; imagem com zoom de entrada; mockup e selo flutuam suavemente.
- **Seções:** cabeçalhos, cards e itens de lista aparecem ao rolar, em cascata (`boReveal`).
- **Cards:** elevação no hover e ícone que gira levemente e ganha fundo laranja (apenas em dispositivos com mouse — `@media (hover: hover)`).
- **Botões:** seta desliza no hover; leve "pressionar" no clique.
- **Drawer mobile:** backdrop com fade, botão fechar gira, bloqueio de rolagem do fundo enquanto aberto.
- Todas as animações são desativadas com `prefers-reduced-motion: reduce`.

---

## 5. Página de login (`/auth`)
- Layout dividido: painel de marca com foto gastronômica, logo, título e 3 benefícios com ícones; painel de acesso com card branco.
- Abas Entrar / Criar conta com indicador deslizante.
- Campos com ícone, foco em laranja, botão mostrar/ocultar senha, validação de e-mail.
- Botão com spinner durante o envio; mensagens de erro com ícone.
- Link "Voltar ao site".
- Mobile: painel de marca vira faixa compacta e o card sobe sobre a foto.
- **Lógica de autenticação inalterada** (mesmos endpoints, mensagens e redirecionamentos do `AuthService`).
- O CSS foi renomeado de `app.css` para `auth.page.css` (era usado apenas pela página de login).

---

## 6. Bugs corrigidos
1. **Fonte dos títulos não carregava:** a URL do Google Fonts tinha `Plus Jakarta+Sans` (com espaço). Corrigido para `Plus+Jakarta+Sans` em `index.html`.
2. **Drawer mobile transparente/cortado:** o drawer ficava dentro do `<header>`, que tem `backdrop-filter` — isso prendia o `position: fixed` à altura do header. O drawer e o backdrop foram movidos para fora do `<header>`.
3. **Drawer focável quando fechado:** agora fica `visibility: hidden` quando fechado (teclado/leitor de tela não entram nele).
4. **Estouro horizontal no celular:** CTAs longos com `white-space: nowrap` alargavam a página para 439px em telas de 375px. Em telas ≤ 640px os botões agora quebram linha.
5. **Links do menu quebrando linha** em larguras intermediárias: menu desktop aparece a partir de 1100px; abaixo disso, menu hambúrguer.

---

## 7. Outros ajustes
- Favicon vetorial (`frontend/public/favicon.svg`) com o símbolo da marca; o PNG oficial continua como fallback, `apple-touch-icon` e `og:image`.
- Logo vetorial também no topo do painel (`dashboard.page.ts`), sem outras mudanças no painel.
- Orçamento de CSS por componente (`angular.json`): aviso de 4 kB → 6 kB (erro continua em 8 kB). Hero e login passam de 4 kB por causa das animações e do novo layout.

---

## 8. Arquivos
**Criados**
- `frontend/src/app/shared/icon.component.ts`
- `frontend/src/app/shared/brand-logo.component.ts`
- `frontend/src/app/shared/reveal.directive.ts`
- `frontend/public/favicon.svg`
- `docs/changes/frontend-brand-refresh-2026-11-13.md`

**Modificados**
- `frontend/src/app/landing/*.component.ts` (todos os componentes de seção, header e footer)
- `frontend/src/app/auth.page.ts` e `frontend/src/app/auth.page.css` (antigo `app.css`)
- `frontend/src/app/dashboard.page.ts` (apenas a logo)
- `frontend/src/styles.css`, `frontend/src/index.html`, `frontend/angular.json`

---

## 9. Testes executados
- `npm run build` (produção): sucesso, **0 avisos, 0 erros**. Initial total 334.85 kB (≈92 kB transferidos); landing 73.6 kB (≈14.5 kB); login 15.2 kB (≈4.4 kB).
- Verificação no navegador (desktop 1024px e mobile 375px): landing completa, header compacto ao rolar, drawer mobile, rodapé, login (abas Entrar/Criar conta).
- Conferência automática: nenhum emoji no texto renderizado; nenhum elemento mais largo que a tela no mobile.
- Não testado: login real contra a API (depende do backend no Render e de CORS).

---

## 10. Publicação do frontend no Render (conta de Renan Cristiano)
O backend continua na conta do outro desenvolvedor (`https://borae.onrender.com`). O frontend é publicado separadamente. Duas opções:

### Opção A — Static Site (recomendada: gratuita, sem container, com CDN)
1. Render → **New → Static Site** → conectar o repositório `GabStefanoni01/borae`.
2. **Branch:** `main` (após o merge) ou a branch da entrega para testar.
3. **Root Directory:** `frontend`
4. **Build Command:** `npm ci && npm run build`
5. **Publish Directory:** `dist/borae-frontend/browser`
6. **Redirects/Rewrites:** adicionar regra *Rewrite* de `/*` para `/index.html` (necessário para as rotas `/auth` e `/app` funcionarem ao recarregar a página).
7. Variável opcional: `NODE_VERSION = 22`.

### Opção B — Web Service Docker (o que já existe no `render.yaml`)
1. Render → **New → Web Service** → mesmo repositório, **Runtime: Docker**.
2. **Dockerfile Path:** `./frontend/Dockerfile` · **Docker Context:** `./frontend`
3. **Health Check Path:** `/health` (o Nginx escuta na porta 10000, que é a padrão do Render).
4. Observação: o `render.yaml` da raiz também declara o `borae-api`; **não** use o Blueprint na sua conta, senão ele tentará criar o backend também.

### Obrigatório nos dois casos — CORS no backend
O backend só aceita origens listadas em `CORS_ALLOWED_ORIGINS` (`backend/src/main/resources/application.yml`). Depois que o Render gerar a URL do frontend (ex.: `https://borae-web-xxxx.onrender.com`), o outro desenvolvedor precisa adicioná-la nessa variável no serviço `borae-api`, separada por vírgula, por exemplo:

```
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://borae-web-xxxx.onrender.com
```

Sem isso, o login exibirá "Não foi possível conectar à API... configuração de CORS".

---

### Situação atual da publicação (13/11/2026)
- Static Site **`borae-web`** criado na conta de Renan Cristiano: **https://borae-web.onrender.com**
- Configuração: branch `feat/frontend-brand-refresh` (trocar para `main` após o merge), Build Command `cd frontend && npm ci && npm run build`, Publish Directory `frontend/dist/borae-frontend/browser`, `NODE_VERSION=22`, auto-deploy ativo.
- Pendente no Render: regra de *Rewrite* `/*` → `/index.html` (sem ela, abrir `/auth` ou `/app` direto pela URL retorna 404; navegar pelos links do site funciona).
- Pendente no backend: incluir a origem no `borae-api`:
  ```
  CORS_ALLOWED_ORIGINS=http://localhost:4200,https://borae-web.onrender.com
  ```

---

## 11. Limitações conhecidas
- A URL da API continua fixa em `auth.service.ts` (`https://borae.onrender.com`), como já documentado no guia de desenvolvimento.
- O painel (`/app`) mantém o tema escuro anterior; somente a logo foi atualizada. Alinhar o painel à identidade laranja é uma entrega futura.
- A branch parte de `feat/borae-landing-page`, que ainda não está em `main`; um PR desta branch para `main` incluirá também o commit da landing page.
