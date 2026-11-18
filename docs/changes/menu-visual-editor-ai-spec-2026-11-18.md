# Registro de Alterações: Especificação do Editor Inteligente de Cardápios em Tempo Real

**Modificações feitas por Renan Cristiano**  
**Data:** 18/11/2026

- **Projeto:** Boraê
- **Entrega:** Relatório técnico, especificação arquitetural, contratos REST, plano de implementação, variáveis de ambiente e alinhamento frontend/backend (Renan Cristiano & Gabriel) para o Editor Visual de Cardápios em Tempo Real integrado à IA (Gemini + fallback Grok).
- **Branch:** `docs/menu-visual-editor-ai-spec` (a partir de `main`)
- **PRs de referência:** #30 (`docs/3D_DISH_CAPTURE_WORKFLOW.md`), #27 (`feat/3d-ar-experiences`), #28 (`feat/frontend-3d-ar`), #22 (`catalog`), #29 (`feat/ai-gastronomic-assistant`)

---

## 1. Resumo da entrega

- Diagnóstico completo da arquitetura atual de frontend (`frontend/src/app/dashboard/`, `public-menu/`, `shared/`) e backend (`com.borae.catalog`, `com.borae.identity`, migrations `V5`–`V9` e referência `PR #29`).
- Identificação de lacunas e conflitos com decisões prévias (`D-03`, `D-10`, `D-11`) antes do início da codificação.
- Especificação definitiva em [`docs/MODULE_MENU_VISUAL_EDITOR_AI.md`](../MODULE_MENU_VISUAL_EDITOR_AI.md) cobrindo:
  - Editor visual em tempo real com prévia fiel (`MenuRendererComponent` compartilhado com `/r/:slug`), seletor de viewport, autosave com debounce e lock otimista, recuperação local e Undo/Redo.
  - Reorganização da navegação unificando a aba **Imagens** de `Meu Restaurante` dentro de **Meu Cardápio** (`Banner & Mídia`), com redirecionamento de compatibilidade em `/app/restaurante/imagens`.
  - Personalização completa de banner (enquadramento/focal point, zoom, overlay e textos), cores com validação WCAG AA, tipografia allowlist e layout.
  - Estratégia para múltiplos cardápios sem duplicação desnecessária de mídia e produtos.
  - Arquitetura de IA B2B com **Google Gemini** (primário), **xAI Grok** (fallback automático) e geração de banner desacoplada via **ImageKit**, preservando autonomia humana (preview, aceite parcial/total, desfazer).
  - Preservação integral dos fluxos de modelos 3D/AR da PR #27, PR #28 e futura captura guiada da PR #30.
  - Contratos REST, DTOs, variáveis de ambiente e instruções técnicas para implementação no Spring Boot (Gabriel).

## 2. Arquivos alterados/criados

- **Criado:** `docs/MODULE_MENU_VISUAL_EDITOR_AI.md`
- **Criado:** `docs/changes/menu-visual-editor-ai-spec-2026-11-18.md`
- **Modificado:** `docs/README.md`

## 3. Verificações

- Especificação conferida contra o código real da branch `main` e das branches `origin/docs/3d-dish-capture-workflow` (PR #30) e `origin/feat/ai-gastronomic-assistant` (PR #29).
- Nenhuma alteração foi feita em código de aplicação (`backend/` ou `frontend/`) nesta PR de especificação, mantendo o escopo estritamente documental e pronto para revisão antes da execução das etapas.
