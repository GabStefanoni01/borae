# Diretrizes e contratos planejados da API

Este documento é um guia de desenho para módulos futuros, não uma declaração de que os endpoints já existem. Os contratos finais devem ser aprovados e publicados no OpenAPI antes da implementação.

## 1. Convenções gerais

- Base: `/api/v1`.
- JSON em UTF-8.
- Datas em ISO-8601 com timezone.
- UUIDs em formato string.
- Paginação futura: `page`, `size`, `sort`, com limites máximos no servidor.
- Erros padronizados (preferencialmente Problem Details/RFC 9457): type, title, status, detail e campos de validação.
- Não retornar entidades JPA.
- Não confiar em restaurantId, userId, role ou ownerId como prova de autorização.
- Toda operação privada deriva usuário do JWT, valida membership ativo no restaurante solicitado e aplica a matriz D-07 no backend. OWNER tem controle total; MANAGER administra perfil, catálogo, publicação, disponibilidade, equipe e convites, sem alterar papéis, propriedade ou cobrança; STAFF tem acesso operacional ao painel e à disponibilidade de produtos, além de pedidos quando o módulo existir. O mesmo usuário pode ter papéis diferentes por restaurante. Autorização não pode depender apenas de controles visuais no frontend.

## Convites de equipe — regras D-08

- Criar convite vinculado ao e-mail e papel autorizado: OWNER pode convidar MANAGER/STAFF; MANAGER somente STAFF.
- Aceitar convite exige usuário autenticado com e-mail correspondente ao convite.
- Validade de 7 dias a partir da criação ou reenvio.
- Permitir revogação de convites pendentes por OWNER/MANAGER.
- Permitir reenvio de convites pendentes ou expirados, rotacionando o token e renovando a validade por 7 dias.
- Aceitação deve ser atômica e de uso único; convite aceito, revogado ou expirado não pode ser aceito.
- Persistir somente hash do token; nunca registrar o token em logs.
- Validar papel de quem convida e membership ativo no restaurante em toda operação.

O limite numérico de convites simultâneos e a política de entrega de e-mail ainda não foram definidos. Os endpoints e payloads finais serão especificados antes da implementação.

## 2. Recursos já existentes

Consultar [API.md](API.md) para a lista de endpoints confirmados. Não alterar contratos existentes sem compatibilidade ou migração coordenada do frontend.

## 3. Perfil do restaurante — proposta

- `GET /api/v1/restaurants/{restaurantId}`
- `PATCH /api/v1/restaurants/{restaurantId}`
- `GET /api/v1/public/restaurants/{slug}`

O GET público deve retornar somente campos publicados e seguros. O PATCH exige o papel autorizado pela matriz D-07: OWNER ou MANAGER para edição de perfil.

## 4. Catálogo — proposta

- `GET /api/v1/restaurants/{restaurantId}/menus`
- `POST /api/v1/restaurants/{restaurantId}/menus`
- `PATCH /api/v1/restaurants/{restaurantId}/menus/{menuId}`
- `GET/POST /api/v1/restaurants/{restaurantId}/menus/{menuId}/categories`
- `PATCH/DELETE /api/v1/restaurants/{restaurantId}/categories/{categoryId}`
- `GET/POST /api/v1/restaurants/{restaurantId}/products`
- `GET/PATCH/DELETE /api/v1/restaurants/{restaurantId}/products/{productId}`
- `POST /api/v1/restaurants/{restaurantId}/products/{productId}/publish`
- `POST /api/v1/restaurants/{restaurantId}/products/{productId}/availability`
- `GET /api/v1/public/restaurants/{slug}/menu`

Todos os IDs relacionados devem ser validados contra o mesmo restaurante. Operações de ordenação devem receber uma lista explícita de IDs e validar que todos pertencem ao conjunto autorizado.

### Exemplo conceitual de produto (não é contrato fechado)
```json
{
  "name": "Nome do prato",
  "description": "Descrição informativa",
  "priceInCents": 3990,
  "currency": "BRL",
  "addons": [
    { "name": "Bacon extra", "priceDeltaInCents": 500 }
  ],
  "categoryId": "uuid",
  "ingredients": ["ingrediente A", "ingrediente B"],
  "availability": "AVAILABLE"
}
```

