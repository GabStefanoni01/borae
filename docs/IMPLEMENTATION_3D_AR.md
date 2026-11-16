# Plano de implementação — Experiências 3D e realidade aumentada

**Status:** planejamento técnico para a próxima etapa do produto. Esta PR não implementa o módulo; define o trabalho de backend e frontend antes da implementação funcional.

## 1. Auditoria da base e justificativa

As PRs #22 (backend de catálogo/cardápio) e #25 (frontend de catálogo/cardápio) estão integradas à `main`. O Boraê já possui restaurantes, equipe, perfil, horários, mídia de imagem, cardápios, categorias, produtos, adicionais, publicação, página pública e QR Code.

O módulo 3D/AR é a próxima experiência prevista em `docs/ROADMAP.md` e já possui requisitos em `docs/MODULE_3D_AR.md`. O catálogo é a dependência funcional necessária: os modelos serão associados a produtos existentes.

**Inconsistência técnica que precisa ser resolvida antes do upload 3D:** a decisão aprovada D-09 define Cloudflare R2 como object storage, mas a implementação atual de imagens de restaurante e produto usa ImageKit (`ImageKitRestaurantObjectStorage`). O módulo não deve ampliar essa divergência. A camada de armazenamento deve ser alinhada ao R2 antes de habilitar modelos 3D em produção.

## 2. Objetivo

Permitir que OWNER/MANAGER associem um modelo 3D a um produto do cardápio, visualizem e calibrem suas dimensões e publiquem a experiência. O consumidor poderá visualizar o modelo em 3D e, quando o dispositivo/navegador suportar, iniciar AR.

A experiência deve ser progressiva: o cardápio continua funcional mesmo sem modelo, com erro de carregamento ou em dispositivo incompatível.

## 3. Backend — implementação

### 3.1 Infraestrutura de mídia e Cloudflare R2 (pré-requisito)

- Auditar `RestaurantObjectStorage`, `ImageKitRestaurantObjectStorage`, `RestaurantMedia`, `RestaurantMediaType` e o fluxo de imagem de produto.
- Implementar adaptador de Cloudflare R2 usando API compatível com S3, credenciais exclusivamente no backend e configuração por variáveis de ambiente.
- Preservar a abstração `RestaurantObjectStorage` ou evoluí-la para uma interface neutra de object storage; controllers e serviços de domínio não devem conhecer o provedor.
- Migrar as imagens existentes do ImageKit para R2 com estratégia segura e rastreável: inventário, cópia, verificação de integridade, atualização de referências e possibilidade de rollback. Não apagar objetos antigos antes de confirmar a migração.
- Manter os contratos HTTP atuais de imagem de restaurante e produto, para não exigir retrabalho desnecessário no Angular.
- Configurar URLs públicas/CDN ou URLs assinadas conforme a política definida para cada tipo de asset. Nunca expor chaves de acesso.
- Documentar limites, CORS, domínio, lifecycle e custos dentro das cotas aprovadas. Não presumir que a configuração do bucket já existe.

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
- Definir limites numéricos de tamanho, polígonos, texturas e dimensões após teste de desempenho e capacidade do R2/CDN. Não inventar limites sem medição.
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
- Testar migração de mídia ImageKit → R2 com verificação de integridade e rollback documentado.
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
- Não implementar upload direto ao R2 com credenciais no navegador; usar os endpoints/fluxo seguro definido pelo backend.

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

- [ ] Armazenamento de mídia alinhado ao Cloudflare R2 conforme D-09, com migração segura das imagens atuais.
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

1. Validar configuração real do Cloudflare R2 e executar a migração de mídia atual do ImageKit com rollback.
2. Implementar domínio, upload, validação, metadados e endpoints 3D no backend.
3. Publicar contratos/Swagger e testar API/storage em homologação.
4. Implementar painel e prévia Angular.
5. Implementar visualização pública 3D e AR progressiva.
6. Validar dispositivos compatíveis, fallback, desempenho e segurança antes de habilitar em produção.

A implementação deve ocorrer em etapas revisáveis. Não fazer merge automático; aprovação humana é obrigatória. A especificação original em `docs/MODULE_3D_AR.md` continua sendo a referência de produto, e este documento detalha o plano de engenharia.
