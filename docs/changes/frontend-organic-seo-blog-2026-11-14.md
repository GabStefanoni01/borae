# Registro de Alterações: Fundação de SEO Orgânico e Blog Editorial do Boraê

## Identificação
- **Projeto:** Boraê
- **Entrega:** SEO técnico, seção editorial na landing page e blog público de aquisição orgânica (10 artigos em 5 pilares)
- **Responsável:** Renan Cristiano
- **Data da publicação:** 14/11/2026

**Modificações feitas por Renan Cristiano**

---

## 1. Objetivo da entrega
Construir a primeira estrutura editorial e de aquisição orgânica da plataforma Boraê no frontend público (`frontend/`), preparando o site para rastreamento e indexação no Google, compartilhamento social qualificado e conversão ética de três públicos estratégicos: proprietários e gestores de restaurantes, consumidores apaixonados por gastronomia e potenciais colaboradores e parceiros comerciais.

---

## 2. Estratégia de aquisição orgânica
O Blog Boraê foi concebido como um canal contínuo de aquisição informacional e comercial:
1. **Conteúdo útil com valor próprio e linguagem natural:** cada artigo responde dúvidas reais de gestão gastronômica, apresentação de cardápios digitais, cultura alimentar brasileira, inteligência artificial e 3D na alimentação e fidelização de clientes, utilizando pontuação fluida do português brasileiro (vírgulas, dois-pontos, ponto e vírgula e parênteses, sem uso de travessões artificiais).
2. **Transparência sobre o estágio real da plataforma (`docs/ROADMAP.md` e `docs/OPEN_DECISIONS.md`):**
   - Não promete pedidos online ou integração com PDV/ERP no lançamento (D-20).
   - Apresenta o cardápio digital responsivo por QR Code como o núcleo do produto previsto no roadmap (D-02 a D-06).
   - Apresenta o assistente gastronômico com IA (Gemini Flash-Lite, D-10/D-11) e experiências 3D/AR (D-09) como módulos planejados, sem prometer diagnósticos nutricionais nem garantia de ausência de alérgenos.
   - Apresenta a fidelidade individual por restaurante (D-12 a D-15) como funcionalidade planejada e mantém o Boraê Club compartilhado fora do MVP (D-14).
3. **Caminhos naturais de conversão:**
   - **Proprietários e gestores de restaurantes:** direcionamento para `/auth` (*"Conheça o Boraê para restaurantes"*).
   - **Consumidores:** direcionamento para `/` (*"Conheça o Boraê"*).
   - **Colaboradores e parceiros:** direcionamento para `/#sobre` (*"Conheça nosso projeto"*), sem formulários fictícios sem endpoint.

---

## 3. Cinco pilares editoriais

| # | Pilar Editorial | Slug | Público Principal | Objetivo Estratégico |
|---|---|---|---|---|
| 1 | Gestão e inovação para restaurantes | `gestao-e-inovacao` | Proprietários, gestores e operadores | Atrair decisores B2B abordando dores reais de operação, equipe e presença digital. |
| 2 | Cardápios digitais e experiência do cliente | `cardapios-e-experiencia` | Restaurantes e consumidores | Conectar a busca por QR Code, clareza de informações e fotografia de pratos ao núcleo do Boraê. |
| 3 | Cultura, curiosidades e gastronomia | `cultura-e-gastronomia` | Consumidores e entusiastas | Ampliar alcance orgânico informacional valorizando ingredientes e tradições regionais brasileiras. |
| 4 | Tecnologia e o futuro da gastronomia | `tecnologia-e-gastronomia` | Restaurantes, consumidores e talentos tech | Posicionar o Boraê como referência em inovação responsável (IA, dados estruturados e 3D/AR). |
| 5 | Marketing, descoberta e relacionamento | `marketing-e-relacionamento` | Gestores e empreendedores gastronômicos | Educar sobre recorrência pós-visita, reputação verificada e programas de fidelidade individuais. |

---

## 4. Dez artigos originais publicados (2 por pilar)

1. **Presença digital para restaurantes (`gestao-e-inovacao`):**
   - Título: *Como a presença digital pode transformar a experiência de um restaurante*
   - Slug: `/blog/presenca-digital-para-restaurantes` (Destaque editorial com capa da identidade oficial Boraê em fundo branco: `assets/images/blog/borae-destaque-editorial.jpg`)
2. **Cardápio digital (`cardapios-e-experiencia`):**
   - Título: *Cardápio digital: como apresentar seus pratos de forma mais clara e atrativa*
   - Slug: `/blog/como-criar-um-cardapio-digital`
3. **Diversidade gastronômica (`cultura-e-gastronomia`):**
   - Título: *Por que a gastronomia brasileira é tão diversa? Uma viagem pelos sabores do país*
   - Slug: `/blog/diversidade-da-gastronomia-brasileira`
4. **Inteligência artificial (`tecnologia-e-gastronomia`):**
   - Título: *Como a inteligência artificial pode mudar a forma de descobrir pratos e restaurantes*
   - Slug: `/blog/inteligencia-artificial-na-gastronomia`
5. **Fidelização de clientes (`marketing-e-relacionamento`):**
   - Título: *Como restaurantes podem construir relacionamentos além da primeira visita*
   - Slug: `/blog/fidelizacao-de-clientes-em-restaurantes`
6. **Gestão de equipe e papéis (`gestao-e-inovacao`):**
   - Título: *Como organizar funções e permissões da equipe na rotina de um restaurante*
   - Slug: `/blog/organizacao-de-equipe-e-papeis-no-restaurante`
