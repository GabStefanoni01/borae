# Registro de Alterações: Dashboard do Restaurante, Tema Claro/Escuro e Modo Demonstração

## Identificação
- **Projeto:** Boraê
- **Entrega:** Dashboard do restaurante (estrutura visual definitiva + módulos atuais integrados)
- **Responsável:** Renan Cristiano
- **Data da publicação:** 13/11/2026
- **Branch:** `feat/restaurant-dashboard` (a partir de `main`, após o merge do PR #13)

**Modificações feitas por Renan Cristiano**

---

## 1. Objetivo
Estabelecer o ambiente autenticado definitivo do Boraê como um SaaS profissional centrado no **restaurante selecionado**, com navegação por módulos, separação rigorosa entre o que já funciona (dados reais da API) e o que está planejado, além de tema claro/escuro e um modo de demonstração local para testes sem tocar na API real.

## 2. Estrutura do dashboard
Rota `/app` (protegida pelo `authGuard`, carregada sob demanda):

- **Sidebar** (fixa no desktop, gaveta no mobile/tablet): logo oficial, identificação "Painel do restaurante", seletor de restaurante e navegação em seis grupos — Visão geral, Estabelecimento, Cardápio, Experiência, Relacionamento e Conta. Módulos planejados exibem a marca discreta "Em breve".
- **Topbar**: breadcrumb, restaurante em gestão + papel do usuário, alternador de tema e menu da conta (nome/e-mail, Minha conta, Site do Boraê, tema, Sair). Não há notificações: não existe suporte no backend.
- **Conteúdo**: largura máxima de 1180px, cabeçalho de página padrão, estados de carregamento, erro (com "Tentar novamente"), vazio, sem permissão e planejado.

| Rota | Página | Situação |
|---|---|---|
| `/app` | Visão geral | Disponível (dados reais) |
| `/app/restaurante` | Meu restaurante | Parcial (dados reais + perfil completo planejado) |
| `/app/equipe` | Equipe | Disponível (integrada aos 4 endpoints) |
| `/app/novo-restaurante` | Cadastrar restaurante | Disponível |
| `/app/convite` | Aceitar convite | Disponível |
| `/app/configuracoes` | Configurações | Parcial (conta somente leitura) |
| `/app/cardapios`, `categorias`, `produtos`, `qr-code`, `experiencias-3d`, `assistente-ia`, `fidelidade`, `assinatura`, `integracoes` | Página explicativa | Em planejamento |

## 3. Identidade visual
- **Logo oficial**: extraída da folha oficial de variações (`versoes.logo.borae.png`) sem redesenho, com fundo transparente gerado por "un-premultiply" (preserva o antisserrilhado). Arquivos em `frontend/public/assets/images/brand/`:
  - `borae-lockup-on-light.png` / `borae-lockup-on-dark.png` — cloche | Boraê (na versão para fundo claro, apenas o texto foi recolorido para grafite);
  - `borae-stacked-on-light.png` / `borae-stacked-on-dark.png` — com "Gastronomia ao seu alcance";
  - `borae-app-icon.png` — ícone do app (favicon e apple-touch-icon).
- O componente `bo-brand-logo` passou a usar esses arquivos oficiais em **toda a plataforma** (landing, login e painel), substituindo o desenho vetorial anterior e o `favicon.svg`.
- **Paleta do painel** (especificação): laranja `#FF7200`, interação `#F06400`, laranja suave `#FFF0E3`, branco, creme `#FFF9F2`, grafite `#191919`, texto secundário `#707070`, fundo `#F7F7F5`, bordas `#EAEAEA`, sucesso `#16834A`, erro `#D92D20`, atenção `#B7791F`. Laranja reservado a ação, destaque e item ativo.
- **Tipografia do painel**: Inter (400–700).

### Tema claro e escuro
- Alternador **Claro / Escuro / Sistema** no topo do painel, no menu da conta e no login.
- Preferência salva no navegador (`borae.theme`); padrão = seguir o sistema.
- `src/index.html` aplica `html[data-theme]` antes do Angular iniciar (sem "piscar").
- Painel e login usam tokens semânticos (`--d-*` / `--a-*`) com valores próprios para cada tema; a logo alterna automaticamente entre as versões para fundo claro e escuro.
- A landing page permanece em tema claro (página institucional).

## 4. Componentes criados
| Componente / arquivo | Função |
|---|---|
| `dashboard/shell/dashboard-shell.component.*` | Layout (sidebar, topbar, conteúdo, gaveta mobile) |
| `dashboard/shell/restaurant-switcher.component.ts` | Seletor de restaurante (RestaurantSwitcher) |
| `dashboard/ui/ui.components.ts` | `bo-status-badge`, `bo-role-badge`, `bo-page-header`, `bo-state` (vazio/carregando/erro/sem permissão/planejado/não encontrado), `bo-confirm-dialog` (`<dialog>` nativo) |
| `dashboard/restaurant-context.service.ts` | Contexto: usuário, restaurantes, seleção, permissões espelhadas da API e mensagens de erro HTTP padronizadas |
| `dashboard/modules.ts` | Catálogo de módulos (fonte da navegação e dos status) |
| `dashboard/pages/*.page.ts` | Páginas listadas na seção 2 |
| `shared/theme.service.ts`, `shared/theme-toggle.component.ts` | Tema claro/escuro |
| `demo/demo-api.interceptor.ts`, `demo/demo-data.ts` | Modo demonstração (somente configuração `demo`) |

Estilos do painel em `dashboard/styles/` (`tokens`, `layout`, `ui`, `ui-menus`, `ui-forms`), todos prefixados por `.dash` / `.d-`.

## 5. Módulos implementados
- **Visão geral**: boas-vindas, card do estabelecimento (nome, slug, situação, papel), card da equipe (contagem real apenas para OWNER/MANAGER), ações rápidas reais e card "Seu espaço está evoluindo" com o status de cada módulo. Estado vazio "Vamos começar pelo seu restaurante?" com "Cadastrar restaurante" e "Tenho um convite".
- **Equipe**: tabela de membros (responsiva, vira cartões no mobile), troca de papel MANAGER↔STAFF, remoção com diálogo de confirmação, convite com exibição única do token (copiar, validade, avisos de segurança) e feedback de sucesso/erro.
- **Cadastrar restaurante**, **Aceitar convite**, **Meu restaurante** (dados atuais) e **Configurações** (conta pessoal somente leitura + sair).

## 6. Módulos planejados (somente visuais)
Cardápios, Categorias, Produtos, QR Code, Experiências 3D, Assistente IA, Fidelidade, Plano e assinatura e Integrações: página explicativa com descrição, benefício esperado, o que está previsto e as **definições já aprovadas** (ex.: valores do plano único, regras da fidelidade individual, limites da API pública). Nenhum formulário, botão de operação, QR Code, upload, chat ou cobrança fictícia. Boraê Club não aparece como módulo operacional (fora do MVP).

## 7. Funcionalidades conectadas ao backend
`GET /users/me`, `GET /users/me/restaurants`, `POST /restaurants`, `GET /restaurants/{id}/members`, `POST /restaurants/{id}/invitations`, `POST /restaurant-invitations/accept`, `PATCH /restaurants/{id}/members/{membershipId}`, `DELETE /restaurants/{id}/members/{membershipId}`. Reutilizam `AuthService`, interceptor JWT e `authGuard`.

## 8. Funcionalidades não conectadas
Todo o restante. Observações de contrato:
- A API não retorna **descrição** nem **flag ativo/inativo** em `/users/me/restaurants`. A situação "Ativo" é exibida porque o endpoint lista somente restaurantes ativos; a descrição é salva no banco, mas não pode ser exibida até o backend retorná-la.
- Edição de dados da conta e do restaurante não existe na API — exibidos somente para consulta.

## 9. Permissões respeitadas
| Ação | OWNER | MANAGER | STAFF |
|---|---|---|---|
| Ver equipe | Sim | Sim | Estado "Sem acesso à equipe" |
| Convidar | Sim | Oculto + aviso | — |
| Alterar papel / remover | Sim (exceto OWNER) | Oculto + aviso | — |

A interface apenas esconde ações indisponíveis; o backend continua sendo a autoridade (erros 403 são tratados). O restaurante selecionado é guardado no navegador só como preferência e nunca concede acesso. O token de convite fica apenas em memória e some ao trocar de restaurante ou sair da página.

## 10. Arquivos criados
- `frontend/src/app/dashboard/**` (shell, páginas, UI, estilos, contexto, catálogo e rotas)
- `frontend/src/app/shared/theme.service.ts`, `theme-toggle.component.ts`
- `frontend/src/app/demo/demo-api.interceptor.ts`, `demo-data.ts`
- `frontend/src/environments/environment.ts`, `environment.demo.ts`
- `frontend/src/app/auth.tokens.css`, `auth.showcase.css`
- `frontend/public/assets/images/brand/*.png` (5 arquivos)
- `docs/changes/frontend-restaurant-dashboard-2026-11-13.md`

## 11. Arquivos modificados / removidos
- `app.routes.ts` (rota `/app` com `loadChildren`), `app.config.ts` (`withComponentInputBinding` e interceptores de ambiente), `app.ts` (inicia o tema)
- `auth.interceptor.ts`: em 401, limpa a sessão e leva a `/auth?sessao=expirada` (antes ia para `/`)
- `auth.page.ts` / `auth.page.css`: aviso de sessão expirada, tema escuro, alternador de tema, logo oficial
- `shared/brand-logo.component.ts` (logo oficial), `shared/icon.component.ts` (+24 ícones)
- `index.html` (favicon oficial, script de tema, Inter 700), `angular.json` (configuração `demo`), `package.json` (`start:demo`)
- **Removidos:** `dashboard.page.ts` (todas as funções migradas para as novas páginas) e `public/favicon.svg` (redesenho substituído pela logo oficial)

## 12. Dependências
Nenhuma dependência nova.

## 13. Responsividade
Desktop com sidebar fixa (288px); abaixo de 1024px a sidebar vira gaveta com fundo escurecido e bloqueio de rolagem; tabelas viram cartões abaixo de 720px; formulários em uma coluna; sem rolagem horizontal (verificado em 375px).

## 14. Acessibilidade
Link "Ir para o conteúdo", `aria-current` na navegação, breadcrumb semântico, `role="radiogroup"` no tema, `listbox` no seletor, diálogo nativo com foco preso e Esc, `role="alert"/"status"` nos feedbacks, legendas de tabela para leitores de tela, `prefers-reduced-motion` respeitado. Observação: texto branco sobre o laranja `#FF7200` da especificação tem contraste abaixo de 4.5:1; usado apenas em botões com texto em negrito.

## 15. SEO
Títulos de página por rota (`<Página> · Painel Boraê`). Área autenticada não é indexável por natureza (exige login).

## 16. Testes
- Verificação no navegador (desktop 1366px e mobile 375px, temas claro e escuro) com o modo demonstração: login, visão geral, troca de restaurante, equipe (convite, troca de papel, remoção com diálogo), página planejada, estado de erro com "Tentar novamente" e aviso de sessão expirada.
- Conferido que o build de produção **não contém** dados nem credenciais de demonstração.
- Não testado contra a API real (depende de CORS no `borae-api` para `localhost:4200`/Render).

## 17. Build
`npm run build` e `ng build --configuration demo`: sucesso, 0 avisos, 0 erros. Initial total 307 kB (≈87.5 kB transferidos); painel carregado sob demanda (chunk principal ≈8 kB transferidos + páginas).

## 18. Limitações
- Descrição e situação do restaurante dependem de o backend passar a retorná-las.
- Sem envio de convite por e-mail (compartilhamento manual, conforme API).
- Landing page apenas em tema claro.
- Sessão continua em `localStorage` (limitação já documentada do MVP).

## 19. Próximas etapas
Perfil completo do restaurante (D-01), catálogo/cardápios (D-02 a D-05), QR Code e publicação — cada um trocando a página planejada pela página real e atualizando `dashboard/modules.ts`.

## 20. Modo demonstração e credenciais de teste
```bash
cd frontend
npm run start:demo
```
Abre `http://localhost:4200` com a API **simulada localmente** (nenhuma requisição vai para `borae.onrender.com`) e um aviso "Modo demonstração" visível. As contas de teste, a senha e o token de convite de exemplo estão documentados em `frontend/src/app/demo/demo-data.ts`. Os dados ficam no `sessionStorage` da aba e somem ao fechá-la. O simulador reproduz as regras atuais do backend (papéis, 403/404/409/410, validade de 7 dias, proprietário não removível).

## Branch e commits
- Branch: `feat/restaurant-dashboard`
- Commits: `docs(frontend): record Render static site URL and pending CORS/rewrite steps` (trazido da branch anterior) e `feat(frontend): establish Boraê restaurant dashboard`

---

## Atualização de 14/11/2026 — Cadastro de restaurante e lentidão da API

**Modificações feitas por Renan Cristiano**

### Problemas relatados
1. A tela de cadastro de restaurante (painel antigo, tema verde-limão) não seguia o branding.
2. Depois de cadastrar o restaurante, não havia como entrar no painel dele.
3. Cadastro e login demoravam muito.

### Diagnóstico da lentidão
O backend roda no plano gratuito do Render, que desliga o serviço após ~15 min sem uso. A primeira requisição seguinte espera o Spring Boot iniciar (30–60 s). Com a API ativa, a resposta medida foi de ~0,25 s. A solução definitiva é do lado da infraestrutura (instância paga ou serviço sempre ativo); o frontend passou a reduzir e explicar a espera.

### O que mudou
- **Aquecimento da API** (`shared/api-warmup.ts`): ao abrir a landing ou o login, um `GET` simples em `/actuator/health` (rota pública, `mode: no-cors`) começa a reativar o servidor enquanto a pessoa ainda navega. Uma vez por carregamento; desligado no modo demonstração.
- **Aviso de servidor iniciando**: se login, criação de conta, carregamento do painel ou cadastro de restaurante passar de 5 s, aparece "O servidor do Boraê está sendo iniciado e a primeira resposta pode levar até 1 minuto. Não feche esta página." O botão de login também mostra "Entrando..." / "Criando sua conta...".
- **Fluxo de conta nova**: após criar a conta, a pessoa vai direto para "Cadastrar restaurante", com mensagem de boas-vindas.
- **Cadastro de restaurante** no branding do painel, com etapas (Conta Boraê → Dados do restaurante → Painel do restaurante), botão "Cadastrar e abrir o painel" e link para aceitar convite.
- **Entrar no painel do restaurante**: ao concluir o cadastro, o restaurante criado é selecionado e a visão geral abre com a confirmação "Restaurante cadastrado!". A visão geral ganhou a seção **"Seus restaurantes"**, com o restaurante em gestão destacado e o botão **"Abrir painel"** nos demais.

### Testes
Fluxo completo no modo demonstração: criar conta → cadastro com boas-vindas → cadastrar restaurante → visão geral com confirmação → cadastrar segundo restaurante → "Abrir painel" alterna o restaurante em gestão. Builds de produção e demonstração sem avisos. O aviso de lentidão não pôde ser reproduzido no modo demonstração (latência simulada de 350 ms).
