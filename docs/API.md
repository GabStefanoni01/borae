# API e estado implementado

## 1. Base e autenticação

A API utiliza prefixo `/api/v1`. Em produção, a base está publicada em:

`https://borae.onrender.com`

A API usa JWT para endpoints protegidos. O frontend obtém o token no fluxo de autenticação, envia `Authorization: Bearer <token>` e protege a área autenticada.

Consulte a documentação OpenAPI/Swagger publicada para obter os contratos completos, schemas, códigos de resposta e endpoints de autenticação. Este documento lista apenas os endpoints confirmados no desenvolvimento registrado; não substitui o Swagger.

## 2. Endpoints confirmados

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| GET | `/api/v1/users/me` | Autenticado | Retorna os dados da conta autenticada. |
| GET | `/api/v1/users/me/restaurants` | Autenticado | Lista os restaurantes aos quais o usuário possui vínculo ativo. |
| POST | `/api/v1/restaurants` | Autenticado | Cria restaurante e vínculo OWNER na mesma transação. |
| GET | `/api/v1/restaurants/{restaurantId}/members` | OWNER ou MANAGER | Lista membros ativos do restaurante. |
| POST | `/api/v1/restaurants/{restaurantId}/invitations` | OWNER | Cria convite para MANAGER ou STAFF. |
| POST | `/api/v1/restaurant-invitations/accept` | Autenticado | Aceita convite usando o token e a conta com e-mail correspondente. |
| PATCH | `/api/v1/restaurants/{restaurantId}/members/{membershipId}` | OWNER | Altera papel de membro entre MANAGER e STAFF. |
| DELETE | `/api/v1/restaurants/{restaurantId}/members/{membershipId}` | OWNER | Desativa vínculo de membro; não remove o proprietário. |

## 3. Regras de equipe implementadas

### Papéis
- OWNER: proprietário do restaurante.
- MANAGER: gerente.
- STAFF: colaborador.

### Convites
- Somente OWNER cria convites.
- O papel do convite deve ser MANAGER ou STAFF; não é permitido convidar OWNER.
- O token é gerado com fonte criptograficamente segura e tem validade de 7 dias.
- O banco armazena o hash SHA-256 do token, não o token em texto puro.
- A resposta da criação inclui o token para compartilhamento manual. Ainda não há envio automático por e-mail.
- O usuário autenticado só pode aceitar convite se o e-mail da conta corresponder ao e-mail convidado.
- Convite expirado ou já aceito não pode ser reutilizado.
- Se o usuário já teve vínculo removido, o vínculo pode ser reativado conforme a regra implementada.

### Gestão de membros
- OWNER e MANAGER podem consultar a equipe.
- Somente OWNER pode alterar papel ou remover membros.
- OWNER não pode ser removido nem ter seu papel alterado pelos endpoints de equipe.
- Remoção é lógica: o vínculo fica inativo.

## 4. Criação de restaurante
`POST /api/v1/restaurants` cria o estabelecimento e o vínculo OWNER em uma única transação. Identidade do proprietário e papel são definidos no servidor a partir do usuário autenticado; não confiar em valores enviados pelo frontend.

## 5. Respostas e erros
Os detalhes completos de DTOs, validações, status HTTP e exemplos devem ser consultados no Swagger/OpenAPI da versão publicada. Ao alterar um contrato:
1. Atualizar DTO/controller e testes.
2. Atualizar esta tabela e regras relevantes.
3. Conferir a documentação OpenAPI gerada.
4. Atualizar o frontend consumidor do contrato no mesmo PR, quando aplicável.

## 6. Funcionalidades ainda não disponíveis na API
Não assumir que já existem endpoints de cardápio, categorias, produtos, imagens, modelos 3D, IA, fidelidade, Boraê Club, descoberta, pedidos, pagamentos ou administração global. Esses módulos estão no roadmap e devem ser implementados somente na sequência aprovada.
