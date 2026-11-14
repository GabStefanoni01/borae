# Registro de Alterações: Fundação de SEO Orgânico e Blog Editorial do Boraê

## Identificação
- **Projeto:** Boraê
- **Entrega:** SEO técnico, seção editorial na landing page e blog público de aquisição orgânica
- **Responsável:** Renan Cristiano
- **Data da publicação:** 14/11/2026

**Modificações feitas por Renan Cristiano**

---

## 1. Objetivo da entrega
Construir a primeira estrutura editorial e de aquisição orgânica da plataforma Boraê no frontend público (`frontend/`), preparando o site para rastreamento e indexação no Google, compartilhamento social qualificado e conversão ética de três públicos estratégicos: proprietários e gestores de restaurantes, consumidores apaixonados por gastronomia e potenciais colaboradores e parceiros comerciais.

---

## 2. Estratégia de aquisição orgânica
O Blog Boraê foi concebido como um canal contínuo de aquisição informacional e comercial:
1. **Conteúdo útil com valor próprio:** cada artigo responde dúvidas reais de gestão gastronômica, apresentação de cardápios digitais, cultura alimentar brasileira, inteligência artificial na alimentação e fidelização de clientes, sem transformar os textos em anúncios artificiais.
2. **Transparência sobre o estágio real da plataforma (`docs/ROADMAP.md` e `docs/OPEN_DECISIONS.md`):**
   - Não promete pedidos online ou integração com PDV/ERP no lançamento (D-20).
   - Apresenta o cardápio digital responsivo por QR Code como o núcleo do produto previsto no roadmap (D-02 a D-06).
   - Apresenta o assistente gastronômico com IA (Gemini Flash-Lite, D-10/D-11) e experiências 3D/AR (D-09) como módulos planejados, sem prometer diagnósticos nutricionais nem garantia de ausência de alérgenos.
   - Apresenta a fidelidade individual por restaurante (D-12 a D-15) como funcionalidade planejada e mantém o Boraê Club compartilhado fora do MVP (D-14).
3. **Caminhos naturais de conversão:**
   - **Proprietários e gestores de restaurantes:** direcionamento para `/auth` (*"Conheça o Boraê para restaurantes"*).
   - **Consumidores:** direcionamento para `/` (*"Conheça o Boraê"*).
   - **Colaboradores e parceiros:** direcionamento para `/#sobre` (*"Conheça nosso projeto"*), sem formulários sem endpoint.

---

## 3. Cinco pilares editoriais

| # | Pilar Editorial | Slug | Público Principal | Objetivo Estratégico |
|---|---|---|---|---|
| 1 | Gestão e inovação para restaurantes | `gestao-e-inovacao` | Proprietários, gestores e operadores | Atrair decisores B2B abordando dores reais de operação, organização de catálogo e presença digital. |
| 2 | Cardápios digitais e experiência do cliente | `cardapios-e-experiencia` | Restaurantes e consumidores | Conectar a busca por QR Code, clareza de informações e fotografia de pratos ao núcleo do Boraê. |
| 3 | Cultura, curiosidades e gastronomia | `cultura-e-gastronomia` | Consumidores e entusiastas | Ampliar alcance orgânico informacional valorizando ingredientes e tradições regionais brasileiras. |
| 4 | Tecnologia e o futuro da gastronomia | `tecnologia-e-gastronomia` | Restaurantes, consumidores e talentos tech | Posicionar o Boraê como referência em inovação responsável (IA, dados estruturados e 3D/AR). |
| 5 | Marketing, descoberta e relacionamento | `marketing-e-relacionamento` | Gestores e empreendedores gastronômicos | Educar sobre recorrência pós-visita, reputação local e programas de fidelidade individuais. |

---

## 4. Copy e artigos iniciais publicados

### Artigo 1 — Pilar: Gestão e inovação (`gestao-e-inovacao`)
- **Título:** Como a presença digital pode transformar a experiência de um restaurante
- **SEO Title:** `Presença digital para restaurantes: por onde começar | Boraê`
- **Meta Description:** `Entenda como uma presença digital bem estruturada ajuda restaurantes a apresentar seus pratos, comunicar sua identidade e melhorar a experiência dos clientes.`
- **Slug:** `presenca-digital-para-restaurantes`
- **CTA:** *"Seu restaurante merece uma presença digital à altura da experiência que oferece."* → Botão: *"Conheça o Boraê para restaurantes"* (`/auth`)

