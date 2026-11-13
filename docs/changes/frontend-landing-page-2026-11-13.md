# Registro de Alterações: Landing Page Institucional e Comercial do Boraê

## Identificação
- **Projeto:** Boraê
- **Entrega:** Landing page institucional e comercial
- **Responsável:** Renan Cristiano
- **Data da publicação:** 13/11/2026

**Modificações feitas por Renan Cristiano**

---

## 1. Objetivo da entrega
Desenvolver a nova landing page oficial do Boraê, estabelecendo a presença digital institucional e comercial da marca como uma startup de tecnologia gastronômica de alto padrão. O objetivo principal é comunicar com clareza e autoridade a proposta de valor para restaurantes e consumidores, sem confundir o estágio atual da fundação tecnológica com funcionalidades ainda planejadas no roadmap.

---

## 2. Resumo da landing page
A página funciona como a porta de entrada pública do produto Boraê. Ela narra uma jornada coesa:
1. Apresenta o Boraê como a ponte inteligente entre quem prepara e quem aprecia experiências gastronômicas.
2. Segmenta o valor comercial para restaurantes (presença digital rica, valorização de marca, cardápio responsivo e preparação para 3D/AR).
3. Demonstra a experiência visual e inovadora para os consumidores (descoberta de restaurantes, cardápios interativos, clareza sobre pratos e benefícios futuros).
4. Elucida a visão de ecossistema, incluindo o Boraê Club como rede de benefícios compartilhados.
5. Esclarece o status real da plataforma através de uma seção de Perguntas Frequentes (FAQ) 100% alinhada à documentação oficial.
6. Direciona fluxos de conversão para o onboarding/login já existente no sistema (`/auth`).

---

## 3. Seções desenvolvidas
A página foi construída em 12 seções estruturadas:
- **Seção 1 — Header / Navegação:** Sticky header translúcido com efeito de blur, exibindo a logo oficial Boraê, links de navegação para todas as seções por âncoras (`#inicio`, `#restaurantes`, `#consumidores`, `#como-funciona`, `#sobre`, `#faq`), link de login para a plataforma (`/auth`), CTA destacado "Conheça o Boraê" e menu responsivo tipo drawer com acessibilidade completa (suporte a teclado e leitor de tela).
- **Seção 2 — Hero Principal:** Eyebrow oficial *"TECNOLOGIA E GASTRONOMIA EM UMA NOVA EXPERIÊNCIA"*, título principal *"A gastronomia vai além do prato. Descubra o Boraê."*, subtítulo envolvente, botões de ação ("Conheça o Boraê" e "Sou um restaurante"), indicadores de confiança e composição visual fotográfica acompanhada de mockup conceitual de cardápio digital explicitamente identificado como demonstração em desenvolvimento.
- **Seção 3 — Apresentação da Proposta:** Título *"Um novo jeito de viver a gastronomia."*, texto institucional e dois blocos de alto impacto com direcionamento específico: Bloco A (Para restaurantes — *"Sua marca merece ser descoberta."*) e Bloco B (Para consumidores — *"Descubra o próximo lugar que vai conquistar você."*).
- **Seção 4 — Experiência Gastronômica:** Título *"A tecnologia encontra o sabor."*, apresentando os quatro pilares essenciais do produto: Cardápio Digital, Experiência 3D e Realidade Aumentada, Inteligência Gastronômica e Fidelidade, todos sinalizados com tags de roadmap para transparência total.
- **Seção 5 — Para Restaurantes:** Foco comercial assertivo com o título *"Seu restaurante tem muito mais para mostrar."*, detalhamento dos 6 benefícios centrais sem promessas infladas de faturamento, fotografia de cozinha profissional e CTA dedicado.
- **Seção 6 — Para Consumidores:** Foco no público consumidor com o título *"Sua próxima experiência gastronômica começa aqui."*, detalhamento dos 5 passos da experiência e fotografia gourmet vibrante.
- **Seção 7 — Como o Boraê Funcionará:** Linha do tempo conceitual em 3 etapas sequenciais (01 Descubra, 02 Explore, 03 Conecte-se) com indicadores visuais de conexão.
- **Seção 8 — O Ecossistema Boraê:** Apresentação dos 6 pilares conectados (Restaurantes parceiros, Consumidores, Experiências digitais, Inteligência gastronômica, Fidelidade individual e o destaque ao Boraê Club como rede de benefícios compartilhados).
- **Seção 9 — Identidade e Propósito:** Seção institucional emotiva com o título *"Acreditamos que boas experiências merecem novas possibilidades."*, unindo fotografia de ambiente gastronômico e os três valores pilares da marca.
- **Seção 10 — Perguntas Frequentes (FAQ):** Accordion acessível com suporte total a ARIA, teclado e alternância suave, contemplando as 8 perguntas e respostas oficiais da documentação do Boraê.
- **Seção 11 — CTA Final:** Bloco de fechamento em degradê grafite e toques em laranja Boraê com o título *"A próxima experiência gastronômica pode começar aqui."*, direcionando usuários e restaurantes para o acesso à plataforma.
- **Seção 12 — Footer:** Rodapé institucional com a logo oficial, descrição *"Boraê — Experiências gastronômicas mais inteligentes."*, navegação pelas seções, link de status operacional e direitos autorais protegidos (2026).

