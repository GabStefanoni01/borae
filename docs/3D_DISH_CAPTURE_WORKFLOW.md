# Experiência 3D de pratos — especificação e plano de desenvolvimento

**Status:** planejamento aprovado para implementação; geração automática ainda não implementada.  
**Branch de documentação:** `docs/3d-dish-capture-workflow`  
**Base:** `main`

## 1. Objetivo do produto

Permitir que um restaurante crie e publique a representação 3D de um prato inteiramente dentro do Boraê.

O restaurante não deve precisar conhecer formatos como GLB, usar um editor 3D, contratar uma ferramenta externa manualmente ou sair do Boraê para gerar e depois enviar um arquivo.

A experiência desejada é:

1. Selecionar um produto já cadastrado no cardápio.
2. Abrir a captura guiada pelo Boraê.
3. Fotografar o prato de diferentes ângulos e/ou gravar um vídeo, conforme o método suportado pelo serviço de geração escolhido.
4. Enviar a captura e acompanhar o processamento dentro do sistema.
5. Visualizar o resultado em 3D, girar e aproximar para conferir.
6. Aprovar e publicar ou refazer a captura.

**Não haverá editor de modelagem 3D no MVP.** A conferência oferece apenas visualização, rotação e zoom. Ajustes técnicos de malha, textura, iluminação, materiais ou geometria ficam fora da responsabilidade do restaurante.

## 2. Experiência do consumidor

- No card do produto do cardápio público, mostrar a ação **“Ver em 3D”** somente quando existir um modelo pronto e publicado.
- Abrir o visualizador sob demanda, sem carregar modelos 3D na listagem inicial.
- Permitir rotação e zoom.
- Exibir a ação **“Ver na minha mesa”** (AR) somente quando houver suporte no dispositivo/navegador.
- Se AR não estiver disponível, manter a visualização 3D.
- Se o modelo não carregar ou houver incompatibilidade, manter foto, descrição e demais informações convencionais do prato.
- Nunca expor modelos em processamento, rejeitados, não aprovados ou não publicados.

## 3. Decisões e princípios

- Reaproveitar o módulo 3D/AR existente na `main`: entidade, persistência, validação GLB, autorização, API pública e visualizador, ajustando contratos onde necessário.
- O GLB continua sendo o formato de entrega do modelo ao visualizador, mas deixa de ser um arquivo que o restaurante precisa produzir e enviar manualmente.
- A geração será feita por um serviço especializado integrado ao backend. O fornecedor ainda deverá ser escolhido após uma prova de conceito com pratos reais; não assumir fornecedor, preço, limite ou qualidade sem avaliação.
- Manter ImageKit como provedor de mídia atual do Boraê, validando tecnicamente o armazenamento e a entrega dos arquivos gerados. Não trocar de provedor sem evidência e decisão explícita.
- A geração deve ser assíncrona. Uma chamada externa demorada não pode manter a requisição HTTP aberta até terminar.
- O backend é a autoridade para autorização, validação, estado do processamento e publicação.
- A captura pode oferecer fotos, vídeo ou ambos. A decisão final do método do MVP depende da prova de conceito do fornecedor: qualidade do modelo, taxa de sucesso, custo, tempo de processamento e compatibilidade móvel.
- O restaurante precisa aprovar o resultado antes de publicar.
- A imagem convencional do produto permanece intacta e é sempre o fallback.

## 4. Backend — instruções de desenvolvimento

Responsável pela implementação: Gabriel.

### 4.1 Auditoria e reaproveitamento

Antes de alterar código:

- Ler `docs/MODULE_3D_AR.md`, `docs/IMPLEMENTATION_3D_AR.md` e `docs/API.md`.
- Revisar `Product3DModel`, `Product3DModelService`, `Product3DModelController`, `GlbFileValidator`, `PublicRestaurantMenuResponse` e `PublicRestaurantMenuService`.
- Reaproveitar a associação de um modelo por produto, o isolamento multi-tenant, as permissões OWNER/MANAGER, o histórico/auditoria e a exposição pública condicionada à publicação.
- Evitar criar uma segunda entidade ou um segundo fluxo de publicação que concorra com o módulo atual. Fazer migration incremental somente quando necessária.

### 4.2 Prova de conceito do serviço de geração

Antes de fechar a integração:

- Comparar serviços/APIs de reconstrução 3D a partir de fotos e/ou vídeo de alimentos.
- Testar com pratos reais variados, incluindo superfícies brilhantes, molhos, itens com pouca textura e apresentações com vários componentes.
- Avaliar qualidade visual, fidelidade, taxa de sucesso, tempo, custo por geração, limites, formatos de entrada/saída, direitos de uso, retenção dos arquivos e condições de tratamento dos dados.
- Confirmar que o resultado pode ser obtido como GLB (ou convertido com processo confiável no backend).
- Documentar fornecedor escolhido, limites e custos estimados antes de habilitar a funcionalidade.
- Não colocar credenciais do fornecedor no Angular nem em código versionado.

### 4.3 Arquitetura de integração

- Criar uma abstração de domínio para geração, por exemplo `Dish3DGenerationProvider`, sem acoplar controllers ou serviços de catálogo ao SDK de um fornecedor.
- Implementar o adaptador do fornecedor escolhido atrás dessa interface.
- Receber arquivos de captura pelo backend em fluxo autenticado e seguro; armazenar temporariamente as entradas em storage apropriado.
- Enviar ao fornecedor referências temporárias/seguras ou os arquivos conforme o contrato real da API.
- Receber o resultado, validar o GLB com as regras existentes e armazená-lo no ImageKit após homologar upload e entrega.
- Persistir identificadores externos, estado, timestamps, tentativas, erros normalizados e referências de entrada/saída necessárias para operação e auditoria.
- Não expor URLs privadas, chaves, tokens ou identificadores sensíveis do fornecedor na API pública.
- Definir política de retenção e remoção dos arquivos brutos de captura; manter somente o necessário para geração, suporte e auditoria.
- Garantir idempotência na criação de jobs e proteção contra duplicidade de cobranças/gerações em retries.

### 4.4 Processamento assíncrono e estados