### Artigo 2 — Pilar: Cardápios digitais e experiência (`cardapios-e-experiencia`)
- **Título:** Cardápio digital: como apresentar seus pratos de forma mais clara e atrativa
- **SEO Title:** `Cardápio digital para restaurantes: guia completo | Boraê`
- **Meta Description:** `Descubra como estruturar um cardápio digital com boas imagens, descrições claras, categorias organizadas e uma experiência simples para o consumidor.`
- **Slug:** `como-criar-um-cardapio-digital`
- **CTA:** *"Transforme a maneira como seu restaurante apresenta seus pratos."* → Botão: *"Conheça a proposta do Boraê"* (`/auth`)

### Artigo 3 — Pilar: Cultura, curiosidades e gastronomia (`cultura-e-gastronomia`)
- **Título:** Por que a gastronomia brasileira é tão diversa? Uma viagem pelos sabores do país
- **SEO Title:** `Gastronomia brasileira: sabores e tradições regionais | Boraê`
- **Meta Description:** `Conheça a diversidade da gastronomia brasileira e descubra como ingredientes, tradições e influências regionais ajudam a construir a identidade culinária do país.`
- **Slug:** `diversidade-da-gastronomia-brasileira`
- **CTA:** *"Descobrir novos sabores também é descobrir novas histórias."* → Botão: *"Conheça o universo Boraê"* (`/`)

### Artigo 4 — Pilar: Tecnologia e o futuro da gastronomia (`tecnologia-e-gastronomia`)
- **Título:** Como a inteligência artificial pode mudar a forma de descobrir pratos e restaurantes
- **SEO Title:** `Inteligência artificial na gastronomia: possibilidades e tendências | Boraê`
- **Meta Description:** `Entenda como a inteligência artificial pode apoiar a descoberta gastronômica, interpretar preferências e tornar a experiência digital mais personalizada.`
- **Slug:** `inteligencia-artificial-na-gastronomia`
- **CTA:** *"A tecnologia pode aproximar pessoas de experiências que combinam com seus gostos."* → Botão: *"Conheça a visão do Boraê"* (`/#sobre`)

### Artigo 5 — Pilar: Marketing, descoberta e relacionamento (`marketing-e-relacionamento`)
- **Título:** Como restaurantes podem construir relacionamentos além da primeira visita
- **SEO Title:** `Fidelização de clientes para restaurantes: estratégias | Boraê`
- **Meta Description:** `Conheça estratégias para fortalecer o relacionamento entre restaurantes e clientes, criar experiências memoráveis e estimular novas visitas.`
- **Slug:** `fidelizacao-de-clientes-em-restaurantes`
- **CTA:** *"Construa uma experiência que seus clientes tenham vontade de repetir."* → Botão: *"Conheça o Boraê para restaurantes"* (`/auth`)

---

## 5. Arquitetura do blog
Implementada em componentes standalone do Angular 20 em `frontend/src/app/blog/`:
- `blog.models.ts`: contratos tipados para categorias, blocos de conteúdo estruturados (`heading`, `paragraph`, `list`, `callout`, `image`), segmentos inline com suporte nativo a `RouterLink` (sem uso de `innerHTML`), fontes externas, créditos de fotografia e metadados de artigo.
- `blog.data.ts`: catálogo estático versionado dos 5 pilares e 5 artigos completos.
- `blog-seo.service.ts`: serviço de gerenciamento de `<title>`, `<meta name="description">`, `<link rel="canonical">`, Open Graph (`og:*`), Twitter Cards (`twitter:*`) e JSON-LD (`Organization`, `WebSite`, `Blog`, `BlogPosting` e `BreadcrumbList`).
- `blog.routes.ts`: definição de rotas filhas sob `/blog` com carregamento sob demanda (`loadComponent`).
- `blog-section.component.ts`: seção editorial incorporada à landing page principal (`/`), entre a seção de Identidade/Propósito (`#sobre`) e a FAQ (`#faq`).
- `blog.page.ts`: página pública `/blog` com cabeçalho editorial, busca textual sobre o acervo estático, filtro pelos 5 pilares (integrado ao query param `?categoria=`), listagem responsiva e seção de conversão para os 3 públicos.
- `blog-article.page.ts`: página individual `/blog/:slug` com breadcrumb, categoria, H1 único, subtítulo, autor, datas, tempo de leitura, capa 16:9 com créditos e links externos, corpo semântico, links internos contextuais, fontes consultadas, compartilhamento por link, artigos relacionados e CTA contextual.

