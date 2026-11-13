# Guia de contribuição

## 1. Regra principal

A documentação e o código precisam evoluir juntos. Não introduza regra de negócio, dependência, endpoint, papel ou integração que contradiga os documentos. Se houver ambiguidade, alinhe com o responsável pelo produto antes de codificar.

## 2. Fluxo Git

1. Atualize sua branch local a partir de `main`.
2. Crie uma branch específica: `feat/<dominio>-<resumo>`, `fix/<resumo>` ou `docs/<resumo>`.
3. Faça alterações pequenas e relacionadas ao objetivo.
4. Execute testes e build disponíveis.
5. Abra Pull Request para `main`.
6. Descreva objetivo, decisões, endpoints, migrações, testes e impacto no frontend.
7. Aguarde CI e revisão. Não faça push direto em `main` nem faça merge sem aprovação.

Exemplos de nomes:
- `feat/catalog-categories`
- `feat/restaurant-profile`
- `fix/invitation-expiration`
- `docs/update-api-contracts`

## 3. Antes de implementar
- Leia a seção correspondente de PRODUCT, ARCHITECTURE, API e ROADMAP.
- Verifique o código existente e reutilize padrões estabelecidos.
- Confirme se a funcionalidade está marcada como planejada e se é a próxima prioridade.
- Identifique permissões, impacto multi-tenant, alterações de banco e efeitos no frontend.
- Não crie abstrações genéricas ou microserviços sem necessidade demonstrada.

## 4. Backend
- Controller fino; regras no service.
- DTOs para entrada e saída; validação com Bean Validation.
- Repositories para persistência.
- Transações para operações atômicas.
- Migrações Flyway novas e imutáveis após aplicadas.
- Validar acesso por usuário, restaurante, vínculo ativo e papel.
- Não aceitar identidade ou papel privilegiado do corpo da requisição.
- Adicionar testes para sucesso, falha de validação, falta de autenticação, falta de permissão e acesso cruzado entre restaurantes.

## 5. Frontend
- Manter responsabilidades separadas entre componentes e serviços.
- Reutilizar os serviços HTTP e interceptor existentes.
- Tratar carregamento, erros, listas vazias e feedback de ações.
- Guard de rota é conveniência de navegação; autorização real é sempre do backend.
- Não codificar preços, disponibilidade, regras de pontos ou permissões como valores fixos no frontend.

## 6. Testes e validação
- Backend: executar testes com Maven/Gradle conforme wrapper/configuração do projeto.
- Frontend: executar testes e build definidos em package.json.
- Validar os fluxos ponta a ponta afetados.
- Nunca afirmar que um teste passou sem executá-lo ou consultar o resultado da CI.
- Não usar dados/credenciais reais em testes versionados.

## 7. Segurança
- Nunca commitar `.env`, tokens, senhas, connection strings ou JWT_SECRET.
- Não imprimir segredos nos logs.
- Validar autorização no backend em toda operação protegida.
- Garantir isolamento multi-tenant.
- Em convites, não armazenar token em texto puro.
- Em saldos futuros, usar ledger transacional, idempotência e auditoria.

## 8. Definition of Done
- [ ] Implementação segue a regra de negócio aprovada.
- [ ] Autorização e isolamento multi-tenant foram testados.
- [ ] Testes relevantes foram adicionados e executados.
- [ ] Build/CI estão verdes.
- [ ] Swagger/OpenAPI atualizado.
- [ ] Documentação de produto, API, arquitetura ou roadmap atualizada.
- [ ] PR descreve limitações, riscos e passos de validação.
- [ ] Nenhuma credencial ou dado sensível foi versionado.
