# Boraê Backend

API da plataforma Boraê, construída com Java 21 e Spring Boot.

## Stack
- Java 21, Spring Boot 3 e Maven
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
2. Configure as variáveis de ambiente conforme `.env.example` (Spring Boot não carrega automaticamente arquivos `.env`).
3. Na pasta `backend`, execute `mvn spring-boot:run`.

## Executar com Docker
Na raiz do repositório:
```bash
cp .env.example .env
# Edite DB_PASSWORD e JWT_SECRET com valores próprios
docker compose up --build
```
API: `http://localhost:8080`; Swagger: `http://localhost:8080/swagger-ui.html`. O Compose sobe PostgreSQL local com volume persistente.

Para construir somente a imagem: `docker build -t borae-backend ./backend`.

## Autenticação
| Método | Endpoint | Acesso |
|---|---|---|
| POST | `/api/v1/auth/register` | Público |
| POST | `/api/v1/auth/login` | Público |
| GET | `/actuator/health` | Público |
| GET | `/swagger-ui.html` | Público |
| GET | `/v3/api-docs` | Público |
| GET | `/api/v1/system/status` | Bearer token |

O cadastro armazena a senha com BCrypt e retorna um access token. Envie o token nas rotas protegidas usando `Authorization: Bearer <token>`.

O access token expira por padrão em 15 minutos. Configure `JWT_SECRET` com uma chave Base64 forte de pelo menos 256 bits. O valor padrão da aplicação existe apenas para desenvolvimento local e não deve ser usado em produção. Refresh tokens, recuperação de senha, verificação de e-mail e rate limiting ainda não fazem parte desta etapa.

## Banco e testes
A migration inicial cria `user_account`, `restaurant` e `restaurant_membership`. A aplicação usa `ddl-auto: validate`; evolua o schema por migrations Flyway.
```bash
mvn verify
```
Os testes de integração usam H2.
