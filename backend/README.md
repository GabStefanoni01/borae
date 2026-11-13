# Boraê Backend

API principal do Boraê, planejada em Java e Spring Boot.

## Responsabilidades iniciais
- Identidade e autenticação.
- Gestão de restaurantes e vínculos de usuários.
- Aplicação das regras multi-tenant.
- Catálogo e cardápios.
- Regras de fidelidade e benefícios.
- Orquestração segura de recomendações.

## Diretrizes
- Organizar o código por módulos de domínio.
- Manter regras de negócio fora de controllers.
- Usar DTOs nas fronteiras HTTP.
- Versionar o schema com migrations.
- Documentar endpoints com OpenAPI.
- Testar autorização e isolamento entre restaurantes.

A configuração executável do Spring Boot será adicionada na etapa de scaffold do backend.