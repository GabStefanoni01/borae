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
