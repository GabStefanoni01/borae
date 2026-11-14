import { BoraeIconName } from '../shared/icon.component';

/**
 * Catálogo de módulos do painel do restaurante.
 *
 * Fonte de verdade da navegação e do status de disponibilidade de cada módulo.
 * O status descreve o MÓDULO na plataforma (não o restaurante nem os dados).
 * Ao implementar um módulo no backend, atualize o status aqui e troque a rota
 * da página planejada pela página real.
 *
 * Referências: docs/ROADMAP.md, docs/SCREEN_FLOWS.md, docs/MODULE_*.md,
 * docs/DECISIONS_COMMERCIAL_AND_OPERATIONS.md (13/11/2026).
 */
export type ModuleStatus = 'available' | 'partial' | 'development' | 'validation' | 'planned';

export const MODULE_STATUS_LABEL: Record<ModuleStatus, string> = {
  available: 'Disponível',
  partial: 'Parcial',
  development: 'Em desenvolvimento',
  validation: 'Em validação',
  planned: 'Em planejamento'
};

export interface DashboardModule {
  id: string;
  label: string;
  icon: BoraeIconName;
  group: string;
  /** Caminho relativo a /app ('' = início). */
  path: string;
  status: ModuleStatus;
  title: string;
  subtitle: string;
  /** Texto de apresentação do recurso (módulos planejados). */
  summary?: string;
  benefit?: string;
  features?: string[];
  /** Decisões de produto já aprovadas, exibidas como informação (não como funcionalidade). */
  decisions?: { title: string; items: string[] };
}

export const MODULE_GROUPS = ['Visão geral', 'Estabelecimento', 'Cardápio', 'Experiência', 'Relacionamento', 'Conta'] as const;

