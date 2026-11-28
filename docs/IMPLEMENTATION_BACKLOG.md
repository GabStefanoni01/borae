# Backlog completo de funcionalidades — Boraê

> Checklist mestre do produto, separado por **BACKEND** e **FRONTEND**. Cada item deve ser atualizado conforme for implementado e validado. Os checks representam evidência conhecida no código/documentação integrada; itens sem confirmação ficam desmarcados.

## Regras de acompanhamento

- [x] Backlog separado por módulo e por camada (backend/frontend).
- [ ] Ao concluir uma tarefa, marcar o checkbox correspondente no mesmo PR que entrega a funcionalidade.
- [ ] Não marcar como concluído apenas por existir tela, endpoint, DTO ou código que compila: exigir regra, autorização, testes e validação adequada.
- [ ] Se uma tarefa estiver parcialmente pronta, manter desmarcada e descrever a parte faltante no PR.
- [ ] Registrar dependências externas (credenciais, domínio, provedor, aprovação fiscal, decisão de produto) sem simular conclusão.
- [ ] Atualizar documentação de API, fluxos e decisões no mesmo PR de mudanças relevantes.
- [ ] Não misturar funcionalidades fora do MVP com entregas do MVP.

## Legenda de status

- **[x]** implementação confirmada no código integrado ou validação explicitamente realizada.
- **[ ]** não implementado, não auditado por completo ou ainda depende de teste/validação.
- **Fora do MVP**: não implementar sem nova decisão de produto.

---

# 1. Fundação, conta e autenticação

## Backend
- [x] Estrutura Spring Boot, PostgreSQL e migrações Flyway.
- [x] Cadastro/autenticação inicial com JWT.
- [x] Endpoint de consulta do usuário autenticado (/users/me).
- [ ] Revisar política de senha, hash, validação e respostas que não revelem existência de conta.
- [ ] Implementar encerramento/revogação de sessão conforme política final de sessão.
- [ ] Implementar proteção contra força bruta e rate limit nos fluxos de autenticação.
- [ ] Implementar recuperação de senha com token/link de uso único e expiração.
- [ ] Implementar verificação de e-mail/telefone nos fluxos em que for requisito.
- [ ] Implementar notificações de segurança para novos logins e alterações de credenciais.
- [ ] Implementar exclusão/anonimização de conta e retenção legal.
- [ ] Testar autenticação, autorização, expiração, abuso, concorrência e recuperação.

## Frontend
- [ ] Tela de cadastro e validação dos campos.
- [ ] Tela de login e feedback de erro sem enumerar contas.
- [ ] Persistência/renovação da sessão conforme contrato seguro.
- [ ] Tratamento global de 401, sessão expirada e logout.
- [ ] Fluxo de recuperação e redefinição de senha.
- [ ] Fluxos de verificação de contato e notificações de segurança.
- [ ] Tela de dados da conta e solicitação de exclusão.
- [ ] Estados loading, erro, sucesso, acessibilidade e responsividade.

# 2. Restaurantes, onboarding e multi-tenant

## Backend
- [x] Entidade e criação de restaurante.
- [x] Criação transacional do vínculo OWNER.
- [x] Listagem de restaurantes vinculados ao usuário.
- [x] Isolamento de acesso baseado em membership ativo nas operações de perfil.
- [ ] Auditar isolamento multi-tenant em todos os módulos e endpoints, não apenas perfil.
- [ ] Testar tentativa de acesso cruzado por troca de UUIDs em restaurante, equipe, mídia, catálogo e recursos futuros.
- [ ] Implementar transferência/exclusão de propriedade conforme regras aprovadas.
- [ ] Implementar desativação/reativação de restaurante e fluxo administrativo de suspensão.
- [ ] Padronizar erros 403/404 para não revelar recursos de outros tenants.
- [ ] Testar transações e concorrência no onboarding e nos vínculos.

## Frontend
- [ ] Tela/listagem de restaurantes do usuário.
- [ ] Fluxo de criação/onboarding e seleção do restaurante ativo.
- [ ] Troca de restaurante sem vazar dados da seleção anterior.
- [ ] Estados de lista vazia, criação, falha e sucesso.
- [ ] Tratamento de restaurante inativo/suspenso.
- [ ] Testes de navegação com usuário vinculado a mais de um restaurante.

