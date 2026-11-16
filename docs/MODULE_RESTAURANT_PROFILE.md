# Escopo técnico — Perfil e configurações do restaurante

**Módulo:** Perfil e estrutura do restaurante (Fase 2 do roadmap)  
**Decisão de produto:** D-01, aprovada em 13/11/2026  
**Responsáveis:** backend e frontend; Renan (@renancristianobn-del) no frontend  
**Dependência:** PR #22 (catálogo/cardápio), da qual esta branch parte inicialmente.

Este documento descreve o trabalho necessário. A aprovação do escopo não significa que qualquer endpoint ou tela listados abaixo já estejam implementados. Antes de codificar, conferir o modelo atual, os contratos existentes e evitar duplicar campos ou comportamentos já entregues.

## 1. Objetivo

Permitir que OWNER e MANAGER mantenham a identidade e as informações operacionais do restaurante, preparando o perfil para a página pública do estabelecimento e para a experiência do cardápio. O preenchimento deve ser progressivo: campos opcionais não podem impedir a criação do restaurante nem a montagem do catálogo. A publicação pública, porém, deve validar os campos obrigatórios.

## 2. Regras de produto — D-01

- Nome do restaurante: obrigatório.
- Nome fantasia/nome público: usado na apresentação pública.
- Descrição: opcional.
- Logo e imagem de capa: opcionais.
- Telefone/WhatsApp, Instagram e site: opcionais.
- Endereço: obrigatório para estabelecimentos físicos.
- Localização no mapa: suportada; coordenadas associadas ao endereço quando disponíveis.
- Horários de funcionamento: configuráveis por dia da semana.
- Categoria gastronômica: obrigatória.
- Slug público: gerado automaticamente; edição permitida somente se único e válido.
- Estado de publicação: DRAFT, PUBLISHED ou SUSPENDED.
- Não exigir todos os opcionais para iniciar o cadastro ou montar o cardápio.
- Validar os campos obrigatórios antes da publicação pública.
- Registrar quem alterou dados relevantes.
- Não permitir que a equipe do restaurante reverta uma suspensão administrativa.

## 3. Backend — entregas necessárias

### 3.1 Modelo e persistência
- Auditar a entidade Restaurant e migrations existentes antes de propor novas colunas.
- Modelar/adicionar os dados do perfil: nome público, descrição, contatos, links, endereço estruturado, coordenadas, categoria gastronômica, horários, slug e referências de logo/capa.
- Definir campos obrigatórios/opcionais de acordo com D-01; endereço deve admitir o caso de estabelecimento sem endereço físico, sem enfraquecer a regra para estabelecimentos físicos.
- Criar migrations Flyway incrementais, constraints e índices (especialmente unicidade do slug normalizado).
- Preservar compatibilidade com restaurantes já cadastrados: estratégia de backfill/defaults e tratamento de dados incompletos, sem migration destrutiva.
- Definir estratégia de auditoria: autor da alteração, timestamps e registro de mudanças relevantes, sem guardar dados sensíveis desnecessários.

### 3.2 Contratos e regras de negócio
- Implementar consulta autenticada do perfil do restaurante e atualização parcial/total conforme padrão atual da API.
- Implementar validação de nome, categoria, endereço quando aplicável, URLs/contatos, horários e slug.
- Gerar slug automaticamente a partir do nome público/nome do restaurante; permitir alteração explícita somente após validar formato, normalização e unicidade. Tratar colisões sem sobrescrever outro restaurante.
- Definir catálogo controlado de categorias gastronômicas ou reutilizar o mecanismo já existente, se houver; não aceitar texto arbitrário se o domínio já possuir catálogo fechado.
- Modelar horários por dia da semana, incluindo dias fechados e intervalos; validar horários inválidos e intervalos inconsistentes. Não inferir funcionamento 24h nem regras de feriados sem decisão de produto.
- Expor status de publicação e campos faltantes necessários para publicação.
- Integrar com os endpoints de publish/unpublish já introduzidos na PR #22, evitando duplicar transições ou criar uma segunda fonte de verdade. Publicação deve validar os requisitos D-01; suspensão permanece reservada à operação administrativa.
- Garantir que OWNER e MANAGER possam consultar/editar perfil; STAFF apenas consulta o necessário para a operação, sem editar. Aplicar autorização no backend em toda rota.
- Validar membership ativo e escopo do restaurantId em cada operação; impedir IDOR e acesso cruzado entre restaurantes.

