# Boraê Backend

API da plataforma Boraê, construída com Java 21 e Spring Boot.

## Stack

- Java 21
- Spring Boot 3
- Maven
- Spring Web, Validation, Security e Data JPA
- PostgreSQL + Flyway
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

## Endpoints

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/actuator/health` | Público |
| GET | `/swagger-ui.html` | Público |
| GET | `/v3/api-docs` | Público |
| GET | `/api/v1/system/status` | Autenticado |

Nesta etapa, o Spring Security usa HTTP Basic com o usuário temporário gerado pelo Spring Boot. A autenticação definitiva com cadastro, hash de senha, JWT e refresh token será implementada em uma etapa própria. Não utilize a credencial temporária em produção.

## Banco de dados

A migration inicial cria a fundação de identidade e multi-tenancy:

- `user_account`: contas de usuários.
- `restaurant`: estabelecimentos.
- `restaurant_membership`: vínculo entre usuário e restaurante, com papel no estabelecimento.

A aplicação usa `ddl-auto: validate`; a evolução do schema deve acontecer por migrations Flyway, nunca por atualização automática do Hibernate.

## Testes

```bash
mvn test
```

Os testes de contexto usam H2 e não exigem um PostgreSQL local.
