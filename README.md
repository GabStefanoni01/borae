# Boraê

**Experiências gastronômicas mais inteligentes.**

Boraê é uma plataforma que conecta restaurantes e consumidores por meio de cardápios digitais, experiências 3D/AR, recomendações gastronômicas com IA, fidelidade e descoberta de estabelecimentos.

O projeto está em desenvolvimento incremental. Este repositório é a fonte de verdade técnica; decisões e funcionalidades futuras devem seguir a documentação antes de serem implementadas.

## Documentação do projeto

Comece por **[docs/README.md](docs/README.md)**. Ele apresenta a ordem de leitura e direciona para:

- [PRD — requisitos do produto](docs/PRD.md)
- [Visão do produto e regras de negócio](docs/PRODUCT.md)
- [Fluxos de tela](docs/SCREEN_FLOWS.md)
- [Modelo de domínio e dados](docs/DOMAIN_MODEL.md)
- [Arquitetura e decisões técnicas](docs/ARCHITECTURE.md)
- [API implementada](docs/API.md)
- [Desenho dos contratos futuros da API](docs/API_DESIGN.md)
- [Especificação do catálogo](docs/MODULE_CATALOG.md)
- [Especificação do assistente IA](docs/MODULE_AI_ASSISTANT.md)
- [Especificação de fidelidade e Boraê Club](docs/MODULE_LOYALTY_CLUB.md)
- [Especificação 3D/AR](docs/MODULE_3D_AR.md)
- [Especificação de descoberta](docs/MODULE_DISCOVERY.md)
- [Decisões pendentes](docs/OPEN_DECISIONS.md)
- [Roadmap e critérios de conclusão](docs/ROADMAP.md)
- [Guia de contribuição e fluxo de trabalho](docs/CONTRIBUTING.md)

## Stack

| Camada | Tecnologia |
| --- | --- |
| Frontend | Angular 20 + TypeScript |
| Backend | Java 21 + Spring Boot |
| Persistência | PostgreSQL + Spring Data JPA |
| Migrações | Flyway |
| Segurança | Spring Security + JWT |
| Documentação da API | OpenAPI / Swagger |
| Hospedagem atual | Render |
| Banco gerenciado | Neon PostgreSQL |
| Cache futuro | Redis |
| Serviço de IA planejado | Python + FastAPI |
| Experiência 3D/AR planejada | GLB/glTF + model-viewer |

## Ambientes publicados

- Frontend: https://borae1.onrender.com/
- API: https://borae.onrender.com

Consulte a documentação OpenAPI publicada no backend para conferir os contratos atuais. Endpoints e exemplos descritos em `docs/API.md` são apenas os que já foram confirmados no código; funcionalidades futuras estão explicitamente identificadas como planejadas.

## Executar localmente

O projeto possui configuração Docker Compose, mas o Docker é opcional para desenvolvimento. Para executar sem Docker, instale Java 21, Node.js compatível com Angular 20 e PostgreSQL; configure as variáveis de ambiente conforme `.env.example` e os arquivos de configuração de cada aplicação.

Com Docker, na raiz do projeto:

```bash
docker compose up --build
```

Frontend: http://localhost:4200  
API: http://localhost:8080

## Regras essenciais

1. Não implemente uma funcionalidade futura como se já fizesse parte do produto.
2. Não altere contratos, regras de negócio ou arquitetura sem atualizar a documentação e explicar a motivação no PR.
3. Nunca confie no frontend para autorização: valide usuário, restaurante, vínculo ativo e papel no backend.
4. Não exponha segredos, tokens, credenciais ou dados de outros restaurantes.
5. Trabalhe em branch própria e abra PR para `main`; não faça commits diretamente em `main`.
6. Inclua testes para regras de negócio e mantenha a CI aprovada antes do merge.