# 3. Equipe, convites e papéis

## Backend
- [x] Papéis OWNER, MANAGER e STAFF.
- [x] Listagem de membros.
- [x] Convites com token aleatório, hash persistido e validade de 7 dias.
- [x] Aceite de convite vinculado ao e-mail autenticado.
- [x] Limites de convites por administrador/hora e restaurante/dia.
- [x] Alteração de papel e remoção lógica de membro pelo OWNER.
- [ ] Alinhar regra implementada e PRD: OWNER pode convidar MANAGER/STAFF; MANAGER pode convidar somente STAFF.
- [ ] Implementar envio de convite por e-mail via Resend.
- [ ] Implementar reenvio/renovação de convite pendente ou expirado conforme regra aprovada.
- [ ] Implementar revogação de convite pendente.
- [ ] Revisar proteção contra convite duplicado, abuso e corrida de aceite.
- [ ] Completar testes de matriz OWNER/MANAGER/STAFF para cada operação.
- [ ] Auditar mudanças de papel, convites e remoções.

## Frontend
- [ ] Tela de equipe com listagem, papel e estado do convite.
- [ ] Formulário de convite com opções condicionadas ao papel do ator.
- [ ] Tela/fluxo para aceitar convite e lidar com e-mail divergente, token inválido/expirado/usado.
- [ ] Ações de reenviar e revogar convite.
- [ ] Alteração de papel e remoção de membro com confirmação.
- [ ] Exibir token/ação de compartilhamento com aviso de segurança quando aplicável.
- [ ] Ocultar ações não permitidas sem depender disso como segurança.
- [ ] Estados loading, vazio, erro, sucesso e sem permissão.

# 4. Perfil, configurações e presença do restaurante

## Backend
- [x] Consultar e atualizar perfil (nome, slug, descrição, contatos, endereço, coordenadas e categoria).
- [x] Publicar e despublicar restaurante com validação de campos obrigatórios.
- [x] Horários de funcionamento por dia e múltiplos intervalos.
- [x] Gestão de logo e fotos do restaurante via backend/provedor de mídia.
- [x] Validação de coordenadas, slug e campos do perfil.
- [ ] Decidir/implementar nome público ou fantasia separado do nome legal, se aplicável.
- [ ] Decidir/implementar imagem de capa/banner (não presumir que já existe).
- [ ] Revisar configuração de estado operacional (aberto/fechado) e regras de fuso horário.
- [ ] Implementar auditoria de alterações de perfil, horário, publicação e mídia.
- [ ] Completar testes de autorização OWNER/MANAGER/STAFF para perfil, horário, publicação e mídia.
- [ ] Validar upload, substituição e remoção no ambiente publicado.
- [ ] Validar CORS, limites, formato real, processamento e credenciais do provedor de mídia.
- [ ] Validar integração do status aberto/fechado com os horários e exceções futuras.

## Frontend
- [x] Página/formulário de perfil do restaurante existente.
- [x] Tela de horários de funcionamento existente.
- [x] Tela de mídias/logo/fotos existente.
- [ ] Confirmar todos os campos previstos no fluxo: nome, descrição, categoria, endereço/localização, contatos, links e slug.
- [ ] Exibir prévia pública do perfil.
- [ ] Indicar campos obrigatórios pendentes antes da publicação.
- [ ] Implementar confirmação e feedback de publicar/despublicar.
- [ ] Exibir estado aberto/fechado conforme contrato da API.
- [ ] Implementar estados de upload, progresso, erro, troca e remoção.
- [ ] Validar acessibilidade e experiência mobile das telas de perfil/horários/mídia.
- [ ] Tratar 401/403/404 e conflitos de slug sem perder dados do formulário.

# 5. Catálogo: cardápios, categorias e produtos