A geração deve ocorrer por job durável, usando a infraestrutura assíncrona/filas já existente se adequada. Se não houver infraestrutura apropriada, documentar a escolha antes de introduzir uma nova dependência.

Estados funcionais sugeridos:

- `CAPTURE_RECEIVED`: arquivos recebidos.
- `QUEUED`: aguardando processamento.
- `PROCESSING`: fornecedor está gerando o modelo.
- `READY_FOR_REVIEW`: modelo gerado e validado, aguardando aprovação do restaurante.
- `PUBLISHED`: aprovado e publicado.
- `FAILED`: geração ou validação falhou; permite nova tentativa/captura conforme a causa.

Os nomes finais devem seguir as convenções atuais do domínio. Não duplicar estado técnico e estado de publicação sem necessidade.

Requisitos:

- Criar, consultar e cancelar quando suportado um job de geração.
- Consultar o status sem polling excessivo; avaliar polling com intervalo progressivo ou mecanismo já adotado pelo projeto.
- Aplicar timeout, retry limitado e backoff para falhas transitórias.
- Distinguir falha recuperável, captura inadequada, limite do fornecedor e erro permanente.
- Não publicar automaticamente após a geração.
- Preservar o modelo publicado anteriormente até que uma nova geração seja aprovada e publicada.
- Limpar arquivos órfãos e jobs expirados de forma segura.

### 4.5 API e autorização

Definir contratos REST alinhados ao padrão atual da API. Rotas abaixo são ilustrativas e devem ser compatibilizadas com `docs/API.md`:

- `POST /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations` — iniciar geração com arquivos de captura.
- `GET /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations/{generationId}` — consultar status e resultado para revisão.
- `POST /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations/{generationId}/approve` — aprovar/publicar conforme regra de domínio.
- `POST .../{generationId}/retry` — repetir quando permitido, ou iniciar nova captura.
- Manter/ajustar consulta, substituição, publicação e remoção do modelo atual conforme necessário.

Regras:

- Exigir vínculo ativo do usuário ao restaurante e validar que o produto pertence ao restaurante correto.
- Restringir iniciar geração, aprovar, publicar, substituir e remover a OWNER/MANAGER; STAFF não administra modelos 3D.
- Validar tamanho, tipo real, quantidade, duração e resolução dos arquivos de captura conforme limites definidos pela prova de conceito.
- Validar o arquivo de saída como GLB; não confiar apenas em extensão ou Content-Type.
- Aplicar limites de uso por restaurante, concorrência e custo para evitar abuso e cobranças inesperadas.
- Proteger contra acesso cruzado entre restaurantes, replay, upload abusivo e exposição de arquivos privados.
- Não aceitar URL arbitrária fornecida pelo cliente para o backend buscar arquivos (proteção contra SSRF).
- Atualizar Swagger, DTOs e `docs/API.md`.

### 4.6 Testes backend

Cobrir, no mínimo:

- Autorização OWNER/MANAGER e bloqueio de STAFF.
- Isolamento entre restaurantes e vínculo do produto.
- Captura válida/inválida e limites de arquivo.
- Criação idempotente de job e retries.
- Transições válidas e inválidas de estado.
- Falhas/timeout do fornecedor e do storage.
- Validação do GLB retornado.
- Aprovação explícita e preservação do modelo anterior em caso de falha.
- API pública sem modelo, modelo em processamento, aguardando aprovação, publicado e removido.
- Retenção/limpeza de arquivos temporários.
- Limites de uso e proteção contra geração duplicada.

## 5. Frontend Angular — instruções de desenvolvimento

Responsável pela implementação frontend: **Renan (@renancristianobn-del)**. Renan é responsável pelo desenvolvimento do frontend, **não está sendo solicitado como reviewer desta PR**.

### 5.1 Entrada pelo produto existente

- Integrar a funcionalidade ao editor/detalhe do produto já existente no painel.
- Não criar um cadastro separado de pratos nem uma tela que exija conhecimento técnico.
- Exibir uma seção simples: **“Experiência 3D do prato”**.
- Mostrar o estado atual: não configurado, captura em andamento, processando, pronto para conferir, publicado ou falhou.
- Respeitar as permissões retornadas pelo backend; esconder ações administrativas para quem não pode gerenciar.

### 5.2 Captura guiada

- Criar uma experiência mobile-first que abra a câmera do dispositivo dentro do Boraê.
- Orientar visualmente a pessoa a posicionar o prato, manter iluminação adequada, evitar movimento e capturar ângulos suficientes.
- Mostrar exemplos/indicações de ângulos e progresso da captura.
- Implementar o método de captura definido após a prova de conceito: sequência de fotos, vídeo ou ambos.
- Solicitar permissões de câmera de forma contextual e explicar como continuar se forem negadas.
- Permitir revisar e descartar a captura antes do envio.
- Validar no cliente apenas para dar feedback rápido; o backend continua responsável pela validação final.
- Não exigir que o usuário baixe, converta ou envie manualmente um arquivo GLB.

### 5.3 Acompanhamento e aprovação

- Enviar a captura ao backend e apresentar progresso/status de processamento.
- Permitir sair da tela e voltar depois sem perder o estado do job.
- Tratar falha, timeout, cancelamento e possibilidade de refazer a captura com mensagens claras e sem códigos técnicos.
- Quando o modelo estiver pronto, abrir uma prévia 3D com rotação e zoom.
- Oferecer somente **“Aprovar e publicar”** ou **“Refazer captura”**.
- Não implementar editor 3D, controles de malha, textura, materiais, iluminação, escala técnica ou edição de geometria.
- Se uma nova geração substituir um modelo já publicado, deixar claro que a substituição só ocorre após aprovação; preservar o modelo atual até lá.

### 5.4 Cardápio público

- Reaproveitar o visualizador 3D existente quando compatível com o contrato atualizado.
- Mostrar **“Ver em 3D”** somente para modelo publicado e elegível.
- Carregar o modelo sob demanda e exibir estado de carregamento.
- Exibir **“Ver na minha mesa”** somente quando o dispositivo/browser suportar o modo AR correspondente.
- Manter a experiência 3D quando AR não for suportado.
- Manter a foto convencional quando o modelo falhar, estiver indisponível ou não for compatível.
- Não carregar arquivos 3D na listagem inicial de produtos.

