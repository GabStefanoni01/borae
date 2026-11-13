# Visão do produto

## Problema
A experiência digital de muitos restaurantes é fragmentada: cardápios pouco interativos, pouca informação visual sobre os pratos, descoberta limitada e programas de fidelidade isolados. Restaurantes também precisam de ferramentas simples para atualizar conteúdo e entender o comportamento dos clientes.

## Proposta
Boraê conecta a jornada do consumidor à operação do restaurante. O cliente acessa o cardápio por QR Code ou encontra estabelecimentos na plataforma, explora pratos e opções, visualiza modelos 3D/AR, recebe recomendações contextualizadas e participa dos benefícios disponíveis.

## Públicos
- Consumidores que desejam descobrir restaurantes, entender pratos e aproveitar benefícios.
- Restaurantes que buscam digitalizar o cardápio, melhorar a apresentação dos produtos e criar relacionamento com clientes.
- Operação Boraê, responsável por parceiros, contas, planos, campanhas compartilhadas, suporte e auditoria.

## Capacidades
### Cardápio digital
Categorias e itens com descrição, preço, imagem, disponibilidade, variações, adicionais e observações. Identidade visual por restaurante e QR Codes por estabelecimento; QR por mesa poderá vir depois.

### 3D e realidade aumentada
Associação de arquivos GLB/glTF aos itens, visualização 3D no navegador e AR quando suportada pelo dispositivo. Deve existir uma alternativa quando AR não estiver disponível. Escala real depende de modelos produzidos e calibrados com dimensões confiáveis.

### Assistente gastronômico
Entende intenção, orçamento e preferências; recomenda apenas itens e dados fornecidos pelo backend; considera preço e disponibilidade atuais. O modelo não acessa diretamente o banco e não pode concluir pedidos, alterar preços, conceder pontos ou efetuar resgates.

Informações sobre ingredientes e alergênicos devem ser declaradas e mantidas pelo restaurante. A IA não deve garantir ausência de contaminação cruzada nem substituir a confirmação com o estabelecimento.

### Fidelidade individual
Cada restaurante configura regras próprias de pontuação, recompensas, campanhas, validade e resgate.

### Boraê Club
Rede opcional de benefícios compartilhados entre estabelecimentos participantes. O saldo do Club é distinto do saldo de fidelidade individual. Antes de ativar resgates reais, definir contratos, financiamento dos benefícios, compensação entre parceiros, prevenção a fraudes e auditoria.

### Descoberta e gestão
Diretório de restaurantes com localização, categorias e benefícios. Painel para gestão de cardápio, conteúdo, campanhas e indicadores.

## Fora do primeiro MVP
Pedidos completos, pagamentos, integrações PDV, aplicativos nativos, assinatura premium para consumidores, avaliações públicas e analytics avançado.

## Hipóteses a validar
- Restaurantes percebem valor suficiente para pagar uma assinatura SaaS.
- Clientes usam visualização 3D/AR para decidir o que pedir.
- Recomendações contextualizadas aumentam o interesse pelos itens.
- Benefícios compartilhados geram recorrência sem comprometer a margem dos parceiros.