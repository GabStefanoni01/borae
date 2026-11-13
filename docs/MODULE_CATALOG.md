# Especificação de módulo — Catálogo e cardápio

**Prioridade:** próxima grande entrega de produto após perfil/configuração do restaurante.  
**Status:** planejado; nenhum endpoint de catálogo deve ser presumido existente.

## 1. Objetivo

Permitir que o restaurante cadastre e mantenha um cardápio confiável, e que consumidores consultem somente conteúdo publicado.

## 2. Escopo inicial

### Cardápios
- Um restaurante pode possuir múltiplos cardápios, por exemplo Almoço, Jantar e Bebidas.
- Cada cardápio tem suas próprias categorias e produtos.
- Vários cardápios podem estar publicados simultaneamente.
- A página pública do restaurante lista os cardápios publicados para o consumidor escolher.
- No MVP, não existe alternância automática por horário.
- O QR Code principal aponta para a página pública do restaurante, não para um cardápio individual.

**Decisão de produto D-02 aprovada em 13/11/2026.**

### Categorias
- Nome obrigatório.
- Descrição opcional.
- Ordem de exibição.
- Estado ativo/inativo.
- Não permitir categorias duplicadas com mesmo nome no mesmo menu, conforme regra de normalização a definir.
- Categoria inativa não deve aceitar novos produtos publicados; comportamento de produtos já associados deve ser definido antes do schema.

### Produtos
Campos mínimos propostos:
- Nome obrigatório.
- Descrição opcional.
- Preço obrigatório e não negativo.
- Moeda.
- Categoria.
- Imagem opcional.
- Lista de ingredientes: itens individuais cadastrados livremente pelo restaurante, cada um associado ao produto e com ordem de exibição.
- O restaurante pode adicionar, editar e remover ingredientes. Exemplo: pão, carne, queijo, cebola e bacon.
- Disponibilidade: AVAILABLE / UNAVAILABLE.
- Ordem de exibição.

Não misturar disponibilidade operacional com publicação: um produto pode estar publicado e temporariamente indisponível.

**Decisão de produto D-04 aprovada em 13/11/2026:** ingredientes são cadastrados como uma lista livre de itens individuais associados ao produto. Não há catálogo padronizado de alergênicos nem declaração específica de contaminação cruzada neste escopo.

### Adicionais e instruções no pedido
- O restaurante pode cadastrar adicionais opcionais, como bacon, cheddar e ovo.
- Cada adicional deve ter preço/acréscimo definido e visível ao consumidor no cardápio.
- Remoção de ingredientes não é cadastrada como opção do produto.
- No momento do pedido, o cliente pode informar instruções como “sem cebola” ou “sem tomate”; essas observações devem ser exibidas claramente ao garçom.
- Ponto da carne, quando aplicável, também pode ser informado como instrução no pedido.
- Não haverá tamanhos ou combos nesta etapa.

**Decisão de produto D-05 aprovada em 13/11/2026.**

## 3. Regras de preço

- Preço é mantido pelo restaurante e exibido como valor atual.
- API não aceita preço calculado pelo frontend como autoridade.
- O preço de cada adicional deve ser apresentado antes da seleção; backend calcula o valor considerando os adicionais escolhidos.
- Observações de remoção de ingredientes não alteram o preço.
- Histórico de preço não é obrigatório no primeiro MVP, mas alterações administrativas devem ser auditáveis.
- O MVP utiliza somente BRL.\n- Preços de produtos e adicionais são armazenados como inteiros em centavos.\n- A API utiliza campos como `priceInCents` e `priceDeltaInCents`; a interface formata os valores para exibição em reais.\n- O backend realiza os cálculos monetários, sem `float` ou `double`.\n- Preços não podem ser negativos.

## 4. Publicação

**Decisão de produto D-03 aprovada em 13/11/2026.** A publicação é controlada por cardápio (`Menu`), não por produto nem globalmente por restaurante.

Fluxo:
1. Um cardápio novo é criado como `DRAFT`.
2. O restaurante adiciona categorias e produtos e revisa a prévia.
3. A ação de publicar valida a consistência mínima do cardápio e muda seu estado para `PUBLISHED`.
4. Categorias ativas e produtos elegíveis do cardápio passam a aparecer na página pública.
5. Em cardápio publicado, alterações salvas entram no ar imediatamente, sem uma segunda etapa de publicação.
6. O restaurante pode despublicar ou arquivar o cardápio sem apagar seus dados.

Não existe publicação individual de produto no MVP. Produto arquivado não aparece publicamente. Produto indisponível pode continuar visível, identificado como indisponível. A disponibilidade operacional é independente da publicação do cardápio.

## 5. Menu público

- Endpoint público usa slug estável.
- Retorna apenas menu publicado, categorias ativas e produtos publicados.
- Pode incluir produtos indisponíveis com status claro.
- Não retorna campos internos, IDs de usuário, equipe ou notas administrativas.
- Dados são cacheáveis apenas com estratégia de invalidação ao atualizar preço/disponibilidade.

## 6. Upload de imagem

- Upload via serviço de mídia, não guardar binário no PostgreSQL.
- Validar MIME real, extensão, tamanho máximo e checksum.
- Gerar variantes otimizadas para mobile.
- Remover metadados EXIF sensíveis quando possível.
- Restringir acesso a arquivos privados e validar escopo do restaurante.

Limites de tamanho e formatos permitidos precisam ser definidos na configuração do produto.

## 7. Critérios de aceite

- OWNER consegue criar/editar categoria e produto.
- Usuário de outro restaurante não consegue ler ou alterar recurso privado por troca de ID.
- Produto DRAFT não aparece no menu público.
- Produto indisponível não é apresentado como disponível.
- Preço e acréscimos são calculados pelo backend.
- Alterações são refletidas na página pública dentro do SLA de cache definido.
- Fluxos têm estados de carregamento, erro, vazio e sucesso.
- Testes cobrem validação, autorização, isolamento e publicação.

## 8. Fora do primeiro recorte
- Estoque integrado a ERP/POS.
- Pedidos e pagamento.
- Importação automática de cardápios.
- Gestão complexa de combos e promoções.
- Tradução automática.
