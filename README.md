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

## Estratégia técnica
| Camada | Tecnologia |
| --- | --- |
| Frontend | Angular 20 + TypeScript |
| Servidor web | Nginx em container Docker |
| Backend | Java 21 + Spring Boot |
| Containerização | Docker multi-stage + Docker Compose |
| Banco de dados | PostgreSQL |
| Cache futuro | Redis |
| IA futura | Python + FastAPI |
| 3D/AR | glTF/GLB + model-viewer |

## Executar com Docker
1. Instale Docker Engine e Docker Compose.
2. Copie `.env.example` para `.env`.
3. Defina DB_PASSWORD e um JWT_SECRET Base64 forte (mínimo 32 bytes).
4. Na raiz, execute:

```bash
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432

O PostgreSQL usa volume persistente. Não versione o arquivo `.env`. Em produção, publique frontend e backend como serviços Docker separados e use PostgreSQL gerenciado, como Neon.

## Estrutura
- docs/: visão, arquitetura e decisões do produto.
- backend/: API Java/Spring Boot e Dockerfile.
- frontend/: aplicação Angular e Dockerfile.
- docker-compose.yml: ambiente integrado.

## Status
**Sprint 0 — Fundação:** documentação, backend com autenticação inicial, scaffold Angular e configuração Docker.