---

## 4. Decisões de identidade visual
- **Uso estrito da logo oficial:** Utilizada a imagem original fornecida nos assets do projeto (`frontend/public/assets/images/borae-logo.png`), contendo o símbolo da tampa/cúpula gastronômica em laranja, tipografia preta "Boraê" e fundo claro. Não houve redesenho nem substituição arbitrária da marca.
- **Equilíbrio cromático:** Predomínio de fundos brancos (`#FFFFFF`) e superfícies quentes em creme (`#FFF9F2`, `#FAF5EE`), com o laranja oficial (`#FF7200`) atuando estritamente como ponto de reconhecimento, conversão e hierarquia.
- **Linguagem SaaS contemporânea:** Bordas arredondadas moderadas (`8px` a `28px`), sombras sutis de elevação, microinterações limpas em hover/focus e composição com fotografias gastronômicas profissionais.

---

## 5. Paleta de cores

| Token / Variável CSS | Código Hex | Aplicação Principal |
|---|---|---|
| `--color-primary` | `#FF7200` | Laranja principal oficial de destaque, CTAs e conversão |
| `--color-primary-dark` | `#E85D00` | Estados de hover em botões primários e ênfase tipográfica |
| `--color-primary-light` | `#FFF0E3` | Fundo de badges, chips e superfícies de destaque suave |
| `--color-primary-subtle` | `#FFF8F2` | Degradês sutis de seções e cartões conceituais |
| `--color-white` | `#FFFFFF` | Fundo principal da página e cartões de conteúdo |
| `--color-cream` | `#FFF9F2` | Superfície suave de contraste para blocos alternados |
| `--color-surface` | `#F5F5F5` | Fundo de seções estruturais (Experiência, Consumidores, FAQ) |
| `--color-graphite` | `#191919` | Tipografia de títulos, botões escuros e rodapé institucional |
| `--color-text-main` | `#191919` | Cor base de textos e títulos |
| `--color-text-muted` | `#666666` | Parágrafos de apoio e legendas secundárias |
| `--color-border` | `#ECE7E1` | Linhas delimitadoras e contornos de cards |

---

## 6. Tipografia
- **Títulos e Headings (`h1`, `h2`, `h3`):** *Plus Jakarta Sans* (pesos 700 e 800), transmitindo modernidade, precisão tecnológica e expressividade gastronômica.
- **Corpo de texto e interface:** *Inter* (pesos 400, 500 e 600), garantindo máxima legibilidade em telas de qualquer densidade de pixels.
- **Hierarquia:** Escalonamento fluido utilizando a função CSS `clamp()`, ajustando automaticamente o tamanho de títulos de `32px` a `60px` sem quebras de layout.

---

## 7. Componentes criados
Todos os componentes foram desenvolvidos como componentes standalone do Angular 20 sob o diretório `frontend/src/app/landing/`:
1. `landing.page.ts`: Orquestrador da rota `/`, gestor de metadados SEO e montagem semântica.
2. `header.component.ts`: Barra de navegação responsiva e drawer móvel acessível.
3. `hero.component.ts`: Seção de abertura com proposição de valor, fotografia e mockup.
4. `proposal.component.ts`: Blocos comparativos de público (Restaurantes vs. Consumidores).
5. `experience.component.ts`: Apresentação dos 4 pilares do produto com tags de roadmap.
6. `restaurants.component.ts`: Seção de valor comercial e operacional para parceiros.
7. `consumers.component.ts`: Seção de valor experiencial para o consumidor final.
8. `how-it-works.component.ts`: Jornada conceitual em 3 etapas conectadas.
9. `ecosystem.component.ts`: Grade do ecossistema e apresentação do Boraê Club.
10. `purpose.component.ts`: Identidade, propósito e manifesto de valor gastronômico.
11. `faq.component.ts`: Accordion acessível com 8 perguntas e respostas oficiais.
12. `cta.component.ts`: Chamada final de alto contraste para ação.
13. `footer.component.ts`: Rodapé semântico institucional com links de navegação.

---