---

## 6. Rotas criadas
- `/blog` (`BlogPage`)
- `/blog/:slug` (`BlogArticlePage`), atendendo:
  - `/blog/presenca-digital-para-restaurantes`
  - `/blog/como-criar-um-cardapio-digital`
  - `/blog/diversidade-da-gastronomia-brasileira`
  - `/blog/inteligencia-artificial-na-gastronomia`
  - `/blog/fidelizacao-de-clientes-em-restaurantes`

---

## 7. Metadados SEO
- **Domínio canônico configurado:** `https://borae1.onrender.com`
- **Verificação do Google Search Console (`src/index.html`):**
  `<meta name="google-site-verification" content="LNBtubRhP0jRhpRvVbi9xvTFXKFZvfm5lO8qnl0Ml94" />`
- **Títulos e descrições exclusivos por rota:** atualizados dinamicamente via `BlogSeoService` e presentes na página inicial.
- **Open Graph e Twitter Cards:** `og:type` (`website` ou `article`), `og:site_name`, `og:title`, `og:description`, `og:url`, `og:image`, `og:locale` (`pt_BR`), `twitter:card` (`summary_large_image`), `twitter:title`, `twitter:description` e `twitter:image`.

---

## 8. Dados estruturados (JSON-LD)
- **Home (`/`):** `Organization` e `WebSite`.
- **Blog (`/blog`):** `Blog` (com lista de `BlogPosting`) e `BreadcrumbList`.
- **Artigos (`/blog/:slug`):** `BlogPosting` e `BreadcrumbList`.
- Nenhuma avaliação fictícia, endereço inexistente ou telefone inventado foi incluído.

---

## 9. Sitemap (`frontend/public/sitemap.xml`)
Disponibilizado na raiz pública (`/sitemap.xml`), listando `/` (`priority 1.0`), `/blog` (`priority 0.9`) e os 5 artigos publicados (`priority 0.8`). Rotas privadas (`/auth`, `/login`, `/cadastro`, `/app`) foram excluídas.

---

## 10. Robots (`frontend/public/robots.txt`)
Disponibilizado na raiz pública (`/robots.txt`), permitindo o rastreamento de `/`, `/blog` e `/assets/`, bloqueando `/app`, `/auth`, `/login` e `/cadastro`, e apontando para `Sitemap: https://borae1.onrender.com/sitemap.xml`.

---

## 11. Estratégia de imagens
- Imagens salvas localmente em `frontend/public/assets/images/blog/` com nomes descritivos orientados a SEO (`chef-preparando-prato-restaurante.webp`, `cardapio-digital-experiencia-cliente.webp`, `gastronomia-brasileira-pratos-regionais.webp`, incluindo variantes responsivas `-640w.webp` para `srcset`).
- Proporção 16:9 com atributos `width` e `height` explícitos, `loading="eager"` na imagem principal do artigo e `loading="lazy"` nas demais imagens.

---

## 12. Créditos dos fotógrafos
1. **Jonathan Borba / Pexels** — https://www.pexels.com/photo/a-chef-preparing-a-dish-in-a-restaurant-26733054/
2. **Jonathan Borba / Pexels** — https://www.pexels.com/photo/professional-chef-preparing-meal-in-modern-kitchen-28703283/
3. **Alex Urezkov / Pexels** — https://www.pexels.com/photo/man-preparing-food-in-a-restaurant-19300180/
4. **Acervo Editorial Boraê** — Fotografias institucionais locais do projeto.

---

