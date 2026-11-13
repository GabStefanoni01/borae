# Arquitetura e decisões técnicas

## 1. Direção arquitetural

Começar com um **monólito modular**, organizado por domínios. Não dividir em microserviços agora. Essa escolha reduz custo operacional, complexidade de deploy e esforço de manutenção enquanto o produto e as regras são validados.

Separar módulos por responsabilidade e manter contratos claros. Extrair um serviço independente somente quando houver necessidade técnica/operacional comprovada.

## 2. Stack

| Camada | Tecnologia | Situação |
| --- | --- | --- |
| Frontend web | Angular 20 + TypeScript | Implementado como aplicação inicial |
| Estilos | CSS atual; Tailwind pode ser adotado após decisão | Não assumir Tailwind configurado |
| Backend | Java 21 + Spring Boot | Implementado |
| API | REST versionada em /api/v1 | Implementado |
| Persistência | PostgreSQL + Spring Data JPA | Implementado |
| Migrações | Flyway | Implementado |
| Autenticação | Spring Security + JWT | Implementado |
| Contrato/documentação API | OpenAPI/Swagger | Configurado |
| Hospedagem | Render | Publicado |
| Banco gerenciado | Neon PostgreSQL | Publicado |
| Cache | Redis | Planejado, não obrigatório no MVP inicial |
| IA | Python + FastAPI | Planejado; integrar via backend |
| 3D/AR | GLB/glTF + model-viewer | Planejado |
| Arquivos | Cloudflare R2 (object storage) | Provedor definido para imagens e modelos 3D; PostgreSQL armazena somente metadados e referências |

## 3. Organização por domínio

A estrutura atual inclui o domínio de identidade/autenticação e os recursos iniciais de restaurante. À medida que o produto evoluir, organizar os módulos em domínios como:

- **identity:** contas, autenticação, sessão e autorização.
- **restaurant:** estabelecimento, perfil, membros e configurações.
- **catalog:** categorias, produtos, adicionais, preços e disponibilidade.
- **loyalty:** fidelidade individual do restaurante.
- **club:** possível domínio futuro do Boraê Club; fora do MVP e não deve ser implementado nesta fase.
- **discovery:** diretório e busca de estabelecimentos.
- **recommendation:** integração com o assistente gastronômico.
- **media:** imagens e modelos 3D.
- **platform-admin:** administração global.

Os nomes são uma direção de organização; não criar todos os pacotes vazios agora. Criar cada domínio quando houver funcionalidade real a implementar.

## 4. Frontend
- Angular com componentes, serviços e rotas por funcionalidade.
- Serviços Angular encapsulam chamadas HTTP.
- Interceptor inclui JWT nas requisições protegidas.
- Guard protege rotas autenticadas, mas não substitui autorização no backend.
- Tratar estados de carregamento, erro, vazio e sucesso.
- Evitar colocar regra de negócio crítica apenas no componente.
- Não armazenar segredos no frontend.

A sessão atual usa JWT persistido no localStorage para o MVP. Antes de ampliar a exposição em produção, avaliar estratégia de refresh token e cookie HttpOnly/Secure/SameSite, mitigação de XSS/CSRF, expiração e revogação.

## 5. Backend
- Controllers recebem e validam requisições e delegam aos serviços.
- Services concentram regras de negócio e limites transacionais.
- Repositories encapsulam persistência.
- DTOs definem contratos HTTP; não expor entidades JPA diretamente.
- Usar Bean Validation para validar entrada.
- Usar transações para operações que alterem múltiplas entidades.
- Respostas e erros devem ser consistentes; não revelar detalhes internos ou segredos.
- Documentar endpoints com OpenAPI.

## 6. Banco e migrações
- PostgreSQL é a fonte persistente de verdade.
- Toda mudança de schema deve ser uma nova migração Flyway versionada; não editar migrações já aplicadas.
- Preferir UUID para identificadores públicos.
- Definir constraints e índices no banco para invariantes importantes.
- Não salvar arquivos de imagem/GLB diretamente no banco: armazenar em object storage e persistir metadados/URLs/chaves.
- Não usar dados de um restaurante para responder consultas de outro.