export const DASHBOARD_MODULES: DashboardModule[] = [
  {
    id: 'inicio', label: 'Início', icon: 'home', group: 'Visão geral', path: '', status: 'available',
    title: 'Visão geral', subtitle: 'Bem-vindo ao seu espaço de gestão no Boraê.'
  },
  {
    id: 'restaurante', label: 'Meu restaurante', icon: 'store', group: 'Estabelecimento', path: 'restaurante', status: 'available',
    title: 'Meu restaurante', subtitle: 'Gerencie as informações que representam seu estabelecimento dentro do Boraê.'
  },
  {
    id: 'equipe', label: 'Equipe', icon: 'users', group: 'Estabelecimento', path: 'equipe', status: 'available',
    title: 'Equipe', subtitle: 'Organize as pessoas que participam da gestão do seu restaurante.'
  },
  {
    id: 'cardapios', label: 'Cardápios', icon: 'book-open', group: 'Cardápio', path: 'cardapios', status: 'planned',
    title: 'Cardápios', subtitle: 'Organize as opções do seu restaurante e prepare uma experiência digital para seus clientes.',
    summary: 'Crie cardápios para diferentes momentos e organize seus produtos de acordo com a identidade e a operação do seu estabelecimento.',
    benefit: 'Vários cardápios por restaurante (por exemplo, almoço, jantar ou bebidas), cada um com suas categorias e produtos, publicados quando você decidir.',
    features: [
      'Criar, editar, listar, abrir e arquivar cardápios',
      'Publicar, despublicar e pré-visualizar como consumidor',
      'Estados de publicação: rascunho, publicado e arquivado — todo cardápio novo começa como rascunho',
      'Mais de um cardápio publicado ao mesmo tempo'
    ],
    decisions: {
      title: 'Definições aprovadas',
      items: [
        'No MVP não haverá troca automática de cardápio por horário.',
        'O QR Code principal abrirá a página pública do restaurante, onde o cliente escolhe o cardápio.'
      ]
    }
  },
  {
    id: 'categorias', label: 'Categorias', icon: 'tag', group: 'Cardápio', path: 'categorias', status: 'planned',
    title: 'Categorias', subtitle: 'Organize seu cardápio em seções claras e fáceis de explorar.',
    summary: 'Categorias como entradas, pratos principais, bebidas e sobremesas vão estruturar a navegação de cada cardápio.',
    benefit: 'Clientes encontram o que procuram mais rápido, e você mantém o cardápio organizado.',
    features: ['Listar, criar e renomear categorias', 'Ordenar a sequência de exibição', 'Ativar e desativar categorias'],
    decisions: {
      title: 'Definições aprovadas',
      items: ['Categorias com produtos não poderão ser apagadas sem uma regra de realocação ou arquivamento.']
    }
  },
  {
    id: 'produtos', label: 'Produtos', icon: 'utensils', group: 'Cardápio', path: 'produtos', status: 'planned',
    title: 'Produtos', subtitle: 'Apresente seus pratos com informações claras, ingredientes e opções adicionais.',
    summary: 'Cada produto terá nome, descrição, preço, categoria, imagem, disponibilidade, ingredientes e adicionais.',
    benefit: 'O cliente entende exatamente o que está escolhendo antes de pedir.',
    features: [
      'Busca e filtros por categoria e disponibilidade',
      'Ingredientes cadastrados um a um, indicando quais o cliente pode remover',
      'Adicionais com preço visível no cardápio',
      'Arquivamento e pré-visualização'
    ],
    decisions: {
      title: 'Definições aprovadas',
      items: [
        'Remoções de ingredientes e ponto da carne serão instruções informadas no pedido e apresentadas ao garçom.',
        'Não haverá tamanhos ou combos nesta etapa.',
        'Ingredientes são declarados pelo restaurante; não há formulário de alergênicos nem garantia de ausência de contaminação cruzada.'
      ]
    }
  },
  {
    id: 'qr-code', label: 'QR Code', icon: 'qr-code', group: 'Cardápio', path: 'qr-code', status: 'planned',
    title: 'QR Code', subtitle: 'Prepare uma forma simples de conectar o ambiente físico do seu restaurante à experiência digital do Boraê.',
    summary: 'Com o cardápio publicado, seus clientes poderão acessar a experiência digital do estabelecimento por meio de um QR Code.',
    benefit: 'Acesso imediato ao cardápio na mesa, sem instalar aplicativo.',
    features: ['QR Code principal do restaurante', 'Pré-visualização do destino', 'Download da imagem e formatos de impressão', 'Instruções de utilização'],
    decisions: {
      title: 'Definições aprovadas',
      items: ['Um QR Code por estabelecimento. QR Codes por mesa poderão ser avaliados posteriormente.']
    }
  },
  {
    id: 'experiencias-3d', label: 'Experiências 3D', icon: 'cube', group: 'Experiência', path: 'experiencias-3d', status: 'planned',
    title: 'Experiências 3D', subtitle: 'Prepare seus pratos para uma apresentação mais visual e interativa.',
    summary: 'O Boraê está sendo desenvolvido para permitir a associação de imagens e modelos tridimensionais aos produtos do cardápio, aproximando os consumidores dos detalhes de cada prato.',
    benefit: 'Pratos apresentados com mais detalhe, em 3D e realidade aumentada nos dispositivos compatíveis.',
    features: [
      'Upload e gerenciamento de imagens',
      'Associação de mídia aos produtos',
      'Modelos GLB/glTF com visualização 3D',
      'Realidade aumentada em dispositivos compatíveis, com imagem comum como alternativa'
    ]
  },
  {
    id: 'assistente-ia', label: 'Assistente IA', icon: 'sparkles', group: 'Experiência', path: 'assistente-ia', status: 'planned',
    title: 'Assistente gastronômico', subtitle: 'Uma nova maneira de conectar preferências e possibilidades do cardápio.',
    summary: 'O assistente gastronômico do Boraê está sendo planejado para compreender preferências, intenção de consumo e orçamento, oferecendo recomendações baseadas nos produtos reais do estabelecimento.',
    benefit: 'Clientes encontram opções que combinam com o momento e o orçamento, com justificativa clara.',
    features: [
      'O consumidor informa o que procura, tipo de refeição, orçamento, preferências e ingredientes a evitar',
      'Recomendações com nome, preço, justificativa e acesso aos detalhes do produto',
      'Somente itens reais retornados pelo backend'
    ],
    decisions: {
      title: 'Limites definidos',
      items: [
        'A IA não acessa o banco diretamente, não inventa produtos ou preços e não altera cardápios.',
        'Não realiza pagamentos, não concede pontos e não executa resgates.',
        'Não garante ausência de alérgenos ou contaminação cruzada.'
      ]
    }
  },
  {
    id: 'fidelidade', label: 'Fidelidade', icon: 'award', group: 'Relacionamento', path: 'fidelidade', status: 'planned',
    title: 'Programa de fidelidade', subtitle: 'Construa relacionamentos que continuam além de uma visita.',
    summary: 'Com o programa de fidelidade do Boraê, cada restaurante poderá estruturar suas próprias regras de acúmulo, recompensas e relacionamento com consumidores.',
    benefit: 'Clientes que voltam com mais frequência, com regras definidas por você.',
    features: ['Regras de pontuação e validade', 'Recompensas e resgates', 'Campanhas', 'Histórico de movimentações'],
    decisions: {
      title: 'Definições aprovadas',
      items: [
        'No MVP a fidelidade é individual por restaurante: regras, carteira e histórico próprios.',
        'Cada estabelecimento financia seus benefícios; pontos não são transferidos entre restaurantes.',
        'O saldo será derivado de um registro de transações rastreáveis.'
      ]
    }
  },
  {
    id: 'assinatura', label: 'Plano e assinatura', icon: 'credit-card', group: 'Conta', path: 'assinatura', status: 'planned',
    title: 'Plano e assinatura', subtitle: 'Gerencie a assinatura do seu restaurante e acompanhe as condições de utilização do Boraê.',
    summary: 'O modelo comercial já foi aprovado. A cobrança ainda não está integrada: nenhuma assinatura pode ser contratada por aqui no momento.',
    benefit: 'Plano único, sem comissão por pedido ou transação.',
    features: [
      'Plano atual, situação da assinatura e período de teste',
      'Próxima renovação e forma de pagamento',
      'Histórico de cobranças e notas fiscais',
      'Cancelamento a qualquer momento'
    ],
    decisions: {
      title: 'Condições aprovadas',
      items: [
        '30 dias de teste gratuito; a cobrança só começa após cadastrar a forma de pagamento e confirmar a assinatura.',
        'Pagamento por cartão de crédito ou Pix.',
        'Cancelamento sem multa, com acesso até o fim do período pago.',
        'Tolerância de sete dias em falhas de cobrança.'
      ]
    }
  },
  {
    id: 'integracoes', label: 'Integrações', icon: 'plug', group: 'Conta', path: 'integracoes', status: 'planned',
    title: 'Integrações', subtitle: 'Conecte serviços externos ao Boraê de maneira controlada e segura.',
    summary: 'A API pública do Boraê terá chaves individuais com escopos, limites de uso, auditoria, rotação e revogação, além de webhooks e ambiente Sandbox.',
    benefit: 'Integre o Boraê a outros sistemas com controle e segurança.',
    features: ['API Keys com escopos, limites e expiração', 'Revogação e rotação de chaves', 'Webhooks com histórico de entregas e tentativas', 'Ambiente Sandbox separado de produção'],
    decisions: {
      title: 'Limites aprovados',
      items: [
        '60 requisições por minuto, 10.000 por dia e burst de 10 por segundo.',
        'O segredo da chave será exibido uma única vez, na criação.',
        'Rotação com período de transição de até 24 horas.'
      ]
    }
  },
  {
    id: 'configuracoes', label: 'Configurações', icon: 'sliders', group: 'Conta', path: 'configuracoes', status: 'partial',
    title: 'Configurações', subtitle: 'Administre as preferências e os dados associados à sua conta e ao restaurante.'
  }
];

export const SUBSCRIPTION_PRICES = [
  { period: 'Mensal', price: 'R$ 49,90', discount: '—' },
  { period: 'Trimestral', price: 'R$ 134,73', discount: '10%' },
  { period: 'Anual', price: 'R$ 479,04', discount: '20%' }
];

export function findModule(id: string): DashboardModule | undefined {
  return DASHBOARD_MODULES.find(module => module.id === id);
}

export function moduleByPath(path: string): DashboardModule | undefined {
  return DASHBOARD_MODULES.find(module => module.path === path);
}
