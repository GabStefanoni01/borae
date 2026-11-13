# Produto e regras de negócio\n\n> Especificação detalhada: consulte [PRD.md](PRD.md), [SCREEN_FLOWS.md](SCREEN_FLOWS.md), os documentos de módulo e [OPEN_DECISIONS.md](OPEN_DECISIONS.md).

## 1. Visão

Boraê é uma plataforma gastronômica que aproxima consumidores e restaurantes. A experiência começa no cardápio digital acessado por QR Code e evolui para descoberta de estabelecimentos, visualização 3D/realidade aumentada, recomendações com IA e programas de fidelidade.

O produto deve ser simples para o consumidor e oferecer ferramentas úteis para o restaurante administrar sua presença e relacionamento com clientes.

## 2. Públicos e perfis

### Consumidor
- Acessa cardápios por QR Code ou pela descoberta de restaurantes.
- Consulta pratos, preços, descrições, ingredientes, opções e disponibilidade.
- Poderá receber recomendações personalizadas com base no cardápio real.
- Poderá participar dos programas de fidelidade individuais dos restaurantes. O Boraê Club compartilhado está fora do MVP e poderá ser reavaliado futuramente.

### Restaurante
- Administra dados do estabelecimento, cardápio, produtos e equipe.
- Poderá configurar recursos de IA, experiências 3D/AR, campanhas e fidelidade.
- Consulta informações e indicadores do próprio estabelecimento.

### Equipe do restaurante
A autorização é definida por vínculo com cada restaurante e validada no backend em toda operação privada.
- **OWNER:** controle total do restaurante, incluindo perfil, catálogo, publicação, disponibilidade, equipe, papéis, propriedade e cobrança; também poderá operar pedidos quando esse módulo existir.
- **MANAGER:** administra perfil, catálogo, publicação, disponibilidade, equipe e convites; poderá operar pedidos quando esse módulo existir. Não altera papéis, propriedade ou cobrança. Ao convidar, pode convidar somente STAFF.
- **STAFF:** acesso operacional ao painel, disponibilidade de produtos e pedidos (visualização/atualização de status quando o módulo existir). Não administra perfil, catálogo, publicação, equipe, propriedade ou cobrança.

OWNER pode convidar MANAGER ou STAFF. Um usuário pode ter papéis diferentes em restaurantes diferentes. Permissões futuras de pedidos não significam que pedidos façam parte do MVP.

### Administração global da plataforma
O Boraê terá um administrador principal e equipe de suporte com permissões limitadas. Esses papéis são independentes de OWNER/MANAGER/STAFF dos restaurantes. O suporte acessa apenas dados técnicos, pode suspender restaurantes temporariamente e não acessa dados pessoais, histórico de compras ou dados financeiros. Todas as ações administrativas são auditadas. O administrador principal pode criar outros administradores; somente ele, como administrador original, pode excluí-los.

## 3. Funcionalidades do produto

### 3.1 Cardápio digital — núcleo do produto
O consumidor acessa uma página responsiva do restaurante por QR Code, sem precisar instalar aplicativo.

O cardápio deverá suportar:
- Categorias e ordenação.
- Produtos com nome, descrição, preço, imagens e ingredientes.
- Variações, complementos e adicionais, quando aplicável.
- Disponibilidade do item.
- Identidade visual do restaurante.
- QR Codes associados ao restaurante e, futuramente, às mesas.

Regra: preço e disponibilidade apresentados devem vir dos dados atuais do restaurante. Não inventar itens, preços ou ingredientes.

### 3.2 Visualização 3D e realidade aumentada — planejado
- Associar modelos 3D em formato GLB/glTF a produtos.
- Permitir que o restaurante envie e gerencie os arquivos.
- Calibrar dimensões para aproximar a escala real.
- Exibir modelo 3D e oferecer AR apenas quando o dispositivo/navegador suportar.
- Disponibilizar fallback de imagem e visualização 3D sem AR.

A escala 1:1 só pode ser anunciada quando o modelo tiver dimensões físicas calibradas. Arquivos 3D devem ficar em armazenamento de objetos, não como blobs no PostgreSQL.

### 3.3 Assistente gastronômico com IA — planejado
O consumidor poderá informar vontade, orçamento, tipo de refeição, preferências e ingredientes que deseja evitar. O assistente recomenda somente itens existentes e disponíveis no cardápio do restaurante selecionado.

