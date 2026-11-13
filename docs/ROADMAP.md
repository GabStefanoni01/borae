# Roadmap do Boraê\n\nA especificação funcional e técnica dos módulos está em [PRD.md](PRD.md), [DOMAIN_MODEL.md](DOMAIN_MODEL.md), [API_DESIGN.md](API_DESIGN.md) e nos documentos MODULE_*.md. A existência de uma especificação não significa que o módulo esteja implementado.

Este roadmap organiza dependências. Não é autorização para implementar todas as etapas de uma vez: concluir, testar e validar cada fase antes de avançar.

## Fase 0 — Fundação técnica e identidade
**Status: implementada em grande parte**
- [x] Repositório, estrutura inicial e documentação base.
- [x] Backend Java/Spring Boot e frontend Angular.
- [x] Configuração de execução Docker/CI e publicação no Render.
- [x] PostgreSQL gerenciado e migrações Flyway.
- [x] Cadastro/autenticação inicial e sessão JWT.
- [x] Endpoint de perfil autenticado (`/users/me`).
- [x] Proteção inicial de rotas no frontend.

## Fase 1 — Restaurante e equipe
**Status: implementada; validar continuamente**
- [x] Entidade de restaurante e vínculo de usuário.
- [x] Listagem dos restaurantes do usuário.
- [x] Onboarding: criação de restaurante e vínculo OWNER transacional.
- [x] Papéis OWNER, MANAGER e STAFF.
- [x] Convites com token seguro e validade.
- [x] Aceite de convite vinculado ao e-mail autenticado.
- [x] Listagem, alteração de papel e remoção lógica de membros.
- [ ] Envio de convite por e-mail.
- [ ] Revisão de permissões operacionais detalhadas para cada papel.
- [ ] Fluxo de recuperação/revogação de convite, se necessário.

## Fase 2 — Perfil e estrutura do restaurante
- [ ] CRUD do perfil do restaurante (nome público, descrição, endereço, contatos, identidade visual).
- [ ] Estado operacional e configurações básicas.
- [ ] Regras de acesso por papel aplicadas em todos os módulos.
- [ ] Auditoria de alterações relevantes.
- [ ] Testes multi-tenant garantindo isolamento entre restaurantes.

## Fase 3 — Catálogo e cardápio digital (prioridade central do produto)
- [ ] CRUD de categorias.
- [ ] CRUD de produtos e imagens.
- [ ] Preços, descrições, ingredientes e disponibilidade.
- [ ] Variações, complementos e adicionais.
- [ ] Página pública responsiva do cardápio.
- [ ] QR Code do restaurante e, posteriormente, por mesa.
- [ ] Testes de autorização e de exposição pública apenas dos dados publicados.

## Fase 4 — Experiência 3D/AR
- [x] Definir armazenamento de mídia: Cloudflare R2 (D-09 aprovada).
- [ ] Upload e validação de GLB/glTF.
- [ ] Associação do modelo a um produto.
- [ ] Calibração de dimensões físicas.
- [ ] Visualização 3D e fallback.
- [ ] AR em dispositivos compatíveis.

## Fase 5 — Assistente gastronômico
- [ ] Definir contratos backend para contexto e recomendações.
- [ ] Criar serviço de IA isolado (Python/FastAPI) quando necessário.
- [ ] Recomendar somente itens reais do catálogo.
- [ ] Validar preço e disponibilidade no backend.
- [ ] Tratar preferências, restrições e alergênicos com avisos apropriados.
- [ ] Medir qualidade, custo, latência e taxa de recomendações úteis.

## Fase 6 — Fidelidade individual
**Regras de produto aprovadas; implementação planejada.**
- [x] Definir programas individuais por restaurante (D-12/D-14).
- [x] Definir cálculo proporcional/fixo, base de cálculo e arredondamento (D-12).
- [x] Definir confirmação de compra, telefone verificado e idempotência (D-13).
- [x] Definir estorno, compensação, validade, FEFO, expiração e notificações (D-15).
- [ ] Implementar configuração e versionamento de regras por restaurante.
- [ ] Implementar ledger, créditos, resgates e compensação de estornos.
- [ ] Implementar expiração automática e notificações diárias nos 14 dias anteriores ao vencimento.
- [ ] Testar concorrência, idempotência, fraude e auditoria.