### 5.5 Testes frontend

Cobrir:

- Permissão/negação de câmera e ausência de câmera.
- Captura, revisão, descarte e envio.
- Estados de processamento, retorno à tela e atualização de status.
- Erros, retries e nova captura.
- Prévia e aprovação.
- Produto sem modelo, modelo em processamento e modelo publicado.
- AR compatível/incompatível e fallback de imagem.
- Responsividade, acessibilidade, teclado e leitores de tela.
- Garantir que nenhuma chave privada do fornecedor/storage esteja no bundle Angular.

## 6. Critérios de aceite ponta a ponta

- [ ] O restaurante inicia a criação 3D a partir de um produto existente.
- [ ] Captura de fotos/vídeo é guiada e ocorre dentro do Boraê, conforme método aprovado na prova de conceito.
- [ ] O restaurante não precisa criar, editar, converter ou enviar GLB manualmente.
- [ ] A geração ocorre de forma assíncrona e seu status pode ser consultado.
- [ ] O resultado passa por validação antes de ser apresentado para aprovação.
- [ ] O restaurante consegue girar/aproximar o modelo e aprovar ou refazer a captura.
- [ ] Não existe editor de modelagem 3D no fluxo.
- [ ] O modelo só é publicado após aprovação explícita.
- [ ] O modelo antigo permanece disponível até a nova versão ser aprovada.
- [ ] O cardápio oferece “Ver em 3D” apenas para modelos publicados.
- [ ] AR é progressiva e condicionada à compatibilidade do dispositivo.
- [ ] A foto convencional continua sendo fallback.
- [ ] ImageKit e o fornecedor de geração foram homologados com arquivos reais.
- [ ] Custos, limites, retenção e proteção contra abuso estão documentados.
- [ ] Testes backend, frontend e integração ponta a ponta passam.

## 7. Fora do escopo do MVP

- Editor ou ferramenta de modelagem 3D.
- Ajuste manual de malha, textura, material, iluminação ou geometria.
- Geração manual fora do Boraê e upload de GLB pelo restaurante.
- Promessa de escala física perfeita ou fidelidade absoluta.
- Pedidos, carrinho, checkout, pagamento ou integração com PDV/ERP.
- Geração ilimitada sem controle de custo e uso.

## 8. Ordem sugerida de execução

1. Prova de conceito e seleção do serviço de geração com pratos reais.
2. Definição de limites, custos, retenção e contrato de integração.
3. Backend: abstração do fornecedor, armazenamento de captura, jobs, estados, validação, API e testes.
4. Frontend: captura guiada, acompanhamento, prévia simples e aprovação.
5. Integração do resultado com o visualizador público 3D/AR já existente.
6. Homologação em dispositivos reais, testes de carga/custo e liberação gradual.

## 9. Regras de colaboração e entrega

- Backend e frontend devem trabalhar em branches isoladas e integrar por Pull Request.
- O contrato da API deve ser acordado antes da integração final.
- Não fazer merge automático; aprovação humana é obrigatória.
- Não alterar diretamente `main`.
- Registrar decisões do fornecedor e mudanças de contrato nesta documentação e em `docs/API.md`.


## 10. Contrato de API proposto — alinhamento entre backend e frontend

> Este contrato é uma proposta inicial para orientar as duas implementações. Os caminhos e nomes finais devem ser conferidos com os padrões já existentes em `docs/API.md` e com os DTOs reais antes de codificar. A semântica abaixo é o comportamento esperado; não acoplar o contrato ao fornecedor de geração.

### 10.1 Iniciar uma geração

`POST /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations`

- Autenticação obrigatória; somente OWNER/MANAGER.
- `multipart/form-data` para permitir envio de mídia sem base64.
- Campos propostos:
  - `captureMethod`: `PHOTOS` ou `VIDEO`, conforme método liberado no MVP.
  - `files`: uma ou mais imagens ou um vídeo, conforme `captureMethod`.
  - `idempotencyKey`: identificador único da tentativa, se o padrão de idempotência do backend permitir.
- O backend valida vínculo do produto, quantidade/tamanho/formato e limites definidos na prova de conceito.
- Resposta esperada: HTTP 202 Accepted, com `generationId`, `productId`, `status`, `createdAt` e `statusUrl`.
- Não retornar credenciais, URL privada permanente ou identificador sensível do fornecedor.

Exemplo ilustrativo de resposta:

```json
{
  "generationId": "uuid",
  "productId": "uuid",
  "status": "QUEUED",
  "createdAt": "2026-11-17T12:00:00Z",
  "statusUrl": "/api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations/{generationId}"
}
```

### 10.2 Consultar o processamento

`GET /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations/{generationId}`

- Autenticação obrigatória; OWNER/MANAGER do restaurante.
- Resposta inclui `generationId`, `status`, `progress` quando o fornecedor oferecer progresso confiável, `createdAt`, `updatedAt`, `failureCode` e `failureMessage` amigável quando houver falha.
- Quando `status=READY_FOR_REVIEW`, incluir um objeto `preview` com URL temporária/segura do modelo, formato e metadados necessários ao visualizador administrativo.
- Não inventar percentual de progresso quando o fornecedor não fornecer essa informação; nesse caso retornar progresso ausente e um estado textual.
- A consulta deve permitir que o frontend recupere o estado ao reabrir a tela, sem depender de estado apenas em memória.

### 10.3 Aprovar e publicar

`POST /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations/{generationId}/approve`

- Autenticação obrigatória; somente OWNER/MANAGER.
- O backend confirma que o job pertence ao restaurante/produto, está em `READY_FOR_REVIEW` e que o arquivo final foi validado e armazenado.
- A aprovação promove o modelo gerado para o modelo ativo/publicado do produto usando o domínio existente de `Product3DModel`.
- A troca deve ser transacional do ponto de vista do catálogo: o modelo anterior continua ativo até a aprovação válida; não deixar o produto sem modelo se a nova publicação falhar.
- Resposta retorna o estado publicado e os metadados públicos/administrativos necessários. O frontend não deve publicar por conta própria alterando campos locais.

