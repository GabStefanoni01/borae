# Documentação do Boraê

Esta documentação é a referência de produto e engenharia do Boraê. Ela separa o que já existe no código, o comportamento esperado do produto, as decisões aprovadas e as questões ainda pendentes.

## Ordem de leitura obrigatória

1. [PRD — Requisitos do produto](PRD.md): problema, objetivos, personas, jornadas, requisitos funcionais e não funcionais.
2. [Produto e regras de negócio](PRODUCT.md): visão geral, princípios e regras transversais.
3. [Fluxos de tela](SCREEN_FLOWS.md): comportamento esperado das principais jornadas e estados da interface.
4. [Modelo de domínio e dados](DOMAIN_MODEL.md): entidades atuais e propostas, relações e invariantes.
5. [Arquitetura](ARCHITECTURE.md): stack, módulos, segurança, infraestrutura e decisões técnicas.
6. [API implementada](API.md): endpoints e regras que já existem no código.
7. [Desenho da API futura](API_DESIGN.md): recursos e contratos candidatos ainda não implementados.
8. [Especificação do catálogo](MODULE_CATALOG.md): categorias, produtos, opções, publicação e cardápio.
9. [Especificação do assistente IA](MODULE_AI_ASSISTANT.md): fluxo, limites, validação e segurança.
10. [Fidelidade e Boraê Club](MODULE_LOYALTY_CLUB.md): ledgers, resgates, controles e decisões comerciais.
11. [Experiência 3D/AR](MODULE_3D_AR.md): mídia, modelos, calibração e fallback.
12. [Descoberta](MODULE_DISCOVERY.md): diretório, busca e dados públicos.
13. [Roadmap](ROADMAP.md): sequência de implementação e critérios de conclusão.
14. [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md): preços, provedores e políticas aprovadas em 13/11/2026.\n15. [Registro de decisões e pendências restantes](OPEN_DECISIONS.md): decisões aprovadas e parâmetros técnicos ainda a definir.
15. [Contribuição](CONTRIBUTING.md): Git, testes, PRs e Definition of Done.

## Como interpretar o status

- **Implementado:** confirmado no código integrado à branch principal.
- **Em validação:** implementado, mas ainda depende de validação funcional/operacional.
- **Planejado:** especificado, mas ainda não deve ser tratado como disponível.
- **Proposto:** desenho técnico candidato; precisa ser confirmado antes de virar contrato/código.
- **Pendente de decisão:** não implementar até o responsável pelo produto aprovar uma escolha.
- **Fora do MVP:** não faz parte do recorte atual.

## Regra de precedência

O código e os contratos atualmente publicados determinam o comportamento existente. Para funcionalidades futuras, PRD e especificações de módulo definem o comportamento esperado, mas itens marcados como propostos ou pendentes não são autorização para inventar regras. Em caso de conflito, interromper a implementação e alinhar com o responsável pelo produto.

## Atualização obrigatória

Toda mudança que altere fluxo, regra, entidade, endpoint, permissão, variável ou prioridade deve atualizar os documentos correspondentes no mesmo PR. Não marcar uma funcionalidade como implementada apenas porque o código compila.