## Fase 7 — Descoberta de restaurantes
**Regras de produto aprovadas em D-16; implementação planejada.**
- [x] Definir localização padrão e busca manual de região.
- [x] Definir relevância, filtros, avaliações, favoritos e compartilhamento.
- [x] Definir páginas de marca e unidades, incluindo avaliações separadas.
- [ ] Implementar diretório, busca e filtros combináveis.
- [ ] Implementar páginas públicas de restaurantes e marcas.
- [ ] Implementar avaliações vinculadas a compras confirmadas e respostas dos restaurantes.
- [ ] Implementar favoritos e listas compartilháveis por link.

## Evolução futura — Boraê Club (fora do MVP)
O Boraê Club compartilhado não integra o roadmap de implementação do MVP. Só deverá voltar ao planejamento após validar a fidelidade individual e a adesão dos restaurantes, com nova decisão de produto e aprovação prévia do modelo financeiro, contratual e operacional. Não implementar carteira compartilhada, ledger Club, resgate cruzado ou liquidação entre parceiros nesta fase.

## Fase 8 — Monetização e operação ampliada
**Regras de produto aprovadas em D-18 a D-20; implementação planejada.**
- [x] Definir assinatura fixa, plano único, trial de 30 dias, ciclos de cobrança, cancelamento e inadimplência.
- [x] Definir créditos de indicação, meios de pagamento e emissão de nota fiscal.
- [x] Definir papéis globais, permissões do suporte, auditoria e modo de manutenção.
- [x] Definir API externa com chaves/permissões, rate limiting, Swagger, webhooks e Sandbox.
- [x] Definir preço e descontos: R$ 49,90 mensal, 10% trimestral e 20% anual.
- [ ] Implementar assinaturas, cobranças, notas fiscais e créditos de indicação.
- [ ] Implementar aplicação administrativa global separada e trilha de auditoria.
- [ ] Implementar API keys, rate limiting, webhooks e Sandbox.
- [ ] Implementar notificações por e-mail via Resend e chat de suporte com IA/encaminhamento humano.
- [ ] Implementar retenção de dados e suspensão por inadimplência conforme D-18.

### Limites explícitos do lançamento — D-20
- [x] Sem pedidos online.
- [x] Sem integração com PDV/ERP.
- [x] Sem aplicativos nativos para restaurantes ou consumidores; ambos usam web responsiva.
- A API e os webhooks permitem integrações futuras autorizadas, mas não alteram a exclusão de POS/ERP do lançamento.

## Critério de conclusão de cada etapa
Uma etapa só é considerada concluída quando:
1. Regras de negócio estão documentadas.
2. Backend valida identidade, papel e escopo do restaurante.
3. Frontend cobre estados de carregamento, erro, vazio e sucesso.
4. Testes relevantes passam na CI.
5. Fluxo foi validado em ambiente de teste ou produção controlada.
6. Swagger e documentação foram atualizados.
7. PR foi revisado e integrado à branch principal.

## Ordem de prioridade atual
Após a fase de equipe, priorizar perfil/configurações do restaurante e, em seguida, catálogo/cardápio. Não começar IA, fidelidade, Club ou 3D/AR antes de existir catálogo confiável e fluxo básico de gestão.


## Preparação transversal aprovada em 13/11/2026

Antes de liberar os módulos comerciais/operacionais em produção, planejar e validar:
- Integração Asaas para assinaturas/cobranças e validação fiscal da emissão de NFS-e.
- Resend com domínio autenticado e tratamento de falhas de entrega.
- Cloudflare R2 com limites, validação de MIME/tamanho e otimização de imagens.
- Cotas e monitoramento do Gemini Flash-Lite.
- Fila durável de eventos, idempotência e reprocessamento interno de webhooks.
- Triagem assistida por IA com moderação humana.
- Política LGPD por categoria e fluxo de direitos dos titulares.
- Aplicação administrativa global separada e endpoints privilegiados.
- Gestão segura de API Keys, escopos, rate limits e revogação.
- Instrumentação de erros via Sentry e métricas do painel operacional próprio.

A lista de parâmetros ainda a calibrar está em [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md). Não marcar esses itens como implementados até haver código, testes e validação operacional.