### 3.3 Mídia
- Implementar upload, substituição e remoção de logo e capa através da abstração de storage do backend e do provedor aprovado para mídia (Cloudflare R2, conforme decisões operacionais).
- Validar conteúdo real, MIME/extensão e tamanho no servidor; limites aprovados: logo até 5 MB, demais imagens até 10 MB; JPEG, PNG e WebP.
- Aplicar otimização e orientação adequada das imagens, conforme infraestrutura existente.
- Não expor credenciais do R2 nem permitir upload direto sem mecanismo seguro definido pelo backend.
- Não remover arquivo antigo antes de confirmar persistência da nova referência; tratar falhas de limpeza remota sem mascarar erro original.

### 3.4 API, documentação e testes
- Atualizar DTOs, controllers, services, repositories, OpenAPI/Swagger e docs/API.md.
- Cobrir criação/consulta/edição, slug único, validação de publicação, horários, upload/substituição/remoção de mídia, papéis e isolamento multi-tenant.
- Testar migrations e compatibilidade com restaurantes existentes usando PostgreSQL real em homologação.
- Garantir respostas HTTP coerentes (400/401/403/404/409/413/415, conforme caso) e mensagens seguras.
- Registrar auditoria de alterações relevantes.
- Não confiar em validação do frontend para garantir integridade.

## 4. Frontend Angular — implementação do Renan

### 4.1 Navegação e acesso
- Criar entrada “Perfil do restaurante”/“Configurações” no painel do restaurante selecionado.
- Disponibilizar edição somente para OWNER e MANAGER; STAFF pode visualizar somente os dados operacionais permitidos.
- Carregar o restaurante selecionado e manter o contexto correto ao alternar entre restaurantes.
- Consumir os endpoints e DTOs reais documentados no Swagger; centralizar chamadas em services tipados.

### 4.2 Formulário do perfil
- Criar tela responsiva dividida em seções: informações básicas, contato e links, endereço/localização, categoria gastronômica, horários e identidade visual.
- Campos: nome, nome público/fantasia, descrição, telefone/WhatsApp, Instagram, site, endereço, categoria e slug.
- Indicar claramente campos obrigatórios e opcionais.
- Permitir salvar progresso com perfil incompleto; não bloquear cadastro/edição de catálogo por campos opcionais vazios.
- Exibir validações por campo retornadas pelo backend, estados de carregamento, salvamento, sucesso, erro e recuperação.
- Para slug, mostrar prévia da URL pública, disponibilidade/erro de conflito e confirmação quando houver alteração, sem prometer disponibilidade antes da resposta do servidor.

### 4.3 Endereço e localização
- Criar formulário de endereço com campos coerentes com o contrato backend.
- Permitir registrar coordenadas/localização no mapa quando o recurso estiver disponível.
- Não tornar geolocalização automática obrigatória; permitir preenchimento manual.
- Se o tipo de estabelecimento permitir ausência de endereço físico, representar isso claramente e não inventar localização.

### 4.4 Horários
- Criar editor por dia da semana com opção aberto/fechado e um ou mais intervalos conforme o contrato final.
- Permitir editar e salvar horários; exibir resumo legível.
- Validar formato no cliente para UX, mas respeitar sempre as validações do backend.
- Não implementar feriados, reservas ou agendamento.

