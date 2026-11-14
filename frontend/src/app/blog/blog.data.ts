import { BlogArticle, BlogCategory, BlogCategorySlug } from './blog.models';

export const BORAE_CANONICAL_ORIGIN = 'https://borae-web.onrender.com';

export const BLOG_CATEGORIES: readonly BlogCategory[] = [
  {
    id: 'cat-1',
    slug: 'gestao-e-inovacao',
    name: 'Gestão e inovação para restaurantes',
    shortName: 'Gestão e inovação',
    description:
      'Estratégias práticas de organização operacional, presença digital, estruturação de equipe e adoção consciente de tecnologia para estabelecimentos gastronômicos.',
    audienceLabel: 'Para donos e gestores de restaurantes',
    icon: 'store',
    accentColor: '#FF7200'
  },
  {
    id: 'cat-2',
    slug: 'cardapios-e-experiencia',
    name: 'Cardápios digitais e experiência do cliente',
    shortName: 'Cardápios digitais',
    description:
      'Boas práticas para apresentar pratos, organizar categorias, descrever ingredientes, trabalhar fotografia gastronômica e oferecer leitura fluida por QR Code.',
    audienceLabel: 'Experiência na mesa e no mobile',
    icon: 'qr-code',
    accentColor: '#E85D00'
  },
  {
    id: 'cat-3',
    slug: 'cultura-e-gastronomia',
    name: 'Cultura, curiosidades e gastronomia',
    shortName: 'Cultura gastronômica',
    description:
      'Uma leitura editorial sobre ingredientes brasileiros, tradições regionais, memória afetiva à mesa e a riqueza cultural que inspira cozinhas em todo o país.',
    audienceLabel: 'Para quem aprecia gastronomia',
    icon: 'utensils',
    accentColor: '#C85A00'
  },
  {
    id: 'cat-4',
    slug: 'tecnologia-e-gastronomia',
    name: 'Tecnologia e o futuro da gastronomia',
    shortName: 'Tecnologia e gastronomia',
    description:
      'Análises sobre inteligência artificial, sistemas de recomendação baseados em dados reais, modelos 3D e o papel da engenharia de produto na descoberta gastronômica.',
    audienceLabel: 'Inovação, IA e tendências',
    icon: 'sparkles',
    accentColor: '#FF7200'
  },
  {
    id: 'cat-5',
    slug: 'marketing-e-relacionamento',
    name: 'Marketing, descoberta e relacionamento',
    shortName: 'Marketing e relacionamento',
    description:
      'Caminhos para fortalecer a identidade da marca, estimular recorrência depois da primeira visita, gerenciar reputação e planejar programas de fidelidade sustentáveis.',
    audienceLabel: 'Visibilidade, marca e recorrência',
    icon: 'heart',
    accentColor: '#D95300'
  }
] as const;

export const BLOG_EDITORIAL_AUTHOR = {
  name: 'Redação Editorial Boraê',
  role: 'Equipe de Produto, Gastronomia e Tecnologia do Boraê',
  bio: 'Conteúdo produzido pela equipe do Boraê com foco em boas práticas de gestão gastronômica, experiência do consumidor e transparência tecnológica.'
} as const;

