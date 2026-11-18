# Especificação de módulo — Assistente gastronômico

**Status:** implementação em andamento na PR #29 (draft); depende do catálogo publicado e confiável e de homologação ponta a ponta.

## Provedor e arquitetura de integração — D-10

- Google Gemini é o provedor aprovado. O modelo escolhido nesta fase é Gemini 3.5 Flash-Lite (`gemini-3.5-flash-lite`), versão estável GA. A modalidade de faturamento e os limites efetivos devem ser validados antes de produção; esta decisão não aprova automaticamente plano gratuito ou pago.
- Somente o backend Java/Spring Boot integra com a API do provedor; o frontend conversa exclusivamente com a API do Boraê.
- Manter credenciais em segredos/variáveis de ambiente do servidor. Nunca incluir chaves no frontend, repositório, logs ou respostas.
- Isolar a integração atrás de uma interface interna de provedor/serviço de IA, mantendo o domínio e os casos de uso independentes do SDK e dos tipos específicos do Gemini.
- O backend seleciona o restaurante e monta o contexto mínimo a partir do catálogo publicado; o modelo não acessa banco, ferramentas administrativas nem dados de outros restaurantes.
- Aplicar rate limits por sessão/IP e controles de custo/consumo com visibilidade por restaurante.
- Em falha, timeout ou saída inválida, manter a navegação disponível e recorrer à busca/filtros convencionais.

Cotas implementadas nesta etapa: até 20 interações por sessão anônima de navegador por dia e até 100 por restaurante por dia, aplicadas simultaneamente. A cota do consumidor é por sessão/cookie, não uma identificação garantida de pessoa física: limpar cookies ou trocar de navegador/dispositivo cria outra sessão. Há limite de 30 requisições por IP por minuto para antiabuso, separado da cota de sessão. A saída do modelo tem limite configurável de 500 tokens; o contexto de catálogo está limitado a 4.000 caracteres. O limite exato de tokens de entrada ainda precisa ser validado/definido antes de produção. Registrar tokens/consumo, latência, falhas e custo estimado por restaurante sem persistir conversas completas. Os agregados são retidos por 180 dias; o rate limit guarda HMAC do IP por no máximo 2 horas. O modelo padrão configurado é `gemini-3.5-flash-lite`; validar faturamento, termos de tratamento de dados e limites vigentes do Google antes de habilitar em produção. O provedor permanece desabilitado por padrão.

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
5. Serviço retorna IDs de produtos candidatos.
6. Backend valida cada ID contra catálogo, preço e disponibilidade atuais; justificativas exibidas são construídas pelo backend a partir de atributos verificados.
7. Backend remove referências inválidas e devolve resposta estruturada.
8. Frontend apresenta cards de produtos com link para detalhes.

## 4. Contrato de saída

Cada recomendação deve incluir:
- productId real.
- nome e preço obtidos do catálogo pelo backend.
- justificativa curta e baseada em atributos cadastrados.
- sinalização de disponibilidade.
- composição baseada nos ingredientes cadastrados pelo restaurante.

O texto gerado pelo modelo não é fonte de verdade para preço, ingredientes ou disponibilidade. O backend não repassa texto livre/justificativas do modelo ao consumidor.

## 5. Regras de segurança e confiabilidade

- IA não consulta banco diretamente.
- IA não cria produtos nem promoções.
- IA não inventa disponibilidade.
- Prompt injection em descrição de produto ou mensagem do consumidor deve ser tratado como conteúdo não confiável.
- Backend valida saída estruturada e descarta IDs não pertencentes ao restaurante.
- Rate limit por sessão/IP e limites de custo.
- Timeout e fallback para busca/filtros tradicionais.
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
- Custo estimado por restaurante, calculado com preços configuráveis por milhão de tokens e revisados antes de produção.

Exibir somente para usuários autorizados daquele restaurante, respeitando o isolamento multi-tenant. Definir retenção mínima necessária para os agregados na implementação. Logs técnicos não devem conter mensagens completas.

Cliques indicam interesse, não comprovam compra ou conversão. Métricas de pedidos/vendas dependem de integração confiável com pedidos, fora deste recorte.

## 9. Critérios de aceite
- Nenhum produto fora do restaurante pode ser retornado.
- Produto não publicado ou indisponível não é sugerido como disponível.
- Preço exibido vem do backend.
- Timeout/falha da IA não bloqueia cardápio.
- Respostas sobre composição correspondem aos ingredientes cadastrados e não inventam componentes.
- Testes incluem prompt injection, saída malformada e IDs inventados.

## Parâmetros aprovados em 13/11/2026

- Modelo único: Gemini Flash-Lite.
- Cotas simultâneas: 20 interações por sessão anônima de navegador/dia e 100 por restaurante/dia.
- Limite de saída do modelo: 500 tokens, configurável. Contexto de catálogo: até 4.000 caracteres. Limite exato de tokens de entrada ainda pendente antes de produção.
- Registrar consumo, tokens disponíveis, latência, erros e custo estimado por restaurante.
- Não persistir conversas, prompts ou respostas completas; manter somente métricas/eventos agregados, conforme D-11.
- O backend aplica as cotas e nunca expõe a chave do Gemini ao frontend.
- A integração real com Gemini exige configuração de chave, faturamento e aprovação dos termos de tratamento de dados; permanece desabilitada por padrão.

Consulte [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md) para as condições completas.
