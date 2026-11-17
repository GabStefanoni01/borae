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
| POST | `/api/v1/restaurants/{restaurantId}/publish` | OWNER ou MANAGER | Publica o perfil do restaurante e habilita sua página pública. |
| POST | `/api/v1/restaurants/{restaurantId}/unpublish` | OWNER ou MANAGER | Despublica o perfil sem arquivar cardápios ou apagar conteúdo. |
| GET | `/api/v1/restaurants/{restaurantId}/qrcode` | OWNER ou MANAGER | Gera e baixa um QR Code PNG para a URL pública do restaurante. |
| GET | `/api/v1/restaurants/{restaurantId}/members` | OWNER ou MANAGER | Lista membros ativos do restaurante. |
| POST | `/api/v1/restaurants/{restaurantId}/invitations` | OWNER | Cria convite para MANAGER ou STAFF. |
| POST | `/api/v1/restaurant-invitations/accept` | Autenticado | Aceita convite usando o token e a conta com e-mail correspondente. |
| PATCH | `/api/v1/restaurants/{restaurantId}/members/{membershipId}` | OWNER | Altera papel de membro entre MANAGER e STAFF. |
| DELETE | `/api/v1/restaurants/{restaurantId}/members/{membershipId}` | OWNER | Desativa vínculo de membro; não remove o proprietário. |
| GET | `/api/v1/restaurants/{restaurantId}/menus` | Membro ativo | Lista cardápios do restaurante. |
| POST | `/api/v1/restaurants/{restaurantId}/menus` | OWNER ou MANAGER | Cria cardápio. |
| GET | `/api/v1/restaurants/{restaurantId}/menus/{menuId}` | Membro ativo | Consulta cardápio. |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}` | OWNER ou MANAGER | Atualiza cardápio. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/publish` | OWNER ou MANAGER | Publica cardápio somente quando existe ao menos uma categoria ativa com um produto não arquivado. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/unpublish` | OWNER ou MANAGER | Despublica cardápio. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/archive` | OWNER ou MANAGER | Arquiva cardápio. |
| GET | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/categories` | Membro ativo | Lista categorias. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/categories` | OWNER ou MANAGER | Cria categoria. |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/categories/{categoryId}` | OWNER ou MANAGER | Atualiza categoria. |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/categories/order` | OWNER ou MANAGER | Reordena categorias. |
| GET | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products` | Membro ativo | Lista produtos; aceita filtro opcional `categoryId`. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products` | OWNER ou MANAGER | Cria produto com ingredientes e adicionais. |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}` | OWNER ou MANAGER | Atualiza dados, ingredientes e adicionais. |
| PATCH | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/availability` | OWNER, MANAGER ou STAFF | Altera somente a disponibilidade operacional. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/archive` | OWNER ou MANAGER | Arquiva produto. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/restore` | OWNER ou MANAGER | Reativa produto arquivado. |
| PUT | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/order` | OWNER ou MANAGER | Reordena os produtos de uma categoria; o corpo informa `categoryId` e a lista completa de `productIds`. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/image` | OWNER ou MANAGER | Envia ou substitui imagem do produto (JPEG, PNG ou WebP, até 10 MB), processada no backend e armazenada no ImageKit. |
| DELETE | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/image` | OWNER ou MANAGER | Remove a imagem associada ao produto e agenda a remoção do arquivo remoto após commit. |
| GET | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d` | OWNER ou MANAGER | Consulta metadados do modelo 3D associado ao produto. |
| POST | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d` | OWNER ou MANAGER | Envia/substitui GLB 2.0 via multipart (`file`, `widthMm`, `heightMm`, `depthMm`); limite provisório de 10 MB. |
| PATCH | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d/publication` | OWNER ou MANAGER | Publica/despublica o modelo com body `published: true/false`. |
| DELETE | `/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d` | OWNER ou MANAGER | Remove a associação e agenda a exclusão do asset remoto após commit. |