export const BLOG_ARTICLES: readonly BlogArticle[] = [
  // =========================================================================
  // ARTIGO 1 (PILAR 1: GESTÃO E INOVAÇÃO PARA RESTAURANTES)
  // =========================================================================
  {
    id: 'art-1',
    slug: 'presenca-digital-para-restaurantes',
    title: 'Como a presença digital pode transformar a experiência de um restaurante',
    subtitle:
      'Antes mesmo do primeiro pedido, clientes buscam clareza sobre o ambiente, os pratos e o funcionamento da casa. Veja como organizar a identidade digital do seu estabelecimento com consistência.',
    seoTitle: 'Presença digital para restaurantes: por onde começar | Boraê',
    description:
      'Entenda como uma presença digital bem estruturada ajuda restaurantes a apresentar seus pratos, comunicar sua identidade e melhorar a experiência dos clientes.',
    excerpt:
      'A experiência de um restaurante começa muito antes de o cliente se sentar à mesa. Entenda como informações claras, fotografias autênticas e cardápios organizados fortalecem a percepção de valor.',
    category: 'gestao-e-inovacao',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: true,
    coverImage: 'assets/images/blog/borae-destaque-editorial.jpg',
    coverWidth: 1280,
    coverHeight: 720,
    coverAlt: 'Identidade oficial do Boraê sobre fundo claro representando presença digital gastronômica',
    imageCredit: {
      label: 'Identidade Visual: Acervo Oficial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/presenca-digital-para-restaurantes`,
    tags: [
      'Presença digital',
      'Gestão de restaurantes',
      'Experiência do cliente',
      'Cardápio digital',
      'Identidade gastronômica'
    ],
    relatedArticles: [
      'organizacao-de-equipe-e-papeis-no-restaurante',
      'como-criar-um-cardapio-digital',
      'fidelizacao-de-clientes-em-restaurantes'
    ],
    internalLinks: [
      {
        label: 'Como organizar funções e permissões da equipe no restaurante',
        description: 'Aprenda a distribuir responsabilidades entre proprietário, gerência e operação.',
        routerLink: '/blog/organizacao-de-equipe-e-papeis-no-restaurante'
      },
      {
        label: 'Guia completo de cardápio digital para restaurantes',
        description: 'Veja como estruturar categorias, descrições, imagens e leitura mobile por QR Code.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      },
      {
        label: 'Estratégias de fidelização além da primeira visita',
        description: 'Como construir memória de marca e incentivar o retorno consistente dos clientes.',
        routerLink: '/blog/fidelizacao-de-clientes-em-restaurantes'
      }
    ],
    externalSources: [
      {
        title: 'Central da Pesquisa Google: Guia de início de SEO',
        publisher: 'Google Search Central',
        url: 'https://developers.google.com/search/docs/fundamentals/seo-starter-guide',
        note: 'Diretrizes oficiais sobre clareza de informações, títulos descritivos e estrutura de páginas para buscadores.'
      },
      {
        title: 'Abrasel: Panorama e gestão de bares e restaurantes no Brasil',
        publisher: 'Associação Brasileira de Bares e Restaurantes (Abrasel)',
        url: 'https://abrasel.com.br/',
        note: 'Referência setorial sobre profissionalização da operação e jornada de consumo fora do lar.'
      }
    ],
    cta: {
      audience: 'restaurants',
      eyebrow: 'PARA RESTAURANTES E GESTORES',
      title: 'Seu restaurante merece uma presença digital à altura da experiência que oferece.',
      description:
        'Seu restaurante tem uma história, uma identidade e pratos que merecem ser apresentados com cuidado. O Boraê está desenvolvendo uma plataforma para aproximar restaurantes e consumidores através de experiências digitais mais completas.',
      buttonLabel: 'Conheça o Boraê para restaurantes',
      routerLink: '/auth',
      secondaryLabel: 'Conhecer a plataforma',
      secondaryRouterLink: '/',
      secondaryFragment: 'restaurantes'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'experiencia-comeca-antes-da-visita',
        text: '1. Introdução: a experiência do restaurante começa antes da primeira visita'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'A experiência de um restaurante não começa apenas quando o cliente se senta à mesa. Muitas vezes, ela começa muito antes: na pesquisa por um lugar para conhecer, na primeira imagem de um prato ou na facilidade de encontrar informações sobre o estabelecimento.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Em um cenário cada vez mais conectado, a presença digital passou a fazer parte da maneira como restaurantes apresentam sua identidade e se aproximam de novos consumidores.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Mais do que estar presente na internet, o desafio está em oferecer informações claras, uma apresentação consistente e uma experiência que represente a qualidade do estabelecimento.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'o-que-significa-presenca-digital',
        text: '2. O que significa ter presença digital na gastronomia'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Durante muito tempo, ter presença digital no setor de alimentação era visto apenas como manter um perfil ativo em redes sociais. Embora as redes cumpram um papel relevante de inspiração, elas raramente organizam de forma estruturada aquilo que o consumidor precisa decidir em poucos segundos: onde o restaurante fica, em quais horários atende, qual é a proposta culinária e o que realmente consta no cardápio do dia.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Construir uma presença digital madura significa reunir, em pontos de contato confiáveis, a identidade pública do estabelecimento: nome fantasia claro, categoria gastronômica bem definida, endereço físico verificável, horários de funcionamento atualizados por dia da semana e canais diretos de contato.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'por-que-informacoes-claras-fazem-diferenca',
        text: '3. Por que informações claras fazem diferença na decisão do cliente'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Imagine um consumidor planejando um jantar em família ou procurando um almoço executivo próximo ao trabalho. Quando ele encontra um arquivo PDF antigo que exige zoom constante na tela do celular, horários contraditórios ou preços desatualizados, a insegurança gerada pode levá-lo a escolher outro estabelecimento.'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Previsibilidade de horário e endereço:',
            segments: [
              {
                text: 'informar com precisão os dias e turnos de atendimento evita deslocamentos frustrados e reduz dúvidas repetitivas no atendimento.'
              }
            ]
          },
          {
            title: 'Transparência sobre o que é servido:',
            segments: [
              {
                text: 'apresentar ingredientes, adicionais e disponibilidade atualizada transmite respeito ao tempo e às expectativas do cliente.'
              }
            ]
          },
          {
            title: 'Alinhamento de expectativa de preço e ocasião:',
            segments: [
              {
                text: 'quando o cliente compreende previamente a proposta da casa, seja um bistrô autoral, uma hamburgueria artesanal ou uma casa de massas, a visita ocorre com muito mais satisfação.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-apresentar-melhor-o-cardapio',
        text: '4. Como apresentar melhor o cardápio em ambientes digitais'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'O cardápio é o principal ponto de encontro entre a cozinha e o salão. Em vez de arquivos estáticos difíceis de atualizar, a transição para '
          },
          {
            text: 'cardápios digitais estruturados e responsivos',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ' permite que o restaurante separe suas ofertas por ocasião, como Almoço, Jantar ou Carta de Bebidas, mantendo o controle sobre o que está disponível em tempo real.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/organizacao-cozinha-apresentacao-pratos.jpg',
        alt: 'Finalização de prato na bancada de um restaurante com atenção aos detalhes visuais',
        width: 1200,
        height: 675,
        caption:
          'A apresentação digital deve refletir o mesmo cuidado dedicado à montagem dos pratos na cozinha.',
        credit: {
          label: 'Fotografia: Acervo Editorial Boraê',
          photographer: 'Acervo Boraê',
          sourceName: 'Boraê'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'papel-das-imagens-e-descricoes',
        text: '5. O papel das imagens e descrições na percepção de valor'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Na gastronomia, a decisão passa pelos sentidos. Uma fotografia fiel ao prato real, com iluminação natural e foco na textura dos ingredientes, combinada a uma descrição objetiva, ajuda até mesmo clientes indecisos a escolherem com confiança.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Mais importante do que adjetivos exagerados é informar a composição real do prato: quais ingredientes acompanham a receita, quais adicionais opcionais estão disponíveis e como o prato é preparado. Essa organização também prepara o terreno para novas frentes tecnológicas, como explicamos em nosso artigo sobre '
          },
          {
            text: 'inteligência artificial aplicada à descoberta gastronômica',
            routerLink: '/blog/inteligencia-artificial-na-gastronomia'
          },
          {
            text: '.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'tecnologia-aproxima-restaurante-e-consumidor',
        text: '6. Como a tecnologia pode aproximar restaurante e consumidor'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Tecnologia gastronômica não deve afastar a hospitalidade humana; pelo contrário, seu papel é liberar a equipe de ruídos operacionais para que o atendimento no salão seja mais atencioso. Quando o consumidor escaneia um QR Code na mesa e encontra imediatamente um cardápio legível no celular, sem precisar baixar aplicativos, a jornada flui com naturalidade.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Além disso, uma presença digital bem cuidada cria pontes para o pós-visita, apoiando '
          },
          {
            text: 'estratégias de relacionamento e fidelização de clientes',
            routerLink: '/blog/fidelizacao-de-clientes-em-restaurantes'
          },
          {
            text: ' que valorizam quem frequenta a casa regularmente.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'etapas-para-organizar',
        text: '7. Quais etapas um restaurante pode começar a organizar hoje'
      },
      {
        type: 'list',
        ordered: true,
        items: [
          {
            title: 'Consolidar a identidade básica:',
            segments: [
              {
                text: 'revisar nome público, descrição concisa da cozinha, endereço completo, contatos e horários de funcionamento por dia da semana.'
              }
            ]
          },
          {
            title: 'Estruturar o catálogo por cardápios e categorias:',
            segments: [
              {
                text: 'agrupar entradas, pratos principais, sobremesas e bebidas em uma ordem intuitiva de leitura.'
              }
            ]
          },
          {
            title: 'Padronizar fichas de produtos:',
            segments: [
              {
                text: 'listar os ingredientes principais de cada item, revisar preços e definir possíveis adicionais pagos com clareza.'
              }
            ]
          },
          {
            title: 'Selecionar fotografias autênticas:',
            segments: [
              {
                text: 'priorizar imagens reais dos pratos carros-chefe em boa resolução e formatos leves para carregamento rápido no celular.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-o-borae-contribui',
        text: '8. Como o Boraê pretende contribuir para essa transformação'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'O Boraê está sendo construído passo a passo para oferecer aos restaurantes uma base digital sólida e acessível. Na etapa atual da plataforma, proprietários e gestores já contam com estrutura de autenticação, criação de restaurante, gestão de equipe por papéis (proprietário, gerente e colaborador) e configuração do perfil público do estabelecimento, com horários de funcionamento e identidade visual.'
          }
        ]
      },
      {
        type: 'callout',
        eyebrow: 'TRANSPARÊNCIA DE PRODUTO',
        title: 'Evolução incremental guiada pelo roadmap',
        text: 'O módulo completo de catálogo e cardápios digitais por QR Code é a próxima prioridade central do roadmap do Boraê, preparando a base de dados estruturada que futuramente apoiará descoberta, fidelidade individual e recursos de inteligência gastronômica.',
        tone: 'warm'
      }
    ]
  },

  // =========================================================================
  // ARTIGO 2 (PILAR 2: CARDÁPIOS DIGITAIS E EXPERIÊNCIA DO CLIENTE)
  // =========================================================================
  {
    id: 'art-2',
    slug: 'como-criar-um-cardapio-digital',
    title: 'Cardápio digital: como apresentar seus pratos de forma mais clara e atrativa',
    subtitle:
      'Um bom cardápio digital vai muito além de substituir o papel por uma tela: ele organiza categorias, valoriza fotografias, detalha ingredientes e facilita a escolha no celular.',
    seoTitle: 'Cardápio digital para restaurantes: guia completo | Boraê',
    description:
      'Descubra como estruturar um cardápio digital com boas imagens, descrições claras, categorias organizadas e uma experiência simples para o consumidor.',
    excerpt:
      'Substituir um PDF estático por um cardápio digital responsivo melhora a leitura no smartphone, dá transparência aos ingredientes e facilita atualizações do dia a dia.',
    category: 'cardapios-e-experiencia',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 8,
    featured: false,
    coverImage: 'assets/images/blog/cardapio-digital-experiencia-cliente.webp',
    coverSrcset:
      'assets/images/blog/cardapio-digital-experiencia-cliente-640w.webp 640w, assets/images/blog/cardapio-digital-experiencia-cliente.webp 1280w',
    coverSizes: '(max-width: 768px) 100vw, 1120px',
    coverWidth: 1280,
    coverHeight: 720,
    coverAlt: 'Chef em cozinha moderna preparando refeição com ingredientes frescos organizados na bancada',
    imageCredit: {
      label: 'Fotografia: Jonathan Borba / Pexels',
      photographer: 'Jonathan Borba',
      sourceName: 'Pexels',
      sourceUrl:
        'https://www.pexels.com/photo/professional-chef-preparing-meal-in-modern-kitchen-28703283/'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/como-criar-um-cardapio-digital`,
    tags: [
      'Cardápio digital',
      'QR Code',
      'Experiência mobile',
      'Fotografia gastronômica',
      'Engenharia de cardápio'
    ],
    relatedArticles: [
      'fotografia-gastronomica-e-apresentacao-de-pratos',
      'presenca-digital-para-restaurantes',
      'inteligencia-artificial-na-gastronomia'
    ],
    internalLinks: [
      {
        label: 'Fotografia gastronômica para cardápios: guia prático',
        description: 'Como iluminar e apresentar pratos reais sem distorcer a expectativa do cliente.',
        routerLink: '/blog/fotografia-gastronomica-e-apresentacao-de-pratos'
      },
      {
        label: 'Presença digital para restaurantes: por onde começar',
        description: 'Como alinhar perfil, horários e identidade visual antes de publicar o cardápio.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      },
      {
        label: 'Inteligência artificial e cardápios estruturados',
        description: 'Veja como descrições e ingredientes bem cadastrados ajudam sistemas de recomendação.',
        routerLink: '/blog/inteligencia-artificial-na-gastronomia'
      }
    ],
    externalSources: [
      {
        title: 'Web.dev: Diretrizes de usabilidade, imagens responsivas e acessibilidade web',
        publisher: 'Google Chrome Developers / web.dev',
        url: 'https://web.dev/learn/design/',
        note: 'Boas práticas técnicas para leitura mobile, contraste tipográfico e carregamento rápido de imagens.'
      },
      {
        title: 'W3C WAI: Fundamentos de acessibilidade para interfaces móveis e web',
        publisher: 'World Wide Web Consortium (W3C)',
        url: 'https://www.w3.org/WAI/fundamentals/accessibility-intro/',
        note: 'Padrões internacionais para garantir que qualquer pessoa consiga navegar e ler conteúdos digitais.'
      }
    ],
    cta: {
      audience: 'restaurants',
      eyebrow: 'EXPERIÊNCIA DO CARDÁPIO AO SALÃO',
      title: 'Transforme a maneira como seu restaurante apresenta seus pratos.',
      description:
        'O Boraê tem o cardápio digital responsivo por QR Code como núcleo da sua arquitetura de produto. Cadastre seu estabelecimento e acompanhe a evolução das ferramentas de catálogo.',
      buttonLabel: 'Conheça a proposta do Boraê',
      routerLink: '/auth',
      secondaryLabel: 'Ver visão do produto',
      secondaryRouterLink: '/',
      secondaryFragment: 'como-funciona'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'o-que-e-cardapio-digital',
        text: '1. O que é um cardápio digital de verdade'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Muitos estabelecimentos deram o primeiro passo rumo ao digital disponibilizando uma foto ou um arquivo PDF do cardápio impresso. Embora quebre um galho inicial, um PDF estático não se adapta à tela vertical do smartphone, obriga o cliente a ampliar a tela continuamente e exige gerar um novo arquivo toda vez que um ingrediente muda ou um item se esgota.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Um verdadeiro cardápio digital é uma página web responsiva, leve e organizada em dados estruturados. Nela, cada categoria, prato, descrição, lista de ingredientes, adicional e status de disponibilidade existe de forma independente, oferecendo leitura imediata para o consumidor e agilidade operacional para o gestor, fortalecendo a '
          },
          {
            text: 'presença digital para restaurantes',
            routerLink: '/blog/presenca-digital-para-restaurantes'
          },
          {
            text: '.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'por-que-organizacao-importa',
        text: '2. Por que a organização visual e informacional importa'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Quando um cliente abre o cardápio à mesa, ele geralmente está conversando com outras pessoas e deseja encontrar rapidamente aquilo que procura. Se todas as opções de almoço, jantar, sobremesas e bebidas estiverem misturadas em uma única lista longa, a fadiga de escolha aumenta.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Separar a oferta em cardápios distintos quando necessário, mantendo por exemplo um cardápio de Almoço, um de Jantar e uma Carta de Bebidas, permite que o consumidor navegue apenas pelo que faz sentido para aquele momento.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-escrever-descricoes-melhores',
        text: '3. Como escrever descrições melhores para os seus pratos'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Uma boa descrição gastronômica responde a três perguntas rápidas do cliente: o que é o prato, como ele é preparado e o que o acompanha. Textos excessivamente longos dificultam a leitura em telas pequenas, enquanto nomes enigmáticos sem explicação geram insegurança.'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Comece pelo elemento principal e pelo método de preparo:',
            segments: [
              {
                text: 'por exemplo, "Filé de peixe grelhado na manteiga de ervas" comunica imediatamente textura e sabor.'
              }
            ]
          },
          {
            title: 'Destaque acompanhamentos e molhos:',
            segments: [
              {
                text: 'informe se o prato inclui purê rústico, legumes tostados ou arroz cremoso.'
              }
            ]
          },
          {
            title: 'Evite jargões sem contexto:',
            segments: [
              {
                text: 'quando utilizar termos técnicos ou regionais, complemente com os ingredientes que compõem a receita, valorizando inclusive a riqueza da '
              },
              {
                text: 'diversidade da gastronomia brasileira',
                routerLink: '/blog/diversidade-da-gastronomia-brasileira'
              },
              {
                text: '.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'importancia-de-boas-fotografias',
        text: '4. A importância de boas fotografias gastronômicas'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Fotografias reais ajudam o consumidor a dimensionar porções, reconhecer o ponto de preparo e sentir confiança no pedido. É fundamental, porém, que a imagem represente fielmente o prato que chega à mesa: fotos genéricas de bancos de imagem que não correspondem à receita real quebram a confiança do cliente. Você pode aprofundar esse assunto em nosso guia sobre '
          },
          {
            text: 'fotografia gastronômica e apresentação de pratos',
            routerLink: '/blog/fotografia-gastronomica-e-apresentacao-de-pratos'
          },
          {
            text: '.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/ambiente-restaurante-hospitalidade-mesa.jpg',
        alt: 'Salão de restaurante iluminado preparado para receber clientes com conforto',
        width: 1200,
        height: 675,
        caption:
          'Coerência entre o ambiente físico do restaurante e a apresentação visual do cardápio digital reforça a identidade da casa.',
        credit: {
          label: 'Fotografia: Acervo Editorial Boraê',
          photographer: 'Acervo Boraê',
          sourceName: 'Boraê'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'organizar-categorias-e-produtos',
        text: '5. Como organizar categorias e produtos com fluidez'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Dentro de cada cardápio, as categorias funcionam como capítulos. Uma sequência natural, iniciando com Entradas e Para Compartilhar, seguida por Pratos Principais, Guarnições, Sobremesas e Bebidas, permite que o cliente salte direto para a seção desejada por meio de uma barra de navegação fixa no topo da tela.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'ingredientes-adicionais-disponibilidade',
        text: '6. Ingredientes, adicionais e controle de disponibilidade'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Listar os ingredientes individuais de cada prato (como pão brioche, blend bovino, queijo prato, cebola caramelizada e tomate) traz transparência imediata para quem deseja evitar determinado componente. Da mesma forma, exibir adicionais opcionais com seus respectivos acréscimos de preço evita surpresas na conta.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Outro ponto decisivo na operação diária é separar publicação de disponibilidade: se um prato acabou durante o movimento de sábado à noite, o colaborador da operação deve conseguir marcá-lo rapidamente como indisponível sem precisar excluir o item do catálogo.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'experiencia-em-dispositivos-moveis',
        text: '7. Experiência em dispositivos móveis e acessibilidade'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'A quase totalidade dos acessos a cardápios por QR Code ocorre pelo navegador do celular, muitas vezes em ambientes com iluminação baixa no salão ou sob redes móveis variáveis. Por isso, tipografia legível, alto contraste, botões com área de toque confortável e imagens otimizadas em formatos como WebP são indispensáveis.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'qr-code-e-acesso-ao-cardapio',
        text: '8. QR Code e acesso imediato sem necessidade de aplicativo'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Ninguém quer instalar um aplicativo pesado apenas para consultar os preços de um restaurante. O QR Code posicionado na mesa ou no balcão deve abrir diretamente a página pública do estabelecimento no navegador, com endereço estável que não mude a cada atualização de preço.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'o-que-considerar-ao-escolher-plataforma',
        text: '9. O que considerar ao escolher uma plataforma para o seu cardápio'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Ao avaliar soluções tecnológicas, observe se a ferramenta garante autonomia para sua equipe atualizar itens, se preserva a identidade visual da sua marca e se estrutura os dados do catálogo de forma limpa, preparando o restaurante tanto para '
          },
          {
            text: 'ações de marketing e relacionamento com clientes',
            routerLink: '/blog/fidelizacao-de-clientes-em-restaurantes'
          },
          {
            text: ' quanto para recursos futuros de descoberta.'
          }
        ]
      },
      {
        type: 'callout',
        eyebrow: 'ESTÁGIO REAL NO BORAÊ',
        title: 'Como o cardápio digital está planejado no Boraê',
        text: 'No Boraê, a arquitetura aprovada prevê múltiplos cardápios simultâneos por restaurante, controle de publicação por cardápio, lista de ingredientes por produto, adicionais opcionais com preços transparentes e QR Code apontando para a página pública do estabelecimento. O módulo completo de catálogo está em evolução conforme a sequência oficial do nosso roadmap.',
        tone: 'warm'
      }
    ]
  },

  // =========================================================================
  // ARTIGO 3 (PILAR 3: CULTURA, CURIOSIDADES E GASTRONOMIA)
  // =========================================================================
  {
    id: 'art-3',
    slug: 'diversidade-da-gastronomia-brasileira',
    title: 'Por que a gastronomia brasileira é tão diversa? Uma viagem pelos sabores do país',
    subtitle:
      'Dos biomas amazônicos aos pampas do Sul, passando pelo sertão, pelo litoral e pelas serras: entenda como território, história e criatividade moldam os pratos que contam quem somos.',
    seoTitle: 'Gastronomia brasileira: sabores e tradições regionais | Boraê',
    description:
      'Conheça a diversidade da gastronomia brasileira e descubra como ingredientes, tradições e influências regionais ajudam a construir a identidade culinária do país.',
    excerpt:
      'Não existe apenas uma cozinha brasileira, mas um mosaico vivo de cozinhas regionais. Descubra como ingredientes nativos, saberes tradicionais e restaurantes contemporâneos mantêm essa história viva.',
    category: 'cultura-e-gastronomia',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/gastronomia-brasileira-pratos-regionais.webp',
    coverSrcset:
      'assets/images/blog/gastronomia-brasileira-pratos-regionais-640w.webp 640w, assets/images/blog/gastronomia-brasileira-pratos-regionais.webp 1280w',
    coverSizes: '(max-width: 768px) 100vw, 1120px',
    coverWidth: 1280,
    coverHeight: 720,
    coverAlt: 'Cozinheiro preparando prato artesanal com ingredientes frescos em cozinha de restaurante',
    imageCredit: {
      label: 'Fotografia: Alex Urezkov / Pexels',
      photographer: 'Alex Urezkov',
      sourceName: 'Pexels',
      sourceUrl: 'https://www.pexels.com/photo/man-preparing-food-in-a-restaurant-19300180/'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/diversidade-da-gastronomia-brasileira`,
    tags: [
      'Gastronomia brasileira',
      'Culinária regional',
      'Cultura alimentar',
      'Ingredientes brasileiros',
      'Experiências gastronômicas'
    ],
    relatedArticles: [
      'cafe-pao-de-queijo-e-hospitalidade-brasileira',
      'como-criar-um-cardapio-digital',
      'presenca-digital-para-restaurantes'
    ],
    internalLinks: [
      {
        label: 'Do café coado ao pão de queijo: a cultura da hospitalidade à mesa',
        description: 'Conheça as tradições brasileiras de receber bem e os saberes artesanais reconhecidos no país.',
        routerLink: '/blog/cafe-pao-de-queijo-e-hospitalidade-brasileira'
      },
      {
        label: 'Como apresentar pratos e ingredientes em um cardápio digital',
        description: 'Valorize a origem dos ingredientes e o preparo artesanal nas descrições do seu menu.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      },
      {
        label: 'A importância da presença digital para restaurantes autorais e tradicionais',
        description: 'Como comunicar a identidade cultural da sua casa para novos consumidores.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      }
    ],
    externalSources: [
      {
        title: 'IPHAN: Patrimônio Cultural Imaterial e Sistemas Culinários Brasileiros',
        publisher: 'Instituto do Patrimônio Histórico e Artístico Nacional (IPHAN)',
        url: 'http://portal.iphan.gov.br/pagina/detalhes/234',
        note: 'Registros oficiais de bens culturais imateriais relacionados a saberes e modos de fazer da culinária no Brasil.'
      },
      {
        title: 'Guia Alimentar para a População Brasileira (2ª edição)',
        publisher: 'Ministério da Saúde do Brasil',
        url: 'https://www.gov.br/saude/pt-br/assuntos/saude-brasil/publicacoes-para-promocao-a-saude/guia_alimentar_populacao_brasileira_2ed.pdf',
        note: 'Documento público de referência sobre alimentos in natura, preparações culinárias regionais e dimensão cultural da alimentação.'
      }
    ],
    cta: {
      audience: 'consumers',
      eyebrow: 'CULTURA E DESCOBERTA À MESA',
      title: 'Descobrir novos sabores também é descobrir novas histórias.',
      description:
        'Uma boa experiência gastronômica começa com a descoberta. Conheça a proposta do Boraê e acompanhe a evolução de uma nova forma de conectar pessoas e restaurantes.',
      buttonLabel: 'Conheça o universo Boraê',
      routerLink: '/',
      secondaryLabel: 'Explorar mais artigos',
      secondaryRouterLink: '/blog'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'introducao-diversidade-brasileira',
        text: '1. Introdução à diversidade gastronômica brasileira'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Falar sobre gastronomia brasileira no singular é sempre um exercício incompleto. Com dimensões continentais, seis biomas terrestres distintos, extenso litoral atlântico e uma formação histórica marcada pelo encontro de povos indígenas originários, matrizes africanas, tradições ibéricas e fluxos migratórios de diversas partes do mundo, o Brasil abriga dezenas de cozinhas regionais com personalidade própria.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Mais do que uma lista de receitas, a alimentação no país expressa formas de convivência, adaptação ao clima e aproveitamento criativo da biodiversidade local.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'influencia-das-diferentes-regioes',
        text: '2. A influência das diferentes regiões e biomas'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Cada região brasileira desenvolveu repertórios técnicos diretamente ligados à sua geografia. Na região Norte, a floresta e os rios oferecem peixes de água doce e derivados da mandioca brava, como o tucupi e a goma de tapioca, além de ervas como o jambu. No Nordeste, a dualidade entre a brisa litorânea (rica em frutos do mar, leite de coco e azeite de dendê na Bahia) e o semiárido sertanejo (marcado pela carne de sol, manteiga de garrafa, macaxeira e feijão-verde) revela duas vertentes culinárias vibrantes e complementares.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'No Centro-Oeste e no Sudeste, o encontro das rotas tropeiras, das cozinhas de fazenda em fogão a lenha e dos frutos do Cerrado, como o pequi e a castanha-de-baru, convive com grandes centros urbanos cosmopolitas. Já no Sul, as temperaturas mais amenas e a tradição pastoril consolidaram a cultura do assado em brasa, das conservas artesanais e das massas coloniais.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'ingredientes-e-tradicoes',
        text: '3. Ingredientes que conectam o país de ponta a ponta'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Apesar das diferenças regionais, alguns ingredientes funcionam como fios condutores da mesa brasileira. A mandioca (conhecida também como aipim ou macaxeira, dependendo da variedade e da região) dá origem a farinhas de diferentes granulometrias, polvilhos, caldos e bolos. Ao lado dela, a combinação cotidiana de arroz e feijão, variando entre feijão-preto, carioca, vermelho, fradinho ou de-corda, ilustra como uma mesma base nutricional ganha temperos únicos em cada estado.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'pratos-que-representam-identidades',
        text: '4. Saberes e modos de fazer reconhecidos como patrimônio cultural'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'No Brasil, o Instituto do Patrimônio Histórico e Artístico Nacional (IPHAN) reconhece diversos sistemas e saberes culinários como Patrimônio Cultural Imaterial. Exemplos documentados incluem o ofício das baianas de acarajé, o modo artesanal de fazer queijo de Minas, os saberes associados ao tacacá na região amazônica e os sistemas agrícolas tradicionais do Rio Negro, temas que também dialogam com a nossa '
          },
          {
            text: 'cultura do café e da hospitalidade à mesa',
            routerLink: '/blog/cafe-pao-de-queijo-e-hospitalidade-brasileira'
          },
          {
            text: '.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/fidelizacao-clientes-restaurantes-experiencia.jpg',
        alt: 'Mesa posta com pratos compartilhados entre pessoas em um restaurante acolhedor',
        width: 1200,
        height: 675,
        caption:
          'A mesa brasileira é historicamente um espaço de partilha, conversa e celebração coletiva.',
        credit: {
          label: 'Fotografia: Acervo Editorial Boraê',
          photographer: 'Acervo Boraê',
          sourceName: 'Boraê'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'gastronomia-memoria-e-cultura',
        text: '5. A relação entre gastronomia, memória e cultura'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Quando escolhemos um restaurante, raramente buscamos apenas saciedade física. Muitas vezes procuramos um tempero que lembra um almoço de domingo, a curiosidade de experimentar um ingrediente de outra região ou o prazer de apresentar um prato típico a amigos. Essa dimensão afetiva explica por que a hospitalidade e a narrativa por trás de cada receita têm tanto valor.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-restaurantes-preservam-tradicoes',
        text: '6. Como restaurantes preservam e reinventam tradições'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Restaurantes independentes, bistrôs regionais e casas especializadas desempenham um papel cultural importante nas cidades: eles mantêm técnicas tradicionais vivas e apresentam ingredientes brasileiros a novas gerações. Quando esses estabelecimentos cuidam da sua '
          },
          {
            text: 'presença digital',
            routerLink: '/blog/presenca-digital-para-restaurantes'
          },
          {
            text: ' e explicam com clareza os ingredientes em um '
          },
          {
            text: 'cardápio digital bem estruturado',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ', até quem nunca provou determinado ingrediente regional sente-se convidado a experimentar.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'conclusao-gastronomia-experiencia-cultural',
        text: '7. Conclusão: a gastronomia como experiência cultural viva'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Valorizar a diversidade gastronômica brasileira é reconhecer o trabalho diário de produtores, cozinheiros e equipes de salão em todo o país. Na visão do Boraê, a tecnologia existe justamente para iluminar essas histórias, facilitando o encontro entre quem prepara pratos com identidade e quem deseja descobri-los.'
          }
        ]
      }
    ]
  },

  // =========================================================================
  // ARTIGO 4 (PILAR 4: TECNOLOGIA E O FUTURO DA GASTRONOMIA)
  // =========================================================================
  {
    id: 'art-4',
    slug: 'inteligencia-artificial-na-gastronomia',
    title: 'Como a inteligência artificial pode mudar a forma de descobrir pratos e restaurantes',
    subtitle:
      'De buscas rígidas por palavras-chave para diálogos baseados em ocasião, orçamento e preferências: entenda como sistemas de recomendação funcionam e por que dados confiáveis são indispensáveis.',
    seoTitle: 'Inteligência artificial na gastronomia: possibilidades e tendências | Boraê',
    description:
      'Entenda como a inteligência artificial pode apoiar a descoberta gastronômica, interpretar preferências e tornar a experiência digital mais personalizada.',
    excerpt:
      'A inteligência artificial aplicada à gastronomia só gera valor real quando parte de cardápios verificados, respeita limites técnicos e atua como apoio transparente para o consumidor.',
    category: 'tecnologia-e-gastronomia',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 8,
    featured: false,
    coverImage: 'assets/images/blog/inteligencia-artificial-descoberta-gastronomica.jpg',
    coverWidth: 1200,
    coverHeight: 675,
    coverAlt: 'Prato contemporâneo apresentado com iluminação destacada representando inovação gastronômica',
    imageCredit: {
      label: 'Fotografia: Acervo Editorial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/inteligencia-artificial-na-gastronomia`,
    tags: [
      'Inteligência artificial',
      'Tecnologia gastronômica',
      'Sistemas de recomendação',
      'Dados estruturados',
      'Inovação em restaurantes'
    ],
    relatedArticles: [
      'modelos-3d-e-realidade-aumentada-em-cardapios',
      'como-criar-um-cardapio-digital',
      'presenca-digital-para-restaurantes'
    ],
    internalLinks: [
      {
        label: 'Modelos 3D e realidade aumentada na visualização de pratos',
        description: 'Conheça outra frente tecnológica planejada para enriquecer a experiência do cardápio.',
        routerLink: '/blog/modelos-3d-e-realidade-aumentada-em-cardapios'
      },
      {
        label: 'Como estruturar um cardápio digital com ingredientes claros',
        description: 'A base de dados que torna qualquer recomendação tecnológica confiável.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      },
      {
        label: 'Presença digital para restaurantes: guia prático',
        description: 'Por que organizar dados básicos do restaurante vem antes de recursos avançados.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      }
    ],
    externalSources: [
      {
        title: 'Google AI: Princípios para uso responsável de modelos generativos',
        publisher: 'Google AI / Gemini',
        url: 'https://ai.google/responsibility/principles/',
        note: 'Boas práticas de segurança, aterramento em dados reais (grounding) e transparência com o usuário.'
      },
      {
        title: 'Google Search Central: Dados estruturados e compreensão de entidades na web',
        publisher: 'Google Developers',
        url: 'https://developers.google.com/search/docs/appearance/structured-data/intro-structured-data',
        note: 'Como informações padronizadas permitem que sistemas computacionais interpretem atributos com precisão.'
      }
    ],
    cta: {
      audience: 'partners',
      eyebrow: 'TECNOLOGIA E INOVAÇÃO COM PROPÓSITO',
      title: 'A tecnologia pode aproximar pessoas de experiências que combinam com seus gostos.',
      description:
        'Quer construir novas possibilidades para a gastronomia? O Boraê é um projeto em evolução que conecta tecnologia, gastronomia e experiências digitais. Estamos abertos a conversas com pessoas e organizações que compartilham dessa visão.',
      buttonLabel: 'Conheça a visão do Boraê',
      routerLink: '/',
      fragment: 'sobre',
      secondaryLabel: 'Sou um restaurante',
      secondaryRouterLink: '/auth'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'avanco-da-inteligencia-artificial',
        text: '1. O avanço da inteligência artificial no cotidiano digital'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Nos últimos anos, modelos de linguagem e sistemas inteligentes deixaram de ser temas restritos a laboratórios de pesquisa e passaram a auxiliar decisões cotidianas. Na gastronomia, esse movimento desperta uma pergunta importante: como utilizar inteligência artificial para resolver problemas reais de escolha sem perder a autenticidade e a segurança da informação?'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-sistemas-de-recomendacao-funcionam',
        text: '2. Como sistemas de recomendação gastronômica funcionam'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Tradicionalmente, buscar um prato em uma plataforma digital exige saber exatamente o nome da categoria ou do item. Porém, na vida real, as pessoas frequentemente pensam por intenção: "quero algo leve para o jantar até determinado valor", "procuro um prato quente sem laticínios declarados na receita" ou "quero uma entrada para compartilhar em duas pessoas".'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Um assistente gastronômico bem projetado atua traduzindo essa intenção em linguagem natural e cruzando-a com os atributos reais cadastrados no catálogo do restaurante.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'preferencias-e-contexto-de-consumo',
        text: '3. Preferências e contexto de consumo'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'O mesmo consumidor pode buscar experiências completamente diferentes dependendo do dia: um almoço rápido durante a semana, uma refeição reconfortante em um dia chuvoso ou a descoberta de sabores da '
          },
          {
            text: 'gastronomia regional brasileira',
            routerLink: '/blog/diversidade-da-gastronomia-brasileira'
          },
          {
            text: ' no fim de semana. Ao considerar orçamento, ocasião e ingredientes que a pessoa prefere incluir ou evitar, a tecnologia reduz o tempo gasto diante de cardápios extensos.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'recomendacoes-com-cardapios-reais',
        text: '4. Como recomendações devem utilizar cardápios reais'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'O maior risco de sistemas generativos mal arquitetados é a chamada alucinação: sugerir um prato que não existe no restaurante, informar um preço incorreto ou supor ingredientes que não foram declarados pela cozinha. Na engenharia de software aplicada à gastronomia, a regra de ouro é clara: a inteligência artificial nunca deve inventar itens.'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Vínculo estrito com o catálogo publicado:',
            segments: [
              {
                text: 'toda sugestão precisa apontar para um produto real, ativo e disponível no cardápio do restaurante selecionado.'
              }
            ]
          },
          {
            title: 'Validação de preço e disponibilidade no servidor:',
            segments: [
              {
                text: 'quem confirma quanto custa um prato e se ele está disponível no momento é o banco de dados do estabelecimento, nunca o modelo de linguagem isoladamente.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'importancia-de-dados-confiaveis',
        text: '5. A importância de dados confiáveis na base do restaurante'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Nenhuma inteligência artificial consegue compensar um catálogo desatualizado. Por isso, a etapa fundamental continua sendo construir um '
          },
          {
            text: 'cardápio digital bem estruturado',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ', com categorias claras, preços íntegros e lista individual de ingredientes cadastrada pelo próprio restaurante.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/chef-preparando-prato-restaurante.webp',
        srcset:
          'assets/images/blog/chef-preparando-prato-restaurante-640w.webp 640w, assets/images/blog/chef-preparando-prato-restaurante.webp 1280w',
        sizes: '(max-width: 768px) 100vw, 900px',
        alt: 'Chef preparando prato com precisão técnica na bancada da cozinha',
        width: 1280,
        height: 720,
        caption:
          'Dados confiáveis começam na cozinha: somente o restaurante detém a autoridade sobre a composição real dos seus pratos.',
        credit: {
          label: 'Fotografia: Jonathan Borba / Pexels',
          photographer: 'Jonathan Borba',
          sourceName: 'Pexels',
          sourceUrl:
            'https://www.pexels.com/photo/a-chef-preparing-a-dish-in-a-restaurant-26733054/'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'limites-e-cuidados-no-uso-da-ia',
        text: '6. Limites éticos, privacidade e cuidados indispensáveis no uso da IA'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Aplicar inteligência artificial na alimentação exige responsabilidade explícita. Em primeiro lugar, sistemas de recomendação baseiam-se apenas nos ingredientes informados livremente pelo estabelecimento e servem como apoio informativo: eles jamais substituem orientação médica ou nutricional, nem garantem ausência de alergênicos ou de contaminação cruzada na cozinha. Em caso de alergias ou restrições severas, o consumidor deve sempre confirmar diretamente com a equipe do restaurante.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Em segundo lugar, a privacidade precisa ser preservada desde a concepção técnica: não é necessário armazenar o conteúdo textual completo das conversas dos usuários para oferecer boas recomendações.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'futuro-da-descoberta-gastronomica',
        text: '7. O futuro da descoberta gastronômica e experiências visuais'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Ao lado das recomendações contextuais, tecnologias como '
          },
          {
            text: 'modelos 3D e realidade aumentada em cardápios',
            routerLink: '/blog/modelos-3d-e-realidade-aumentada-em-cardapios'
          },
          {
            text: ' em navegadores compatíveis tendem a enriquecer a apresentação de pratos especiais, sempre acompanhadas de fotografias tradicionais como alternativa rápida e acessível.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-o-borae-planeja-explorar',
        text: '8. Como o Boraê planeja explorar esse cenário'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'No Boraê, o assistente gastronômico e a visualização 3D/AR são módulos planejados no nosso roadmap, que serão construídos sobre a fundação do catálogo digital. As diretrizes arquiteturais já aprovadas determinam que a IA operará exclusivamente via backend seguro, validando preços e disponibilidade em itens reais, respeitando cotas de uso e exibindo avisos claros para confirmação de ingredientes junto ao estabelecimento.'
          }
        ]
      }
    ]
  },

  // =========================================================================
  // ARTIGO 5 (PILAR 5: MARKETING, DESCOBERTA E RELACIONAMENTO)
  // =========================================================================
  {
    id: 'art-5',
    slug: 'fidelizacao-de-clientes-em-restaurantes',
    title: 'Como restaurantes podem construir relacionamentos além da primeira visita',
    subtitle:
      'Conquistar um novo cliente exige esforço, mas fazê-lo voltar depende de consistência, hospitalidade e incentivos bem desenhados. Conheça estratégias sustentáveis de fidelização para restaurantes.',
    seoTitle: 'Fidelização de clientes para restaurantes: estratégias | Boraê',
    description:
      'Conheça estratégias para fortalecer o relacionamento entre restaurantes e clientes, criar experiências memoráveis e estimular novas visitas.',
    excerpt:
      'O relacionamento com o cliente não termina quando a conta é encerrada. Descubra como unir qualidade no salão, presença digital consistente e programas de fidelidade individuais.',
    category: 'marketing-e-relacionamento',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/fidelizacao-clientes-restaurantes-experiencia.jpg',
    coverWidth: 1200,
    coverHeight: 675,
    coverAlt: 'Clientes compartilhando refeição em mesa de restaurante com ambiente acolhedor',
    imageCredit: {
      label: 'Fotografia: Acervo Editorial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/fidelizacao-de-clientes-em-restaurantes`,
    tags: [
      'Fidelização de clientes',
      'Marketing para restaurantes',
      'Relacionamento com o cliente',
      'Programas de fidelidade',
      'Recorrência'
    ],
    relatedArticles: [
      'avaliacoes-de-clientes-e-reputacao-digital-restaurantes',
      'presenca-digital-para-restaurantes',
      'como-criar-um-cardapio-digital'
    ],
    internalLinks: [
      {
        label: 'Avaliações de clientes e reputação digital para restaurantes',
        description: 'Como ouvir o retorno real dos consumidores e responder com profissionalismo.',
        routerLink: '/blog/avaliacoes-de-clientes-e-reputacao-digital-restaurantes'
      },
      {
        label: 'Como estruturar a presença digital do seu restaurante',
        description: 'O ponto de partida para que seus clientes encontrem informações atualizadas sobre a casa.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      },
      {
        label: 'Cardápio digital claro e atrativo no celular',
        description: 'Como facilitar a experiência de consulta de pratos em cada nova visita.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      }
    ],
    externalSources: [
      {
        title: 'Sebrae: Atendimento e fidelização de clientes em bares e restaurantes',
        publisher: 'Serviço Brasileiro de Apoio às Micro e Pequenas Empresas (Sebrae)',
        url: 'https://sebrae.com.br/sites/PortalSebrae/segmentos/alimentacao',
        note: 'Guias práticos de gestão de relacionamento, recorrência e sustentabilidade financeira para pequenos negócios de alimentação.'
      },
      {
        title: 'LGPD: Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018)',
        publisher: 'Planalto / Governo Federal',
        url: 'https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm',
        note: 'Marco legal brasileiro sobre consentimento explícito, transparência e respeito às preferências de comunicação do consumidor.'
      }
    ],
    cta: {
      audience: 'restaurants',
      eyebrow: 'RECORRÊNCIA E VALORIZAÇÃO DA MARCA',
      title: 'Construa uma experiência que seus clientes tenham vontade de repetir.',
      description:
        'Seu restaurante tem uma história, uma identidade e pratos que merecem ser apresentados com cuidado. Conheça o Boraê e acompanhe a evolução da plataforma para restaurantes parceiros.',
      buttonLabel: 'Conheça o Boraê para restaurantes',
      routerLink: '/auth',
      secondaryLabel: 'Ver ecossistema Boraê',
      secondaryRouterLink: '/',
      secondaryFragment: 'ecossistema'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'relacionamento-nao-termina-na-visita',
        text: '1. Por que o relacionamento não termina na primeira visita'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'No setor de restaurantes, grande parte do esforço de divulgação costuma se concentrar em atrair pessoas pela primeira vez. No entanto, a sustentabilidade de um estabelecimento gastronômico depende diretamente da recorrência: o cliente que retorna no mês seguinte, que apresenta a casa aos amigos e que escolhe o restaurante para comemorar datas importantes.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Construir esse vínculo começa no cumprimento da promessa feita antes mesmo da visita, desde a '
          },
          {
            text: 'presença digital do restaurante',
            routerLink: '/blog/presenca-digital-para-restaurantes'
          },
          {
            text: ' até a qualidade do prato servido e a cordialidade da equipe.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'experiencia-e-percepcao-de-valor',
        text: '2. Experiência consistente e percepção de valor'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Nenhum programa de benefícios substitui uma operação bem executada. Percepção de valor nasce da soma de detalhes: encontrar o restaurante aberto no horário divulgado, ler um '
          },
          {
            text: 'cardápio digital organizado e sem informações ambíguas',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ', receber os pratos na temperatura correta e sentir que a equipe conhece o que está servindo.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'comunicacao-consistente',
        text: '3. Comunicação consistente antes, durante e depois da refeição'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Quando um restaurante altera seu cardápio sazonal, lança um menu de almoço ou ajusta seu horário de atendimento em feriados, comunicar essas novidades de maneira clara mantém a marca presente na rotina dos clientes sem parecer invasiva.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'identidade-e-memoria-de-marca',
        text: '4. Identidade própria e memória de marca'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Estabelecimentos memoráveis não tentam imitar todos os formatos ao mesmo tempo. Seja valorizando receitas da '
          },
          {
            text: 'cultura gastronômica brasileira',
            routerLink: '/blog/diversidade-da-gastronomia-brasileira'
          },
          {
            text: ', seja aperfeiçoando um cardápio enxuto e autoral, ter uma identidade nítida faz com que o cliente lembre imediatamente da casa quando surge aquela ocasião específica.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/cardapio-digital-experiencia-cliente.webp',
        srcset:
          'assets/images/blog/cardapio-digital-experiencia-cliente-640w.webp 640w, assets/images/blog/cardapio-digital-experiencia-cliente.webp 1280w',
        sizes: '(max-width: 768px) 100vw, 900px',
        alt: 'Chef preparando refeição fresca em cozinha profissional bem estruturada',
        width: 1280,
        height: 720,
        caption:
          'O cuidado na cozinha e a transparência no atendimento são a base de qualquer estratégia duradoura de fidelização.',
        credit: {
          label: 'Fotografia: Jonathan Borba / Pexels',
          photographer: 'Jonathan Borba',
          sourceName: 'Pexels',
          sourceUrl:
            'https://www.pexels.com/photo/professional-chef-preparing-meal-in-modern-kitchen-28703283/'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'programas-de-fidelidade',
        text: '5. Como estruturar programas de fidelidade sustentáveis'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Cartões de papel carimbados no caixa costumam se perder na carteira e não oferecem visibilidade nem para o cliente nem para o gestor. Ao planejar um programa de fidelidade moderno, o restaurante deve ter autonomia para escolher a regra que melhor se adapta à sua margem e ao seu ticket médio:'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Pontuação proporcional ao valor pago:',
            segments: [
              {
                text: 'ideal para estabelecimentos onde o valor da conta varia bastante entre mesas individuais e refeições em grupo.'
              }
            ]
          },
          {
            title: 'Pontuação fixa por visita elegível:',
            segments: [
              {
                text: 'útil para operações de frequência diária ou semanal, como almoços executivos e cafeterias.'
              }
            ]
          },
          {
            title: 'Regras claras de validade e confirmação:',
            segments: [
              {
                text: 'definir prazos transparentes de expiração e garantir que pontos sejam creditados mediante confirmação real da compra evita distorções financeiras.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'beneficios-e-incentivos',
        text: '6. Benefícios e incentivos que valorizam o cliente frequente'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'As melhores recompensas são aquelas que apresentam os pontos fortes da própria cozinha: uma sobremesa autoral, uma entrada para compartilhar na próxima visita ou um benefício especial de aniversário. Dessa forma, o incentivo gera prazer gastronômico real sem comprometer a saúde financeira da operação.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'respeitar-preferencias-do-consumidor',
        text: '7. A importância de respeitar o consentimento e as preferências do consumidor'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Relacionamento saudável exige respeito à privacidade. Em conformidade com as boas práticas e com a legislação brasileira de proteção de dados (LGPD), a participação em um programa de fidelidade deve partir de consentimento explícito do consumidor, separando avisos essenciais sobre a conta de comunicações promocionais opcionais.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-a-tecnologia-apoia-o-relacionamento',
        text: '8. Como a tecnologia pode apoiar o relacionamento no futuro do Boraê'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'No planejamento de produto do Boraê, o módulo de fidelidade foi desenhado para funcionar de maneira individual por restaurante no MVP: cada estabelecimento terá autonomia para definir suas regras, validade de pontos e recompensas, mantendo saldos e históricos independentes. Já a visão do Boraê Club, voltada a benefícios compartilhados entre estabelecimentos, permanece como evolução futura de longo prazo, após a consolidação do catálogo e da fidelidade individual.'
          }
        ]
      }
    ]
  },

  // =========================================================================
  // ARTIGO 6 (NOVO TEMA 1 - PILAR 1: GESTÃO E INOVAÇÃO PARA RESTAURANTES)
  // =========================================================================
  {
    id: 'art-6',
    slug: 'organizacao-de-equipe-e-papeis-no-restaurante',
    title: 'Como organizar funções e permissões da equipe na rotina de um restaurante',
    subtitle:
      'Dividir responsabilidades com clareza entre proprietários, gerentes e colaboradores de operação reduz erros no salão, protege configurações sensíveis e dá agilidade ao atendimento.',
    seoTitle: 'Gestão de equipe em restaurantes: papéis e rotina | Boraê',
    description:
      'Saiba como organizar funções entre proprietário, gerente e equipe operacional do restaurante para manter o cardápio, o perfil e o atendimento alinhados.',
    excerpt:
      'Quando cada integrante da equipe sabe exatamente o que pode atualizar na operação diária, o restaurante ganha velocidade para comunicar horários e disponibilidade sem riscos administrativos.',
    category: 'gestao-e-inovacao',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/organizacao-cozinha-apresentacao-pratos.jpg',
    coverWidth: 1200,
    coverHeight: 675,
    coverAlt: 'Equipe de cozinha organizando o preparo e a apresentação de pratos durante o turno',
    imageCredit: {
      label: 'Fotografia: Acervo Editorial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/organizacao-de-equipe-e-papeis-no-restaurante`,
    tags: [
      'Gestão de equipe',
      'Operação de restaurantes',
      'Liderança gastronômica',
      'Processos operacionais',
      'Controle de acesso'
    ],
    relatedArticles: [
      'presenca-digital-para-restaurantes',
      'como-criar-um-cardapio-digital',
      'avaliacoes-de-clientes-e-reputacao-digital-restaurantes'
    ],
    internalLinks: [
      {
        label: 'Como a presença digital transforma a experiência do restaurante',
        description: 'Veja como manter horários, endereço e perfil público sempre atualizados.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      },
      {
        label: 'Cardápio digital e controle de disponibilidade no dia a dia',
        description: 'Como atualizar itens esgotados sem apagar produtos do catálogo.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      },
      {
        label: 'Avaliações de clientes e melhoria contínua da operação',
        description: 'Como usar a percepção dos clientes para treinar e alinhar a equipe.',
        routerLink: '/blog/avaliacoes-de-clientes-e-reputacao-digital-restaurantes'
      }
    ],
    externalSources: [
      {
        title: 'Sebrae: Gestão de pessoas e liderança em bares e restaurantes',
        publisher: 'Sebrae',
        url: 'https://sebrae.com.br/sites/PortalSebrae/segmentos/alimentacao',
        note: 'Recomendações de organização de turnos, delegação de tarefas e padronização operacional.'
      },
      {
        title: 'Abrasel: Boas práticas operacionais no setor de alimentação fora do lar',
        publisher: 'Abrasel',
        url: 'https://abrasel.com.br/',
        note: 'Referências sobre eficiência no salão e integração entre cozinha e atendimento.'
      }
    ],
    cta: {
      audience: 'restaurants',
      eyebrow: 'GESTÃO ORGANIZADA NO BORAÊ',
      title: 'Estruture seu restaurante e sua equipe com papéis bem definidos.',
      description:
        'No Boraê, a gestão de restaurante e membros da equipe com convites seguros e papéis diferenciados já faz parte da fundação ativa da plataforma.',
      buttonLabel: 'Conheça o Boraê para restaurantes',
      routerLink: '/auth',
      secondaryLabel: 'Ver recursos para restaurantes',
      secondaryRouterLink: '/',
      secondaryFragment: 'restaurantes'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'desafios-da-rotina-operacional',
        text: '1. O desafio de centralizar tudo em uma única pessoa'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Em muitos restaurantes independentes, o proprietário acumula compras, fechamento de caixa, supervisão da cozinha, atendimento a fornecedores e atualização das informações digitais da casa. Quando qualquer ajuste simples, como sinalizar que um prato do almoço esgotou ou ajustar o horário de um feriado, depende exclusivamente do fundador, a operação perde velocidade nos momentos de maior movimento.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Por outro lado, compartilhar uma única senha administrativa entre várias pessoas da equipe gera riscos desnecessários: alterações acidentais em preços, exclusão de categorias inteiras do cardápio ou perda de controle sobre configurações financeiras.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'tres-niveis-de-responsabilidade',
        text: '2. Três níveis naturais de responsabilidade em um restaurante'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Uma operação gastronômica saudável costuma dividir suas responsabilidades digitais em três perfis claros, refletindo o que já acontece fisicamente no salão e no escritório:'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Proprietário (Titular do estabelecimento):',
            segments: [
              {
                text: 'detém a visão estratégica e o controle integral do negócio, incluindo dados institucionais, definição de papéis da liderança, gestão de assinatura e decisões estruturais.'
              }
            ]
          },
          {
            title: 'Gerente (Gestão tática e catálogo):',
            segments: [
              {
                text: 'cuida da atualização do perfil público, horários de funcionamento, organização de cardápios, criação de categorias, revisão de preços e integração de novos colaboradores operacionais.'
              }
            ]
          },
          {
            title: 'Colaborador operacional (Salão e cozinha):',
            segments: [
              {
                text: 'atua no ritmo do turno, precisando de acesso rápido e seguro para consultar o painel e atualizar a disponibilidade de produtos sem risco de alterar preços ou configurações administrativas.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'comunicacao-entre-cozinha-e-salao',
        text: '3. Alinhamento entre cozinha, salão e informação pública'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Um dos maiores pontos de atrito no atendimento ocorre quando o cliente escolhe um prato no cardápio e só descobre que o ingrediente principal acabou depois de chamar o garçom. Quando a equipe operacional tem autonomia supervisionada para atualizar a disponibilidade dos itens, o '
          },
          {
            text: 'cardápio digital do restaurante',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ' passa a refletir a realidade da cozinha em poucos segundos.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/chef-preparando-prato-restaurante.webp',
        srcset:
          'assets/images/blog/chef-preparando-prato-restaurante-640w.webp 640w, assets/images/blog/chef-preparando-prato-restaurante.webp 1280w',
        sizes: '(max-width: 768px) 100vw, 900px',
        alt: 'Chef concentrado na finalização de um prato durante o serviço do restaurante',
        width: 1280,
        height: 720,
        caption:
          'Processos claros permitem que a cozinha mantenha o foco na execução dos pratos enquanto o salão recebe informações precisas.',
        credit: {
          label: 'Fotografia: Jonathan Borba / Pexels',
          photographer: 'Jonathan Borba',
          sourceName: 'Pexels',
          sourceUrl: 'https://www.pexels.com/photo/a-chef-preparing-a-dish-in-a-restaurant-26733054/'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'seguranca-em-convites-e-acessos',
        text: '4. Convites individuais e rotação da equipe com segurança'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'A rotatividade de turnos e equipes faz parte da dinâmica do setor gastronômico. Por isso, cada integrante deve acessar o sistema com sua própria conta individual, vinculada ao seu endereço de e-mail, mediante convite com prazo de validade definido. Quando um colaborador encerra seu ciclo no restaurante, basta desativar seu vínculo individual sem precisar trocar a senha de toda a equipe.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'multiplas-unidades-e-negocios',
        text: '5. Gestão organizada para quem atua em mais de um estabelecimento'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Empreendedores gastronômicos, chefs consultores e gestores frequentemente acompanham mais de uma operação ao mesmo tempo. Manter o isolamento rigoroso dos dados de cada restaurante, permitindo alternar de contexto com uma única conta, evita confusões operacionais e garante que cada marca preserve sua '
          },
          {
            text: 'identidade e presença digital própria',
            routerLink: '/blog/presenca-digital-para-restaurantes'
          },
          {
            text: '.'
          }
        ]
      },
      {
        type: 'callout',
        eyebrow: 'JÁ DISPONÍVEL NO BORAÊ',
        title: 'Fundação de restaurante, equipe e perfil ativa na plataforma',
        text: 'No Boraê, o fluxo de criação de restaurante, convites seguros com expiração de 7 dias e gestão de membros nos papéis de proprietário (OWNER), gerente (MANAGER) e colaborador (STAFF) já está implementado e operacional no painel administrativo.',
        tone: 'warm'
      }
    ]
  },

  // =========================================================================
  // ARTIGO 7 (NOVO TEMA 2 - PILAR 2: CARDÁPIOS DIGITAIS E EXPERIÊNCIA)
  // =========================================================================
  {
    id: 'art-7',
    slug: 'fotografia-gastronomica-e-apresentacao-de-pratos',
    title: 'Fotografia gastronômica para cardápios: como valorizar seus pratos com autenticidade',
    subtitle:
      'Uma imagem bem iluminada desperta o apetite e reduz a indecisão na mesa. Aprenda a planejar fotos reais, nítidas e leves para o cardápio digital do seu restaurante.',
    seoTitle: 'Fotografia de pratos para restaurantes: guia prático | Boraê',
    description:
      'Veja como produzir e selecionar fotografias gastronômicas autênticas para valorizar seus pratos no cardápio digital sem criar falsas expectativas.',
    excerpt:
      'Boas fotografias no cardápio não exigem cenários artificiais: iluminação natural, respeito às cores reais dos ingredientes e proporção adequada transformam a escolha do cliente.',
    category: 'cardapios-e-experiencia',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/chef-preparando-prato-restaurante.webp',
    coverSrcset:
      'assets/images/blog/chef-preparando-prato-restaurante-640w.webp 640w, assets/images/blog/chef-preparando-prato-restaurante.webp 1280w',
    coverSizes: '(max-width: 768px) 100vw, 1120px',
    coverWidth: 1280,
    coverHeight: 720,
    coverAlt: 'Prato recém-preparado recebendo toque final do chef sob luz direcionada',
    imageCredit: {
      label: 'Fotografia: Jonathan Borba / Pexels',
      photographer: 'Jonathan Borba',
      sourceName: 'Pexels',
      sourceUrl: 'https://www.pexels.com/photo/a-chef-preparing-a-dish-in-a-restaurant-26733054/'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/fotografia-gastronomica-e-apresentacao-de-pratos`,
    tags: [
      'Fotografia gastronômica',
      'Apresentação de pratos',
      'Cardápio digital',
      'Visual merchandising',
      'Experiência do consumidor'
    ],
    relatedArticles: [
      'como-criar-um-cardapio-digital',
      'modelos-3d-e-realidade-aumentada-em-cardapios',
      'presenca-digital-para-restaurantes'
    ],
    internalLinks: [
      {
        label: 'Como criar um cardápio digital claro e organizado',
        description: 'Una boas fotografias a descrições objetivas e categorias bem estruturadas.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      },
      {
        label: 'Modelos 3D e realidade aumentada em cardápios',
        description: 'Como fotos otimizadas e modelos tridimensionais se complementam na visualização de pratos.',
        routerLink: '/blog/modelos-3d-e-realidade-aumentada-em-cardapios'
      },
      {
        label: 'Presença digital para restaurantes: guia inicial',
        description: 'Como construir a identidade visual pública do estabelecimento.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      }
    ],
    externalSources: [
      {
        title: 'Web.dev: Otimização de imagens para a web moderna (WebP, AVIF e srcset)',
        publisher: 'Google Developers',
        url: 'https://web.dev/learn/images/',
        note: 'Guia técnico sobre compressão sem perda perceptível de qualidade e carregamento rápido no celular.'
      },
      {
        title: 'Google Search Central: Boas práticas de SEO para imagens',
        publisher: 'Google Search Central',
        url: 'https://developers.google.com/search/docs/appearance/google-images',
        note: 'Recomendações oficiais sobre nomes de arquivos descritivos, atributo alt e dimensões explícitas.'
      }
    ],
    cta: {
      audience: 'restaurants',
      eyebrow: 'IDENTIDADE VISUAL E CATÁLOGO',
      title: 'Valorize cada detalhe dos pratos que sua cozinha prepara.',
      description:
        'O Boraê apoia restaurantes na construção de uma apresentação digital profissional, com suporte a imagens otimizadas de perfil, capa e catálogo.',
      buttonLabel: 'Conheça o Boraê para restaurantes',
      routerLink: '/auth',
      secondaryLabel: 'Explorar o Blog Boraê',
      secondaryRouterLink: '/blog'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'por-que-a-foto-real-importa',
        text: '1. Autenticidade acima de tudo: por que fotografar o prato real'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Quando um consumidor consulta um '
          },
          {
            text: 'cardápio digital no celular',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ', a fotografia funciona como um compromisso visual entre a cozinha e a mesa. Utilizar imagens genéricas retiradas da internet pode até preencher espaço inicialmente, mas causa frustração imediata quando o prato servido possui outra louça, outro corte de carne ou outra guarnição.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Uma fotografia autêntica, registrada no próprio restaurante com a louça real da casa e a porção exata que é servida no salão, gera segurança e reduz dúvidas durante o pedido.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'iluminacao-natural-e-cores',
        text: '2. Iluminação natural e fidelidade das cores dos alimentos'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'O segredo de uma fotografia gastronômica apetitosa raramente está na câmera mais cara, e sim na qualidade da luz. O flash direto do celular costuma criar reflexos duros nos molhos e deixar as cores acinzentadas. Posicionar o prato próximo a uma janela com luz natural lateral suave valoriza o brilho dos vegetais frescos, a textura das massas e a suculência dos grelhados.'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Luz lateral ou diagonal:',
            segments: [
              {
                text: 'destaca o relevo e a textura natural dos ingredientes sem achatar a imagem.'
              }
            ]
          },
          {
            title: 'Balanço de branco neutro:',
            segments: [
              {
                text: 'evite luzes excessivamente amareladas ou azuladas que alterem a cor real de carnes, folhas e molhos.'
              }
            ]
          },
          {
            title: 'Fotografe o prato recém-finalizado:',
            segments: [
              {
                text: 'ervas frescas, azeites e espumas perdem viço poucos minutos após sair da bancada da cozinha.'
              }
            ]
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'angulos-para-cada-tipo-de-prato',
        text: '3. Escolhendo o melhor ângulo para cada categoria do cardápio'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Nem todo prato deve ser fotografado da mesma posição. Pizzas, tábuas de frios, saladas abertas e pratos com muitos acompanhamentos ficam excelentes vistos de cima (ângulo de 90 graus). Já hambúrgueres artesanais, sanduíches altos, cortes de carne espessos e sobremesas em camadas são muito mais valorizados em um ângulo diagonal de 45 graus, que revela a altura e o recheio da receita.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/gastronomia-brasileira-pratos-regionais.webp',
        srcset:
          'assets/images/blog/gastronomia-brasileira-pratos-regionais-640w.webp 640w, assets/images/blog/gastronomia-brasileira-pratos-regionais.webp 1280w',
        sizes: '(max-width: 768px) 100vw, 900px',
        alt: 'Preparação cuidadosa de prato na bancada de um restaurante destacando ingredientes frescos',
        width: 1280,
        height: 720,
        caption:
          'Fotografar o prato assim que ele é finalizado preserva o frescor e o brilho natural dos ingredientes.',
        credit: {
          label: 'Fotografia: Alex Urezkov / Pexels',
          photographer: 'Alex Urezkov',
          sourceName: 'Pexels',
          sourceUrl: 'https://www.pexels.com/photo/man-preparing-food-in-a-restaurant-19300180/'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'otimizacao-e-velocidade-no-celular',
        text: '4. Desempenho técnico: imagens bonitas que carregam rápido no 4G'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'De nada adianta ter fotografias em altíssima resolução se cada arquivo pesar vários megabytes e demorar para abrir no celular do cliente. Utilizar formatos modernos como WebP, manter proporções padronizadas (como 16:9 nas capas) e preencher o texto alternativo descritivo garante carregamento ágil e acessibilidade para leitores de tela.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'infraestrutura-de-midia-no-borae',
        text: '5. Como o Boraê trata imagens e mídia gastronômica'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Na arquitetura definida para o Boraê, o armazenamento de mídia utiliza infraestrutura dedicada em nuvem (Cloudflare R2) com validação de formato (JPEG, PNG e WebP) e otimização, preparando o caminho inclusive para recursos futuros como '
          },
          {
            text: 'modelos 3D e realidade aumentada em cardápios',
            routerLink: '/blog/modelos-3d-e-realidade-aumentada-em-cardapios'
          },
          {
            text: '.'
          }
        ]
      }
    ]
  },

  // =========================================================================
  // ARTIGO 8 (NOVO TEMA 3 - PILAR 3: CULTURA, CURIOSIDADES E GASTRONOMIA)
  // =========================================================================
  {
    id: 'art-8',
    slug: 'cafe-pao-de-queijo-e-hospitalidade-brasileira',
    title: 'Do café coado ao pão de queijo: a cultura da hospitalidade à mesa no Brasil',
    subtitle:
      'Passar um café fresco e oferecer algo recém-saído do forno são gestos que atravessam gerações no país. Entenda como essa tradição inspira cafeterias, padarias e restaurantes.',
    seoTitle: 'Cultura do café e hospitalidade na mesa brasileira | Boraê',
    description:
      'Descubra como o ritual do café, os queijos artesanais e a tradição de receber bem moldam a hospitalidade gastronômica brasileira.',
    excerpt:
      'No Brasil, oferecer um café é sinônimo de boas-vindas. Conheça as raízes culturais desse hábito, a valorização dos queijos artesanais e o impacto na hospitalidade contemporânea.',
    category: 'cultura-e-gastronomia',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/ambiente-restaurante-hospitalidade-mesa.jpg',
    coverWidth: 1200,
    coverHeight: 675,
    coverAlt: 'Ambiente acolhedor de restaurante e cafeteria iluminado para receber clientes',
    imageCredit: {
      label: 'Fotografia: Acervo Editorial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/cafe-pao-de-queijo-e-hospitalidade-brasileira`,
    tags: [
      'Cultura do café',
      'Hospitalidade brasileira',
      'Queijo artesanal',
      'Tradições culinárias',
      'Gastronomia brasileira'
    ],
    relatedArticles: [
      'diversidade-da-gastronomia-brasileira',
      'fidelizacao-de-clientes-em-restaurantes',
      'como-criar-um-cardapio-digital'
    ],
    internalLinks: [
      {
        label: 'Por que a gastronomia brasileira é tão diversa?',
        description: 'Uma viagem pelos biomas, ingredientes e saberes regionais do Brasil.',
        routerLink: '/blog/diversidade-da-gastronomia-brasileira'
      },
      {
        label: 'Como construir relacionamentos além da primeira visita',
        description: 'Transforme a hospitalidade do salão em recorrência e lembrança de marca.',
        routerLink: '/blog/fidelizacao-de-clientes-em-restaurantes'
      },
      {
        label: 'Como apresentar cafés, bebidas e pratos em um cardápio digital',
        description: 'Organize cartas de bebidas e menus por período de atendimento.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      }
    ],
    externalSources: [
      {
        title: 'IPHAN: Modos de fazer o Queijo Minas Artesanal como Patrimônio Cultural',
        publisher: 'IPHAN / Ministério da Cultura',
        url: 'http://portal.iphan.gov.br/pagina/detalhes/65',
        note: 'Documentação histórica e cultural sobre a produção tradicional de queijos artesanais em Minas Gerais.'
      },
      {
        title: 'Embrapa Café: História, regiões produtoras e qualidade do café no Brasil',
        publisher: 'Empresa Brasileira de Pesquisa Agropecuária (Embrapa)',
        url: 'https://www.embrapa.br/cafe',
        note: 'Dados públicos sobre a cafeicultura brasileira, indicações geográficas e diversidade sensorial dos grãos.'
      }
    ],
    cta: {
      audience: 'consumers',
      eyebrow: 'HISTÓRIAS E SABORES DO BRASIL',
      title: 'A hospitalidade começa nos detalhes de cada encontro à mesa.',
      description:
        'Uma boa experiência gastronômica começa com a descoberta. Conheça a proposta do Boraê e acompanhe a evolução de uma nova forma de conectar pessoas e restaurantes.',
      buttonLabel: 'Conheça o Boraê',
      routerLink: '/',
      secondaryLabel: 'Ver todos os artigos',
      secondaryRouterLink: '/blog'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'convite-para-entrar-e-conversar',
        text: '1. "Aceita um cafezinho?": o código brasileiro de boas-vindas'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Em praticamente todas as regiões do Brasil, a pergunta "aceita um café?" significa muito mais do que oferecer uma bebida quente: é um convite para desacelerar, sentar-se à mesa e iniciar uma conversa. Esse gesto doméstico atravessou as portas das casas e moldou profundamente a cultura de hospitalidade dos restaurantes, padarias e cafeterias brasileiras.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Tanto no encerramento de um almoço quanto nas pausas da tarde, a mesa do café funciona como um espaço de acolhimento que complementa a '
          },
          {
            text: 'diversidade da gastronomia brasileira',
            routerLink: '/blog/diversidade-da-gastronomia-brasileira'
          },
          {
            text: '.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'saberes-do-queijo-e-do-polvilho',
        text: '2. O encontro entre o polvilho da mandioca e a tradição queijeira'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Poucas combinações representam tão bem a síntese cultural brasileira quanto o café acompanhado de pão de queijo. A receita une o polvilho (extraído da mandioca, herança direta dos povos indígenas) ao queijo curado das fazendas leiteiras, especialmente de Minas Gerais.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Os modos artesanais de fazer o Queijo Minas, nas regiões do Serro, da Serra da Canastra e do Salitre, foram registrados pelo IPHAN como Patrimônio Cultural Imaterial brasileiro e reconhecidos internacionalmente pela UNESCO em 2024, demonstrando como técnicas tradicionais preservadas por pequenos produtores ganham admiração em todo o mundo.'
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/fidelizacao-clientes-restaurantes-experiencia.jpg',
        alt: 'Pessoas reunidas ao redor da mesa compartilhando alimentos e bebidas em ambiente acolhedor',
        width: 1200,
        height: 675,
        caption:
          'O hábito de compartilhar a mesa transforma refeições cotidianas em momentos de convivência e memória.',
        credit: {
          label: 'Fotografia: Acervo Editorial Boraê',
          photographer: 'Acervo Boraê',
          sourceName: 'Boraê'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'evolucao-das-cafeterias-e-restaurantes',
        text: '3. A nova geração de cafeterias, padarias artesanais e restaurantes'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Nas últimas décadas, o interesse do consumidor brasileiro pela origem dos ingredientes cresceu de forma expressiva. Hoje é comum encontrar cartas de cafés especiais que informam a região produtora (como Cerrado Mineiro, Mantiqueira de Minas, Mogiana Paulista ou Chapada Diamantina), o método de extração e as notas sensoriais da bebida.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Para apresentar essa riqueza sem sobrecarregar o atendimento, muitos estabelecimentos utilizam '
          },
          {
            text: 'cardápios digitais organizados por ocasião',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: ', destacando ingredientes regionais e sugestões de harmonização.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'hospitalidade-no-ambiente-digital',
        text: '4. Como traduzir a hospitalidade brasileira para o ambiente digital'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Receber bem também significa facilitar a vida de quem visita o estabelecimento. Quando o cliente encontra horários claros, descrições cuidadosas e um ambiente digital acolhedor, a hospitalidade começa antes mesmo da primeira xícara servida e fortalece o '
          },
          {
            text: 'relacionamento duradouro com o restaurante',
            routerLink: '/blog/fidelizacao-de-clientes-em-restaurantes'
          },
          {
            text: '.'
          }
        ]
      }
    ]
  },

  // =========================================================================
  // ARTIGO 9 (NOVO TEMA 4 - PILAR 4: TECNOLOGIA E O FUTURO DA GASTRONOMIA)
  // =========================================================================
  {
    id: 'art-9',
    slug: 'modelos-3d-e-realidade-aumentada-em-cardapios',
    title: 'Modelos 3D e realidade aumentada na gastronomia: como funciona a visualização de pratos',
    subtitle:
      'Entenda como arquivos tridimensionais leves no navegador podem complementar a fotografia tradicional e quais cuidados técnicos garantem uma experiência rápida no celular.',
    seoTitle: 'Cardápio 3D e realidade aumentada em restaurantes | Boraê',
    description:
      'Saiba como funcionam os modelos 3D (GLB/glTF) e a realidade aumentada aplicada a cardápios digitais, seus benefícios visuais e requisitos técnicos.',
    excerpt:
      'Visualizar um prato em três dimensões diretamente no navegador do smartphone deixou de exigir aplicativos pesados. Conheça como essa tecnologia funciona e como o Boraê planeja sua adoção.',
    category: 'tecnologia-e-gastronomia',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/inteligencia-artificial-descoberta-gastronomica.jpg',
    coverWidth: 1200,
    coverHeight: 675,
    coverAlt: 'Apresentação gastronômica contemporânea destacando volume, textura e iluminação do prato',
    imageCredit: {
      label: 'Fotografia: Acervo Editorial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/modelos-3d-e-realidade-aumentada-em-cardapios`,
    tags: [
      'Modelos 3D',
      'Realidade aumentada',
      'GLB e glTF',
      'Cardápio interativo',
      'Inovação gastronômica'
    ],
    relatedArticles: [
      'inteligencia-artificial-na-gastronomia',
      'fotografia-gastronomica-e-apresentacao-de-pratos',
      'como-criar-um-cardapio-digital'
    ],
    internalLinks: [
      {
        label: 'Como a inteligência artificial pode apoiar a descoberta gastronômica',
        description: 'Veja como IA e dados estruturados formam a visão tecnológica do Boraê.',
        routerLink: '/blog/inteligencia-artificial-na-gastronomia'
      },
      {
        label: 'Fotografia gastronômica para cardápios digitais',
        description: 'Por que boas fotografias continuam sendo a base indispensável de qualquer cardápio.',
        routerLink: '/blog/fotografia-gastronomica-e-apresentacao-de-pratos'
      },
      {
        label: 'Guia completo de cardápio digital responsivo',
        description: 'Como organizar categorias e produtos pensando na leitura em smartphones.',
        routerLink: '/blog/como-criar-um-cardapio-digital'
      }
    ],
    externalSources: [
      {
        title: 'Khronos Group: Especificação oficial do formato glTF / GLB para 3D na Web',
        publisher: 'The Khronos Group',
        url: 'https://www.khronos.org/gltf/',
        note: 'Padrão aberto de transmissão rápida de cenas e modelos tridimensionais para navegadores modernos.'
      },
      {
        title: 'Model Viewer: Visualização 3D e Realidade Aumentada declarativa na Web',
        publisher: 'Google Web Components / modelviewer.dev',
        url: 'https://modelviewer.dev/',
        note: 'Documentação técnica sobre renderização 3D acessível e compatibilidade com WebXR, Scene Viewer e Quick Look.'
      }
    ],
    cta: {
      audience: 'partners',
      eyebrow: 'ENGENHARIA E INOVAÇÃO GASTRONÔMICA',
      title: 'Construindo experiências visuais com responsabilidade técnica.',
      description:
        'Quer construir novas possibilidades para a gastronomia? Conheça o propósito do Boraê e acompanhe a evolução dos nossos módulos tecnológicos.',
      buttonLabel: 'Conheça nosso projeto',
      routerLink: '/',
      fragment: 'sobre',
      secondaryLabel: 'Sou um restaurante',
      secondaryRouterLink: '/auth'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'o-que-e-visualizacao-3d-na-gastronomia',
        text: '1. O que é a visualização 3D de pratos no navegador'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Durante anos, experiências tridimensionais e de realidade aumentada exigiam instalar aplicativos específicos e pesados no celular. Com a evolução dos padrões abertos da web, como o WebGL e o formato compacto GLB (baseado no padrão glTF), tornou-se possível girar um objeto em três dimensões diretamente na página do navegador.'
          }
        ]
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Aplicada à gastronomia, essa tecnologia permite que o consumidor observe os detalhes de montagem de um prato especial, de um corte compartilhado ou de uma sobremesa autoral sob diferentes ângulos antes de fazer o pedido.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'cuidados-com-escala-e-fidelidade',
        text: '2. Calibração de dimensões e respeito à proporção real'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Assim como ocorre na '
          },
          {
            text: 'fotografia gastronômica para cardápios',
            routerLink: '/blog/fotografia-gastronomica-e-apresentacao-de-pratos'
          },
          {
            text: ', o compromisso principal de um modelo tridimensional é a honestidade visual. Quando a realidade aumentada projeta um prato sobre a mesa, as dimensões físicas em centímetros precisam estar devidamente calibradas para que a porção não pareça maior nem menor do que o prato real servido pela cozinha.'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Carregamento sob demanda:',
            segments: [
              {
                text: 'o arquivo 3D só deve ser baixado quando o consumidor optar por interagir com a visualização tridimensional, preservando a velocidade inicial do cardápio.'
              }
            ]
          },
          {
            title: 'Fotografia tradicional como base obrigatória:',
            segments: [
              {
                text: 'nem todos os aparelhos possuem suporte a realidade aumentada; por isso, todo item precisa manter uma imagem fotográfica rápida como alternativa padrão.'
              }
            ]
          },
          {
            title: 'Armazenamento otimizado em nuvem:',
            segments: [
              {
                text: 'arquivos binários GLB devem ser servidos por armazenamento de objetos com CDN, nunca sobrecarregando o banco de dados transacional.'
              }
            ]
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/cardapio-digital-experiencia-cliente.webp',
        srcset:
          'assets/images/blog/cardapio-digital-experiencia-cliente-640w.webp 640w, assets/images/blog/cardapio-digital-experiencia-cliente.webp 1280w',
        sizes: '(max-width: 768px) 100vw, 900px',
        alt: 'Cozinha profissional preparando refeição com apresentação cuidadosa',
        width: 1280,
        height: 720,
        caption:
          'Recursos interativos funcionam como melhoria progressiva: eles nunca devem atrasar a leitura rápida do cardápio.',
        credit: {
          label: 'Fotografia: Jonathan Borba / Pexels',
          photographer: 'Jonathan Borba',
          sourceName: 'Pexels',
          sourceUrl:
            'https://www.pexels.com/photo/professional-chef-preparing-meal-in-modern-kitchen-28703283/'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'planejamento-3d-ar-no-borae',
        text: '3. Como o módulo 3D e Realidade Aumentada está planejado no Boraê'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'No roadmap do Boraê, a experiência 3D/AR constitui a Fase 4 de evolução da plataforma, posicionada logo após a consolidação do '
          },
          {
            text: 'módulo central de cardápio digital',
            routerLink: '/blog/como-criar-um-cardapio-digital'
          },
          {
            text: '. Os modelos serão opcionais por produto, validados quanto a formato e integridade, armazenados no Cloudflare R2 e exibidos apenas em dispositivos compatíveis, garantindo que a experiência principal permaneça leve em qualquer celular.'
          }
        ]
      }
    ]
  },

  // =========================================================================
  // ARTIGO 10 (NOVO TEMA 5 - PILAR 5: MARKETING, DESCOBERTA E RELACIONAMENTO)
  // =========================================================================
  {
    id: 'art-10',
    slug: 'avaliacoes-de-clientes-e-reputacao-digital-restaurantes',
    title: 'Avaliações de clientes em restaurantes: como construir reputação com transparência',
    subtitle:
      'Notas e comentários reais ajudam novos consumidores a escolher com segurança e oferecem um termômetro valioso para a gestão de comida, atendimento e ambiente.',
    seoTitle: 'Avaliações e reputação digital para restaurantes | Boraê',
    description:
      'Entenda como gerenciar avaliações de clientes em restaurantes, responder com profissionalismo e transformar opiniões verificadas em evolução operacional.',
    excerpt:
      'Construir uma reputação sólida exige escuta ativa e transparência. Veja como avaliações vinculadas a experiências reais fortalecem a confiança entre restaurantes e consumidores.',
    category: 'marketing-e-relacionamento',
    author: BLOG_EDITORIAL_AUTHOR,
    publishedAt: '2026-11-14',
    updatedAt: '2026-11-14',
    readingTime: 7,
    featured: false,
    coverImage: 'assets/images/blog/fidelizacao-clientes-restaurantes-experiencia.jpg',
    coverWidth: 1200,
    coverHeight: 675,
    coverAlt: 'Clientes conversando e avaliando positivamente a experiência à mesa em um restaurante',
    imageCredit: {
      label: 'Fotografia: Acervo Editorial Boraê',
      photographer: 'Acervo Boraê',
      sourceName: 'Boraê'
    },
    canonicalUrl: `${BORAE_CANONICAL_ORIGIN}/blog/avaliacoes-de-clientes-e-reputacao-digital-restaurantes`,
    tags: [
      'Avaliações de clientes',
      'Reputação digital',
      'Descoberta de restaurantes',
      'Atendimento ao cliente',
      'Gestão de marca'
    ],
    relatedArticles: [
      'fidelizacao-de-clientes-em-restaurantes',
      'organizacao-de-equipe-e-papeis-no-restaurante',
      'presenca-digital-para-restaurantes'
    ],
    internalLinks: [
      {
        label: 'Fidelização de clientes em restaurantes: estratégias práticas',
        description: 'Como unir boa reputação e benefícios para incentivar novas visitas.',
        routerLink: '/blog/fidelizacao-de-clientes-em-restaurantes'
      },
      {
        label: 'Como organizar funções e permissões da equipe no restaurante',
        description: 'Alinhe salão e cozinha para manter um padrão consistente de atendimento.',
        routerLink: '/blog/organizacao-de-equipe-e-papeis-no-restaurante'
      },
      {
        label: 'Presença digital para restaurantes: por onde começar',
        description: 'Como apresentar informações completas que evitam frustrações antes da visita.',
        routerLink: '/blog/presenca-digital-para-restaurantes'
      }
    ],
    externalSources: [
      {
        title: 'Senacon / Consumidor.gov.br: Boas práticas de transparência e relacionamento com o consumidor',
        publisher: 'Ministério da Justiça e Segurança Pública',
        url: 'https://www.consumidor.gov.br/',
        note: 'Princípios de clareza, boa-fé e resolução construtiva de apontamentos de consumidores no Brasil.'
      },
      {
        title: 'Sebrae: Como lidar com críticas e avaliações online no setor de alimentação',
        publisher: 'Sebrae',
        url: 'https://sebrae.com.br/sites/PortalSebrae/segmentos/alimentacao',
        note: 'Orientações práticas para responder comentários públicos e aprimorar processos internos.'
      }
    ],
    cta: {
      audience: 'restaurants',
      eyebrow: 'CONFIANÇA E DESCOBERTA',
      title: 'Fortaleça a reputação do seu restaurante com informações transparentes.',
      description:
        'O Boraê está construindo um ecossistema onde a descoberta e a reputação caminham ao lado da autenticidade de cada estabelecimento.',
      buttonLabel: 'Conheça o Boraê para restaurantes',
      routerLink: '/auth',
      secondaryLabel: 'Conhecer a plataforma',
      secondaryRouterLink: '/'
    },
    content: [
      {
        type: 'heading',
        level: 2,
        id: 'papel-das-avaliacoes-na-descoberta',
        text: '1. O papel das avaliações na escolha de um restaurante'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Quando alguém procura um novo lugar para almoçar ou jantar, dois elementos pesam imediatamente na decisão: a completude das informações apresentadas pelo estabelecimento e a percepção de outros consumidores que já estiveram ali. Avaliações autênticas funcionam como uma ponte de confiança para quem ainda não conhece a cozinha da casa.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'importancia-de-avaliacoes-verificadas',
        text: '2. Por que avaliações vinculadas a experiências reais são mais justas'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Um problema frequente em ambientes abertos na internet é a presença de notas publicadas por perfis que nunca visitaram o estabelecimento. Sistemas modernos de descoberta gastronômica buscam proteger tanto o consumidor quanto o restaurante, permitindo que apenas pessoas com visita ou compra efetivamente confirmada registrem sua avaliação.'
          }
        ]
      },
      {
        type: 'list',
        items: [
          {
            title: 'Critérios objetivos (Comida, Atendimento e Ambiente):',
            segments: [
              {
                text: 'separar a nota geral em dimensões claras ajuda o gestor a identificar se um ponto de melhoria está na cozinha, no tempo de serviço do salão ou no conforto acústico e térmico do espaço.'
              }
            ]
          },
          {
            title: 'Direito de resposta pública do restaurante:',
            segments: [
              {
                text: 'agradecer elogios e responder com serenidade a eventuais falhas mostra a futuros clientes que a gestão é atenta e comprometida.'
              }
            ]
          },
          {
            title: 'Integridade do histórico:',
            segments: [
              {
                text: 'manter avaliações legítimas sem edições arbitrárias, contando com moderação humana apenas em casos de violação de regras ou ofensas, preserva a credibilidade de toda a plataforma.'
              }
            ]
          }
        ]
      },
      {
        type: 'image',
        src: 'assets/images/blog/ambiente-restaurante-hospitalidade-mesa.jpg',
        alt: 'Salão de restaurante preparado com cuidado nos detalhes de iluminação e mesas',
        width: 1200,
        height: 675,
        caption:
          'Avaliações divididas entre comida, atendimento e ambiente ajudam o gestor a aperfeiçoar cada etapa da jornada.',
        credit: {
          label: 'Fotografia: Acervo Editorial Boraê',
          photographer: 'Acervo Boraê',
          sourceName: 'Boraê'
        }
      },
      {
        type: 'heading',
        level: 2,
        id: 'como-responder-com-profissionalismo',
        text: '3. Como transformar comentários em melhoria contínua para a equipe'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Compartilhar avaliações construtivas nas reuniões semanais ajuda a valorizar o trabalho da '
          },
          {
            text: 'equipe do restaurante',
            routerLink: '/blog/organizacao-de-equipe-e-papeis-no-restaurante'
          },
          {
            text: ' e a corrigir pequenos gargalos antes que se tornem recorrentes. Ao mesmo tempo, manter a '
          },
          {
            text: 'presença digital do estabelecimento',
            routerLink: '/blog/presenca-digital-para-restaurantes'
          },
          {
            text: ' alinhada à realidade evita frustrações causadas por expectativas desalinhadas.'
          }
        ]
      },
      {
        type: 'heading',
        level: 2,
        id: 'descoberta-e-avaliacoes-no-borae',
        text: '4. Como a descoberta e as avaliações estão planejadas no Boraê'
      },
      {
        type: 'paragraph',
        segments: [
          {
            text: 'Nas decisões de produto aprovadas para o módulo futuro de descoberta do Boraê, a ordenação dos estabelecimentos considerará distância, qualidade e completude das informações e avaliações verificadas. Somente consumidores com compra confirmada poderão avaliar (com nota de 1 a 5 e critérios de comida, atendimento e ambiente), e os restaurantes poderão responder publicamente com total transparência.'
          }
        ]
      }
    ]
  }
];

export function getCategoryBySlug(slug: BlogCategorySlug | string): BlogCategory | undefined {
  return BLOG_CATEGORIES.find(cat => cat.slug === slug);
}

export function getArticleBySlug(slug: string): BlogArticle | undefined {
  return BLOG_ARTICLES.find(article => article.slug === slug);
}

export function getRelatedArticles(article: BlogArticle): BlogArticle[] {
  return article.relatedArticles
    .map(slug => getArticleBySlug(slug))
    .filter((item): item is BlogArticle => Boolean(item));
}