## Backend
- [x] Estrutura de cardápios e estados de ciclo de vida.
- [x] CRUD de categorias e ordenação.
- [x] CRUD de produtos.
- [x] Ingredientes e adicionais associados a produtos.
- [x] Disponibilidade operacional separada do arquivamento.
- [x] Regras de menu público e QR Code principal identificadas no backend.
- [ ] Auditar cada endpoint por papel: OWNER/MANAGER gerenciam conteúdo; STAFF altera somente disponibilidade.
- [ ] Completar testes multi-tenant para cardápio, categoria, produto, ingrediente, adicional e imagem.
- [ ] Validar pertencimento de categoria/produto ao cardápio/restaurante correto em todas as operações.
- [ ] Validar ordenação, arquivamento e restauração conforme regras.
- [ ] Validar preço sempre em centavos inteiros e serialização BRL.
- [ ] Validar regras de ingredientes removíveis e adicionais com acréscimo.
- [ ] Validar concorrência e consistência ao editar/publicar cardápios.
- [ ] Garantir que produto arquivado não tenha disponibilidade alterada.
- [ ] Atualizar Swagger/API docs para todos os contratos finais.

## Frontend
- [ ] Confirmar/implementar listagem de múltiplos cardápios por restaurante.
- [ ] Criar, editar, publicar, despublicar e arquivar cardápio.
- [ ] Criar, editar, ordenar, ativar e desativar categorias.
- [ ] Listar produtos com busca e filtros por categoria/status.
- [ ] Criar e editar produto (nome, descrição, preço, categoria, ingredientes e disponibilidade).
- [ ] Adicionar, editar, remover e ordenar ingredientes individualmente.
- [ ] Definir quais ingredientes podem ser removidos pelo consumidor em um fluxo futuro de pedido, sem inventar checkout no MVP.
- [ ] Cadastrar, editar, remover e ordenar adicionais com acréscimo de preço.
- [ ] Arquivar produtos sem apagar histórico necessário.
- [ ] Implementar prévia do cardápio como consumidor.
- [ ] Implementar ações rápidas de disponibilidade para STAFF.
- [ ] Bloquear visualmente ações de conteúdo/preço/publicação para STAFF, mantendo backend como autoridade.
- [ ] Integrar upload/troca/remoção de imagem do produto pelo backend.
- [ ] Cobrir loading, vazio, erro, sucesso, 401, 403 e 404.
- [ ] Validar responsividade, teclado, foco e formulários sem perda acidental de dados.

# 6. Menu público, publicação e QR Code

## Backend
- [x] Endpoint público por slug com DTOs/allowlist próprios.
- [x] Exposição de cardápios publicados, categorias ativas, produtos elegíveis e adicionais ativos.
- [x] Produto indisponível pode ser retornado com estado de indisponibilidade.
- [x] QR Code principal do restaurante.
- [ ] Testar restaurante inexistente, inativo, suspenso, rascunho ou não publicado.
- [ ] Testar cardápio vazio, categoria inativa, produto arquivado e adicional inativo.
- [ ] Validar PUBLIC_RESTAURANT_URL_PREFIX, domínio e links gerados no deploy.
- [ ] Revisar cache/invalidação após alteração de preço, disponibilidade e publicação.
- [ ] Validar exposição mínima de dados públicos e ausência de campos internos.
- [ ] Validar CORS e acesso anônimo sem abrir rotas privadas.

## Frontend
- [ ] Página pública responsiva do restaurante por slug.
- [ ] Cabeçalho com nome, descrição, logo e identidade visual disponível.
- [ ] Seletor de múltiplos cardápios publicados.
- [ ] Navegação por categorias ativas.
- [ ] Lista de produtos e detalhe com imagem, descrição, preço, ingredientes e adicionais.
- [ ] Indicação clara de produto indisponível.
- [ ] Não renderizar cardápios em rascunho/arquivados nem produtos arquivados.
- [ ] Estados de restaurante não publicado, suspenso, inexistente e menu vazio.
- [ ] Compartilhamento/SEO/metadados básicos.
- [ ] QR Code exibido no painel, download/impressão e validação do destino.
- [ ] Mobile-first, acessibilidade, carregamento de imagem e lazy loading.
- [ ] Teste ponta a ponta anônimo, sem login.

# 7. Foto em realidade aumentada (AR) — escopo aprovado

## Backend
- [x] Reutilização da foto original do produto; não há geração volumétrica automática aprovada.
- [ ] Confirmar que a API entrega URL de mídia com CORS adequado para os navegadores suportados.
- [ ] Garantir que falha de AR nunca bloqueie o menu/foto convencional.

