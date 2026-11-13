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
- [ ] Definir armazenamento de mídia.
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
- [ ] Regras de pontuação configuráveis por restaurante.
- [ ] Ledger de pontos e histórico.
- [ ] Recompensas, resgates e expiração.
- [ ] Campanhas e benefícios de aniversário.
- [ ] Proteção contra duplicidade, fraude e concorrência.

## Fase 7 — Descoberta de restaurantes
- [ ] Definir taxonomia, categorias e critérios de busca (D-16).
- [ ] Definir geolocalização, raio e ordenação dos resultados.
- [ ] Implementar diretório e busca de restaurantes.
- [ ] Implementar páginas públicas e acesso aos cardápios.

## Evolução futura — Boraê Club (fora do MVP)
O Boraê Club compartilhado não integra o roadmap de implementação do MVP. Só deverá voltar ao planejamento após validar a fidelidade individual e a adesão dos restaurantes, com nova decisão de produto e aprovação prévia do modelo financeiro, contratual e operacional. Não implementar carteira compartilhada, ledger Club, resgate cruzado ou liquidação entre parceiros nesta fase.

## Fase 8 — Monetização e operação ampliada
- [ ] Planos SaaS para restaurantes.
- [ ] Cobrança e gestão de assinatura.
- [ ] Painel administrativo global.
- [ ] Campanhas patrocinadas e métricas.
- [ ] Avaliar pedidos, pagamentos e integrações POS somente após validação.

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