## 13. Links internos
Todos os 5 artigos contêm links internos contextuais distribuídos ao longo dos parágrafos e um painel dedicado de leitura complementar conectando os pilares entre si (`gestao-e-inovacao`, `cardapios-e-experiencia`, `cultura-e-gastronomia`, `tecnologia-e-gastronomia` e `marketing-e-relacionamento`), além de links para `/` e `/auth`.

---

## 14. CTAs
- **Restaurantes:** direcionamento para `/auth` (*"Conheça o Boraê para restaurantes"* / *"Conheça a proposta do Boraê"*).
- **Consumidores:** direcionamento para `/` (*"Conheça o Boraê"* / *"Conheça o universo Boraê"*).
- **Colaboradores e Parceiros:** direcionamento para `/#sobre` (*"Conheça nosso projeto"* / *"Conheça a visão do Boraê"*).

---

## 15. Alterações no Header e Footer
- **Header (`header.component.ts`):** inclusão do link `"Blog"` (`/blog`) no menu desktop e no drawer mobile; ajuste das âncoras institucionais (`/#inicio`, `/#restaurantes`, `/#consumidores`, `/#como-funciona`, `/#sobre`, `/#faq`, `/#conheca`) para funcionarem também a partir de `/blog`.
- **Footer (`footer.component.ts`):** inclusão da coluna **CONTEÚDO** com links para `Blog Boraê` e cada um dos 5 pilares editoriais (`/blog?categoria=...`), preservando todos os links existentes.

---

## 16. Responsividade
Layout fluido testado de `320px` a `1440px+`:
- Mobile (`< 680px`): 1 coluna, botões touch-friendly e tipografia escalável com `clamp()`.
- Tablet (`680px–1040px`): 2 colunas nos grids de artigos e pilares.
- Desktop (`> 1040px`): destaque editorial em duas colunas amplas e grids de 3 a 5 colunas.

---

## 17. Acessibilidade
- Um único `<h1>` por página e hierarquia semântica `<h2>`/`<h3>`.
- Landmarks HTML5 (`<header>`, `<nav>`, `<main>`, `<article>`, `<section>`, `<aside>`, `<figure>`, `<figcaption>`, `<footer>`).
- Atributos ARIA em abas de filtro (`role="tablist"`, `role="tab"`, `aria-selected`), botões de filtro (`aria-pressed`), breadcrumb (`aria-label="Breadcrumb"`, `aria-current="page"`) e busca (`role="search"`).
- Foco visível (`:focus-visible`) e suporte a `prefers-reduced-motion`.

---

## 18. Performance
- Zero novas dependências externas adicionadas ao `package.json`.
- Rotas `/blog` e `/blog/:slug` carregadas via lazy loading (`loadChildren` / `loadComponent`).
- Imagens WebP otimizadas (25 kB a 121 kB) com `srcset` e `loading="lazy"`.

---

## 19. Testes executados
- Compilação completa e verificação de tipos e templates Angular em modo produção (`npm run build`).
- Verificação de budgets de estilo (`anyComponentStyle < 6 kB`) e bundle inicial (`< 500 kB`).
- Verificação da geração de `sitemap.xml`, `robots.txt` e assets em `dist/borae-frontend/browser`.

---

## 20. Resultado do build
Comando executado em `frontend/`:
```bash
npm run build
```
Resultado:
- **Status:** Sucesso (código de saída `0`, `0` erros e `0` avisos).
- **Initial Total:** ~309.42 kB (90.15 kB transferidos).
- **Lazy Chunks:** `landing-page` (~70.95 kB), `blog-article-page` (~20.5 kB), `blog-page` (~17.2 kB).

---

## 21. Limitações do Angular SPA
Como a aplicação é publicada atualmente como SPA estática servida por Nginx (`try_files $uri $uri/ /index.html;`), os metadados específicos de cada artigo (`BlogSeoService`) são injetados no DOM no cliente quando o JavaScript é executado. Embora o Googlebot processe JavaScript, robôs de redes sociais que não executam JS leem os metadados iniciais do `index.html`.

---

