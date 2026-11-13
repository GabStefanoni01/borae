# Especificação de módulo — Descoberta de restaurantes

**Status:** planejado; depende de perfis públicos consistentes.

## 1. Objetivo

Ajudar consumidores a encontrar restaurantes, conhecer sua proposta e abrir seus cardápios.

## 2. Dados públicos mínimos
- Nome público e slug.
- Descrição.
- Logo/capa.
- Tipo de culinária/categorias.
- Endereço ou área de atendimento conforme opção do restaurante.
- Horários, somente quando o restaurante mantiver informação atualizada.
- Benefícios ativos e elegibilidade, quando existirem.
- Link para cardápio.

Não exibir dados internos, equipe, relatórios ou contatos privados.

## 3. Busca e filtros
- Localização (cidade/bairro ou geolocalização opcional com permissão).
- Categoria/tipo de culinária.
- Nome.
- Benefícios/participação em programas, quando habilitados.

Definir raio, ordenação, geocodificação e tratamento de localização indisponível antes de implementar. Não solicitar localização sem explicar finalidade e oferecer busca manual.

## 4. Regras
- Apenas restaurantes publicados e ativos aparecem.
- Restaurante pode ocultar endereço exato e exibir somente área atendida, se aplicável.
- Horário não pode ser inferido como aberto agora se os dados estiverem ausentes/desatualizados.
- Não criar avaliações ou notas fictícias.
- Ranking patrocinado deve ser identificado como publicidade quando existir.

## 5. Critérios de aceite
- Busca retorna apenas estabelecimentos elegíveis.
- Filtros podem ser combinados e paginados.
- Usuário pode abrir perfil e menu.
- Localização é opcional.
- Dados de parceiros e benefícios refletem status atual.
- Resultados têm estados vazios e erros claros.

## 6. Fora do primeiro recorte
- Reviews/ratings públicos.
- Feed social.
- Reservas e pedidos.
- Ranking patrocinado até existir política comercial e transparência.
