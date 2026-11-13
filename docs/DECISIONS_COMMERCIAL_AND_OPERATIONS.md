# Decisões comerciais e operacionais — 13/11/2026

**Status:** decisões aprovadas pelo responsável pelo produto.  
**Escopo:** parâmetros comerciais, provedores e políticas técnicas/operacionais definidos após a consolidação D-01 a D-20.  
**Importante:** aprovação documental não significa que os serviços estejam contratados, configurados ou implementados. Validar contas, credenciais, disponibilidade regional, preços vigentes e requisitos legais antes da ativação em produção.

## 1. Monetização e cobrança — complemento de D-18

O Boraê utilizará um plano único SaaS, com as mesmas funcionalidades e preço-base para todos os restaurantes, sem comissão por compra.

| Periodicidade | Preço final | Desconto | Equivalente mensal |
|---|---:|---:|---:|
| Mensal | R$ 49,90 | — | R$ 49,90 |
| Trimestral | R$ 134,73 | 10% | R$ 44,91 |
| Anual | R$ 479,04 | 20% | R$ 39,92 |

Os valores trimestral e anual correspondem ao preço mensal multiplicado pelo período, com o desconto aplicado e arredondamento para centavos. O teste gratuito de 30 dias, cancelamento sem multa e demais regras de D-18 permanecem válidos.

### Provedor de pagamentos

- **Asaas** aprovado como provedor de pagamentos.
- A integração será feita pelo backend, usando API e webhooks, sem expor credenciais no frontend.
- O fluxo deverá cobrir criação e acompanhamento de assinaturas, cobranças, confirmação/recusa, inadimplência, cancelamento e conciliação.
- Cartão e Pix são meios previstos. A modalidade de Pix disponível em cada ciclo (cobrança Pix, Pix Automático ou outra modalidade suportada) deverá respeitar os recursos e requisitos efetivamente habilitados na conta Asaas; não presumir débito automático sem autorização do pagador.
- Antes de produção, validar taxas, limites, requisitos de conta e comportamento de recorrência na documentação e no Sandbox do Asaas.

### Emissão fiscal

- Asaas também foi escolhido para emissão automática de NFS-e relacionada às assinaturas.
- A decisão é **condicionada à validação fiscal da empresa do Boraê**, incluindo CNPJ, inscrição municipal, enquadramento, código de serviço, município competente e disponibilidade/credenciamento do serviço.
- Se a emissão pelo Asaas não atender à situação fiscal da empresa, a integração fiscal deverá ser reavaliada antes da cobrança em produção.
- A emissão e seu status devem ser rastreáveis; falhas fiscais não podem ser silenciosamente tratadas como notas emitidas.

## 2. Mídia e armazenamento — complemento de D-09

Cloudflare R2 permanece como object storage aprovado.

| Tipo | Limite |
|---|---:|
| Logo do restaurante | 5 MB por arquivo |
| Fotos e demais arquivos permitidos no fluxo de mídia | 10 MB por arquivo |
| Quantidade de imagens | 100 por restaurante |

- Formatos de imagem aceitos: JPEG, PNG e WebP.
- Aplicar otimização automática das imagens antes ou durante a persistência, preservando qualidade adequada e orientação.
- Validar tamanho, extensão e MIME type no backend; não confiar apenas na validação do navegador.
- Armazenar binários no R2 e metadados/referências no PostgreSQL.
- O limite de 100 refere-se às imagens do restaurante; não representa limite global de armazenamento da plataforma.
- Outros formatos de mídia (por exemplo, modelos 3D) precisam respeitar a especificação própria do módulo correspondente; não inferir que GLB/glTF esteja liberado pelo conjunto JPEG/PNG/WebP.

## 3. Equipe e convites — complemento de D-08

- Não haverá limite fixo de funcionários/membros por restaurante.
- O envio de convites terá proteção contra abuso: rate limiting, limites operacionais, auditoria e bloqueio temporário quando necessário.
- Os valores numéricos de frequência/limite de convites serão parâmetros técnicos definidos e calibrados na implementação; não foram fixados nesta decisão.
- Permanecem válidas as regras de papéis, expiração, revogação e reenvio de convites já aprovadas em D-07/D-08.

## 4. E-mails transacionais

**Resend** aprovado como serviço de envio de e-mails transacionais do Boraê, incluindo convites, recuperação de senha, confirmações e avisos operacionais/financeiros.

- Integração exclusivamente pelo backend.
- Domínio remetente deverá ser verificado e configurado com os registros de autenticação exigidos pelo provedor.
- Chaves em secret manager/variáveis seguras; nunca no frontend ou no repositório.
- Tratar falhas, limites de envio, bounces e supressões; não considerar e-mail enviado apenas porque a requisição foi aceita.
- Plano e cotas devem ser reavaliados conforme o volume real; nenhuma contratação paga foi aprovada nesta decisão.

## 5. IA — complemento de D-10 e D-11

- Provedor: Google Gemini.
- Modelo escolhido: **Gemini Flash-Lite**, como único modelo para as funcionalidades de IA do Boraê nesta fase.
- Cotas: até 20 interações por consumidor por dia e até 100 interações por restaurante por dia. As duas cotas são aplicadas simultaneamente; atingir qualquer uma bloqueia novas interações no respectivo escopo até a renovação diária.
- Cada interação também terá limite de tokens, a ser definido como parâmetro técnico antes da ativação em produção.
- Backend controla autenticação, cotas, tokens, concorrência, timeouts, falhas e custos; a chave do provedor nunca é exposta ao frontend.
- Registrar consumo, tokens quando disponibilizados, latência, erros e custo estimado por restaurante, respeitando D-11: não persistir conversas/prompts/respostas completas; manter somente métricas e eventos agregados permitidos.
- O identificador exato da versão estável do modelo deve ser configurado e validado no momento da implementação, evitando fixar neste documento um alias que possa ser descontinuado.
- Antes de produção, revisar modalidade de faturamento, limites e termos de tratamento/uso de dados do Google. A escolha do modelo não aprova automaticamente um plano gratuito ou pago.

