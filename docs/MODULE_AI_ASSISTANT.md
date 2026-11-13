# Especificação de módulo — Assistente gastronômico

**Status:** planejado; depende do catálogo publicado e confiável.

## Provedor e arquitetura de integração — D-10

- Google Gemini é o provedor inicial aprovado, utilizando a Gemini API no plano gratuito inicialmente; não habilitar faturamento para o projeto sem nova aprovação.
- Somente o backend Java/Spring Boot integra com a API do provedor; o frontend conversa exclusivamente com a API do Boraê.
- Manter credenciais em segredos/variáveis de ambiente do servidor. Nunca incluir chaves no frontend, repositório, logs ou respostas.
- Isolar a integração atrás de uma interface interna de provedor/serviço de IA, mantendo o domínio e os casos de uso independentes do SDK e dos tipos específicos do Gemini.
- O backend seleciona o restaurante e monta o contexto mínimo a partir do catálogo publicado; o modelo não acessa banco, ferramentas administrativas nem dados de outros restaurantes.
- Aplicar rate limits por sessão/IP e controles de custo/consumo com visibilidade por restaurante.
- Em falha, timeout ou saída inválida, manter a navegação disponível e recorrer à busca/filtros convencionais.

Modelo/versão e cotas numéricas por modelo permanecem pendentes. O plano gratuito tem limites variáveis e está sujeito às políticas de uso de dados do Google; não enviar informações pessoais ou sensíveis. Evolução para plano pago exige nova aprovação. A decisão não implica integração implementada.

## 1. Objetivo

Ajudar o consumidor a encontrar pratos adequados ao que deseja, usando somente os itens reais do cardápio do restaurante em contexto.

## 2. Contexto permitido

O assistente pode considerar:
- Mensagem do consumidor.
- Restaurante/menu selecionado.
- Preço atual, categoria, descrição e ingredientes declarados.
- Preferências voluntariamente informadas.
- Disponibilidade consultada no backend.

Não deve receber dados privados de outros restaurantes, credenciais, dados desnecessários de conta ou informações pessoais não relacionadas à recomendação.

## 3. Fluxo

1. Consumidor abre assistente no cardápio do restaurante.
2. Frontend envia pergunta e filtros ao backend.
3. Backend identifica restaurante pelo slug/escopo público e carrega catálogo publicado.
4. Backend prepara contexto mínimo e chama serviço de IA.
5. Serviço retorna IDs de produtos candidatos e justificativas.
6. Backend valida cada ID contra catálogo, preço e disponibilidade atuais.
7. Backend remove referências inválidas e devolve resposta estruturada.
8. Frontend apresenta cards de produtos com link para detalhes.

## 4. Contrato de saída

Cada recomendação deve incluir:
- productId real.
- nome e preço obtidos do catálogo pelo backend.
- justificativa curta e baseada em atributos cadastrados.
- sinalização de disponibilidade.
- composição baseada nos ingredientes cadastrados pelo restaurante.

O texto gerado pelo modelo não é fonte de verdade para preço, ingredientes ou disponibilidade.

## 5. Regras de segurança e confiabilidade

- IA não consulta banco diretamente.
- IA não cria produtos nem promoções.
- IA não inventa disponibilidade.
- Prompt injection em descrição de produto ou mensagem do consumidor deve ser tratado como conteúdo não confiável.
- Backend valida saída estruturada e descarta IDs não pertencentes ao restaurante.
- Rate limit por sessão/IP e limites de custo.
- Timeout, retry limitado e fallback para busca/filtros tradicionais.
- Não expor prompt de sistema, chaves ou chain-of-thought.
- Guardar métricas e logs sem conteúdo pessoal desnecessário.

## 6. Uso dos ingredientes

- Usar somente a lista de ingredientes cadastrada pelo restaurante para explicar a composição do prato.
- Não presumir ingredientes, receitas ou modos de preparo que não estejam informados.
- A lista é informativa e não representa uma declaração técnica de alergênicos ou de ausência de contaminação cruzada.
- Se o consumidor perguntar sobre segurança para uma alergia, orientar que confirme diretamente com o estabelecimento; nunca garantir segurança alimentar com base apenas no cardápio.

## 7. Experiência

- Sugestões devem mostrar por que foram recomendadas.
- Oferecer filtros de preço/categoria e opção de refazer.
- Se nenhum item atender, informar claramente; não preencher com itens inventados.
- O consumidor deve poder continuar navegando sem usar IA.

## 8. Métricas e privacidade — D-11

Não persistir conversas, prompts ou respostas completas. Armazenar somente eventos analíticos e métricas agregadas vinculados ao restaurante, sem conteúdo textual das mensagens ou identificadores pessoais.

Métricas por restaurante:
- Quantidade de interações com o assistente.
- Produtos recomendados e frequência de recomendação por produto.
- Cliques nas recomendações e taxa de clique.
- Interações sem recomendação adequada/sem resultado.
- Latência/tempo de resposta.
- Erros, timeouts, respostas inválidas e acionamentos de fallback.
- Quantidade de chamadas e consumo de tokens, quando disponibilizados pelo provedor.

Exibir somente para usuários autorizados daquele restaurante, respeitando o isolamento multi-tenant. Definir retenção mínima necessária para os agregados na implementação. Logs técnicos não devem conter mensagens completas.

Cliques indicam interesse, não comprovam compra ou conversão. Métricas de pedidos/vendas dependem de integração confiável com pedidos, fora deste recorte.

## 9. Critérios de aceite
- Nenhum produto fora do restaurante pode ser retornado.
- Produto não publicado ou indisponível não é sugerido como disponível.
- Preço exibido vem do backend.
- Timeout/falha da IA não bloqueia cardápio.
- Respostas sobre composição correspondem aos ingredientes cadastrados e não inventam componentes.
- Testes incluem prompt injection, saída malformada e IDs inventados.