## Frontend
- [x] Experiência de AR baseada na fotografia original com fallback convencional, validada manualmente pelo responsável do produto.
- [x] Preservação da foto original, sem remoção de fundo ou alteração do arquivo.
- [ ] Manter matriz de compatibilidade de navegador/dispositivo e validar após mudanças.
- [ ] Validar acessibilidade, carregamento, reset e limpeza de recursos temporários.
- [ ] Validar fallback em dispositivos sem suporte e em falhas de CORS/exportação.

**Cancelado/fora do escopo:** reconstrução volumétrica automática de pratos por IA, TripoSR/ZeroGPU, worker de geração 3D e pipeline de publicação de modelos gerados. Não reabrir sem nova decisão de produto.

# 8. Assistente gastronômico com IA

## Backend
- [ ] Criar serviço isolado de IA usando Gemini Flash-Lite conforme decisão aprovada.
- [ ] Definir endpoint e contrato conversacional por restaurante.
- [ ] Fornecer contexto somente do catálogo publicado do restaurante selecionado.
- [ ] Validar no backend produto, preço, disponibilidade e pertencimento antes de devolver recomendação.
- [ ] Impedir que IA invente produtos, preços, ingredientes ou promoções.
- [ ] Implementar cotas: 20 interações por consumidor/dia e 100 por restaurante/dia.
- [ ] Aplicar limites de tokens por interação (parâmetro operacional aprovado).
- [ ] Implementar timeout, fallback, rate limit, proteção contra abuso e controle de custo.
- [ ] Tratar preferências e restrições sem prometer segurança alimentar ou ausência de contaminação cruzada.
- [ ] Instrumentar custo, latência, erros, consumo de cota e qualidade.
- [ ] Testar indisponibilidade do provedor, prompt injection e isolamento multi-tenant.

## Frontend
- [ ] Componente/chat dentro do menu de um restaurante.
- [ ] Coletar intenção, orçamento, tipo de refeição, preferências e ingredientes a evitar.
- [ ] Mostrar recomendações com nome, preço e justificativa baseada no catálogo real.
- [ ] Abrir detalhe do produto recomendado.
- [ ] Informar quando não houver opções compatíveis, sem inventar alternativas.
- [ ] Exibir aviso para confirmar alergênicos diretamente com o restaurante.
- [ ] Exibir loading, erro, limite diário atingido e opção de tentar novamente.
- [ ] Garantir experiência acessível e mobile.

# 9. Conta e experiência do consumidor

## Backend
- [ ] Cadastro global do consumidor com nome, telefone, e-mail e senha.
- [ ] Verificação inicial de telefone por SMS.
- [ ] Login por telefone + senha.
- [ ] Recuperação de senha por SMS ou e-mail com validade limitada e uso único.
- [ ] Troca de telefone com verificação do novo número e preservação de conta/histórico.
- [ ] Uma sessão ativa por vez; novo login encerra a sessão anterior.
- [ ] Bloqueio de 15 minutos após cinco tentativas incorretas consecutivas.
- [ ] Notificações de segurança e atividade suspeita.
- [ ] Exclusão sob solicitação com anonimização/retencão legal.
- [ ] Testes de segurança, rate limit, sessão única e recuperação.

## Frontend
- [ ] Cadastro e verificação por SMS.
- [ ] Login por telefone e senha.
- [ ] Recuperação de senha.
- [ ] Edição de nome/e-mail e troca segura de telefone/senha.
- [ ] Exibição de notificações de segurança.
- [ ] Solicitação de exclusão e informação sobre retenção legal.
- [ ] Histórico/estado de sessão e aviso de sessão encerrada em outro dispositivo.

# 10. Fidelidade individual por restaurante

## Backend
- [ ] Configurar programa individual por restaurante.
- [ ] Configurar regra de pontuação (fixa/proporcional), base de cálculo, arredondamento e versão da regra.
- [ ] Implementar ledger imutável de créditos, débitos, resgates e ajustes.
- [ ] Confirmar compra e elegibilidade por mecanismo operacional aprovado.
- [ ] Exigir telefone verificado para identificação do consumidor conforme regra.
- [ ] Implementar idempotência para eventos de compra.
- [ ] Implementar resgate, estorno e compensação sem saldo inconsistente.
- [ ] Implementar expiração FEFO e notificações nos 14 dias anteriores ao vencimento.
- [ ] Implementar campanhas e benefícios de aniversário conforme regra.
- [ ] Implementar controles de fraude, concorrência e auditoria.
- [ ] Garantir que saldo/pontos de restaurantes diferentes nunca se misturem.
- [ ] Testar cálculos, arredondamento, estornos, concorrência e reprocessamento.

