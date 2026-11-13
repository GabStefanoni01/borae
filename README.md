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
| Banco de dados | PostgreSQL (Neon ou Render) |
| Cache futuro | Redis |
| IA futura | Python + FastAPI |
| 3D/AR | glTF/GLB + model-viewer |

## Executar localmente com Docker
1. Copie `.env.example` para `.env` e configure os valores.
2. Na raiz, execute `docker compose up --build`.
3. Frontend: http://localhost:4200; API: http://localhost:8080.

## Hospedar no Render
O arquivo `render.yaml` define dois Web Services Docker independentes: `borae-api` e `borae-web`. No Render, use **New + Blueprint**, conecte este repositório e informe as variáveis pedidas para a API. O backend usa o `PORT` injetado pelo Render; o frontend serve Nginx na porta 10000.

Configure na API:
- `DB_URL`: JDBC URL do PostgreSQL gerenciado, por exemplo Neon.
- `DB_USERNAME` e `DB_PASSWORD`: credenciais do banco.
- `JWT_SECRET`: segredo Base64 aleatório de pelo menos 32 bytes.
- `JWT_EXPIRATION_MS`: duração do access token em milissegundos.

Não use o PostgreSQL do Docker Compose como banco de produção. Não versione o arquivo `.env`.

## Status
**Sprint 0 — Fundação:** documentação, backend com autenticação inicial, scaffold Angular e configuração Docker/Render.
