# Boraê

**Experiências gastronômicas mais inteligentes.**

Boraê conecta restaurantes e consumidores em uma experiência gastronômica digital: cardápios por QR Code, visualização 3D/AR de pratos, recomendações com IA, fidelidade e descoberta de estabelecimentos.

> Projeto em fase inicial de definição e desenvolvimento. Funcionalidades e prioridades podem evoluir conforme validação com restaurantes e consumidores.

## Visão do produto
- Cardápio digital responsivo com categorias, produtos, opções e disponibilidade.
- Visualização 3D e realidade aumentada de pratos em dispositivos compatíveis.
- Assistente gastronômico que recomenda itens existentes no cardápio.
- Programas de fidelidade individuais por restaurante.
- Boraê Club: benefícios compartilhados entre estabelecimentos parceiros.
- Descoberta de restaurantes e painel de gestão para estabelecimentos.

## Estratégia técnica inicial
| Camada | Tecnologia planejada |
| --- | --- |
| Frontend | Angular + TypeScript + Tailwind CSS |
| Backend | Java + Spring Boot |
| Banco de dados | PostgreSQL |
| Cache (quando necessário) | Redis |
| IA (módulo evolutivo) | Python + FastAPI |
| 3D/AR | glTF/GLB + model-viewer |
| API e testes | OpenAPI, JUnit, Mockito e Playwright |

A primeira versão será uma aplicação web responsiva, com arquitetura de monólito modular e suporte multi-tenant desde o início.

## Estrutura
- docs/: visão, arquitetura e decisões do produto.
- backend/: API Java/Spring Boot.
- frontend/: aplicação Angular.

## Princípios
- Isolamento rigoroso dos dados de cada restaurante.
- O backend é a fonte de verdade para preços, disponibilidade, pontos e resgates.
- A IA não pode inventar produtos nem executar operações sem validação.
- Pontos individuais e pontos Boraê Club terão saldos e regras separados.
- Operações de fidelidade devem ser auditáveis e protegidas contra duplicidade.
- Começar simples, validar com usuários reais e evoluir com segurança.

## Status
**Sprint 0 — Fundação:** documentação inicial, estrutura do projeto e decisões arquiteturais.

Consulte a [visão do produto](docs/product/vision.md), a [arquitetura inicial](docs/architecture/overview.md) e o [roadmap](docs/roadmap.md).