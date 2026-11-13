# Módulo — Descoberta de restaurantes

**Status:** regras de produto aprovadas em D-16. Implementação ainda planejada.

## 1. Objetivo

Ajudar consumidores a encontrar restaurantes, conhecer sua proposta, consultar cardápios e identificar benefícios de fidelidade.

## 2. Localização, busca e ordenação

- Usar a localização atual como referência padrão, mediante permissão do usuário.
- Permitir pesquisar ou selecionar manualmente outra cidade, bairro ou região.
- Ordenar resultados por relevância, considerando distância, avaliações, popularidade e completude/qualidade das informações.
- Personalização da ordenação com base no histórico do consumidor fica para uma evolução futura.
- Não inferir localização precisa quando a permissão não foi concedida; oferecer busca manual.

## 3. Filtros

Permitir combinar:
- Categoria/tipo de culinária.
- Distância.
- Faixa de preço.
- Avaliação.
- Horário de funcionamento.
- Benefícios de fidelidade.
- Modalidade de atendimento: entrega e retirada.

## 4. Página pública do restaurante

A página deve apresentar, quando disponíveis:
- Fotos, nome, descrição e identidade visual.
- Endereço e mapa.
- Horários de funcionamento.
- Cardápios e preços.
- Avaliações.
- Informações do programa de fidelidade.
- Modalidades de atendimento.
- Modelos 3D dos pratos, de forma opcional e somente quando disponíveis.

Dados ausentes não devem ser inventados. Restaurantes com cadastro incompleto podem aparecer com as informações existentes, deixando claro o que não foi informado.

## 5. Avaliações e respostas

- Somente consumidores com compra confirmada podem avaliar.
- A avaliação contém nota geral de 1 a 5, comentário opcional e critérios individuais de atendimento, comida e ambiente.
- O restaurante pode responder publicamente.
- O restaurante não pode editar nem apagar a avaliação publicada pelo consumidor.
- A moderação de denúncias é responsabilidade do Boraê. Não haverá painel dedicado de denúncias/moderação no escopo atual (D-19).

## 6. Favoritos, listas e compartilhamento

- Consumidores podem favoritar restaurantes e criar listas personalizadas.
- Compartilhamento de restaurantes e listas ocorre por links.
- Não haverá mensagens internas, rede social própria nem listas colaborativas no MVP.

## 7. Redes e unidades

- Uma rede com várias unidades terá uma página principal da marca e páginas individuais por unidade.
- A página da marca apresenta identidade, apresentação e lista de unidades.
- Cada unidade mantém endereço, horários, cardápio, avaliações, fidelidade e modalidades de atendimento próprios.
- A marca terá nota geral baseada nas avaliações das unidades; cada unidade terá sua própria nota e avaliações.

## 8. Regras de exibição

- Apenas restaurantes elegíveis/publicados aparecem na descoberta.
- Restaurantes fechados aparecem abaixo dos abertos, com indicação de fechado e próxima abertura quando conhecida.
- Não inferir que um estabelecimento está aberto quando os horários estiverem ausentes ou desatualizados.
- Não criar avaliações, notas, popularidade ou benefícios fictícios.
- Ranking patrocinado, se vier a existir, deve ser identificado claramente como publicidade.

## 9. Fora do escopo atual

- Personalização por histórico do consumidor.
- Feed social, mensagens internas e listas colaborativas.
- Pedidos online, reservas e integração com PDV/ERP no lançamento.
- Aplicativos nativos para consumidor ou restaurante no lançamento.

## 10. Critérios de aceite

- Busca e filtros combináveis retornam apenas restaurantes elegíveis.
- Consumidor pode alternar entre localização atual e região pesquisada manualmente.
- Resultados respeitam ordenação por relevância e exibem estados aberto/fechado.
- Perfil de marca e páginas de unidades mantêm informações e avaliações separadas.
- Avaliações só podem ser publicadas por consumidores com compra confirmada.
- O consumidor pode abrir o perfil e o cardápio sem instalar aplicativo.
- Dados ausentes não são inventados; estados vazios e erros são claros.