## 6. Processamento de webhooks e eventos

Foi aprovada a seguinte política de sete tentativas com intervalos progressivos para o **reprocessamento interno de eventos recebidos** quando o processamento falhar:

| Tentativa | Momento |
|---|---:|
| 1ª | Imediata |
| 2ª | 1 minuto após a falha anterior |
| 3ª | 5 minutos após a falha anterior |
| 4ª | 15 minutos após a falha anterior |
| 5ª | 1 hora após a falha anterior |
| 6ª | 6 horas após a falha anterior |
| 7ª | 24 horas após a falha anterior |

- Registrar payload normalizado/identificador, origem, status, número da tentativa, timestamps e erro técnico, minimizando dados pessoais e segredos.
- Garantir idempotência e deduplicação por identificador do evento/operação.
- Após esgotar as tentativas, manter o evento em estado de falha para análise e reprocessamento manual autorizado.
- Validar autenticidade e integridade dos webhooks antes de processá-los.
- **Limite de responsabilidade:** para webhooks enviados por provedores como Asaas, a política de retentativa de entrega do provedor é controlada pelo próprio provedor. Os intervalos acima governam a fila/reexecução interna do Boraê após o recebimento; não substituem nem alteram a política de entrega externa.
- Se o Boraê futuramente enviar webhooks a terceiros, a entrega de saída deverá ter fila, assinatura, idempotência, tentativas e observabilidade próprias, sem assumir automaticamente que o cronograma acima é o contrato externo.

## 7. Denúncias e moderação de avaliações

- Consumidores e restaurantes podem denunciar avaliações conforme as regras de produto.
- IA faz apenas a triagem: classifica motivo, identifica possíveis violações e sugere prioridade/contexto para análise.
- A decisão final de manter ou remover uma avaliação é sempre humana, por moderador/admin autorizado.
- A IA não remove, oculta nem bloqueia avaliações automaticamente.
- Registrar denúncia, evidências, classificação auxiliar, responsável, decisão, justificativa e histórico de auditoria.

## 8. Privacidade e retenção

- Adotar política de retenção por categoria de dado, finalidade e base legal, respeitando a LGPD e os prazos legais/regulatórios aplicáveis.
- Não existe um prazo universal de retenção para todos os dados.
- Definir por categoria os procedimentos de acesso, exportação, anonimização, exclusão, retenção legal, expurgo de backups e evidências de execução.
- Preservar dados somente enquanto houver finalidade ou obrigação válida; restringir acesso durante retenções legais.
- Prazos operacionais específicos deverão ser documentados na política de retenção e validados juridicamente antes da produção.

## 9. Administração global e monitoramento

- O painel administrativo global será uma **aplicação separada** da aplicação principal do Boraê, com autenticação, autorização e sessão próprias.
- A aplicação administrativa consumirá endpoints internos/administrativos do backend principal, protegidos por permissões específicas; não terá acesso direto ao banco de dados.
- Manter separação entre papéis administrativos globais e papéis de equipe do restaurante (OWNER/MANAGER/STAFF).
- **Sentry** aprovado como ferramenta inicial de captura e acompanhamento de erros, começando no plano gratuito, sujeito às cotas vigentes.
- O painel próprio exibirá métricas de negócio e operação disponíveis ao Boraê (restaurantes, consumidores, compras, assinaturas/receita, uso de IA, pagamentos, webhooks, erros e saúde dos serviços, conforme dados instrumentados).
- Grafana Cloud não será dependência nem contratação inicial obrigatória. Poderá ser avaliado posteriormente para métricas/logs/traces técnicos.
- A construção do painel próprio não substitui logs, alertas, rastreamento de erros nem monitoramento de infraestrutura.

## 10. Integrações externas e API Keys — complemento de D-20

- Cada integração externa terá API Key individual, identificável e revogável.
- Cada chave terá escopos/permissões explícitos e princípio do menor privilégio.
- Aplicar rate limiting e cotas por chave/integração.
- Auditar emissão, uso relevante, falhas, alteração de escopos e revogação.
- Segredos não serão exibidos novamente após criação; armazenar somente material criptograficamente protegido/hash quando compatível com o mecanismo de autenticação.
- Permitir rotação e revogação imediata.
- Não conceder acesso direto ao banco de dados; integrações acessam somente endpoints autorizados.
- OAuth 2.0 não será requisito universal do MVP; poderá ser adotado para integrações que justifiquem fluxos delegados mais complexos.

## 11. Itens técnicos ainda parametrizáveis

As escolhas de produto acima estão aprovadas. Permanecem como parâmetros de implementação, sem bloquear a arquitetura:
- Limite numérico de tokens por interação e orçamento/alertas de IA.
- Frequência exata e janelas de rate limit para convites e API Keys.
- Retenção operacional de logs e métricas por categoria.
- Regras de alerta, limiares de latência/erro e disponibilidade.
- Detalhes de configuração do Sandbox, credenciais, homologação e conciliação do Asaas.
- Configuração fiscal e validação de NFS-e para a empresa e município competentes.
- Versão estável exata do Gemini Flash-Lite no momento da implementação.
- Parâmetros de otimização de imagem e política de formatos de mídia 3D, conforme o módulo.
- Schema físico, índices, constraints, migrações e estratégia de backup/recuperação.

Esses parâmetros devem ser registrados nos documentos técnicos ou de operação correspondentes quando definidos. Não reabrir as escolhas de produto aprovadas sem uma nova decisão explícita.
