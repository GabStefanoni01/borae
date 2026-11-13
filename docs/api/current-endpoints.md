# API atual — contratos implementados

Base path: `/api/v1`. A API publicada usa HTTPS no serviço Render. Consulte também Swagger UI no backend publicado para o schema efetivo de requests/responses.

## Autenticação

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| POST | `/auth/register` | Público | Cria conta e retorna access token |
| POST | `/auth/login` | Público | Autentica e retorna access token |

Cadastro e login retornam `AuthResponse` com `accessToken`, `tokenType`, `expiresIn` e `user` (id, fullName, email). Senha nunca deve ser devolvida.

Para rotas protegidas:
```http
Authorization: Bearer <accessToken>
```

## Usuário autenticado

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| GET | `/users/me` | Autenticado | Perfil da conta atual |
| GET | `/users/me/restaurants` | Autenticado | Restaurantes com vínculo ativo e papel |

A API deriva a identidade do token; não recebe userId como parâmetro de autoridade.

## Restaurantes

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| POST | `/restaurants` | Autenticado | Cria restaurante e vínculo OWNER |

Exemplo de request:
```json
{
  "name": "Restaurante Exemplo",
  "description": "Descrição opcional"
}
```

O backend gera slug e associa o usuário autenticado como OWNER na mesma transação.

## Equipe

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| GET | `/restaurants/{restaurantId}/members` | OWNER ou MANAGER | Lista membros ativos |
| POST | `/restaurants/{restaurantId}/invitations` | OWNER | Cria convite para MANAGER ou STAFF |
| POST | `/restaurant-invitations/accept` | Autenticado | Aceita convite usando token |
| PATCH | `/restaurants/{restaurantId}/members/{membershipId}` | OWNER | Altera papel para MANAGER ou STAFF |
| DELETE | `/restaurants/{restaurantId}/members/{membershipId}` | OWNER | Desativa vínculo do membro |

Criar convite:
```json
{
  "email": "pessoa@exemplo.com",
  "role": "STAFF"
}
```

Aceitar convite:
```json
{
  "token": "token-recebido-do-proprietario"
}
```

Alterar papel:
```json
{
  "role": "MANAGER"
}
```

A resposta de criação de convite inclui o token original para compartilhamento manual e a data de expiração. O banco armazena somente o hash do token. Não registrar esse token em logs nem compartilhar publicamente.

## Resumo de permissões

- Cadastro/login: público.
- Perfil e lista dos próprios restaurantes: qualquer usuário autenticado ativo.
- Criar restaurante: usuário autenticado ativo; ele vira OWNER.
- Listar equipe: OWNER/MANAGER do restaurante.
- Convidar: OWNER do restaurante.
- Aceitar: usuário autenticado cujo e-mail corresponde ao convite.
- Alterar/remover membro: OWNER do restaurante; OWNER não pode ser alvo.
- MANAGER e STAFF não podem administrar equipe.

## Respostas e erros

Os DTOs e validações do código são a fonte final do contrato. Em geral:
- `201 Created`: cadastro de usuário, restaurante ou convite.
- `200 OK`: consultas, login, aceite e alteração de papel.
- `204 No Content`: remoção de membro.
- `400 Bad Request`: payload inválido, convite inválido/expirado ou regra de negócio violada conforme o tratamento do serviço.
- `401 Unauthorized`: token ausente/inválido ou usuário inativo.
- `403 Forbidden`: usuário autenticado sem papel suficiente.
- `409 Conflict`: conflito de unicidade, como e-mail já cadastrado, conforme o tratamento do endpoint.

## Ainda não existe

Não documentar como endpoint disponível: CRUD de cardápio, categorias, pratos, upload de mídia, QR Code, IA, fidelidade, Boraê Club, descoberta, pedidos ou pagamentos. Esses módulos são planejados e ainda precisam de implementação.