### 10.4 Cancelar ou refazer

`POST /api/v1/restaurants/{restaurantId}/products/{productId}/model-3d/generations/{generationId}/cancel`

- Disponibilizar somente se o fornecedor e o worker permitirem cancelamento real; caso contrário, a API deve explicar que o job não pode ser interrompido e permitir descartar o resultado quando terminar.
- Refazer captura significa iniciar uma nova geração, não reutilizar silenciosamente arquivos anteriores.
- Repetição técnica de uma mesma chamada deve respeitar idempotência e não gerar cobranças duplicadas.

### 10.5 Erros HTTP esperados

- `400`: captura ausente, inválida ou fora dos limites.
- `401`: sessão/autenticação ausente ou inválida.
- `403`: papel sem permissão ou acesso a outro restaurante.
- `404`: restaurante, produto ou geração não encontrado no escopo autorizado.
- `409`: transição de estado inválida, geração concorrente não permitida ou produto já em operação incompatível.
- `413`: mídia acima do limite.
- `415`: formato de mídia não suportado.
- `429`: limite de geração/concorrência atingido.
- `502/503`: fornecedor/storage indisponível, com erro normalizado e sem vazar detalhes internos.

### 10.6 Integração com o cardápio público

- Reaproveitar `GET /api/v1/public/restaurants/{slug}/menu` e o campo `model3d` existente no DTO público, se o contrato atual já atender.
- O backend só inclui `model3d` quando o modelo está aprovado/publicado e o restaurante, cardápio, categoria e produto estão elegíveis.
- O payload público deve conter apenas URL de entrega do asset e metadados realmente necessários ao visualizador/AR, nunca status interno de geração, dados de captura, fornecedor, custo ou identificadores privados.
- Se o contrato atual precisar mudar, documentar compatibilidade e atualizar Swagger/API antes de o frontend depender dos novos campos.

### 10.7 Responsabilidade do frontend sobre o contrato

Renan deverá consumir somente os endpoints e DTOs documentados pelo backend. O Angular não deve chamar diretamente o fornecedor de geração, ImageKit com credenciais privadas, nem tomar decisões de autorização/publicação. Os nomes dos services, interfaces e componentes devem seguir a estrutura atual do projeto depois de uma breve auditoria do código existente.

## 11. Componentes e entregáveis esperados

### Backend — entregáveis

- Adaptador do serviço de geração e configuração segura das credenciais.
- Serviço de aplicação responsável por iniciar e acompanhar gerações.
- Persistência de job/estado e migration incremental, caso a estrutura atual não comporte o fluxo.
- Worker/consumer assíncrono durável com retry, timeout, idempotência e limpeza.
- Recepção e retenção temporária da captura.
- Validação do arquivo GLB gerado e armazenamento final no ImageKit homologado.
- Endpoints autenticados para criar, consultar, aprovar e, se suportado, cancelar geração.
- Integração da aprovação com o modelo 3D existente e com o cardápio público.
- DTOs, Swagger, documentação de API, logs sem dados sensíveis, métricas de custo/uso e testes.

### Frontend — entregáveis

- Seção “Experiência 3D do prato” no cadastro/edição do produto existente.
- Fluxo de captura guiada e responsiva, com câmera, instruções, revisão e envio.
- Service Angular tipado para iniciar e consultar gerações, aprovar e cancelar quando disponível.
- Tela/estado de acompanhamento que pode ser reaberto sem perder o job.
- Prévia simples com visualizador 3D existente, rotação e zoom.
- Ações “Aprovar e publicar” e “Refazer captura”; sem editor de modelagem.
- Atualização do cardápio público para exibir “Ver em 3D” e AR conforme os dados e capacidades suportados.
- Estados de carregamento, vazio, falha, sucesso, permissão negada, ausência de câmera e fallback de imagem.
- Testes unitários/componentes e testes de integração dos fluxos principais.

## 12. Decisões que precisam ser fechadas antes da implementação funcional

As decisões abaixo não devem ser inventadas pelo desenvolvimento. Devem ser registradas na PR ou em decisão técnica antes de habilitar a funcionalidade:

1. **Fornecedor de geração 3D:** selecionar após prova de conceito com pratos reais, considerando custo, qualidade, tempo, direitos de uso, retenção e suporte a GLB.
2. **Método de captura do MVP:** fotos guiadas, vídeo guiado ou ambos. Depende dos requisitos do fornecedor e da qualidade real do resultado.
3. **Limites de captura e geração:** quantidade/resolução/duração/tamanho, gerações simultâneas e cotas por restaurante. Definir a partir dos limites do fornecedor e orçamento operacional.
4. **Custeio e cobrança:** confirmar se a geração será incluída no plano, terá cota gratuita ou será cobrada separadamente. Não habilitar geração ilimitada sem decisão de produto.
5. **Retenção de capturas originais:** definir por quanto tempo fotos/vídeos serão mantidos e quando serão excluídos após sucesso/falha.
6. **Fila e execução assíncrona:** confirmar se a infraestrutura atual do Boraê atende a durabilidade, retries e observabilidade necessárias; se não, escolher uma solução antes de implementar.
7. **Publicação e substituição:** confirmar que a aprovação do novo modelo substitui o anterior imediatamente, mantendo o anterior até a aprovação — recomendação técnica deste plano.
8. **AR e calibração física:** confirmar se o MVP exige posicionamento em escala aproximada ou se a primeira versão oferece AR sem promessa de dimensão real. Não solicitar dimensões técnicas ao restaurante sem necessidade comprovada.

**Recomendação inicial:** começar com uma prova de conceito de fotos guiadas e comparar com vídeo apenas se o fornecedor escolhido demonstrar vantagem. Manter aprovação explícita, cotas por restaurante e exclusão programada dos arquivos brutos após o período operacional definido.


## 13. Estratégia de geração própria com custo próximo de zero

### 13.1 Diretriz de produto e custo

A prioridade do Boraê é não depender de uma API comercial paga por geração. A solução deve ser desenvolvida com modelos open source/self-hosted sempre que a licença permitir uso comercial, mantendo o custo operacional inicial o mais próximo possível de zero.

