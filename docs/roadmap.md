# Roadmap do Boraê

O roadmap separa entrega real de intenção de produto. Marque como concluído somente quando o código estiver em `main`, CI estiver verde e a funcionalidade tiver sido validada.

## Etapa 0 — Fundação técnica

- [x] Repositório e estrutura inicial.
- [x] Backend Java 21 + Spring Boot.
- [x] Frontend Angular 20.
- [x] PostgreSQL e migrations Flyway iniciais.
- [x] Swagger/OpenAPI.
- [x] Configuração de CI e deploy Render.
- [x] Cadastro e login com JWT.
- [x] Consulta do usuário autenticado (`GET /users/me`).
- [x] Guard e interceptor no Angular.
- [x] Persistência de sessão no frontend (solução provisória com localStorage).

## Etapa 1 — Restaurantes e equipe

- [x] Cadastro de restaurante.
- [x] Criação transacional do vínculo OWNER.
- [x] Slug único gerado no backend.
- [x] Listagem dos restaurantes vinculados ao usuário.
- [x] Papéis OWNER, MANAGER e STAFF.
- [x] Convites com token aleatório, hash persistido e expiração de sete dias.
- [x] Aceitação de convite validando o e-mail da conta.
- [x] Listagem de membros para OWNER/MANAGER.
- [x] Alteração de papel e remoção de membro por OWNER.
- [x] Reativação de vínculo desativado ao aceitar novo convite.
- [ ] Envio de convites por e-mail.
- [ ] Gestão de convites pendentes (listar, cancelar e reenviar).
- [ ] Gestão de perfil/configurações do restaurante.
- [ ] Definir permissões operacionais detalhadas de MANAGER e STAFF.
- [ ] Estratégia de refresh token e sessão de produção.
- [ ] Verificação de e-mail e recuperação de senha.

## Etapa 2 — Catálogo e cardápio digital

- [ ] CRUD de categorias.
- [ ] CRUD de itens/pratos.
- [ ] Preços, descrições, ingredientes e alergênicos declarados.
- [ ] Opções, tamanhos e adicionais.
- [ ] Disponibilidade e publicação do cardápio.
- [ ] Página pública responsiva do cardápio.
- [ ] QR Code para acesso ao cardápio.
- [ ] Isolamento multi-tenant coberto por testes.

Critério para avançar: um restaurante consegue montar e publicar seu cardápio; consumidor consegue consultar sem login; dados de outro restaurante nunca ficam acessíveis.

## Etapa 3 — Mídia e experiência 3D/AR

- [ ] Upload seguro de imagens.
- [ ] Upload e armazenamento de GLB/glTF.
- [ ] Associação de modelo 3D ao item do cardápio.
- [ ] Visualizador 3D no navegador.
- [ ] AR em dispositivos compatíveis.
- [ ] Fallback quando AR não estiver disponível.
- [ ] Fluxo de calibração/dimensões do modelo.

## Etapa 4 — Assistente gastronômico

- [ ] Definir contrato entre backend e serviço de IA.
- [ ] Disponibilizar ao modelo somente contexto autorizado do cardápio.
- [ ] Coletar intenção, orçamento e preferências.
- [ ] Recomendar somente itens reais.
- [ ] Revalidar preço e disponibilidade no backend.
- [ ] Tratar ingredientes/alergênicos com cautela e aviso de confirmação com o restaurante.
- [ ] Testes contra alucinação, prompt injection e acesso indevido.

## Etapa 5 — Fidelidade individual

- [ ] Requisitos e regras configuráveis por restaurante.
- [ ] Ledger de pontos e histórico.
- [ ] Regras de validade/expiração.
- [ ] Recompensas e resgate.
- [ ] Idempotência, estorno e auditoria.
- [ ] Testes concorrentes de saldo e resgate.

## Etapa 6 — Descoberta e Boraê Club

- [ ] Diretório público de restaurantes.
- [ ] Busca por localização/categoria.
- [ ] Cadastro de parceiros e benefícios.
- [ ] Carteira Boraê Club separada da fidelidade individual.
- [ ] Modelo de financiamento e compensação entre parceiros.
- [ ] Conciliação, antifraude e auditoria.
- [ ] Resgate compartilhado após validação jurídica/comercial/financeira.

## Etapas futuras — fora do MVP inicial

- [ ] Pedidos e checkout.
- [ ] Pagamentos.
- [ ] Integrações com PDV.
- [ ] Notificações e acompanhamento de pedidos.
- [ ] Assinatura premium para consumidores.
- [ ] Avaliações/favoritos e analytics avançado.
- [ ] Aplicativo nativo, se houver demanda.

## Como priorizar

A prioridade é fechar uma jornada utilizável: restaurante cadastrado → equipe configurada → cardápio criado/publicado → consumidor acessa via QR. Só depois ampliar para 3D/AR, IA, fidelidade e Club, salvo decisão explícita baseada em validação.
