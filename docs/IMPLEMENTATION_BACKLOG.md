# Backlog geral de implementação — Boraê

> Documento vivo de acompanhamento. Atualizar os checkboxes na mesma PR em que cada entrega for implementada, testada e validada. A existência de código não significa, por si só, que a etapa esteja concluída.

## Como marcar

- [x] Implementação confirmada no código integrado ou na entrega descrita.
- [ ] Ainda precisa ser implementado, testado, validado em ambiente ou documentado.
- Não marcar como concluído apenas porque compila.
- Quando uma tarefa depender de decisão de produto, provedor, credencial ou validação externa, registrar a dependência em vez de inventar uma solução.
- Para cada item concluído, manter evidência no PR: testes executados, arquivos/rotas afetados e validação manual quando aplicável.

## 0. Fundação, segurança e qualidade

- [x] Backend Spring Boot, frontend Angular, PostgreSQL e migrações Flyway.
- [x] Autenticação JWT e perfil do usuário autenticado.
- [x] CI separada para backend, frontend e Docker.
- [x] Testes automatizados de segurança e integração PostgreSQL existentes.
- [ ] Consolidar execução local reproduzível e atualizar instruções de ambiente.
- [ ] Revisar cobertura real dos fluxos críticos e adicionar testes de regressão para falhas encontradas.
- [ ] Verificar configuração de CORS, headers, limites de upload e segredos nos ambientes publicados.
- [ ] Definir e validar observabilidade operacional (logs sem dados sensíveis, métricas, alertas e rastreamento).
- [ ] Validar backup, restauração e retenção de dados conforme decisões operacionais.
- [ ] Revisar acessibilidade, responsividade e comportamento em navegadores móveis.

## 1. Restaurante, equipe e permissões

### Já existe no código (não duplicar)
- [x] Criação de restaurante e vínculo OWNER transacional.
- [x] Papéis OWNER, MANAGER e STAFF.
- [x] Convites com token aleatório, hash persistido, validade de 7 dias e aceite pelo e-mail autenticado.
- [x] Limites de convite por administrador e por restaurante.
- [x] Listagem de membros, alteração de papel e remoção lógica.
- [x] Perfil do restaurante: consulta e atualização de nome, slug, descrição, contatos, endereço, coordenadas e categoria.
- [x] Publicação e despublicação do perfil, com validação dos campos obrigatórios.
- [x] Horários de funcionamento com múltiplos intervalos por dia.
- [x] Gestão de logo e fotos do restaurante via backend/ImageKit.
- [x] Testes unitários de serviço para perfil, papéis e regras de publicação.

### Falta fechar / validar
- [ ] Revisar matriz de permissões OWNER/MANAGER/STAFF em todos os endpoints, incluindo mídia, horários, equipe, catálogo e publicação.
- [ ] Completar testes de integração de autorização para cada papel e operações proibidas.
- [ ] Completar testes multi-tenant: usuário de restaurante A não lê nem altera recursos de B por troca de IDs.
- [ ] Revisar consistência entre permissões do backend, guards e ações exibidas no Angular.
- [ ] Decidir e implementar auditoria de alterações críticas do restaurante/equipe (ator, ação, recurso e data), respeitando retenção e privacidade.
- [ ] Implementar envio transacional de convites por e-mail via Resend; hoje o convite retorna token para compartilhamento manual.
- [ ] Definir se recuperação/revogação de convite é necessária e implementar apenas após fechar regra de negócio.
- [ ] Validar fluxo completo de upload, substituição e remoção de logo/fotos em ambiente publicado.
- [ ] Validar publicação/despublicação com dados reais e confirmar a apresentação no menu público.

## 2. Catálogo e cardápio digital — prioridade principal

### Backend já identificado
- [x] CRUD de cardápios e ciclo de publicação/rascunho/arquivamento.
- [x] CRUD e ordenação de categorias.
- [x] CRUD de produtos, ingredientes e adicionais.
- [x] Disponibilidade operacional separada do ciclo de vida do produto.
- [x] Upload e remoção de imagem de produto via backend.
- [x] Endpoint público por slug com allowlist de dados publicados.
- [x] QR Code principal do restaurante.
- [x] Testes unitários de serviços de cardápio/produto e serviço de menu público existentes.

