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
- Ingredientes declarados.
- Declaração de alergênicos, se fornecida pelo restaurante.
- Disponibilidade: AVAILABLE / UNAVAILABLE.
- Publicação: DRAFT / PUBLISHED / ARCHIVED.
- Ordem de exibição.

Não misturar disponibilidade operacional com publicação: um produto pode estar publicado e temporariamente indisponível.

### Variações e adicionais
Um produto pode ter grupos de opções. Cada grupo define mínimo e máximo de seleções; cada opção pode ter acréscimo de preço. Backend valida seleção, limites e preço final. A interface deve mostrar claramente o preço base e acréscimos.

**Pendente:** definir se haverá variação de tamanho, ponto de carne, acompanhamentos, adicionais e combos no primeiro lançamento; definir se opções podem ser obrigatórias e se uma opção pode ser selecionada mais de uma vez.

## 3. Regras de preço

- Preço é mantido pelo restaurante e exibido como valor atual.
- API não aceita preço calculado pelo frontend como autoridade.
- Se opções alterarem o valor, backend calcula total a partir da configuração persistida.
- Histórico de preço não é obrigatório no primeiro MVP, mas alterações administrativas devem ser auditáveis.
- Não usar float/double para cálculo monetário.

## 4. Publicação

Proposta de fluxo:
1. Produto é criado como DRAFT.
2. Restaurante preenche dados obrigatórios.
3. Prévia permite conferir a apresentação.
4. Ação publicar valida consistência e muda estado.
5. Alterações posteriores seguem política explícita de publicação.

**Decisão pendente:** publicação imediata ou rascunho de alterações com botão publicar; publicação por produto ou publicação do menu completo. Recomenda-se começar com publicação por produto para reduzir complexidade, mas confirmar com responsável de produto.

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