## 22. Necessidades de SSR / Prerender
Plano recomendado para a próxima etapa técnica no frontend:
- Habilitar **Static Site Generation (Prerender / SSG)** com `@angular/ssr` para as rotas públicas conhecidas (`/`, `/blog` e os 5 slugs de artigos), mantendo `/auth` e `/app` como rotas SPA puras. Isso preserva o deploy estático via Nginx no Render sem necessidade de servidor Node.js em produção.

---

## 23. Pendências de backend
Documentadas na Seção 17 da especificação para evolução futura do blog pelo time de backend:
- CMS administrativo para criação, edição, rascunho, agendamento e publicação de artigos.
- Endpoints públicos (`GET /api/v1/public/blog/articles`, `GET /api/v1/public/blog/articles/{slug}`, `GET /api/v1/public/blog/categories`).
- Gestão de mídia editorial no Cloudflare R2 com variantes WebP/AVIF e créditos obrigatórios.
- Geração dinâmica de `sitemap.xml`, controle de redirects `301` de slugs antigos e status HTTP `404` real.
- Endpoints públicos de captação comercial de restaurantes e contato de colaboradores/parceiros com consentimento LGPD e envio via Resend.

---

## 24. Pendências de frontend
- Ativação de prerender estático (`@angular/ssr`) para geração de um `index.html` por artigo no momento do build.
- Integração com os endpoints públicos de artigos e formulários de contato assim que forem disponibilizados pelo backend.

---

## 25. Configurações necessárias no Search Console
- Meta tag `<meta name="google-site-verification" content="LNBtubRhP0jRhpRvVbi9xvTFXKFZvfm5lO8qnl0Ml94" />` já inserida em `frontend/src/index.html`.
- Após o deploy em produção, clicar em **Verificar** no Google Search Console, submeter `sitemap.xml` e acompanhar cobertura de indexação e desempenho.

---

## 26. Arquivos criados
- `frontend/public/sitemap.xml`
- `frontend/public/robots.txt`
- `frontend/public/assets/images/blog/chef-preparando-prato-restaurante.webp`
- `frontend/public/assets/images/blog/chef-preparando-prato-restaurante-640w.webp`
- `frontend/public/assets/images/blog/cardapio-digital-experiencia-cliente.webp`
- `frontend/public/assets/images/blog/cardapio-digital-experiencia-cliente-640w.webp`
- `frontend/public/assets/images/blog/gastronomia-brasileira-pratos-regionais.webp`
- `frontend/public/assets/images/blog/gastronomia-brasileira-pratos-regionais-640w.webp`
- `frontend/public/assets/images/blog/inteligencia-artificial-descoberta-gastronomica.jpg`
- `frontend/public/assets/images/blog/fidelizacao-clientes-restaurantes-experiencia.jpg`
- `frontend/public/assets/images/blog/organizacao-cozinha-apresentacao-pratos.jpg`
- `frontend/public/assets/images/blog/ambiente-restaurante-hospitalidade-mesa.jpg`
- `frontend/src/app/blog/blog.models.ts`
- `frontend/src/app/blog/blog.data.ts`
- `frontend/src/app/blog/blog-seo.service.ts`
- `frontend/src/app/blog/blog.routes.ts`
- `frontend/src/app/blog/blog-section.component.ts`
- `frontend/src/app/blog/blog.page.ts`
- `frontend/src/app/blog/blog-article.page.ts`
- `docs/changes/frontend-organic-seo-blog-2026-11-14.md`

---

## 27. Arquivos modificados
- `frontend/src/index.html`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/landing/landing.page.ts`
- `frontend/src/app/landing/header.component.ts`
- `frontend/src/app/landing/footer.component.ts`

---

## 28. Branch
- `feat/organic-seo-blog` (atualizada com `main`).

---

## 29. Commits
- `feat(frontend): add organic SEO foundation and Boraê editorial blog`

---

## 30. Próximas etapas
1. Validar a verificação de propriedade no Google Search Console após a publicação no Render e enviar `/sitemap.xml`.
2. Planejar a ativação de prerender estático (`@angular/ssr`) para as rotas públicas do blog.
3. Alinhar com o desenvolvedor de backend os contratos futuros de API pública do blog e captação de contatos.
