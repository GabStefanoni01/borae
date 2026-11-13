# Boraê Backend

API da plataforma Boraê, construída com Java 21 e Spring Boot.

## Stack

- Java 21
- Spring Boot 3
- Maven
- Spring Web, Validation, Security e Data JPA
- PostgreSQL + Flyway
- JWT (JJWT) e BCrypt
- Springdoc OpenAPI / Swagger UI
- JUnit 5 e Spring Boot Test

## Pré-requisitos

- JDK 21
- Maven 3.9+
- PostgreSQL 15+

## Executar localmente

1. Crie um banco PostgreSQL chamado `borae`.
2. Copie `.env.example` para `.env` e ajuste as credenciais.
3. Exporte as variáveis de ambiente no terminal (o Spring Boot não carrega automaticamente arquivos `.env`).
4. Na pasta `backend`, execute:

```bash
mvn spring-boot:run
```

O Flyway cria as tabelas iniciais automaticamente na primeira inicialização.

## Autenticação

A API utiliza JWT stateless:

| Método | Endpoint | Acesso |
|---|---|---|
| POST | `/api/v1/auth/register` | Público |
| POST | `/api/v1/auth/login` | Público |
| GET | `/actuator/health` | Público |
| GET | `/swagger-ui.html` | Público |
| GET | `/v3/api-docs` | Público |
| GET | `/api/v1/system/status` | Bearer token |

O cadastro cria a conta, armazena a senha com BCrypt e retorna um access token. O login valida as credenciais e retorna um novo token. Envie o token nas rotas protegidas usando `Authorization: Bearer <token>`.

O access token expira por padrão em 15 minutos. Configure `JWT_SECRET` com uma chave Base64 forte de pelo menos 256 bits e `JWT_EXPIRATION_MS` conforme o ambiente. O valor padrão existe apenas para desenvolvimento local e não deve ser usado em produção. Refresh tokens, recuperação de senha, verificação de e-mail e rate limiting ainda não fazem parte desta etapa.

## Banco de dados

A migration inicial cria a fundação de identidade e multi-tenancy:

- `user_account`: contas de usuários.
- `restaurant`: estabelecimentos.
- `restaurant_membership`: vínculo entre usuário e restaurante, com papel no estabelecimento.

A aplicação usa `ddl-auto: validate`; a evolução do schema deve acontecer por migrations Flyway, nunca por atualização automática do Hibernate.

## Testes

```bash
mvn verify
```

Os testes de integração usam H2 e não exigem um PostgreSQL local.