## Frontend
- [ ] Painel para ativar/configurar programa e regras.
- [ ] Cadastro/gestão de recompensas e campanhas.
- [ ] Visão de saldo, extrato e transações para o restaurante.
- [ ] Experiência do consumidor para consultar saldo, histórico e recompensas.
- [ ] Fluxo de resgate e confirmação conforme operação aprovada.
- [ ] Exibir validade/expiração e notificações de pontos próximos do vencimento.
- [ ] Estados sem programa, sem saldo, erro e indisponibilidade.

**Boraê Club compartilhado:** fora do MVP; não implementar carteira compartilhada, transferência de pontos, resgate cruzado ou liquidação entre restaurantes.

# 11. Descoberta de restaurantes, avaliações e favoritos

## Backend
- [ ] Diretório público de restaurantes.
- [ ] Busca por localização atual e busca manual de região.
- [ ] Filtros combináveis: categoria, distância, preço, avaliação, horário, fidelidade e modalidade quando aplicável.
- [ ] Relevância considerando distância, avaliações, popularidade e completude.
- [ ] Expor estado aberto/fechado e próxima abertura quando conhecida.
- [ ] Página de marca/rede e páginas por unidade.
- [ ] Avaliações somente após compra confirmada.
- [ ] Nota geral de 1 a 5 e critérios de atendimento, comida e ambiente.
- [ ] Resposta do restaurante sem editar/apagar avaliação.
- [ ] Denúncia de avaliação com triagem assistida por IA e decisão humana.
- [ ] Favoritos, listas personalizadas e compartilhamento por link.
- [ ] Paginação, índices geográficos, rate limit e proteção contra abuso.
- [ ] Testes de privacidade, elegibilidade de avaliação e relevância.

## Frontend
- [ ] Página de descoberta/diretório.
- [ ] Solicitação de localização com alternativa de busca manual.
- [ ] Busca e filtros combináveis.
- [ ] Ordenação por relevância e apresentação de abertos antes de fechados.
- [ ] Cards e página pública de restaurante.
- [ ] Páginas de marca e seleção de unidades.
- [ ] Formulário de avaliação apenas para consumidor elegível.
- [ ] Exibição de avaliações e respostas do restaurante.
- [ ] Denunciar avaliação.
- [ ] Favoritar restaurante.
- [ ] Criar/editar listas e compartilhar por link.
- [ ] Estados de localização negada, sem resultados, erro e carregamento.

# 12. Assinaturas, cobrança e indicação

## Backend
- [ ] Implementar plano único e preços aprovados: R$ 49,90 mensal, R$ 134,73 trimestral e R$ 479,04 anual.
- [ ] Implementar trial de 30 dias.
- [ ] Exigir forma de pagamento e confirmação expressa antes da primeira cobrança.
- [ ] Integrar Asaas para cartão/Pix e assinatura.
- [ ] Implementar webhooks com assinatura/autenticidade, idempotência e reprocessamento.
- [ ] Implementar renovação, cancelamento e acesso até fim do período pago.
- [ ] Implementar tentativas de cobrança e tolerância de sete dias antes da suspensão.
- [ ] Implementar reativação e mudança de periodicidade na próxima renovação.
- [ ] Implementar créditos de indicação não sacáveis/transferíveis, expiração em 12 meses e regras pós-cancelamento.
- [ ] Implementar conciliação, histórico de cobrança e trilha de auditoria.
- [ ] Validar requisitos fiscais antes de emissão de NFS-e.

## Frontend
- [ ] Tela de planos e periodicidades.
- [ ] Tela de ativação do trial e contratação.
- [ ] Cadastro/gestão de forma de pagamento via fluxo seguro do Asaas.
- [ ] Exibição de status, próxima renovação e histórico de cobranças.
- [ ] Cancelamento com confirmação e informação de fim de acesso.
- [ ] Avisos de cobrança falha e prazo de tolerância.
- [ ] Exibição de créditos de indicação e regras de uso.
- [ ] Exibição/download de notas fiscais quando disponíveis.

