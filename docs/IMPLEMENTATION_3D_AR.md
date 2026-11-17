# Plano de implementação — Experiências 3D e realidade aumentada

**Status:** backend MVP implementado na branch da PR #27. Upload, persistência, publicação, API pública, validação estrutural GLB e testes PostgreSQL com Testcontainers estão implementados. O Backend CI passou no commit atual; a execução Docker CI ainda está em andamento. Homologação real do ImageKit e frontend permanecem pendentes.

## 1. Auditoria da base e justificativa

As PRs #22 (backend de catálogo/cardápio) e #25 (frontend de catálogo/cardápio) estão integradas à `main`. O Boraê já possui restaurantes, equipe, perfil, horários, mídia de imagem, cardápios, categorias, produtos, adicionais, publicação, página pública e QR Code.

O módulo 3D/AR é a próxima experiência prevista em `docs/ROADMAP.md` e já possui requisitos em `docs/MODULE_3D_AR.md`. O catálogo é a dependência funcional necessária: os modelos serão associados a produtos existentes.

**Decisão de armazenamento para esta etapa:** o Boraê continuará utilizando ImageKit como provedor oficial de mídia. A decisão/documentação D-09 deverá ser revisada para refletir essa escolha. Antes de habilitar modelos 3D em produção, será necessário validar a compatibilidade do ImageKit com arquivos GLB e os requisitos de entrega, tamanho, MIME, cache e URLs; não presumir suporte sem testes.

## 2. Objetivo

Permitir que OWNER/MANAGER associem um modelo 3D a um produto do cardápio, visualizem e calibrem suas dimensões e publiquem a experiência. O consumidor poderá visualizar o modelo em 3D e, quando o dispositivo/navegador suportar, iniciar AR.

A experiência deve ser progressiva: o cardápio continua funcional mesmo sem modelo, com erro de carregamento ou em dispositivo incompatível.

## 3. Backend — implementação

### 3.1 Infraestrutura de mídia com ImageKit

- Auditar `RestaurantObjectStorage`, `ImageKitRestaurantObjectStorage`, `RestaurantMedia`, `RestaurantMediaType` e o fluxo de imagem de produto.
- Manter ImageKit como provedor oficial de armazenamento e entrega de mídia; não implementar adaptador para outro provedor nem migração das imagens existentes.
- Preservar a abstração `RestaurantObjectStorage` ou evoluí-la para uma interface neutra de object storage; controllers e serviços de domínio não devem conhecer o provedor.
- Validar upload, armazenamento e entrega de GLB pelo ImageKit: formatos aceitos, limites de tamanho, MIME, URLs, transformações, cache/CDN e comportamento de download. Registrar os resultados antes de fechar os limites do MVP.
- Se houver limitação do ImageKit para arquivos 3D, apresentar evidência e alternativa técnica antes de alterar a decisão de infraestrutura. Não trocar o provedor por antecipação.
- Manter os contratos HTTP atuais de imagem de restaurante e produto, para não exigir retrabalho desnecessário no Angular.
- Manter credenciais e operações privilegiadas exclusivamente no backend; o frontend não deve receber chaves privadas do ImageKit.
- Documentar configuração, limites, domínio, políticas de entrega e custos aplicáveis.

### 3.2 Modelo de domínio 3D

- Criar entidade de mídia 3D associada a `Restaurant` e `MenuProduct`, sem permitir associação cruzada entre restaurantes.
- Persistir no PostgreSQL somente metadados e referências: provider/object key, URL ou estratégia de entrega, formato, MIME validado, tamanho, dimensões do arquivo, dimensões físicas calibradas, estado de processamento/publicação e timestamps.
- Definir estados explícitos, por exemplo `PENDING_VALIDATION`, `READY` e `REJECTED`; o produto só pode expor um modelo `READY`.
- Permitir um modelo ativo por produto no MVP, com substituição e remoção sem apagar o histórico técnico necessário para auditoria.
- Preservar imagens convencionais do produto como fallback e não substituir a imagem principal automaticamente.

### 3.3 Upload e validação segura

