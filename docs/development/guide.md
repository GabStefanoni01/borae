# Guia de desenvolvimento e integração

Este documento define como contribuir para o Boraê sem quebrar decisões já tomadas. Leia antes de começar.

## 1. Objetivo e princípio de desenvolvimento

O Boraê é uma plataforma gastronômica para consumidores, restaurantes parceiros e operação da plataforma. O produto será construído por etapas, validando cada domínio antes de ampliar o escopo.

Princípios obrigatórios:
- Entregar incrementos pequenos, testáveis e revisáveis.
- Não implementar funcionalidades futuras como se já estivessem aprovadas para a etapa atual.
- Não introduzir microserviços sem justificativa técnica e decisão conjunta.
- Não duplicar regras de negócio no frontend: o backend é a autoridade.
- Não confiar em IDs, papéis, preços, saldos ou permissões enviados pelo navegador.
- Não acessar banco de dados diretamente pelo frontend ou pelo modelo de IA.
- Não adicionar serviços pagos ou infraestrutura nova sem avaliar custo e alternativa simples.
- Atualizar testes, OpenAPI e documentação junto com a funcionalidade.

## 2. Stack definida

| Camada | Tecnologia / decisão |
| --- | --- |
| Frontend | Angular 20 + TypeScript |
| Estilo | CSS/Tailwind conforme estrutura já adotada; não trocar framework sem alinhamento |
| Backend | Java 21 + Spring Boot 3 |
| Persistência | Spring Data JPA |
| Banco | PostgreSQL |
| Evolução do schema | Flyway |
| Autenticação | Spring Security + JWT |
| Senhas | BCrypt |
| API | REST versionada em `/api/v1` |
| Documentação da API | Springdoc OpenAPI / Swagger UI |
| Testes backend | JUnit 5 + Spring Boot Test; H2 nos testes de integração atuais |
| Frontend | Angular CLI e TypeScript |
| Deploy atual | Render; PostgreSQL gerenciado (Neon) |
| Cache futuro | Redis, somente quando houver caso de uso mensurável |
| IA futura | Serviço Python + FastAPI, isolado por contrato HTTP |
| 3D/AR futuro | GLB/glTF e model-viewer |

Não é obrigatório executar Docker localmente para desenvolver. Backend pode ser executado com JDK/Maven e banco PostgreSQL acessível; frontend com Node.js e Angular CLI. Docker/Compose existem para ambientes compatíveis e deploy.

## 3. Estrutura do repositório

- `backend/`: API Java/Spring Boot.
- `frontend/`: aplicação Angular.
- `docs/`: visão do produto, requisitos, arquitetura, API, modelo de domínio e roadmap.
- `render.yaml`: configuração de serviços no Render.
- `.github/workflows/`: automações de CI.

No backend, mantenha os pacotes por domínio/feature. A fundação atual concentra autenticação em `auth`, identidade e restaurantes em `identity`, segurança em `security` e configuração em `config`. Novos domínios devem ter seus próprios pacotes quando forem implementados.

## 4. Convenções de backend

- Controllers recebem HTTP, validam entrada e delegam para Services.
- Services concentram regras de negócio e limites transacionais.
- Repositories encapsulam acesso a dados; controllers não executam consultas diretamente.
- DTOs de request/response são separados das entidades JPA.
- Use Bean Validation nos requests (`@Valid`, `@NotBlank`, limites de tamanho etc.).
- Use UUID como identificador público das entidades, seguindo o padrão atual.
- Não exponha entidades JPA diretamente na API.
- Use `@Transactional` nos casos de uso que alteram múltiplas entidades.
- Retorne códigos HTTP coerentes: 201 para criação, 204 para remoção sem corpo, 400 para entrada inválida, 401 para não autenticado e 403 para sem permissão.
- Toda alteração de schema deve ser uma nova migration Flyway. Nunca edite uma migration já aplicada em produção.
- Evite lógica de negócio em filtros, controllers ou componentes de interface.

## 5. Convenções de frontend

- Mantenha componentes responsáveis pela apresentação e interação; regras críticas permanecem no backend.
- Centralize chamadas HTTP e contratos de resposta nos serviços Angular.
- Use o interceptor para enviar o JWT em rotas protegidas e o guard para proteger páginas privadas.
- Trate estados de carregamento, sucesso, vazio e erro.
- Não esconda apenas botões para implementar autorização: a API precisa validar a permissão.
- Não salve segredos no frontend. A URL pública da API pode ser configurável, mas credenciais nunca.
- A sessão atual usa localStorage como solução provisória do MVP. Não considere isso a estratégia final de autenticação de produção.