## 8. Arquivos criados
- `frontend/src/app/landing/landing.page.ts`
- `frontend/src/app/landing/header.component.ts`
- `frontend/src/app/landing/hero.component.ts`
- `frontend/src/app/landing/proposal.component.ts`
- `frontend/src/app/landing/experience.component.ts`
- `frontend/src/app/landing/restaurants.component.ts`
- `frontend/src/app/landing/consumers.component.ts`
- `frontend/src/app/landing/how-it-works.component.ts`
- `frontend/src/app/landing/ecosystem.component.ts`
- `frontend/src/app/landing/purpose.component.ts`
- `frontend/src/app/landing/faq.component.ts`
- `frontend/src/app/landing/cta.component.ts`
- `frontend/src/app/landing/footer.component.ts`
- `frontend/public/assets/images/borae-logo.png` (Logo oficial da marca)
- `frontend/public/assets/images/hero-dish.jpg` (Fotografia culinária para o Hero)
- `frontend/public/assets/images/chef-prep.jpg` (Fotografia profissional para Restaurantes)
- `frontend/public/assets/images/consumer-dish.jpg` (Fotografia de mesa compartilhada para Consumidores)
- `frontend/public/assets/images/restaurant-kitchen.jpg` (Fotografia de ambiente para Propósito)
- `docs/changes/frontend-landing-page-2026-11-13.md` (Este documento)

---

## 9. Arquivos modificados
- `frontend/src/app/app.routes.ts`: Rota raiz (`path: ''`) configurada para a `LandingPage`; rota de autenticação configurada em `path: 'auth'` (com redirecionamentos das rotas `login` e `cadastro`); rota `app` mantida sob proteção de `authGuard`.
- `frontend/src/app/auth.guard.ts`: Redirecionamento de usuários não autenticados ajustado para `/auth`.
- `frontend/src/app/auth.page.ts`: Inclusão da logo oficial Boraê no cabeçalho e botão de acesso direto ao painel (`/app`) quando o usuário já possuir sessão ativa.
- `frontend/src/app/dashboard.page.ts`: Inclusão da logo oficial Boraê na barra superior de gestão.
- `frontend/src/app/app.css`: Encapsulamento de plano de fundo e cores do `AuthPage` no seletor `:host`, preservando intacta sua estética anterior.
- `frontend/src/index.html`: Metadados de SEO, Open Graph, Favicon oficial e fontes Google Fonts (*Plus Jakarta Sans* e *Inter*).
- `frontend/src/styles.css`: Definição dos tokens de design, reset responsivo, estados acessíveis de foco e classes de botões do ecossistema Boraê.

---

## 10. Dependências adicionadas
Nenhuma dependência externa ou biblioteca de terceiros foi adicionada. Todo o layout, comportamento responsivo, animações e accordion foram concebidos com **CSS nativo e Angular puro**, mantendo o `package.json` enxuto e sem riscos de segurança.

---

## 11. Melhorias de responsividade
- **Layout fluido com CSS Grid e Flexbox:** Ajuste automático entre resoluções desktop (telas amplas), notebooks (1024px-1366px), tablets (768px-1024px) e smartphones (320px-640px).
- **Sem rolagem horizontal:** Nenhuma seção ou componente extrapola a largura da viewport (`overflow-x: hidden` no body e containers limitados).
- **Menu mobile acessível:** Menu gaveta (drawer) com abertura lateral fluida, fechamento por clique fora, clique em âncora ou tecla `Escape`.
- **Área de toque touch-friendly:** Botões e âncoras no mobile possuem altura mínima de 48px para facilitar o toque com o polegar.

---

## 12. Melhorias de acessibilidade
- **HTML Semântico:** Uso apropriado de `<header>`, `<main>`, `<section>`, `<article>`, `<nav>`, `<footer>` e hierarquia correta de headings (`h1` único por página, seguido por `h2` e `h3`).
- **Contraste em conformidade WCAG AA:** Relação de contraste testada e validada para leitura confortável de textos sobre superfícies brancas, creme e grafite.
- **Suporte total a leitores de tela:** Tags `aria-expanded`, `aria-controls`, `role="region"`, `aria-labelledby` e textos auxiliares acessíveis com a classe `.sr-only`.
- **Foco por teclado visível:** `:focus-visible` estilizado com contorno de 2px na cor primária laranja.
- **Prefers-reduced-motion:** Media query implementada que desativa transições e animações para usuários com sensibilidade a movimentos.

---

## 13. SEO implementado
- **Título da página:** `Boraê | Experiências gastronômicas mais inteligentes`
- **Meta Description:** `Conheça o Boraê, uma plataforma que conecta restaurantes e consumidores por meio de tecnologia, descoberta e novas experiências gastronômicas.`
- **Open Graph (OG):** Tags `og:title`, `og:description`, `og:type`, `og:image` (apontando para a logo oficial Boraê) e `og:locale` (`pt_BR`).
- **Favicon:** Configurado como link rel icon apontando para o asset oficial `assets/images/borae-logo.png`.
- **Indexação:** Meta tag `robots` configurada como `index, follow`.