Isso não significa prometer geração ilimitada e gratuita em produção. Inferência 3D exige GPU, memória, armazenamento e disponibilidade. Cotas gratuitas de GPU são compartilhadas, limitadas, sujeitas a filas e podem mudar. Portanto:

- Não assumir API comercial paga como dependência obrigatória do produto.
- Não habilitar geração ilimitada.
- Não apresentar cotas gratuitas de terceiros como SLA de produção.
- Medir custo, duração, taxa de sucesso, consumo de GPU e qualidade antes de liberar para restaurantes.
- Manter o serviço de geração desacoplado para trocar de modelo ou infraestrutura sem alterar o domínio do Boraê.
- Se a infraestrutura gratuita não suportar a demanda, limitar/colocar gerações em fila e só propor custo pago mediante decisão explícita de produto.

### 13.2 Arquitetura proposta: Render como API/orquestrador e GPU externa

O Render pode hospedar o serviço leve de coordenação em Python/FastAPI, mas não deve executar os modelos de geração 3D no plano gratuito.

Responsabilidades propostas:

**Render — FastAPI/orquestração**
- Receber chamadas autenticadas do backend Spring Boot ou atuar como worker/orquestrador interno.
- Validar contrato e metadados da tarefa; não duplicar autorização de negócio do backend Java.
- Criar/consultar tarefas, persistir ou delegar o estado ao mecanismo durável definido pelo Boraê.
- Encaminhar a inferência ao worker de GPU.
- Normalizar estados, erros e metadados do resultado.
- Não armazenar arquivos temporários no filesystem efêmero como se fossem persistentes.
- Não carregar modelos de IA pesados no serviço web gratuito.

**Hugging Face Spaces/ZeroGPU — execução experimental**
- Hospedar um Space com o modelo open source selecionado e executar inferência em GPU compartilhada, quando houver cota disponível.
- Expor uma interface de inferência autenticada ao orquestrador, sem colocar tokens no Angular.
- Tratar fila, limite diário, indisponibilidade, timeout e resposta 429/5xx.
- O ZeroGPU é adequado para prova de conceito e experimentação; não é garantia de disponibilidade, latência ou capacidade contínua para produção.
- Confirmar as regras atuais de uso, cota, elegibilidade, licença do modelo e condições comerciais antes de qualquer liberação pública.

**ImageKit — armazenamento de mídia**
- Continuar como destino do GLB final após validar upload, MIME, tamanho, entrega e compatibilidade com o visualizador.
- Capturas originais devem ficar em armazenamento temporário apropriado, com política de retenção e exclusão.
- Não presumir que o armazenamento temporário ou persistência de jobs esteja coberto pelo filesystem do Render Free.

**Google model-viewer — apresentação 3D/AR**
- Usar a biblioteca open source model-viewer no frontend para carregar o GLB aprovado, oferecer visualização 3D e solicitar AR quando o navegador/dispositivo suportar.
- model-viewer não gera modelos 3D: ele somente apresenta assets 3D já existentes.
- Não requer API paga de geração para a camada de visualização; continuam existindo custos de entrega/armazenamento do GLB e requisitos de compatibilidade de dispositivo.

Fluxo lógico:

1. Angular captura as fotos guiadas e envia ao backend Spring Boot.
2. Spring Boot autentica, autoriza, valida produto/restaurante, cria geração durável e registra a tarefa.
3. Spring Boot/worker chama o serviço FastAPI hospedado no Render.
4. FastAPI encaminha a inferência para o Space/worker GPU do Hugging Face.
5. O worker executa o modelo e retorna um GLB ou artefato que possa ser convertido de forma reproduzível para GLB.
6. O backend valida o arquivo com GlbFileValidator e limites adicionais; armazena o GLB final no ImageKit.
7. O restaurante confere a prévia e aprova explicitamente.
8. O cardápio público expõe apenas o modelo publicado; model-viewer oferece 3D e AR compatível.

A comunicação entre componentes deve ser autenticada, limitada por timeout e protegida contra repetição. O identificador interno da geração pertence ao Boraê; identificadores externos não devem ser expostos na API pública.

### 13.3 Limitações do Render Free

O Render Free é adequado para um serviço HTTP leve de coordenação, não para inferência 3D pesada. O plano gratuito tem recursos computacionais limitados, pode suspender serviços inativos e não oferece GPU dedicada. Portanto:

- Não executar Stable Fast 3D, TripoSR ou TRELLIS.2 diretamente no Render Free.
- Não assumir que 512 MB de RAM e CPU compartilhada sejam suficientes para carregar modelos 3D de IA.
- Usar Render apenas para API/orquestração, se os limites atuais do plano forem compatíveis com o volume de testes.
- Verificar novamente limites, suspensão, disco e condições do plano no momento da implantação, pois podem mudar.
- Jobs duráveis não podem depender apenas do processo web gratuito ou de memória local.

### 13.4 Modelos open source candidatos

A prova de conceito deve comparar ao menos:

**TripoSR**
- Candidato inicial por ser relativamente leve para inferência em GPU comparado a modelos maiores e possuir licença MIT no repositório.
- Entrada principal: uma imagem.
- Avaliar qualidade de alimentos, reconstrução de partes ocultas, textura, geometria, GLB exportado e necessidade de pós-processamento.
- Não presumir que uma imagem seja suficiente para fidelidade de prato; testar captura multiângulo e definir como as vistas adicionais serão utilizadas pelo pipeline.

**Stable Fast 3D**
- Candidato para reconstrução a partir de imagem única, com pipeline de geometria e textura.
- Conferir cuidadosamente a licença e os termos de uso comercial do código e dos pesos antes de incluir em produto comercial.
- Validar requisitos de GPU e qualidade em alimentos reais.

**TRELLIS.2**
- Candidato experimental de maior exigência computacional, com suporte a representação 3D e materiais.
- Requer GPU com memória elevada; não é candidato para Render Free ou execução gratuita garantida.
- Avaliar somente se houver acesso temporário a GPU compatível e se os resultados justificarem a complexidade.