### Falta fechar / validar
- [ ] Auditar endpoints e regras de autorização de cada operação de catálogo por papel.
- [ ] Completar testes multi-tenant envolvendo restaurante, cardápio, categoria, produto, imagem e adicionais.
- [ ] Validar que STAFF só altera disponibilidade e não modifica conteúdo, preço, imagem ou publicação.
- [ ] Confirmar integridade de ordenação e pertencimento de categorias/produtos ao cardápio correto.
- [ ] Confirmar tratamento de preços em centavos inteiros em todos os contratos e telas.
- [ ] Revisar validação de MIME real, tamanho, processamento, remoção remota e falhas do provedor de imagens.
- [ ] Confirmar no frontend os fluxos completos de cardápio, categoria, produto, ingredientes, adicionais e imagem, sem mocks.
- [ ] Confirmar estados loading, vazio, erro, sucesso, 401, 403 e 404 nas telas de gestão.
- [ ] Validar página pública em celular/desktop, acessibilidade, imagens e estados sem conteúdo.
- [ ] Validar filtros de publicação: cardápios publicados, categorias ativas, produtos não arquivados e adicionais ativos.
- [ ] Validar indicação de produto indisponível sem removê-lo do menu público.
- [ ] Validar QR Code em ambiente publicado e conferir PUBLIC_RESTAURANT_URL_PREFIX.
- [ ] Atualizar Swagger e docs/API.md após qualquer mudança de contrato.

## 3. Experiência pública e publicação

- [x] Endpoint público de consulta do menu por slug.
- [x] Separação de DTOs públicos e administrativos documentada.
- [x] QR Code principal apontando para a página pública do restaurante.
- [ ] Validar domínio/base URL pública e deep links em produção.
- [ ] Testar restaurante rascunho, suspenso, inativo e slug inexistente.
- [ ] Testar menu sem cardápios, sem categorias e sem produtos elegíveis.
- [ ] Revisar metadados de compartilhamento, SEO básico e carregamento de imagens.
- [ ] Fazer validação ponta a ponta com um restaurante de teste, do cadastro à consulta anônima.

## 4. Foto em realidade aumentada — escopo mantido

- [x] Experiência de AR baseada na fotografia original do produto, com fallback para a visualização convencional (implementada na entrega anterior e validada manualmente pelo responsável do produto).
- [x] Preservar a foto original; não remover fundo nem modificar o arquivo enviado.
- [ ] Manter matriz de compatibilidade atualizada para navegadores/dispositivos suportados.
- [ ] Validar acessibilidade, desempenho, tratamento de CORS e fallback em dispositivos reais após mudanças relevantes.

**Fora do escopo:** geração automática volumétrica de modelos 3D a partir de fotos, TripoSR/ZeroGPU e pipeline de worker/jobs. A PR #41 documenta a decisão de produto e permanece separada até ser integrada. Não reintroduzir essas tarefas neste backlog.

## 5. Assistente gastronômico com IA

- [ ] Confirmar contrato e contexto de catálogo enviados ao assistente.
- [ ] Isolar serviço de IA e política de fallback quando o provedor estiver indisponível.
- [ ] Recomendar somente produtos reais do catálogo publicado.
- [ ] Validar preço, disponibilidade e pertencimento do produto no backend antes de responder.
- [ ] Tratar preferências, restrições e alergênicos com linguagem segura e sem prometer ausência de contaminação cruzada.
- [ ] Implementar cotas por consumidor/restaurante e limites de tokens aprovados.
- [ ] Implementar proteção contra abuso, timeouts, retries e controle de custo.
- [ ] Medir qualidade, custo, latência, falhas e taxa de recomendações úteis.
- [ ] Validar UX de loading, erro, limite diário atingido e fallback.

## 6. Fidelidade individual