7. **Fotografia gastronômica (`cardapios-e-experiencia`):**
   - Título: *Fotografia gastronômica para cardápios: como valorizar seus pratos com autenticidade*
   - Slug: `/blog/fotografia-gastronomica-e-apresentacao-de-pratos`
8. **Cultura do café e hospitalidade (`cultura-e-gastronomia`):**
   - Título: *Do café coado ao pão de queijo: a cultura da hospitalidade à mesa no Brasil*
   - Slug: `/blog/cafe-pao-de-queijo-e-hospitalidade-brasileira`
9. **Modelos 3D e Realidade Aumentada (`tecnologia-e-gastronomia`):**
   - Título: *Modelos 3D e realidade aumentada na gastronomia: como funciona a visualização de pratos*
   - Slug: `/blog/modelos-3d-e-realidade-aumentada-em-cardapios`
10. **Avaliações e reputação digital (`marketing-e-relacionamento`):**
    - Título: *Avaliações de clientes em restaurantes: como construir reputação com transparência*
    - Slug: `/blog/avaliacoes-de-clientes-e-reputacao-digital-restaurantes`

---

## 5. Arquitetura do blog
Implementada em componentes standalone do Angular 20 em `frontend/src/app/blog/`:
- `blog.models.ts`: contratos tipados para categorias, blocos de conteúdo estruturados (`heading`, `paragraph`, `list`, `callout`, `image`), segmentos inline com suporte nativo a `RouterLink` (sem uso de `innerHTML`), fontes externas, créditos de fotografia e metadados de artigo.
- `blog.data.ts`: catálogo estático versionado dos 5 pilares e 10 artigos completos.
- `blog-seo.service.ts`: serviço de gerenciamento de `<title>`, `<meta name="description">`, `<link rel="canonical">`, Open Graph (`og:*`), Twitter Cards (`twitter:*`) e JSON-LD (`Organization`, `WebSite`, `Blog`, `BlogPosting` e `BreadcrumbList`).
- `blog.routes.ts`: definição de rotas filhas sob `/blog` com carregamento sob demanda (`loadComponent`).
- `blog-section.component.ts`: seção editorial incorporada à landing page principal (`/`), entre a seção de Identidade/Propósito (`#sobre`) e a FAQ (`#faq`).
- `blog.page.ts`: página pública `/blog` com cabeçalho editorial, busca textual sobre o acervo estático, filtro pelos 5 pilares (integrado ao query param `?categoria=`), listagem responsiva e seção de conversão para os 3 públicos.
- `blog-article.page.ts`: página individual `/blog/:slug` com breadcrumb, categoria, H1 único, subtítulo, autor, datas, tempo de leitura, capa 16:9 com créditos e links externos, corpo semântico, links internos contextuais, fontes consultadas, compartilhamento por link, artigos relacionados e CTA contextual.

---

## 6. Rotas criadas
- `/blog` (`BlogPage`)
- `/blog/:slug` (`BlogArticlePage`), atendendo aos 10 artigos publicados.

---

## 7. Metadados SEO e Domínio Oficial
- **Domínio canônico oficial configurado:** `https://borae-web.onrender.com`
- **Verificação do Google Search Console (`src/index.html`):**
  `<meta name="google-site-verification" content="LNBtubRhP0jRhpRvVbi9xvTFXKFZvfm5lO8qnl0Ml94" />`
- **Títulos e descrições exclusivos por rota:** atualizados dinamicamente via `BlogSeoService` e presentes na página inicial.
- **Open Graph e Twitter Cards:** `og:type` (`website` ou `article`), `og:site_name`, `og:title`, `og:description`, `og:url`, `og:image`, `og:locale` (`pt_BR`), `twitter:card` (`summary_large_image`), `twitter:title`, `twitter:description` e `twitter:image`.

---

## 8. Dados estruturados (JSON-LD)
- **Home (`/`):** `Organization` e `WebSite`.
- **Blog (`/blog`):** `Blog` (com lista dos 10 `BlogPosting`) e `BreadcrumbList`.
- **Artigos (`/blog/:slug`):** `BlogPosting` e `BreadcrumbList`.

---

## 9. Sitemap (`frontend/public/sitemap.xml`)
Disponibilizado na raiz pública (`https://borae-web.onrender.com/sitemap.xml`), listando `/` (`priority 1.0`), `/blog` (`priority 0.9`) e os 10 artigos publicados (`priority 0.8`). Rotas privadas (`/auth`, `/login`, `/cadastro`, `/app`) foram excluídas.

---

## 10. Robots (`frontend/public/robots.txt`)
Disponibilizado na raiz pública (`https://borae-web.onrender.com/robots.txt`), permitindo o rastreamento de `/`, `/blog` e `/assets/`, bloqueando `/app`, `/auth`, `/login` e `/cadastro`, e apontando para `Sitemap: https://borae-web.onrender.com/sitemap.xml`.

---

## 11. Estratégia de imagens e créditos
- Destaque editorial utilizando a imagem oficial da logo Boraê sobre fundo claro (`assets/images/blog/borae-destaque-editorial.jpg`).
- Imagens fotográficas em WebP/JPEG otimizadas em `frontend/public/assets/images/blog/` com créditos a Jonathan Borba e Alex Urezkov (Pexels) e ao Acervo Editorial Boraê.

---

## 12. Testes e Build
- `npm run build` executado em `frontend/` com código de saída `0`, `0` erros e `0` avisos.