**COLMAP/fotogrametria**
- Alternativa complementar baseada em múltiplas fotografias sobrepostas, não em geração generativa de uma única imagem.
- Pode reconstruir geometria a partir de imagens com sobreposição e textura suficiente, mas pratos podem ser difíceis por superfícies brilhantes, ingredientes deformáveis, oclusão, reflexos e falta de textura.
- Avaliar em paralelo com IA, sem assumir que fotogrametria produzirá automaticamente um modelo pronto para AR.

As licenças devem ser verificadas nos repositórios e nos arquivos de pesos específicos, na versão efetivamente utilizada. Licença do código não garante automaticamente que os pesos tenham os mesmos termos.

### 13.5 Prova de conceito obrigatória

Antes de escolher o modelo e implementar integração definitiva, executar um benchmark reproduzível com pratos reais e autorização para uso das imagens.

Conjunto mínimo sugerido:
- Hambúrguer/sanduíche com camadas.
- Massa ou prato com molho brilhante.
- Prato feito com componentes distintos.
- Um prato com elementos de pouca textura ou superfícies reflexivas.

Comparar:
- Uma foto versus sequência de fotos guiadas.
- TripoSR versus Stable Fast 3D; incluir COLMAP como baseline multi-view se viável.
- TRELLIS.2 somente se houver GPU adequada disponível sem custo relevante para o experimento.

Registrar por execução:
- Modelo/versão, licença e configuração.
- Quantidade, resolução e ângulos das imagens.
- GPU utilizada, VRAM, tempo de inferência e falhas.
- Fidelidade visual avaliada por pessoas, especialmente ingredientes, formato e apresentação.
- Geometria, textura, materiais e partes inventadas/ausentes.
- Validade do GLB, tamanho, número de polígonos, tamanho de texturas e desempenho em celular.
- Resultado no model-viewer e compatibilidade de AR em dispositivos reais.
- Custo efetivo e limites da cota gratuita.

Critérios de aprovação do PoC:
- Resultado reconhecível e suficientemente fiel ao prato fotografado.
- GLB validado e com desempenho aceitável em celular.
- Processo repetível sem intervenção de modelagem manual.
- Licença compatível com uso comercial do Boraê.
- Custo e capacidade dentro de limites explicitamente aprovados.
- Falhas e baixa qualidade devem ser comunicadas ao restaurante; não publicar automaticamente.

### 13.6 Segurança, operação e limites

- O frontend nunca chama diretamente Hugging Face nem recebe token privado.
- Tokens e secrets ficam no backend/orquestrador.
- Limitar tamanho, resolução e quantidade de imagens.
- Aplicar cotas por restaurante, concorrência máxima e fila.
- Usar timeout, retry limitado, backoff e idempotência.
- Não repetir inferência automaticamente em erros permanentes ou sem controlar consumo de cota.
- Não registrar imagens de pratos, URLs privadas ou tokens em logs.
- Definir retenção e exclusão de imagens originais.
- Persistir jobs e estados em armazenamento durável; não confiar em memória do Render ou no filesystem efêmero.
- Implementar fallback: se a geração estiver indisponível, o produto continua com a foto convencional e o módulo 3D não fica em estado inconsistente.
- Manter o modelo 3D publicado anteriormente até aprovação de nova geração.

### 13.7 Decisão técnica proposta para a primeira prova de conceito

- Serviço de orquestração: Python/FastAPI no Render Free, apenas para coordenação leve, sujeito à validação dos limites atuais.
- GPU experimental: Hugging Face Spaces/ZeroGPU, sem promessa de SLA ou geração ilimitada.
- Primeiro modelo: TripoSR, comparado com Stable Fast 3D após checagem de licença e requisitos.
- Baseline alternativo: COLMAP para sequência de fotos, caso a captura multiângulo seja viável.
- Formato de saída: GLB validado pelo backend.
- Storage final: ImageKit, após homologação.
- Visualização e AR: Google model-viewer no frontend.
- Backend Java/Spring Boot permanece como autoridade de negócio, autorização, jobs e publicação.
- Nenhum fornecedor comercial pago será dependência obrigatória do MVP.
- Esta arquitetura é uma hipótese de PoC, não uma decisão de produção. A escolha final depende dos resultados de qualidade, custo, licença e disponibilidade.



## 13. Estratégia de geração própria com infraestrutura de baixo custo

### Objetivo e princípio

A prioridade do Boraê é evitar dependência obrigatória de APIs comerciais cobradas por geração. Antes de contratar Tripo, Meshy ou outro fornecedor, será validada uma solução própria baseada em modelos open source e serviços de hospedagem com cotas gratuitas.

**Importante:** modelo open source não significa processamento ilimitado e gratuito. A execução de inferência 3D exige GPU, e hospedagens gratuitas possuem cotas, filas, suspensão, limites de memória e disponibilidade variável. Portanto, custo zero é uma meta para a prova de conceito, não uma garantia de operação comercial ilimitada.

### Arquitetura proposta para a prova de conceito

Separar a API de orquestração do processamento pesado:

1. **Angular — captura:** o restaurante seleciona um produto existente e faz a captura guiada dentro do Boraê. A interface envia fotos para o backend; não chama diretamente serviços de GPU.
2. **Spring Boot — domínio e segurança:** autentica o usuário, valida OWNER/MANAGER, restaurante e produto, aplica limites, cria/persiste a geração e acompanha seu estado. O backend continua sendo a autoridade de publicação.
3. **Render Free — serviço Python/FastAPI de orquestração (opcional):** hospeda uma API leve para receber/encaminhar tarefas e consultar resultados. Não executar o modelo 3D pesado no Render Free.
4. **Hugging Face Spaces com ZeroGPU — worker experimental:** hospeda um Space que executa o modelo open source em GPU compartilhada. A integração deve tratar fila, cotas por usuário, indisponibilidade, cold start, timeout e falhas. Não assumir SLA ou capacidade ilimitada.
5. **Validação e armazenamento:** o resultado GLB retorna ao backend, passa pelo GlbFileValidator e pelas verificações de tamanho/segurança/desempenho; depois é armazenado no ImageKit, cuja entrega de GLB precisa ser homologada.
6. **Publicação e AR:** após aprovação explícita, o modelo é associado ao Product3DModel existente. O cardápio público reutiliza o visualizador atual e o Google model-viewer para 3D/AR quando compatível.