# 13. Notas fiscais

## Backend
- [ ] Integrar emissão de NFS-e pelo Asaas após validação fiscal da empresa e município.
- [ ] Associar nota à cobrança e tratar idempotência.
- [ ] Tratar falhas, cancelamentos, reemissão e conciliação.
- [ ] Armazenar metadados e links com controles de acesso e retenção.

## Frontend
- [ ] Listar notas fiscais vinculadas às cobranças.
- [ ] Disponibilizar consulta/download conforme retorno do provedor.
- [ ] Exibir pendência/falha fiscal sem afirmar emissão concluída.

# 14. Administração global e suporte

## Backend
- [ ] Criar autenticação e papéis globais separados dos papéis de restaurante.
- [ ] Implementar administrador principal e equipe de suporte com escopos limitados.
- [ ] Implementar endpoints administrativos protegidos.
- [ ] Permitir ao suporte consultar somente dados técnicos permitidos.
- [ ] Implementar suspensão temporária de restaurante pelo suporte.
- [ ] Implementar exclusão administrativa com confirmação e justificativa.
- [ ] Implementar criação/remoção de administradores conforme regra do administrador original.
- [ ] Auditar toda ação administrativa.
- [ ] Implementar modo de manutenção e agendamento/comunicação.
- [ ] Garantir que suporte não acesse dados pessoais, histórico de compras ou dados financeiros.
- [ ] Testar segregação de privilégios e trilha de auditoria.

## Frontend
- [ ] Criar aplicação/painel administrativo global separado.
- [ ] Dashboard de indicadores globais de restaurantes, consumidores, compras e receita.
- [ ] Ocultar dados financeiros e pessoais para perfil de suporte.
- [ ] Tela de consulta técnica e suspensão de restaurante.
- [ ] Fluxo de exclusão com justificativa e confirmação.
- [ ] Gestão de administradores conforme privilégios.
- [ ] Consulta de auditoria e ações administrativas.
- [ ] Gestão/aviso de modo de manutenção.

# 15. E-mail, notificações e suporte

## Backend
- [ ] Configurar Resend com domínio autenticado.
- [ ] Enviar convites, avisos de conta, cobrança e notificações transacionais.
- [ ] Registrar status de entrega, bounce e falhas sem expor segredos.
- [ ] Implementar política de retry e idempotência para mensagens.
- [ ] Implementar chat de suporte com IA e encaminhamento humano.
- [ ] Definir retenção e privacidade de conversas de suporte.

## Frontend
- [ ] Preferências/centro de notificações quando aplicável.
- [ ] Chat de suporte dentro da plataforma.
- [ ] Exibir estado de envio, falha e encaminhamento para humano.
- [ ] Exibir mensagens transacionais relevantes dentro do painel.

# 16. API externa, webhooks e Sandbox

## Backend
- [ ] Implementar API Keys individuais.
- [ ] Armazenar apenas hash/segredo de forma segura e permitir exibição única.
- [ ] Implementar escopos, rotação, transição máxima de 24h e revogação imediata.
- [ ] Aplicar limites aprovados: 60/minuto, 10.000/dia e burst 10/segundo.
- [ ] Implementar auditoria de uso e segurança de chaves.
- [ ] Implementar webhooks configuráveis, assinados e idempotentes.
- [ ] Implementar retries progressivos, sete tentativas e reprocessamento manual.
- [ ] Implementar Sandbox separado de produção.
- [ ] Publicar OpenAPI/Swagger para integrações autorizadas.
- [ ] Implementar proteção contra abuso e isolamento por chave/tenant.

## Frontend
- [ ] Tela para criar, listar, rotacionar e revogar API Keys.
- [ ] Exibir segredo apenas uma vez e instruções para armazená-lo com segurança.
- [ ] Configurar endpoints de webhook e eventos assinados.
- [ ] Consultar histórico de entregas, tentativas e erros.
- [ ] Ação de reprocessamento manual autorizada.
- [ ] Acesso à documentação e credenciais do Sandbox.

# 17. Operação, observabilidade, LGPD e infraestrutura