Regras obrigatórias:
- A IA não acessa o banco diretamente; consulta ferramentas/serviços do backend com escopo do restaurante.
- O backend valida restaurante, produto, preço e disponibilidade.
- A IA não inventa pratos, ingredientes, preços ou promoções.
- A IA não executa pedidos, pagamentos ou alterações sem fluxo explícito e validação do backend.
- Preferências e restrições devem ser tratadas como informação de apoio, não como garantia de segurança alimentar.
- Nunca garantir ausência de alérgenos ou contaminação cruzada. Exibir orientação para confirmar diretamente com o restaurante.

### 3.4 Fidelidade individual do restaurante — planejado
Cada restaurante poderá configurar seu próprio programa:
- Regras de pontuação e conversão.
- Recompensas e resgates.
- Campanhas, validade e expiração.
- Benefícios de aniversário.
- Histórico de pontos e transações.

Pontos de um restaurante são independentes dos pontos de outro restaurante. O restaurante define suas regras dentro dos limites da plataforma.

### 3.5 Boraê Club — evolução futura, fora do MVP
O Boraê Club compartilhado entre restaurantes parceiros não será implementado no MVP. Nesta primeira versão, existem somente programas de fidelidade individuais: cada restaurante define suas próprias regras e recompensas, assume o custo dos benefícios e mantém saldo e histórico independentes. Pontos não podem ser transferidos, convertidos ou resgatados em outro estabelecimento.

O Club poderá ser reavaliado depois da validação do produto e da adesão dos restaurantes. Antes de qualquer implementação, será necessário aprovar modelo financeiro, responsabilidades, contratos, compensação, liquidação, estornos e prevenção a fraude. Não desenvolver carteira compartilhada, saldo Club, resgate cruzado ou liquidação entre parceiros no MVP.

### 3.6 Descoberta de restaurantes — planejado
- Diretório de estabelecimentos.
- Busca por localização, categoria e tipo de culinária.
- Página pública do restaurante e acesso ao cardápio.
- Destaque de parceiros e benefícios disponíveis.
- Favoritos e avaliações podem entrar em etapa posterior.

### 3.7 Painel do restaurante — em evolução
Área autenticada para o estabelecimento administrar seu negócio na plataforma. O escopo deverá crescer por módulos, mantendo isolamento entre restaurantes.

### 3.8 Pedidos e pagamentos — fora do MVP inicial
Carrinho, checkout, integração com POS, pagamento, acompanhamento de pedido e notificações são possibilidades futuras. Não implementar antes de validar a necessidade e definir integrações, taxas, cancelamento, reembolso e responsabilidade operacional.

## 4. Modelo de negócio aprovado — D-18

- SaaS B2B por assinatura fixa; não há comissão por transação.
- Um único plano, com mesmas funcionalidades e preço-base para todos os restaurantes.
- Teste gratuito de 30 dias; a assinatura só começa após o restaurante cadastrar forma de pagamento e confirmar a contratação.
- Periodicidades mensal, trimestral e anual, com descontos progressivos.
- Cancelamento a qualquer momento, sem multa, mantendo acesso até o fim do período pago.
- Cobrança não paga: novas tentativas e avisos, com tolerância de sete dias; depois, suspensão.
- Sem taxa de implantação. Reajustes passam a valer na próxima renovação, com aviso mínimo de 30 dias.
- Formas de pagamento: cartão de crédito e Pix; nota fiscal automática a cada cobrança.
- Programa de indicação gera créditos para abater assinaturas. Créditos não são sacáveis nem transferíveis, expiram em 12 meses e, após cancelamento, só podem ser usados novamente se a assinatura for reativada.

O preço nominal e os percentuais de desconto ainda dependem de definição comercial. O modelo de cobrança, por outro lado, está aprovado e não deve ser tratado como hipótese aberta.

## 5. Princípios de experiência
- Mobile-first: QR Code será frequentemente aberto no celular.
- Não exigir instalação de aplicativo para consultar cardápio.
- Carregamento rápido e fallback para dispositivos sem AR.
- Acessibilidade, legibilidade e navegação simples.
- Não usar IA como substituta de dados confiáveis do restaurante.
- Não mostrar ao consumidor funcionalidades indisponíveis ou dados fictícios.