### 4.5 Logo e capa
- Permitir selecionar, visualizar prévia, enviar, substituir e remover logo/capa.
- Exibir limites e formatos aceitos (logo 5 MB; outras imagens 10 MB; JPEG/PNG/WebP).
- Mostrar progresso/estado de upload e erros; não armazenar credenciais no bundle Angular.

### 4.6 Publicação e prévia
- Exibir status DRAFT/PUBLISHED/SUSPENDED e checklist dos campos obrigatórios pendentes.
- Permitir OWNER/MANAGER publicar ou despublicar conforme endpoints existentes e regras do backend.
- Se SUSPENDED, exibir aviso e não mostrar ação para reativar.
- Criar prévia do perfil público para revisar identidade, contato, endereço, horários e links. Não inventar campos que a API não disponibiliza.
- A publicação do perfil e a publicação dos cardápios são estados distintos; deixar isso explícito na interface.

## 5. Segurança, permissões e UX

- OWNER/MANAGER: consultar e editar perfil, mídia e publicação conforme autorização.
- STAFF: leitura somente; não oferecer controles de edição/publicação.
- O backend é a autoridade; tratar 401/403/404/409 e não assumir permissão com base apenas em guards.
- Toda tela deve contemplar loading, erro recuperável, vazio, sucesso, sem permissão e responsividade mobile.
- Não exibir dados de outro restaurante ao trocar o contexto.
- Acessibilidade: labels, mensagens associadas aos campos, navegação por teclado e estados não dependentes apenas de cor.

## 6. Fora do escopo

- Gestão de cardápios, categorias e produtos (PR #22 e entrega de frontend correspondente).
- Pedidos, carrinho, checkout, pagamentos ou integração PDV/ERP.
- Reservas, gestão de mesas ou QR Code por mesa.
- Descoberta, avaliações, favoritos e páginas de marca/rede.
- Assistente gastronômico/IA, fidelidade, Boraê Club e 3D/AR.
- Administração global e cobrança/assinatura.
- Horários especiais/feriados, reservas e automações por horário.

## 7. Critérios de aceite

- [ ] OWNER/MANAGER consultam e atualizam o perfil do restaurante.
- [ ] STAFF não consegue editar perfil nem publicar/despublicar.
- [ ] Campos opcionais podem permanecer vazios sem bloquear a montagem do cardápio.
- [ ] Categoria gastronômica é obrigatória e validada no backend.
- [ ] Endereço é obrigatório para estabelecimentos físicos.
- [ ] Horários por dia podem ser configurados, validados e apresentados corretamente.
- [ ] Slug é gerado automaticamente e edição respeita unicidade/formato.
- [ ] Logo e capa podem ser enviadas, substituídas e removidas com validação no backend.
- [ ] Publicação valida campos obrigatórios e respeita DRAFT/PUBLISHED/SUSPENDED.
- [ ] Suspensão administrativa não pode ser revertida pelo painel do restaurante.
- [ ] Prévia representa os dados que serão exibidos publicamente.
- [ ] Auditoria identifica alterações relevantes.
- [ ] Testes backend/frontend, migrations e isolamento multi-tenant passam.
- [ ] Swagger, API.md, modelo de domínio, fluxos e roadmap são atualizados.

## 8. Sequência de implementação sugerida

1. Auditoria do modelo atual e contratos existentes; fechar schema sem duplicar dados.
2. Migrations e domínio/persistência.
3. Serviços, validações, autorização e endpoints de perfil.
4. Mídia de logo/capa via backend.
5. Integração de publicação e prévia pública.
6. Frontend em paralelo por seções, consumindo contratos estabilizados.
7. Testes integrados, homologação e auditoria final.

**Regra de integração:** esta branch foi criada a partir da branch da PR #22 para manter a dependência explícita. Após a PR #22 ser validada e integrada à main, atualizar/rebasear esta branch e alterar a base desta PR para main antes do merge. Não integrar automaticamente; exigir revisão humana.