- [x] Regras de produto e decisões de cálculo/estorno/expiração documentadas.
- [ ] Configurar e versionar regras de fidelidade por restaurante.
- [ ] Implementar ledger imutável de créditos e débitos.
- [ ] Implementar confirmação de compra, crédito e resgate com idempotência.
- [ ] Implementar estornos, compensações e prevenção de saldo negativo indevido.
- [ ] Implementar expiração FEFO e notificações dentro da janela definida.
- [ ] Implementar controles contra fraude e concorrência.
- [ ] Criar auditoria e relatórios operacionais.
- [ ] Testar concorrência, repetição de eventos, falhas e recuperação.

## 7. Descoberta de restaurantes

- [x] Regras de produto para localização, relevância, filtros, avaliações, favoritos e páginas de marca/unidades documentadas.
- [ ] Implementar diretório e busca por região.
- [ ] Implementar filtros combináveis e ordenação por relevância.
- [ ] Implementar páginas públicas de restaurante/marca/unidades.
- [ ] Implementar avaliações vinculadas a compras confirmadas.
- [ ] Implementar respostas dos restaurantes e moderação com decisão humana.
- [ ] Implementar favoritos e listas compartilháveis.
- [ ] Definir proteção contra abuso, fraude e avaliações indevidas.
- [ ] Testar privacidade, ordenação, paginação e desempenho geográfico.

## 8. Monetização e operação comercial

- [x] Preço, ciclos de cobrança, trial, cancelamento e inadimplência documentados.
- [x] Asaas definido para pagamentos/assinaturas; NFS-e condicionada à validação fiscal.
- [x] Resend definido para e-mails transacionais.
- [ ] Validar cadastro/conta e requisitos fiscais antes de habilitar cobrança e NFS-e.
- [ ] Implementar assinaturas, checkout e webhooks Asaas com idempotência e reprocessamento.
- [ ] Implementar tratamento de falha, período de tolerância, suspensão e reativação.
- [ ] Implementar créditos de indicação conforme regras aprovadas.
- [ ] Implementar emissão e conciliação de NFS-e após validação fiscal.
- [ ] Implementar painel administrativo global separado.
- [ ] Implementar trilha de auditoria para ações administrativas.
- [ ] Implementar notificações transacionais por Resend e tratamento de bounce/falha.
- [ ] Implementar retenção, exportação e exclusão de dados conforme LGPD e obrigações legais.

## 9. API externa e integrações futuras

- [x] Requisitos de API externa, API Keys, escopos, rate limits, webhooks e Sandbox documentados.
- [ ] Implementar API Keys com armazenamento seguro, escopos, rotação e revogação.
- [ ] Implementar rate limiting distribuído por chave/escopo.
- [ ] Implementar webhooks assinados, idempotência, retries e reprocessamento.
- [ ] Implementar Sandbox e documentação OpenAPI pública.
- [ ] Implementar logs de auditoria sem expor segredos ou dados pessoais desnecessários.

## 10. Critérios para encerrar cada entrega

- [ ] Regra de negócio e escopo documentados.
- [ ] Autorização validada no backend; frontend não é fronteira de segurança.
- [ ] Isolamento multi-tenant testado quando houver recurso de restaurante.
- [ ] Testes unitários e de integração relevantes adicionados e executados.
- [ ] Estados de interface e acessibilidade validados quando houver frontend.
- [ ] Migrações revisadas e testadas quando houver alteração de dados.
- [ ] Swagger/API docs e documentação de produto atualizados.
- [ ] CI verde na PR.
- [ ] Validação manual em ambiente de teste/homologação registrada quando necessária.
- [ ] Revisão humana concluída e PR integrada à main.

## Próxima sequência recomendada

1. Fechar autorização, isolamento multi-tenant, auditoria e validação operacional de restaurante/equipe.
2. Fazer auditoria ponta a ponta do catálogo e da página pública, priorizando os itens ainda não comprovados.
3. Corrigir as lacunas encontradas e concluir o fluxo de publicação/QR Code.
4. Somente depois iniciar o assistente gastronômico; fidelidade, descoberta e monetização vêm após validação do núcleo do produto.
