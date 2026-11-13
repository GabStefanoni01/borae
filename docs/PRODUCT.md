# Produto e regras de negócio\n\n> Especificação detalhada: consulte [PRD.md](PRD.md), [SCREEN_FLOWS.md](SCREEN_FLOWS.md), os documentos de módulo e [OPEN_DECISIONS.md](OPEN_DECISIONS.md).

## 1. Visão

Boraê é uma plataforma gastronômica que aproxima consumidores e restaurantes. A experiência começa no cardápio digital acessado por QR Code e evolui para descoberta de estabelecimentos, visualização 3D/realidade aumentada, recomendações com IA e programas de fidelidade.

O produto deve ser simples para o consumidor e oferecer ferramentas úteis para o restaurante administrar sua presença e relacionamento com clientes.

## 2. Públicos e perfis

### Consumidor
- Acessa cardápios por QR Code ou pela descoberta de restaurantes.
- Consulta pratos, preços, descrições, ingredientes, opções e disponibilidade.
- Poderá receber recomendações personalizadas com base no cardápio real.
- Poderá participar de programas de fidelidade e do Boraê Club.

### Restaurante
- Administra dados do estabelecimento, cardápio, produtos e equipe.
- Poderá configurar recursos de IA, experiências 3D/AR, campanhas e fidelidade.
- Consulta informações e indicadores do próprio estabelecimento.

### Equipe do restaurante
- **OWNER:** proprietário/responsável pelo restaurante; controla equipe e convites.
- **MANAGER:** gerente; atualmente pode consultar a equipe, além das permissões operacionais que forem implementadas futuramente.
- **STAFF:** colaborador; papel de equipe com permissões operacionais a definir por módulo.

### Administrador da plataforma
Perfil futuro para operações globais: gestão de restaurantes, usuários, assinaturas, campanhas, transações, auditoria e suporte. Não confundir com OWNER de um restaurante.

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

### 3.5 Boraê Club — planejado
Programa compartilhado de benefícios entre restaurantes parceiros. Deve ser separado da fidelidade individual.

Conceitos:
- Conta de consumidor única no ecossistema.
- Pontos/benefícios compartilhados, sujeitos às regras do Club.
- Participação opcional dos restaurantes.
- Registro auditável de acúmulo, resgate, estorno e liquidação entre parceiros.

Antes de implementar movimentação financeira ou resgates compartilhados, definir regras contratuais, responsabilidade pelo custo do benefício, compensação entre estabelecimentos, prevenção a fraude, idempotência e trilha de auditoria. Não tratar pontos individuais do restaurante como saldo Boraê Club.

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

## 4. Modelo de negócio (hipóteses a validar)
- SaaS B2B com planos para restaurantes.
- Boraê Club e recursos premium para consumidores, mantendo uma experiência básica acessível.
- Campanhas patrocinadas e parcerias comerciais.
- Possíveis taxas por transações ou resgates, apenas após validar operação e viabilidade.

Preços, planos e taxas não estão definidos como regra definitiva. Validar disposição de pagamento com restaurantes e consumidores.

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
