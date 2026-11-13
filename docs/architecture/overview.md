# Arquitetura inicial

## Direção
Começar com um monólito modular organizado por domínios de negócio. Isso reduz custo operacional e complexidade enquanto produto e limites são validados. Serviços separados só devem ser considerados quando houver necessidade comprovada.

## Visão de alto nível
Angular Web/PWA → HTTPS/REST → Java + Spring Boot → PostgreSQL

Redis e um serviço Python para IA poderão ser adicionados quando os casos de uso justificarem.

## Módulos previstos
- identity: usuários, credenciais e perfis.
- access: autenticação, autorização, papéis e permissões.
- tenancy: vínculo entre usuários e restaurantes.
- restaurants: cadastro, perfil e configurações.
- catalog: categorias, produtos, opções, preços e disponibilidade.
- media: imagens e modelos 3D.
- assistant: contexto e orquestração das recomendações.
- loyalty: programas individuais.
- club: carteira e benefícios compartilhados.
- discovery: busca e listagem pública.
- platform: administração global, planos e auditoria.

## Multi-tenancy e autorização
- Todo dado pertencente a um restaurante deve carregar um identificador de tenant.
- O tenant ativo deve ser resolvido e validado no backend, nunca confiado apenas a um ID enviado pelo frontend.
- Consultas e comandos devem aplicar o escopo do tenant.
- Usuários podem ter vínculos e papéis diferentes em restaurantes diferentes.
- Administração global exige permissões explícitas e auditáveis.
- Testes devem comprovar que um restaurante não acessa dados de outro.

A estratégia inicial pode usar banco e schema compartilhados com tenant_id, desde que o isolamento seja aplicado consistentemente. Avaliar Row-Level Security do PostgreSQL como defesa adicional.

## Integridade de fidelidade e benefícios
- Separar carteira individual do restaurante e carteira Boraê Club.
- Registrar lançamentos em ledger imutável; não depender apenas de saldo editável.
- Realizar emissão e resgate em transações atômicas.
- Usar chaves de idempotência para impedir duplicidade.
- Registrar origem, motivo, usuário, estabelecimento e referência de cada movimento.
- Definir expiração, estorno, limites e compensação antes de movimentar valor real.

## IA com limites claros
O backend disponibiliza consultas limitadas ao catálogo. Toda recomendação deve ser validada contra itens, preços e disponibilidade atuais. A IA não executa operações de negócio.

## Mídia e 3D
Imagens e arquivos GLB devem ficar em object storage. O banco guarda metadados, referências, dimensões e vínculo com o item. Validar tipo, tamanho e origem dos arquivos.

## Qualidade
Testes unitários e de integração, testes de autorização e isolamento entre tenants, logs estruturados, auditoria, OpenAPI e CI para build/testes.

## Estrutura planejada
- backend/: API Java/Spring Boot.
- frontend/: aplicação Angular.
- docs/: documentação do produto e arquitetura.