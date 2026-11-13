# Boraê

**Experiências gastronômicas mais inteligentes.**

Boraê conecta restaurantes e consumidores em uma experiência gastronômica digital: cardápios por QR Code, visualização 3D/AR de pratos, recomendações com IA, fidelidade e descoberta de estabelecimentos.

> O projeto está em desenvolvimento incremental. A visão do produto inclui funcionalidades futuras que ainda não estão disponíveis. Consulte o roadmap para distinguir o que já existe do que será construído.

## Comece pela documentação

A documentação oficial para quem vai contribuir está em **[docs/README.md](docs/README.md)**.

Leitura recomendada:
1. [Guia de desenvolvimento](docs/development/guide.md).
2. [Visão do produto](docs/product/vision.md).
3. [Requisitos funcionais](docs/product/requirements.md).
4. [Arquitetura](docs/architecture/overview.md).
5. [Modelo de domínio e permissões](docs/architecture/domain-model.md).
6. [Endpoints implementados](docs/api/current-endpoints.md).
7. [Roadmap](docs/roadmap.md).

## O que já está implementado

- Backend Java 21 + Spring Boot e frontend Angular 20.
- PostgreSQL com migrations Flyway.
- Cadastro e login com JWT.
- Consulta do perfil autenticado e listagem dos restaurantes do usuário.
- Cadastro de restaurante com vínculo OWNER.
- Papéis OWNER, MANAGER e STAFF.
- Convites de equipe com token seguro, aceite, listagem, alteração de papel e remoção.
- Swagger/OpenAPI, CI e configuração de deploy.

## Visão do produto

- Cardápio digital responsivo com categorias, pratos, opções e disponibilidade.
- Visualização 3D e realidade aumentada de pratos.
- Assistente gastronômico que recomenda itens reais do cardápio.
- Fidelidade individual por restaurante.
- Boraê Club: benefícios compartilhados entre estabelecimentos parceiros.
- Descoberta de restaurantes e painel de gestão.

Esses itens fazem parte da visão; consulte o [roadmap](docs/roadmap.md) para o status de cada módulo.

## Estratégia técnica

| Camada | Tecnologia |
| --- | --- |
| Frontend | Angular 20 + TypeScript |
| Backend | Java 21 + Spring Boot |
| Persistência | Spring Data JPA |
| Banco de dados | PostgreSQL |
| Migrations | Flyway |
| Segurança | Spring Security + JWT + BCrypt |
| API | REST + OpenAPI/Swagger |
| Cache futuro | Redis, se necessário |
| IA futura | Python + FastAPI |
| 3D/AR futuro | glTF/GLB + model-viewer |

## Executar localmente

Consulte os pré-requisitos e instruções em [backend/README.md](backend/README.md) e [frontend/README.md](frontend/README.md). Docker Compose está disponível para ambientes compatíveis, mas não é obrigatório para o desenvolvimento local.

## Deploy

A configuração do Render está no `render.yaml`. As variáveis e instruções de deploy devem ser consultadas nos arquivos de configuração e na documentação técnica. Nunca versione `.env), tokens, senhas ou credenciais.