## 3. Consulta pública do cardápio

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| GET | `/api/v1/public/restaurants/{slug}/menu` | Público, sem JWT | Retorna dados públicos do restaurante, cardápios publicados, categorias ativas e produtos não arquivados. |

Regras da resposta pública:
- O restaurante precisa estar ativo e com `publication_status = PUBLISHED`; caso contrário, retorna 404.
- Somente cardápios `PUBLISHED` são retornados.
- Somente categorias ativas são retornadas.
- Produtos com ciclo de vida `ARCHIVED` são omitidos; produtos `UNAVAILABLE` permanecem visíveis com seu estado explícito.
- Somente adicionais ativos são incluídos.
- A resposta usa DTOs públicos próprios e não inclui dados de equipe, membership, credenciais ou configurações internas.
- A imagem é representada pela URL pública do ImageKit quando houver imagem associada.
- O produto inclui `model3d` somente quando há modelo publicado; o objeto contém URL pública, MIME e dimensões em milímetros. Sem modelo publicado, `model3d` é `null`.
- O caminho `/api/v1/public/**` é liberado da autenticação JWT; os demais endpoints continuam protegidos conforme suas regras.

**Publicação do perfil:** OWNER ou MANAGER pode publicar e despublicar o perfil pelos endpoints autenticados. Restaurante suspenso não pode ser publicado nem ter seu estado alterado pelo painel da equipe. Despublicar não arquiva cardápios nem apaga o catálogo.

### QR Code principal

O endpoint autenticado `GET /api/v1/restaurants/{restaurantId}/qrcode` retorna `image/png` como download. O QR Code aponta para a URL pública do restaurante, nunca para um cardápio individual. A URL é configurada por `PUBLIC_RESTAURANT_URL_PREFIX`, usando o prefixo da rota pública e acrescentando o slug do restaurante (exemplo: `https://seudominio.com/r/`). OWNER e MANAGER podem gerar o arquivo; STAFF não tem permissão.

## 4. Regras de equipe implementadas

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

## 5. Catálogo — contratos implementados na branch da PR #22

- Valores de produtos e acréscimos de adicionais são inteiros em centavos (BRL).
- Ingredientes são uma lista textual ordenada por produto; nomes duplicados são rejeitados após normalização sem distinção de caixa/acentos.
- Adicionais possuem nome, preço adicional em centavos e estado ativo/inativo; duplicidades são rejeitadas.
- Produto arquivado não pode ser editado; pode ser reativado por OWNER/MANAGER.
- Produto indisponível permanece cadastrado e pode ser exibido futuramente com indicação própria.
- A imagem do produto é opcional; o binário fica no ImageKit e o banco guarda metadados e associação.
- STAFF pode alterar apenas a disponibilidade.
- Todas as operações validam vínculo ativo com restaurante e pertencimento de cardápio/categoria/produto, evitando troca de IDs entre restaurantes.
- A ordenação de produtos é definida por categoria e exige a lista completa dos produtos daquela categoria.

## 6. Criação de restaurante

`POST /api/v1/restaurants` cria o estabelecimento e o vínculo OWNER em uma única transação. Identidade do proprietário e papel são definidos no servidor a partir do usuário autenticado; não confiar em valores enviados pelo frontend.

## 7. Respostas e erros
Os detalhes completos de DTOs, validações, status HTTP e exemplos devem ser consultados no Swagger/OpenAPI da versão publicada. Ao alterar um contrato:
1. Atualizar DTO/controller e testes.
2. Atualizar esta tabela e regras relevantes.
3. Conferir a documentação OpenAPI gerada.
4. Atualizar o frontend consumidor do contrato no mesmo PR, quando aplicável.

## 8. Funcionalidades ainda não disponíveis na API
O visualizador 3D/AR no frontend, IA, fidelidade, Boraê Club, descoberta, pedidos, pagamentos e administração global ainda não estão disponíveis. O backend de gestão de modelos 3D está em implementação na PR #27 e depende de CI verde, homologação do upload/entrega no ImageKit e aprovação antes da integração à `main`.
