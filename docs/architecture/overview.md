# Arquitetura do Boraê

## Direção arquitetural

Começar com um **monólito modular** organizado por domínios de negócio. Isso reduz custo operacional e complexidade enquanto produto e limites são validados. Serviços separados só devem ser considerados quando houver necessidade comprovada, não por antecipação.

## Visão de alto nível

`Angular Web/PWA → HTTPS/REST → Java + Spring Boot → PostgreSQL`

Redis e um serviço Python para IA poderão ser adicionados quando os casos de uso justificarem. O serviço de IA deve conversar com o backend por contrato explícito; nunca acessar diretamente o banco de produção.

## Estado atual no código

Implementado:
- Backend Java 21 / Spring Boot 3.
- Frontend Angular 20.
- PostgreSQL com Flyway.
- Autenticação JWT e autorização por sessão.
- Domínio inicial de usuário, restaurante, membership e convite.
- Swagger/OpenAPI.
- Deploy dos serviços web no Render.

Ainda não implementado:
- Catálogo/cardápio.
- Object storage para mídia.
- Visualização 3D/AR.
- Serviço de IA.
- Fidelidade individual e Boraê Club.
- Descoberta pública e pedidos.

## Módulos de domínio

- `auth`: cadastro, login e emissão de token.
- `identity`: usuário, restaurante, vínculo e convite.
- `security`: filtro JWT e autenticação.
- `config`: configuração de segurança e OpenAPI.

Módulos planejados:
- `catalog`: categorias, produtos, opções, preços e disponibilidade.
- `media`: imagens e modelos 3D.
- `assistant`: contexto e orquestração de recomendações.
- `loyalty`: programas individuais.
- `club`: carteira e benefícios compartilhados.
- `discovery`: busca e listagem pública.
- `platform`: administração global, planos e auditoria.

## Multi-tenancy e autorização

- Todo dado pertencente a um restaurante deve carregar um identificador de tenant.
- O tenant ativo deve ser resolvido e validado no backend, nunca confiado apenas a um ID enviado pelo frontend.
- Consultas e comandos devem aplicar o escopo do tenant.
- Usuários podem ter vínculos e papéis diferentes em restaurantes diferentes.
- Administração global exige permissões explícitas e auditáveis.
- Testes devem comprovar que um restaurante não acessa dados de outro.

A direção inicial é banco e schema compartilhados com `restaurant_id` em dados pertencentes ao estabelecimento, desde que o isolamento seja aplicado consistentemente. Avaliar Row-Level Security do PostgreSQL como defesa adicional.

## Integridade de fidelidade e benefícios

- Separar carteira individual do restaurante e carteira Boraê Club.
- Registrar lançamentos em ledger imutável; não depender apenas de saldo editável.
- Realizar emissão e resgate em transações atômicas.
- Usar chaves de idempotência para impedir duplicidade.
- Registrar origem, motivo, usuário, estabelecimento e referência de cada movimento.
- Definir expiração, estorno, limites e compensação antes de movimentar valor real.

## IA com limites claros

O backend disponibiliza consultas limitadas ao catálogo. Toda recomendação deve ser validada contra itens, preços e disponibilidade atuais. A IA não executa operações de negócio, não altera registros e não confirma pedidos/resgates.

## Mídia e 3D

Imagens e arquivos GLB devem ficar em object storage. O banco guarda metadados, referências, dimensões e vínculo com o item. Validar tipo, tamanho e origem dos arquivos. A escala real depende de modelos calibrados com dimensões confiáveis.

## Qualidade

Testes unitários e de integração, testes de autorização e isolamento entre tenants, logs estruturados, auditoria, OpenAPI e CI para build/testes. Segredos nunca devem ser versionados.

## Estrutura do repositório

- `backend/`: API Java/Spring Boot.
- `frontend/`: aplicação Angular.
- `docs/`: documentação do produto e engenharia.
- `.github/workflows/`: CI.