## 6. Segurança e multi-tenancy

- O usuário autenticado é identificado pelo principal do Spring Security, derivado do JWT.
- IDs de usuário, papel OWNER e autor da operação devem ser determinados no servidor.
- Toda operação em nome de um restaurante precisa validar vínculo ativo e papel permitido naquele restaurante.
- Nunca considere que conhecer um `restaurantId` concede acesso.
- Um usuário pode pertencer a vários restaurantes, com papéis diferentes.
- Nunca permitir que um MANAGER ou STAFF altere o próprio papel, remova outro membro ou promova alguém a OWNER sem regra explícita aprovada.
- Teste acesso cruzado: membro do restaurante A não pode ler ou alterar dados do restaurante B.
- Senhas devem ser armazenadas somente como hash BCrypt.
- Convites devem usar tokens aleatórios de alta entropia; armazenar somente hash do token.
- Não registrar JWT, senha, token de convite ou credenciais nos logs.
- Não enviar segredos por commit, documentação pública, issue ou mensagem.

## 7. Fluxo Git obrigatório

1. Atualize sua branch local a partir de `main`.
2. Crie uma branch por entrega: `feat/nome-curto`, `fix/nome-curto` ou `docs/nome-curto`.
3. Faça commits pequenos e descritivos, por exemplo: `feat(catalog): create menu category`.
4. Rode os testes e builds disponíveis.
5. Abra Pull Request para `main`, descrevendo objetivo, decisões, endpoints, migrations, testes e limitações.
6. Aguarde CI e revisão. Não faça push direto em `main` nem mescle PR sem aprovação dos responsáveis.
7. Após a aprovação e merge, valide o comportamento no ambiente de teste/produção conforme o risco da mudança.
8. Atualize o roadmap e a documentação da API se a entrega alterou o comportamento.

Não trabalhe simultaneamente no mesmo arquivo/área sem combinar. Se houver conflito, sincronize com `main) e resolva conscientemente; não descarte alterações do colega para fazer o merge passar.

## 8. Definition of Done

Uma funcionalidade só está concluída quando:
- [ ] Regras de negócio e permissões estão no backend.
- [ ] Entrada é validada e erros são tratados.
- [ ] Migration Flyway foi criada quando necessária.
- [ ] Testes cobrem sucesso, falha e autorização.
- [ ] Teste de isolamento entre restaurantes foi incluído quando aplicável.
- [ ] Contrato OpenAPI/Swagger está atualizado.
- [ ] Frontend contempla loading, sucesso, vazio e erro, quando aplicável.
- [ ] CI está verde.
- [ ] Documentação e roadmap foram atualizados.
- [ ] A funcionalidade foi validada por alguém além de quem a implementou.

## 9. Decisões que exigem alinhamento antes de mudar

Converse antes de:
- Trocar Angular, Java/Spring Boot ou PostgreSQL.
- Migrar o monólito modular para microserviços.
- Introduzir um serviço pago, banco adicional ou infraestrutura com custo recorrente.
- Alterar papéis, autorização ou isolamento multi-tenant.
- Definir regras financeiras, emissão/resgate de pontos ou compensação do Boraê Club.
- Permitir que IA altere dados, execute pedidos, conceda pontos ou faça resgates.
- Mudar contratos de API já consumidos pelo frontend.

## 10. Limitações conhecidas da implementação atual

- A sessão JWT do frontend é persistida em localStorage; avaliar cookies HttpOnly e estratégia de renovação antes de ampliar a exposição.
- O backend atual emite access token; refresh token e recuperação/verificação de e-mail ainda não estão implementados.
- O convite de equipe retorna o token na resposta para compartilhamento manual. Não existe envio automático de e-mail.
- O frontend ainda é uma fundação de autenticação e dashboard, não a plataforma gastronômica completa.
- Catálogo, cardápio público, 3D/AR, assistente, fidelidade, Club e descoberta ainda são etapas futuras.
- A URL da API está definida no serviço Angular atual; avaliar configuração por ambiente antes de ampliar ambientes.