- Criar endpoints autenticados para enviar, consultar, substituir, publicar/despublicar e remover o modelo 3D de um produto.
- Restringir operações de gestão a OWNER/MANAGER; STAFF não administra modelos.
- Validar membership ativo, restaurantId, menuId e productId em toda operação.
- Validar conteúdo real do arquivo, não apenas extensão ou Content-Type declarado.
- Preferir GLB para distribuição. Definir e documentar o suporte a glTF e dependências externas antes da implementação; não aceitar referências remotas arbitrárias.
- Definir limites numéricos de tamanho, polígonos, texturas e dimensões após teste de desempenho e limites efetivos do ImageKit. Não inventar limites sem medição.
- Processar/inspecionar arquivos em ambiente isolado, sem executar conteúdo enviado. Rejeitar arquivo corrompido, formato não suportado, dependências ausentes e arquivos acima dos limites.
- Gerar identificadores/chaves aleatórias e separar os objetos por restaurante/produto.
- Tratar falhas de banco/storage com compensação: não perder o modelo anterior se a substituição falhar; limpar objetos órfãos de forma segura.
- Registrar quem enviou, substituiu, publicou ou removeu o modelo.

### 3.4 Dimensões e calibração

- Persistir largura, altura e profundidade físicas com unidade explícita e validação de valores positivos/limites.
- Permitir que o restaurante informe ou confirme dimensões do objeto representado.
- Não declarar escala real 1:1 sem calibração confiável.
- Exibir aviso de que a escala percebida pode variar por dispositivo e ambiente.
- Documentar a estratégia de calibração e os limites do MVP antes de liberar AR.

### 3.5 API pública

- Estender o DTO público do produto para incluir apenas metadados/URL de um modelo que esteja pronto e publicado.
- Não expor object keys privados, credenciais, caminhos internos ou modelos pendentes/rejeitados.
- Respeitar publicação do restaurante, publicação do cardápio, categoria ativa e ciclo de vida do produto já existentes.
- Não criar um novo estado de publicação concorrente com o estado do cardápio; a disponibilidade do modelo deve ser subordinada ao produto/cardápio.
- Manter compatibilidade com consumidores que ainda esperam o DTO atual.

### 3.6 Testes e documentação

- Testar upload válido/inválido, MIME falso, arquivo corrompido, limites, substituição, remoção e falhas do storage.
- Testar acesso cruzado entre restaurantes, papéis OWNER/MANAGER/STAFF e acesso público a modelo não publicado.
- Testar que modelo pendente/rejeitado nunca aparece na API pública.
- Testar upload e entrega de arquivos GLB pelo ImageKit, incluindo integridade, MIME, cache/CDN, URLs e limites aplicáveis.
- Testar persistência com PostgreSQL real e storage de homologação.
- Atualizar Swagger, `docs/API.md`, `docs/MODULE_3D_AR.md`, modelo de domínio, variáveis de ambiente e runbooks operacionais.

## 4. Frontend Angular — implementação do Renan (@renancristianobn-del)

### 4.1 Painel do restaurante

- Adicionar uma área de experiência 3D no editor/detalhe do produto existente, sem criar um cadastro paralelo de produtos.
- Exibir status do modelo: sem modelo, enviando, validando, pronto, publicado ou erro.
- Permitir upload, prévia, substituição e remoção, respeitando limites e formatos informados pela API.
- Exibir progresso, erros de validação e confirmação antes de remover/substituir.
- Permitir informar/ajustar largura, altura e profundidade físicas, com unidade explícita e orientações de calibração.
- OWNER/MANAGER podem gerenciar. STAFF e demais usuários sem permissão veem somente o que a política de leitura permitir.
- Ao trocar o restaurante ou produto selecionado, cancelar/descartar estados locais para não misturar assets de contextos diferentes.
- Não disponibilizar credenciais privadas do ImageKit no navegador; usar o fluxo seguro definido pelo backend.

### 4.2 Prévia administrativa

- Integrar um visualizador 3D lazy-loaded, carregado apenas quando o usuário abrir a prévia.
- Oferecer controles acessíveis de rotação e zoom, instruções e estado de carregamento.
- Exibir fallback para imagem convencional quando o modelo falhar ou não puder ser visualizado.
- Não bloquear salvamento dos demais dados do produto se o visualizador não carregar.

### 4.3 Página pública do cardápio

- Estender o detalhe/visualização do produto para apresentar ação “Visualizar em 3D” somente quando a API retornar modelo pronto e publicado.
- Carregar o asset sob demanda, não no carregamento inicial da lista inteira.
- Em dispositivos compatíveis, apresentar ação para iniciar AR usando capacidade suportada pelo navegador/dispositivo.
- Se AR não estiver disponível, manter o visualizador 3D; se 3D falhar, manter imagem e conteúdo do produto.
- Respeitar o estado público do restaurante/cardápio/produto. Não expor modelos privados, pendentes ou rejeitados.
- Garantir experiência mobile-first, acessibilidade por teclado/leitor de tela, mensagens de erro e desempenho.

### 4.4 Testes de frontend