### Restrições do Render Free

O Render Free pode ser usado para uma API leve de orquestração, mas não para inferência 3D pesada:

- Não oferece GPU dedicada no plano gratuito.
- Possui recursos limitados de CPU/RAM (512 MB RAM no serviço web gratuito, conforme documentação vigente consultada durante o planejamento).
- Serviços podem entrar em suspensão após inatividade e apresentar cold start.
- O sistema de arquivos local é efêmero; não usar disco local como armazenamento durável de capturas, jobs ou GLBs.
- Não assumir disponibilidade contínua nem usar o plano gratuito como garantia de produção.

As capturas e os resultados devem ficar em storage apropriado (ImageKit ou armazenamento temporário compatível), e os estados/jobs devem permanecer persistidos no backend/banco. Confirmar os limites e termos atuais do Render antes de configurar o serviço.

### Modelos open source candidatos

A prova de conceito deve comparar pelo menos:

- **TripoSR:** candidato inicial por ser relativamente leve e ter licença MIT. A entrada principal é uma imagem; não presumir que múltiplas fotos sejam combinadas nativamente sem uma etapa adicional. Avaliar fidelidade de alimentos, geometria, textura, licença dos pesos e dependências.
- **Stable Fast 3D (SF3D):** candidato para geração a partir de imagem e saída 3D texturizada. Verificar os termos da Stability AI Community License e a elegibilidade de uso comercial do Boraê antes de qualquer distribuição/uso em produção; não tratar como MIT ou licença comercial irrestrita.
- **TRELLIS.2:** alternativa exploratória de maior exigência computacional; não é o primeiro alvo para hospedagem gratuita. Validar requisitos de GPU, memória e licença da versão/pesos usados.

A seleção não será feita apenas por benchmarks ou demonstrações genéricas. Modelos devem ser avaliados com fotografias reais de pratos. A geração de uma única imagem pode inferir partes ocultas e não garante fidelidade fotográfica, escala física ou reconstrução exata do prato.

### Prova de conceito obrigatória

Executar o mesmo conjunto de pratos nos candidatos selecionados, registrando:

- Um prato com geometria volumosa (ex.: hambúrguer).
- Um prato com molho/superfície brilhante (ex.: massa).
- Um prato com múltiplos componentes (ex.: prato feito).
- Qualidade e fidelidade visual em comparação com as fotos originais.
- Integridade do GLB, textura/material, tamanho final e desempenho em celular.
- Tempo total, tempo em fila, falhas, limite de GPU e número de tentativas.
- Custo monetário efetivo e cota gratuita consumida.
- Licença do código, dos pesos e permissões de uso comercial.
- Compatibilidade com o visualizador e AR existentes.

Aprovar a prova de conceito somente se o resultado for visualmente aceitável para representar o prato real, produzir GLB validável e funcionar no fluxo móvel. Se nenhum candidato passar, registrar a limitação em vez de publicar modelos enganosos.

### Separação de responsabilidades e desacoplamento

- Criar uma interface de provedor/worker, por exemplo `Dish3DGenerationProvider`, e um adaptador para o Space experimental, sem acoplar o domínio a Hugging Face ou Render.
- O Spring Boot persiste o job e controla autorização, cotas, tentativas, estado e aprovação.
- O serviço FastAPI/Space é stateless sempre que possível; não é fonte de verdade do estado do produto.
- Usar identificadores de tarefa e callbacks/polling autenticados. Não expor tokens do Hugging Face, ImageKit ou Render no Angular.
- Não manter arquivos temporários somente no disco efêmero do Render/Space. Usar URLs temporárias/assinadas ou upload controlado para storage.
- Tratar o ZeroGPU como ambiente experimental sujeito a fila/cota; não prometer geração ilimitada nem disponibilidade para restaurantes em produção.
- Prever substituição do worker por GPU própria ou provedor de inferência pago somente mediante decisão explícita e análise de custo.

### Google model-viewer e escopo AR

O Google model-viewer é a camada de visualização, não um gerador de modelos. Ele pode exibir um GLB já pronto e iniciar experiências AR compatíveis com o dispositivo/navegador. Não converte fotografias em GLB. Reaproveitar o visualizador atual e confirmar requisitos de GLB, Android Scene Viewer e iOS Quick Look no teste em dispositivos reais.

### Critérios de aceite adicionais

- [ ] API leve de orquestração pode ser executada no Render Free sem executar inferência pesada nele.
- [ ] Worker experimental em Hugging Face Spaces/ZeroGPU consegue receber uma tarefa autorizada e devolver GLB, respeitando limites e falhas.
- [ ] Nenhuma captura, geração ou GLB depende de armazenamento local efêmero para persistência.
- [ ] Backend Spring Boot permanece fonte de verdade para jobs, cotas, autorização e publicação.
- [ ] Pelo menos três pratos reais são avaliados e os resultados documentados.
- [ ] Licenças do código e dos pesos são verificadas para uso comercial.
- [ ] Falhas, fila, quota excedida e indisponibilidade do worker são apresentadas de forma amigável.
- [ ] A integração permanece substituível sem alterar o domínio Product3DModel ou o contrato público do cardápio.
- [ ] Nenhuma promessa de custo zero/escala ilimitada é feita com base em cotas gratuitas.

### Decisão provisória

**Direção escolhida para a prova de conceito:** Render Free para orquestração leve (se necessário) + Hugging Face Spaces/ZeroGPU para inferência experimental + modelo open source (TripoSR primeiro; SF3D em comparação) + ImageKit para assets + Google model-viewer para apresentação 3D/AR.

Essa direção não é aprovação para produção. A escolha final depende dos testes com pratos reais, dos termos/licenças atuais e da confirmação de que as cotas gratuitas atendem ao volume pretendido.

## 14. Serviço próprio `borae-3d-engine`: desenho interno da PoC