## 7. Segurança e autorização
- JWT identifica o usuário autenticado; o backend resolve a conta ativa.
- Toda operação de restaurante valida vínculo ativo com o restaurante solicitado.
- Papel deve ser validado no backend em cada endpoint.
- Nunca aceitar userId, role ou ownerId do cliente como fonte de autoridade.
- Senhas devem ser armazenadas com hash forte; nunca em texto puro.
- Tokens de convite devem ser aleatórios, expirar e ser armazenados como hash.
- Não registrar tokens, senhas, connection strings ou dados pessoais sensíveis em logs.
- Segredos somente em variáveis de ambiente/secret manager; nunca em Git.
- CORS deve permitir apenas origens explicitamente configuradas.
- Operações de saldo/pontos no futuro devem usar transações, idempotência, trilha de auditoria e proteção contra concorrência.

## 8. IA: limites de integração
A IA será um consumidor de capacidades internas controladas, não uma camada com acesso irrestrito ao banco.
1. Frontend envia contexto e pergunta para o backend.
2. Backend valida identidade, restaurante e escopo.
3. Serviço de IA recebe somente dados necessários e autorizados.
4. Recomendações são vinculadas a IDs de itens reais do catálogo.
5. Backend revalida preço e disponibilidade antes de devolver resposta ou iniciar qualquer ação.

## 9. Infraestrutura e ambientes
- Render hospeda frontend e backend.
- Neon fornece PostgreSQL gerenciado.
- Configurações variam por ambiente; não copiar segredos entre desenvolvimento e produção.
- Variáveis conhecidas do backend incluem DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION_MS e CORS_ALLOWED_ORIGINS. Consultar configuração da aplicação antes de adicionar ou renomear variáveis.
- JWT_SECRET deve ser Base64 e conter pelo menos 32 bytes após decodificação, conforme configuração atual.
- Nunca commitar .env ou credenciais.
- Docker Compose existe para quem possui Docker; não é requisito para desenvolver localmente.

## 10. Qualidade
- Testes unitários para regras de serviço.
- Testes de integração para endpoints, autenticação, autorização e persistência.
- Testes frontend para fluxos críticos.
- CI deve compilar/testar backend e frontend e validar imagens Docker.
- Não considerar funcionalidade concluída apenas porque compila: validar fluxo real e permissões.


## 11. Serviços externos e decisões operacionais aprovadas

- **Pagamentos e assinaturas:** Asaas, integrado somente pelo backend. A emissão de NFS-e pelo Asaas depende de validação fiscal da empresa e do município competente.
- **E-mail transacional:** Resend, com domínio verificado, credenciais no ambiente seguro e tratamento de bounces/falhas.
- **Mídia:** Cloudflare R2; logos até 5 MB, fotos/arquivos de mídia permitidos até 10 MB, até 100 imagens por restaurante, JPEG/PNG/WebP e otimização automática.
- **IA:** Gemini Flash-Lite como único modelo aprovado nesta fase. Cotas simultâneas de 20 interações por consumidor/dia e 100 por restaurante/dia; limite de tokens por interação será parametrizado antes de produção.
- **Erros:** Sentry inicialmente no plano gratuito, respeitando cotas vigentes. Grafana Cloud é opcional e não é dependência inicial.
- **Administração global:** aplicação frontend separada, com autenticação e autorização próprias, consumindo endpoints administrativos do backend principal. Não conceder acesso direto ao banco.
- **Integrações externas:** API Keys individuais com escopos, rate limits, auditoria, rotação e revogação.
- **Convites:** sem limite fixo de membros por restaurante; proteger o envio com rate limiting e controles contra abuso.

## 12. Eventos, webhooks e resiliência

O processamento interno de eventos recebidos usa sete tentativas progressivas: imediata, 1 minuto, 5 minutos, 15 minutos, 1 hora, 6 horas e 24 horas. Após esgotar as tentativas, manter o evento para análise e reprocessamento manual autorizado. Implementar idempotência, deduplicação, autenticação e trilha de auditoria. Para webhooks recebidos de provedores, respeitar a política de entrega/reenvio do próprio provedor; a agenda acima controla o reprocessamento interno do Boraê.

Detalhes e ressalvas constam em [Decisões comerciais e operacionais](DECISIONS_COMMERCIAL_AND_OPERATIONS.md).