## 6. Regras multi-tenant
- Um usuário pode estar vinculado a vários restaurantes.
- Um restaurante pode ter vários usuários com papéis distintos.
- Toda consulta ou alteração de dado privado deve ser limitada ao restaurante autorizado.
- O ID do restaurante recebido do cliente não prova autorização.
- Backend valida vínculo ativo, papel e estado do restaurante em cada operação.
- Nunca permitir acesso cruzado a cardápios privados, equipe, configurações ou relatórios de outro estabelecimento.

## 7. Fora do escopo imediato
Não antecipar microserviços, marketplace completo, integração com todos os POS, aplicativo nativo, pagamentos próprios ou programa de pontos compartilhados antes de concluir e validar os fundamentos do produto.


## 8. Decisões aprovadas de descoberta, operação e canais (D-16 a D-20)

### Descoberta
A busca usa localização atual por padrão, com alternativa de busca manual de região. A relevância considera distância, avaliações, popularidade e completude. Filtros: categoria, distância, faixa de preço, avaliação, horário, benefícios de fidelidade e entrega/retirada. Avaliações exigem compra confirmada; incluem nota geral de 1 a 5, comentário opcional e critérios de atendimento, comida e ambiente. Restaurantes podem responder, mas não editar/apagar avaliações. Favoritos, listas personalizadas e compartilhamento por links são suportados. Redes têm página da marca e páginas por unidade, com avaliações próprias.

### Consumidor
Conta global com telefone verificado, senha, nome e e-mail. Login por telefone + senha; SMS para verificação inicial e recuperação/troca de telefone. Uma sessão ativa por vez, bloqueio de 15 minutos após cinco tentativas incorretas, notificações de segurança e exclusão sob solicitação com retenção legal/auditável.

### Administração
Painel global com indicadores de restaurantes, consumidores, compras e receita. Suporte não acessa dados pessoais, histórico de compras ou dados financeiros; pode suspender restaurantes temporariamente. Administradores não editam informações de restaurantes; podem excluí-los mediante confirmação e justificativa. Ações são auditadas. Manutenção programada deve ser comunicada previamente.

### Pedidos e integrações
Não haverá pedidos online nem integração PDV/ERP no lançamento. A API externa será pública para integrações autorizadas, protegida por chaves e permissões, rate limiting e documentação Swagger/OpenAPI. Webhooks terão reenvio automático e haverá Sandbox. Restaurantes e consumidores usarão web responsiva, sem aplicativos nativos no lançamento. Notificações por e-mail e chat de suporte com IA e encaminhamento humano serão oferecidos.


## Decisões comerciais e operacionais aprovadas — 13/11/2026

- Plano único SaaS: R$ 49,90/mês; trimestral R$ 134,73 (10% de desconto); anual R$ 479,04 (20% de desconto).
- Pagamentos/assinaturas: Asaas. NFS-e pelo Asaas somente após validação fiscal da empresa e do município.
- E-mails transacionais: Resend.
- Armazenamento: Cloudflare R2; logos até 5 MB; fotos e arquivos de mídia permitidos até 10 MB; 100 imagens por restaurante; JPEG/PNG/WebP e otimização automática.
- Equipe: sem limite fixo de funcionários; convites sujeitos a proteção contra abuso.
- IA: somente Gemini Flash-Lite nesta fase; 20 interações por consumidor/dia e 100 por restaurante/dia, mais limite de tokens por interação a parametrizar.
- Webhooks/eventos: reprocessamento interno em até sete tentativas progressivas, com idempotência, auditoria e reprocessamento manual.
- Avaliações denunciadas: triagem por IA e decisão final humana.
- Privacidade: retenção por categoria, finalidade e obrigação legal, em conformidade com a LGPD.
- Administração: aplicação global separada, com endpoints administrativos protegidos.
- Monitoramento: painel próprio para métricas de negócio; ferramenta de captura de erros ainda não escolhida (Sentry é candidato); Grafana Cloud opcional.
- Integrações: API Keys individuais, escopos, rate limits, auditoria e revogação.

A especificação detalhada, incluindo ressalvas de implementação e validação fiscal, está em [DECISIONS_COMMERCIAL_AND_OPERATIONS.md](DECISIONS_COMMERCIAL_AND_OPERATIONS.md).