---

## 14. Otimizações de performance
- **Assets locais:** Imagens salvas localmente em `frontend/public/assets/images/`, eliminando dependências externas ou latência de CDNs de terceiros.
- **Lazy loading:** Imagens abaixo da primeira dobra configuradas com `loading="lazy"`. Imagem principal do Hero configurada com `loading="eager"`.
- **Carregamento sob demanda (Lazy routing):** A rota da landing page e a rota de autenticação utilizam dynamic import (`loadComponent`), resultando em chunks pequenos e rápidos.
- **Respeito aos budgets do Angular:** Build de produção gerado com total conformidade (`styles.css` em 3.96 kB, bundle total inicial em apenas 90.0 kB transferidos, sem nenhum aviso ou estouro de orçamento).

---

## 15. Testes executados
- **Testes de compilação e tipagem:** `ng build` executado em modo de produção (gerando os artefatos estáticos em `dist/borae-frontend`).
- **Inspeção de rotas:** Verificada a navegação de `/` para `/auth` e `/app`.
- **Validação visual e estrutural:** Checagem da integridade de todos os elementos gráficos e da presença de todas as seções descritas na especificação.
- **Nota sobre testes de backend:** Como estabelecido nas diretrizes, nenhum arquivo de backend foi modificado ou executado para evitar interferência na infraestrutura alheia.

---

## 16. Resultado do build
Comando executado:
```bash
npm run build
```
Resultado:
- **Status:** Sucesso absoluto (Código de saída: 0).
- **Initial Total:** 328.87 kB (Transferência estimada: 90.00 kB).
- **Chunk Lazy (Landing Page):** 66.85 kB (Transferência estimada: 13.02 kB).
- **Avisos de estilo (Style budgets):** 0 avisos.
- **Erros:** 0 erros.

---

## 17. Limitações conhecidas
- A navegação entre seções da landing page opera através de âncoras na mesma página (`#restaurantes`, `#consumidores`, etc.).
- O botão "Conheça o Boraê" e os CTAs comerciais direcionam para a rota de cadastro/login existente (`/auth`), pois formulários específicos de pré-inscrição de parceiros ainda não foram criados no backend.
- A persistência de sessão do usuário no frontend continua utilizando `localStorage` provisoriamente, conforme documentado no roadmap do projeto.

---

## 18. Funcionalidades que continuam no roadmap
Conforme detalhado no `docs/roadmap.md`, permanecem como entregas futuras:
- Módulo de catálogo (CRUD de categorias, pratos, variações e preços).
- Upload e exibição de modelos tridimensionais (GLB) e realidade aumentada (AR).
- Assistente gastronômico inteligente com IA.
- Sistema de fidelidade individual por restaurante e ledger de pontos.
- Rede de benefícios compartilhados do Boraê Club.
- Descoberta pública geolocalizada e pedidos/pagamentos.

---

## 19. Instruções de execução local
Requisitos: Node.js 22 ou superior.
1. Navegue até o diretório do frontend:
   ```bash
   cd frontend
   ```
2. Instale as dependências:
   ```bash
   npm install
   ```
3. Inicie o servidor de desenvolvimento:
   ```bash
   npm start
   ```
4. Acesse no navegador:
   ```
   http://localhost:4200
   ```

---

## 20. Instruções relevantes para publicação independente do frontend
O frontend do Boraê foi arquitetado para ser publicado como um serviço estático independente no Render (na conta de Renan Cristiano):
1. **Dockerfile Multi-Stage:** O arquivo `frontend/Dockerfile` já realiza a compilação do Angular com Node.js e copia os arquivos compilados da pasta `dist/borae-frontend/browser` para a imagem Nginx de runtime (`nginx:1.27-alpine`).
2. **Nginx SPA Routing:** O arquivo `frontend/nginx.conf` já conta com a diretiva `try_files $uri $uri/ /index.html;`, garantindo suporte nativo a rotas SPA (Single Page Application) em qualquer refresh de página.
3. **Healthcheck:** O Nginx responde a `/health` com código `200 ok` na porta `10000`.
4. **Isolamento Total:** A publicação do frontend não requer e não afeta o deploy do backend mantido pelo outro desenvolvedor, comunicando-se estritamente através da API publicada (`https://borae.onrender.com`).

---

## 21. Branch utilizada
- Branch criada e utilizada: `feat/borae-landing-page` (ramificada a partir de `main`).

---

## 22. Commits realizados
- Commit planejado: `feat(frontend): create Boraê institutional landing page`