- Cobrir upload, progresso, substituição, remoção, dimensões, estados de erro e permissões.
- Cobrir produto sem modelo, modelo pronto, asset indisponível e dispositivo sem AR.
- Cobrir fallback para imagem e garantir que a página pública continue utilizável.
- Validar desktop e dispositivos móveis reais ou emuladores representativos; não declarar compatibilidade universal com AR.

## 5. Critérios de aceite

- [ ] ImageKit validado para upload e entrega de modelos GLB, sem migração de provedor.
- [ ] Upload 3D validado no backend e protegido contra arquivos inválidos e acesso cruzado.
- [ ] OWNER/MANAGER gerenciam o modelo; STAFF não altera assets.
- [ ] Um produto pode ter modelo 3D pronto, substituível e removível.
- [ ] Dimensões físicas e método/aviso de calibração são persistidos.
- [ ] API pública só expõe modelo pronto e publicado de produto elegível.
- [ ] Consumidor consegue visualizar em 3D sem AR.
- [ ] AR aparece somente em dispositivos/navegadores compatíveis.
- [ ] Imagem convencional permanece como fallback em todos os cenários de falha/incompatibilidade.
- [ ] Assets são carregados sob demanda e não prejudicam o carregamento inicial do cardápio.
- [ ] Testes backend/frontend, PostgreSQL e storage de homologação passam.
- [ ] Swagger, documentação e parâmetros operacionais são atualizados.

## 6. Fora do escopo

Pedidos, carrinho, checkout, pagamentos, reservas, QR Code por mesa, PDV/ERP, edição/criação de modelos 3D, marketplace de modelos, geração de modelos por IA, reconhecimento automático de pratos, descoberta, avaliações, fidelidade, Boraê Club e assinatura/cobrança.

## 7. Dependências e ordem de execução

1. Validar o ImageKit para upload e entrega dos modelos GLB e revisar a decisão/documentação D-09 para refletir o provedor escolhido.
2. Implementar domínio, upload, validação, metadados e endpoints 3D no backend.
3. Publicar contratos/Swagger e testar API/storage em homologação.
4. Implementar painel e prévia Angular.
5. Implementar visualização pública 3D e AR progressiva.
6. Validar dispositivos compatíveis, fallback, desempenho e segurança antes de habilitar em produção.

A implementação deve ocorrer em etapas revisáveis. Não fazer merge automático; aprovação humana é obrigatória. A especificação original em `docs/MODULE_3D_AR.md` continua sendo a referência de produto, e este documento detalha o plano de engenharia.


## Status da implementação do backend

Primeiro corte implementado nesta branch:

- Migration V9 com tabela de modelo 3D por produto e tabela de auditoria de eventos.
- Entidade com status READY/PUBLISHED, referência ImageKit, MIME, tamanho, dimensões físicas em milímetros e identificação dos usuários responsáveis pela criação/última alteração.
- Validador estrutural GLB 2.0 (magic, versão, comprimento declarado, chunks e `asset.version`).
- Endpoints autenticados de consulta, upload/substituição, publicação/despublicação e remoção.
- Escrita restrita a OWNER/MANAGER; STAFF não administra modelos.
- A troca do arquivo preserva o anterior até o commit do banco; a exclusão remota ocorre após commit.
- DTO público expõe somente URL e dimensões do modelo publicado, junto de produto e cardápio já elegíveis para exibição.
- Testes unitários iniciais para GLB inválido/válido, permissão e contrato público.

**Limite provisório:** o primeiro corte limita o GLB a 10 MB, acompanhando o limite multipart já configurado no backend. Esse valor ainda precisa ser confrontado com modelos reais e com os limites da conta ImageKit antes da decisão final.

**Pendente antes da homologação:** executar testes reais de upload/entrega de GLB no ImageKit, ampliar testes de integração/transação e confirmar os limites e o comportamento do CDN para o tipo de arquivo.


## Atualização da implementação — segurança e contratos

- O upload ImageKit usa `MultipartField<InputStream>` com `ByteArrayInputStream`, compatível com a assinatura multipart do SDK adotado.
- O validador GLB rejeita referências externas em buffers e URLs de imagens, reduzindo risco de carregamento de recursos remotos por consumidores do modelo.
- Foram adicionados testes unitários para referências externas e data URI embutido.
- `docs/MODULE_3D_AR.md` e `docs/API.md` foram alinhados ao escopo atual e à decisão de manter ImageKit.
- Ainda não há confirmação de CI verde nem homologação de upload/entrega real no ImageKit. Não liberar produção nem iniciar frontend antes dessas validações.