D-06 aprovada: o MVP utiliza BRL e representa valores monetários em centavos como inteiros (`priceInCents`, `priceDeltaInCents`). O backend realiza os cálculos; não utilizar `float` ou `double`. A API retorna valores em centavos e a interface os formata para exibição em reais. D-04 e D-05 estão aprovadas: ingredientes são itens livres; adicionais são cadastrados com preço explícito e exibido ao consumidor. Remoções de ingredientes e ponto da carne, quando aplicável, são observações informadas no momento do pedido e apresentadas ao garçom, não opções pré-cadastradas no produto. Não há tamanhos ou combos nesta etapa.

## 5. Mídia — proposta

- `POST /api/v1/restaurants/{restaurantId}/media/upload-intents`
- `POST /api/v1/restaurants/{restaurantId}/media/{mediaId}/complete`
- `DELETE /api/v1/restaurants/{restaurantId}/media/{mediaId}`

D-09 aprovada: usar serviço externo de object storage; PostgreSQL guarda somente metadados e referências. Cloudinary é candidato inicial para avaliação de imagens, não provedor definitivo até validação de custos, limites e suporte a GLB/glTF. Formatos aceitos: JPG/JPEG, PNG, WebP, GLB e glTF. Preferir upload direto por URL assinada quando suportado. Backend valida membership/permissão, MIME real (não confiar no Content-Type declarado), tamanho, checksum, status e vínculo da mídia ao restaurante. Não aceitar URL arbitrária do cliente como mídia confiável. Entrega deve permitir CDN, variantes otimizadas de imagem e carregamento sob demanda. Limites numéricos e provedor final permanecem pendentes.

## 6. IA — proposta

- `POST /api/v1/public/restaurants/{slug}/recommendations`

Entrada conceitual:
```json
{
  "message": "Quero algo leve até 40 reais",
  "filters": {
    "maxPrice": 40
  }
}
```

Saída deve conter referências a produtos existentes:
```json
{
  "recommendations": [
    {
      "productId": "uuid",
      "reason": "Explicação baseada nos dados cadastrados",
      "catalogVersion": "versão ou timestamp consultado"
    }
  ],
  "notice": "Confirme alergênicos e risco de contaminação cruzada com o restaurante."
}
```

O backend deve revalidar IDs, publicação, preço e disponibilidade antes de responder. Não expor chain-of-thought, prompt interno ou credenciais do provedor.

## 7. Fidelidade — proposta

Recursos candidatos:
- `GET /api/v1/restaurants/{restaurantId}/loyalty/program`
- `PUT /api/v1/restaurants/{restaurantId}/loyalty/program`
- `GET /api/v1/me/loyalty-accounts`
- `GET /api/v1/me/loyalty-accounts/{accountId}/ledger`
- `POST /api/v1/restaurants/{restaurantId}/loyalty/transactions`
- `POST /api/v1/me/reward-redemptions`

Crédito/resgate deve exigir chave de idempotência e autorização específica. O consumidor não pode criar créditos para si mesmo. O evento que gera pontos precisa ser validado por origem confiável (ex.: integração aprovada ou ação autorizada do restaurante).

## 8. Boraê Club — proposta, bloqueada por decisões comerciais

Recursos candidatos:
- adesão e saída do programa;
- consulta de saldo e ledger;
- consulta de parceiros e benefícios;
- criação e confirmação de resgate;
- estorno e liquidação de parceiro.

Não criar endpoints de movimentação real até definir contratos, responsabilidade financeira, fraude, cancelamento, chargeback e conciliação.

## 9. Descoberta — proposta

- `GET /api/v1/public/restaurants` com filtros por categoria, localização e paginação.
- `GET /api/v1/public/restaurants/{slug}`.
- `GET /api/v1/public/restaurants/{slug}/menu`.

Não expor endereço privado, contatos internos, membros ou indicadores do painel.

## 10. Autorização e erros

Testar pelo menos:
- Sem token: 401.
- Token inválido/expirado: 401.
- Usuário sem vínculo: 403 ou 404 conforme política de não enumeração definida.
- Membership inativo: acesso negado.
- Papel insuficiente: 403.
- Recurso inexistente: 404.
- Payload inválido: 400 com campos identificados.
- Conflito de slug/duplicidade: 409.

A decisão entre 403 e 404 para evitar enumeração deve ser uniforme por recurso e documentada.

## 11. Compatibilidade

- Alterações aditivas preferidas.
- Renomear/remover campo exige estratégia de depreciação.
- Versionar breaking changes.
- Atualizar OpenAPI, frontend, testes e docs no mesmo PR.