O objetivo não é treinar um modelo fundacional do zero nem criar uma API de geração baseada em regras. O Boraê desenvolverá seu próprio serviço/pipeline de geração, combinando modelos open source existentes, pré-processamento, pós-processamento, validação e integração com o domínio atual. Assim, o produto controla a experiência e pode trocar o modelo de inferência sem acoplar o backend Java a um fornecedor específico.

### 14.1 Responsabilidade e fronteiras

- Repositório/serviço lógico: `borae-3d-engine`, implementado em Python.
- API de orquestração: FastAPI hospedado inicialmente no Render Free, somente para coordenação leve; não carregar pesos nem executar inferência 3D pesada no Render.
- Runner de inferência: processo/Space separado com GPU compartilhada no Hugging Face ZeroGPU durante a PoC. A integração deve ser substituível por worker GPU próprio ou outro host no futuro.
- Backend Spring Boot continua sendo o sistema de registro e autoridade: autenticação, autorização, tenant, produto, cotas, geração durável, aprovação, publicação e auditoria.
- O serviço Python não decide quem pode publicar um modelo e não escreve diretamente em `Product3DModel`.
- O serviço Python devolve um artefato e metadados técnicos; o backend Java valida novamente e controla armazenamento/publicação.

### 14.2 Contrato interno sugerido

Contrato ilustrativo, a compatibilizar com a implementação e com autenticação serviço-a-serviço:

- `POST /internal/v1/generations`: recebe `generationId`, referência temporária/assinada às imagens, método de captura, modelo/versão solicitados e parâmetros permitidos; retorna `202` e `engineTaskId`.
- `GET /internal/v1/generations/{engineTaskId}`: retorna estado normalizado (`QUEUED`, `PROCESSING`, `SUCCEEDED`, `FAILED`), timestamps, código de falha e metadados; progresso percentual somente se mensurável.
- `GET /internal/v1/generations/{engineTaskId}/artifact`: disponibiliza o resultado por canal autenticado/URL temporária, se a arquitetura de entrega exigir endpoint separado.
- Callback/webhook opcional somente se necessário; deve validar assinatura/autenticidade, tolerar repetição e não ser a única fonte de estado.
- Toda chamada deve usar credencial serviço-a-serviço, timeout, limite de payload e correlação por `generationId`. Nenhum token do Hugging Face ou segredo interno pode chegar ao Angular.

O contrato final deve evitar transferir arquivos grandes em JSON/base64. Preferir upload/transferência binária ou URLs temporárias com escopo e expiração definidos. Não aceitar URL arbitrária do usuário para o serviço buscar.

### 14.3 Pipeline de geração

1. Validar quantidade, MIME real, resolução, tamanho e integridade das imagens recebidas.
2. Normalizar orientação, dimensões e formato; preservar originais apenas pelo período de retenção aprovado.
3. Aplicar validações de qualidade da captura (nitidez, exposição, enquadramento e cobertura de ângulos) e solicitar nova captura quando insuficiente, se tecnicamente viável.
4. Selecionar estratégia de inferência por adaptador: imagem única (TripoSR/Stable Fast 3D) ou reconstrução multi-view (COLMAP e alternativas que comprovadamente consumam as várias vistas).
5. Executar inferência no runner GPU e capturar versão do modelo, parâmetros, consumo e duração.
6. Converter/exportar para GLB de maneira reproduzível; não assumir que toda saída de modelo já será GLB pronto.
7. Otimizar geometria e texturas com limites configuráveis; não degradar a aparência sem registrar a transformação.
8. Validar GLB, limites de polígonos/textura/tamanho e compatibilidade com o visualizador atual.
9. Retornar artefato e metadados ao backend Java para validação final, armazenamento no ImageKit e aprovação humana.

### 14.4 Estratégia de modelos e decisão

- Começar por TripoSR como baseline leve e simples de experimentar.
- Comparar Stable Fast 3D somente após validar separadamente os termos da licença do código e dos pesos para uso comercial.
- Tratar TRELLIS.2 como experimento posterior, condicionado a acesso a GPU de memória suficiente; não assumir que ZeroGPU fornecerá sempre a GPU necessária.
- Usar COLMAP como baseline de fotogrametria multi-view, sabendo que alimentos brilhantes, deformáveis, repetitivos ou parcialmente oclusos podem impedir reconstrução confiável.
- Não assumir que modelos de imagem única aproveitam automaticamente todas as fotos. A prova de conceito deve provar o suporte multi-view ou escolher um pipeline específico para isso.

### 14.5 Limites operacionais da PoC

- Render Free pode suspender por inatividade e não oferece GPU dedicada; o serviço deve tolerar cold start e indisponibilidade.
- ZeroGPU tem cotas/filas compartilhadas e não é SLA. Tratar `429`, timeout e indisponibilidade sem loop de retry que consuma a cota.
- Jobs duráveis, estado e referências de arquivos devem ficar em componentes persistentes já aprovados; nunca depender da memória do processo ou disco efêmero do Render.
- Limitar gerações por restaurante, concorrência global, tamanho de captura e tentativas.
- Se o runner gratuito estiver indisponível, deixar job em fila/estado recuperável ou informar indisponibilidade; não trocar silenciosamente para API paga.
- Nenhum fallback pago deve ser acionado sem decisão explícita do produto.

### 14.6 Entrega incremental

**PoC 1 — prova de geração:** serviço mínimo FastAPI + runner ZeroGPU + um modelo; gerar um GLB a partir de uma imagem de prato real autorizada e medir qualidade/tempo/VRAM.

**PoC 2 — comparação:** testar TripoSR, Stable Fast 3D (se licença permitir) e COLMAP com o mesmo conjunto de pratos; documentar diferenças entre entrada de imagem única e multi-view.

**PoC 3 — integração:** conectar Spring Boot ao serviço interno, persistir estado, validar artefato e armazenar no ImageKit; ainda sem expor geração a todos os restaurantes.

**MVP controlado:** habilitar para grupo limitado de restaurantes, com cotas, monitoramento, aprovação humana e fallback para foto convencional.

Critério para avançar de fase: resultado visual reconhecível e suficientemente fiel, GLB seguro e leve para celular, licença comercial compatível, processo repetível e capacidade/custo dentro dos limites aprovados.