## Backend/Infra
- [x] CI para backend, frontend e Docker identificada.
- [x] Migrações versionadas com Flyway.
- [ ] Revisar CORS, headers, rate limiting, multipart e limites de proxy em produção.
- [ ] Configurar logs estruturados sem segredos e dados pessoais desnecessários.
- [ ] Instrumentar métricas de erro, latência, uso e custo.
- [ ] Configurar alertas e painel operacional; ferramenta externa de erro ainda depende de decisão.
- [ ] Implementar fila durável/eventos e reprocessamento conforme política aprovada.
- [ ] Implementar backups diários, retenção 30 dias e testes trimestrais de restauração.
- [ ] Validar RPO de 1h e RTO de 4h.
- [ ] Implementar retenção por categoria: logs de aplicação 30 dias, segurança/autenticação 180 dias, auditoria administrativa/API Keys 365 dias.
- [ ] Implementar exportação, correção e exclusão de dados de titulares conforme LGPD.
- [ ] Validar segredos e variáveis de ambiente em cada ambiente.
- [ ] Documentar execução local, homologação, deploy, rollback e recuperação.
- [ ] Validar desempenho, acessibilidade e compatibilidade mobile de ponta a ponta.

## Frontend
- [ ] Implementar tratamento consistente de loading, vazio, erro, sucesso, 401, 403 e 404.
- [ ] Garantir navegação por teclado, labels, contraste e foco visível.
- [ ] Garantir layout responsivo em painel e páginas públicas.
- [ ] Evitar perda acidental de formulários e submissões duplicadas.
- [ ] Implementar estados de manutenção/indisponibilidade.
- [ ] Instrumentar erros de interface sem capturar dados sensíveis.

# 18. Critérios obrigatórios para concluir qualquer feature

## Backend
- [ ] Regras e contratos documentados.
- [ ] Autenticação/autorização aplicadas no servidor.
- [ ] Isolamento multi-tenant testado para recurso de restaurante.
- [ ] Validação de entrada e erros previsíveis.
- [ ] Testes unitários e de integração relevantes passando.
- [ ] Migrações revisadas/testadas quando houver alteração de dados.
- [ ] Idempotência/concorrência tratadas em operações críticas.
- [ ] Logs/observabilidade sem exposição de dados sensíveis.

## Frontend
- [ ] Integração com API real, sem mocks no fluxo liberado.
- [ ] Estados loading, vazio, erro, sucesso e sem permissão.
- [ ] UX respeita os papéis retornados pelo backend.
- [ ] Responsividade e acessibilidade verificadas.
- [ ] Testes relevantes passando.
- [ ] Feedback de sucesso/erro e prevenção de submissão duplicada.
- [ ] Validação manual em ambiente de teste/homologação registrada.

## Entrega
- [ ] CI verde na PR.
- [ ] Documentação atualizada.
- [ ] Revisão humana concluída.
- [ ] PR integrada à main.
- [ ] Deploy validado quando aplicável.

---

# 19. Ordem de implementação recomendada

1. **Fechar fundamentos existentes:** matriz de permissões, multi-tenant, convites/e-mail, auditoria e validação operacional de perfil/equipe.
2. **Concluir núcleo do catálogo:** auditoria backend, telas Angular integradas, estados e testes ponta a ponta.
3. **Concluir menu público:** página por slug, seleção de cardápios, QR Code e validação de publicação.
4. **Manter AR por fotografia:** compatibilidade/fallback; geração volumétrica automática segue cancelada.
5. **Assistente gastronômico:** somente com catálogo confiável e cotas/validações no backend.
6. **Fidelidade individual:** ledger, resgates, estornos e interface do consumidor/restaurante.
7. **Descoberta:** diretório, filtros, avaliações, favoritos e páginas de marca/unidade.
8. **Monetização e operação:** Asaas, NFS-e, painel global, API externa, suporte e LGPD.

## Fora do MVP / não implementar sem nova aprovação

- Pedidos online, carrinho, checkout e pagamentos de pedidos.
- Integração com PDV/ERP.
- Aplicativos nativos.
- Boraê Club compartilhado, carteira ou resgate cruzado.
- QR Code por mesa.
- Tamanhos, combos, promoções e alternância automática de cardápio por horário.
- Geração automática volumétrica de modelos 3D a partir de fotos.
