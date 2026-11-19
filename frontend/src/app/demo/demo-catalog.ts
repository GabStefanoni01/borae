import { HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { DemoDatabase, DemoRestaurant, DemoUser } from './demo-data';
import { createDemoPlateGlb } from './demo-glb';

/**
 * MODO DEMONSTRAÇÃO — simulação do catálogo (PR #22) para `npm run start:demo`.
 * Reproduz as regras dos services do backend (MenuService, MenuProductService,
 * MenuProductImageService, PublicRestaurantMenuService, RestaurantService.publish/unpublish
 * e RestaurantQrCodeService) para que as telas possam ser exercitadas sem a API real.
 * Não faz parte do build de produção.
 */

type Reply = Observable<HttpResponse<unknown>>;
export interface DemoHelpers {
  ok(body: unknown, status?: number): Reply;
  fail(status: number, message: string): Observable<never>;
  save(db: DemoDatabase): void;
  uid(prefix: string): string;
}

export interface DemoMenu { id: string; restaurantId: string; name: string; description: string | null; status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'; displayOrder: number; }
export interface DemoCategory { id: string; menuId: string; name: string; description: string | null; active: boolean; displayOrder: number; }
export interface DemoAddon { name: string; priceDeltaInCents: number; active: boolean; }
export interface DemoProduct {
  id: string; categoryId: string; name: string; description: string | null; priceInCents: number;
  lifecycleStatus: 'ACTIVE' | 'ARCHIVED'; availability: 'AVAILABLE' | 'UNAVAILABLE'; displayOrder: number;
  ingredients: string[]; addons: DemoAddon[]; imageUrl: string | null;
  model3d?: DemoModel3d | null;
}
export interface DemoModel3d {
  id: string; status: 'READY' | 'PUBLISHED'; url: string; fileSize: number;
  widthMm: number; heightMm: number; depthMm: number; createdAt: string; updatedAt: string;
}
export interface DemoCatalog { menus: DemoMenu[]; categories: DemoCategory[]; products: DemoProduct[]; }

/** Imagens enviadas no modo demonstração ficam só na memória da aba (object URLs). */
const LIVE_IMAGES = new Set<string>();
/** Marcador do modelo 3D de exemplo (gerado em memória na primeira consulta). */
const SEED_MODEL = 'demo:prato';
let seedModelUrl: string | null = null;
let seedModelBytes = 0;

export function createDemoCatalog(restaurantId: string): DemoCatalog {
  const menu = (id: string, name: string, description: string | null, status: DemoMenu['status'], displayOrder: number): DemoMenu =>
    ({ id, restaurantId, name, description, status, displayOrder });
  const cat = (id: string, menuId: string, name: string, displayOrder: number, active = true, description: string | null = null): DemoCategory =>
    ({ id, menuId, name, description, active, displayOrder });
  const prod = (id: string, categoryId: string, name: string, description: string, priceInCents: number, displayOrder: number,
    ingredients: string[] = [], addons: DemoAddon[] = [], extra: Partial<DemoProduct> = {}): DemoProduct =>
    ({ id, categoryId, name, description, priceInCents, displayOrder, ingredients, addons, lifecycleStatus: 'ACTIVE', availability: 'AVAILABLE', imageUrl: null, ...extra });

  return {
    menus: [
      menu('demo-menu-almoco', 'Almoço', 'Servido de terça a sexta, das 11h30 às 15h.', 'PUBLISHED', 0),
      menu('demo-menu-vinhos', 'Carta de vinhos', 'Rótulos selecionados para acompanhar as massas da casa.', 'PUBLISHED', 1),
      menu('demo-menu-inverno', 'Jantar de inverno', 'Pratos sazonais em preparação.', 'DRAFT', 2),
      menu('demo-menu-verao', 'Verão 2025', 'Cardápio da temporada passada.', 'ARCHIVED', 3)
    ],
    categories: [
      cat('demo-cat-entradas', 'demo-menu-almoco', 'Entradas', 0, true, 'Para começar a refeição.'),
      cat('demo-cat-massas', 'demo-menu-almoco', 'Massas', 1, true, 'Massas frescas feitas na casa todos os dias.'),
      cat('demo-cat-semana', 'demo-menu-almoco', 'Pratos da semana', 2, false),
      cat('demo-cat-sobremesas', 'demo-menu-almoco', 'Sobremesas', 3),
      cat('demo-cat-tintos', 'demo-menu-vinhos', 'Tintos', 0),
      cat('demo-cat-brancos', 'demo-menu-vinhos', 'Brancos', 1),
      cat('demo-cat-inverno', 'demo-menu-inverno', 'Sopas', 0),
      cat('demo-cat-verao', 'demo-menu-verao', 'Saladas', 0)
    ],
    products: [
      prod('demo-p-bruschetta', 'demo-cat-entradas', 'Bruschetta clássica', 'Pão de fermentação natural, tomate italiano, manjericão e azeite extravirgem.', 2890, 0,
        ['Pão de fermentação natural', 'Tomate italiano', 'Manjericão', 'Azeite extravirgem']),
      prod('demo-p-burrata', 'demo-cat-entradas', 'Burrata com pesto', 'Burrata cremosa, pesto de manjericão e tomates assados.', 4890, 1,
        ['Burrata', 'Pesto de manjericão', 'Tomate assado'], [], { availability: 'UNAVAILABLE' }),
      prod('demo-p-carbonara', 'demo-cat-massas', 'Spaghetti alla carbonara', 'Massa fresca, guanciale crocante, gema e pecorino romano.', 5890, 0,
        ['Spaghetti fresco', 'Guanciale', 'Gema de ovo', 'Pecorino romano', 'Pimenta-do-reino'],
        [{ name: 'Guanciale extra', priceDeltaInCents: 900, active: true }, { name: 'Pecorino extra', priceDeltaInCents: 600, active: true }, { name: 'Trufa negra', priceDeltaInCents: 2500, active: false }],
        { model3d: { id: 'demo-model-carbonara', status: 'PUBLISHED', url: SEED_MODEL, fileSize: 0, widthMm: 280, heightMm: 68, depthMm: 280, createdAt: '2026-11-17T12:00:00Z', updatedAt: '2026-11-17T12:00:00Z' } }),
      prod('demo-p-ragu', 'demo-cat-massas', 'Pappardelle ao ragu', 'Ragu de costela cozido por oito horas.', 6490, 1,
        ['Pappardelle', 'Ragu de costela', 'Parmesão'], [{ name: 'Burrata', priceDeltaInCents: 1800, active: true }]),
      prod('demo-p-lasanha', 'demo-cat-massas', 'Lasanha da nonna', 'Receita antiga, fora do cardápio.', 5490, 2, [], [], { lifecycleStatus: 'ARCHIVED' }),
      prod('demo-p-semana', 'demo-cat-semana', 'Risoto de cogumelos', 'Disponível apenas em eventos.', 6990, 0),
      prod('demo-p-tiramisu', 'demo-cat-sobremesas', 'Tiramisù', 'Mascarpone, café espresso e cacau.', 2490, 0, ['Mascarpone', 'Café espresso', 'Biscoito champagne', 'Cacau']),
      prod('demo-p-chianti', 'demo-cat-tintos', 'Chianti Classico', 'Toscana · taça 150 ml.', 3900, 0),
      prod('demo-p-primitivo', 'demo-cat-tintos', 'Primitivo di Manduria', 'Puglia · taça 150 ml.', 4200, 1),
      prod('demo-p-pinot', 'demo-cat-brancos', 'Pinot Grigio', 'Vêneto · taça 150 ml.', 3600, 0),
      prod('demo-p-salada', 'demo-cat-verao', 'Salada caprese', 'Tomate, muçarela de búfala e manjericão.', 3490, 0)
    ]
  };
}

/** Rotas públicas (sem sessão). Devolve null quando a rota não é do catálogo ou assistente. */
export function handlePublicCatalog(method: string, path: string, db: DemoDatabase, h: DemoHelpers, body?: unknown): Reply | null {
  if (method === 'POST' && path === '/public/assistant/sessions') {
    return h.ok(null, 204);
  }
  const clickMatch = path.match(/^\/public\/restaurants\/([^/]+)\/assistant\/recommendations\/([^/]+)\/click$/);
  if (clickMatch && method === 'POST') {
    return h.ok(null, 204);
  }
  const interactMatch = path.match(/^\/public\/restaurants\/([^/]+)\/assistant\/interactions$/);
  if (interactMatch && method === 'POST') {
    const slug = decodeURIComponent(interactMatch[1]);
    const restaurant = db.restaurants.find(r => r.slug === slug && r.publicationStatus === 'PUBLISHED');
    if (!restaurant) return h.fail(404, 'Restaurante não encontrado ou não publicado.');
    const payload = (body ?? {}) as { message?: string; menuId?: string | null; filters?: { maxPriceInCents?: number | null; excludedIngredients?: string[] | null } | null };
    const c = catalog(db);
    const pubMenus = c.menus.filter(m => m.restaurantId === restaurant.id && m.status === 'PUBLISHED' && (!payload.menuId || m.id === payload.menuId));
    const catIds = new Set(c.categories.filter(x => x.active && pubMenus.some(m => m.id === x.menuId)).map(x => x.id));
    const excluded = (payload.filters?.excludedIngredients ?? []).map(v => v.trim().toLowerCase()).filter(Boolean);
    const maxPrice = payload.filters?.maxPriceInCents ?? null;
    const eligible = c.products.filter(p =>
      catIds.has(p.categoryId) && p.lifecycleStatus === 'ACTIVE' && p.availability === 'AVAILABLE'
      && (maxPrice === null || p.priceInCents <= maxPrice)
      && (!excluded.length || !p.ingredients.some(i => excluded.some(ex => i.toLowerCase().includes(ex))))
    );
    const picks = eligible.slice(0, 3).map(p => ({
      productId: p.id, name: p.name, priceInCents: p.priceInCents, availability: p.availability,
      imageUrl: liveImage(p.imageUrl), ingredients: [...p.ingredients],
      reason: p.description || 'Opção disponível no cardápio publicado.'
    }));
    return h.ok({
      answer: picks.length
        ? `Separei ${picks.length} ${picks.length === 1 ? 'opção disponível' : 'opções disponíveis'} no cardápio de ${restaurant.name} para você.`
        : 'Não encontrei opções disponíveis com esses filtros no cardápio.',
      recommendations: picks,
      quota: { remainingConsumer: 19, remainingRestaurant: 99, resetsAt: new Date(Date.now() + 3600_000).toISOString() },
      fallback: false,
      fallbackCode: null
    });
  }
  const match = path.match(/^\/public\/restaurants\/([^/]+)\/menu$/);
  if (!match || method !== 'GET') return null;
  const slug = decodeURIComponent(match[1]);
  const restaurant = db.restaurants.find(r => r.slug === slug && r.publicationStatus === 'PUBLISHED');
  if (!restaurant) return h.fail(404, 'Restaurante não encontrado ou não publicado.');
  const c = catalog(db);
  const menus = c.menus.filter(m => m.restaurantId === restaurant.id && m.status === 'PUBLISHED').sort(byOrder).map(m => ({
    id: m.id, name: m.name, description: m.description, displayOrder: m.displayOrder,
    categories: c.categories.filter(x => x.menuId === m.id && x.active).sort(byOrder).map(x => ({
      id: x.id, name: x.name, description: x.description, displayOrder: x.displayOrder,
      products: c.products.filter(p => p.categoryId === x.id && p.lifecycleStatus === 'ACTIVE').sort(byOrder).map(p => ({
        id: p.id, name: p.name, description: p.description, priceInCents: p.priceInCents, availability: p.availability,
        displayOrder: p.displayOrder, ingredients: [...p.ingredients],
        addons: p.addons.filter(a => a.active).map(a => ({ name: a.name, priceDeltaInCents: a.priceDeltaInCents })),
        imageUrl: liveImage(p.imageUrl),
        model3d: p.model3d?.status === 'PUBLISHED' && modelUrl(p.model3d)
          ? { modelUrl: modelUrl(p.model3d)!, contentType: 'model/gltf-binary', widthMm: p.model3d.widthMm, heightMm: p.model3d.heightMm, depthMm: p.model3d.depthMm }
          : null
      }))
    }))
  }));
  return h.ok({
    id: restaurant.id, name: restaurant.name, slug: restaurant.slug, description: restaurant.description ?? null,
    phone: restaurant.phone ?? null, whatsapp: restaurant.whatsapp ?? null, instagram: restaurant.instagram ?? null,
    website: restaurant.website ?? null, category: restaurant.category ?? null, menus
  });
}

/** Rotas autenticadas do catálogo, publicação e QR Code. Devolve null quando a rota não é do catálogo. */
export function handleCatalog(req: HttpRequest<unknown>, path: string, db: DemoDatabase, user: DemoUser, h: DemoHelpers): Reply | null {
  const method = req.method;
  const body = (req.body ?? {}) as Record<string, unknown>;

  const pub = path.match(/^\/restaurants\/([^/]+)\/(publish|unpublish|qrcode)$/);
  if (pub) {
    const [, restaurantId, action] = pub;
    const denied = requireRole(db, user, restaurantId, true, h);
    if (denied) return denied;
    const restaurant = db.restaurants.find(r => r.id === restaurantId)!;
    if (action === 'qrcode' && method === 'GET') return demoQrCode(restaurant, h);
    if (method !== 'POST' || action === 'qrcode') return null;
    if (restaurant.publicationStatus === 'SUSPENDED') return h.fail(409, 'Este restaurante está suspenso e não pode ser alterado pelo painel.');
    restaurant.publicationStatus = action === 'publish' ? 'PUBLISHED' : 'DRAFT';
    h.save(db);
    return h.ok(profileView(restaurant));
  }

  const route = path.match(/^\/restaurants\/([^/]+)\/menus(?:\/([^/]+)(?:\/(publish|unpublish|archive|categories|products)(?:\/([^/]+)(?:\/([^/]+)(?:\/(publication))?)?)?)?)?$/);
  if (!route) return null;
  const [, restaurantId, menuId, section, itemId, itemAction, subAction] = route;
  const c = catalog(db);
  const needsManager = (method !== 'GET' && !(section === 'products' && itemAction === 'availability')) || itemAction === 'model-3d';
  const denied = requireRole(db, user, restaurantId, needsManager, h);
  if (denied) return denied;

  // ---------- Cardápios ----------
  if (!menuId) {
    if (method === 'GET') return h.ok(c.menus.filter(m => m.restaurantId === restaurantId).sort(byOrder).map(menuView));
    if (method === 'POST') {
      const name = text(body['name']);
      if (!name || name.length > 120) return h.fail(400, 'O nome é obrigatório.');
      const menu: DemoMenu = { id: h.uid('menu'), restaurantId, name, description: optional(body['description']), status: 'DRAFT', displayOrder: Number(body['displayOrder'] ?? 0) };
      c.menus.push(menu);
      h.save(db);
      return h.ok(menuView(menu), 201);
    }
    return null;
  }
  const menu = c.menus.find(m => m.id === menuId && m.restaurantId === restaurantId);
  if (!menu) return h.fail(404, 'Cardápio não encontrado.');
  const archived = () => h.fail(409, 'Cardápio arquivado não pode ser alterado.');

  if (!section) {
    if (method === 'GET') return h.ok(menuView(menu));
    if (method === 'PUT') {
      if (menu.status === 'ARCHIVED') return archived();
      const name = text(body['name']);
      if (!name || name.length > 120) return h.fail(400, 'O nome é obrigatório.');
      menu.name = name;
      menu.description = optional(body['description']);
      if (body['displayOrder'] != null) menu.displayOrder = Number(body['displayOrder']);
      h.save(db);
      return h.ok(menuView(menu));
    }
    return null;
  }

  if (section === 'publish' || section === 'unpublish' || section === 'archive') {
    if (method !== 'POST') return null;
    if (section === 'archive') { menu.status = 'ARCHIVED'; h.save(db); return h.ok(menuView(menu)); }
    if (menu.status === 'ARCHIVED') return archived();
    if (section === 'publish') {
      const activeCats = c.categories.filter(x => x.menuId === menu.id && x.active).map(x => x.id);
      if (!c.products.some(p => activeCats.includes(p.categoryId) && p.lifecycleStatus !== 'ARCHIVED')) {
        return h.fail(409, 'O cardápio precisa ter ao menos uma categoria ativa com um produto não arquivado antes da publicação.');
      }
    }
    menu.status = section === 'publish' ? 'PUBLISHED' : 'DRAFT';
    h.save(db);
    return h.ok(menuView(menu));
  }

  // ---------- Categorias ----------
  const categories = () => c.categories.filter(x => x.menuId === menu.id).sort(byOrder);
  if (section === 'categories') {
    if (method === 'GET' && !itemId) return h.ok(categories().map(categoryView));
    if (menu.status === 'ARCHIVED') return archived();
    if (method === 'PUT' && itemId === 'order') {
      const ids = Array.isArray(req.body) ? req.body as string[] : null;
      const existing = categories().map(x => x.id);
      if (!ids || new Set(ids).size !== ids.length || ids.length !== existing.length || ids.some(id => !existing.includes(id))) {
        return h.fail(400, 'A ordenação deve conter todas as categorias deste cardápio e nenhuma categoria externa.');
      }
      ids.forEach((id, index) => { c.categories.find(x => x.id === id)!.displayOrder = index; });
      h.save(db);
      return h.ok(categories().map(categoryView));
    }
    const name = text(body['name']);
    if (!name || name.length > 120) return h.fail(400, 'O nome é obrigatório.');
    const duplicate = categories().some(x => x.id !== itemId && normalize(x.name) === normalize(name));
    if (method === 'POST' && !itemId) {
      if (duplicate) return h.fail(409, 'Já existe uma categoria com esse nome neste cardápio.');
      const category: DemoCategory = { id: h.uid('cat'), menuId: menu.id, name, description: optional(body['description']), active: true, displayOrder: Number(body['displayOrder'] ?? 0) };
      c.categories.push(category);
      h.save(db);
      return h.ok(categoryView(category), 201);
    }
    if (method === 'PUT' && itemId) {
      const category = c.categories.find(x => x.id === itemId && x.menuId === menu.id);
      if (!category) return h.fail(404, 'Categoria não encontrada.');
      if (duplicate) return h.fail(409, 'Já existe uma categoria com esse nome neste cardápio.');
      if (typeof body['active'] !== 'boolean') return h.fail(400, 'Dados inválidos.');
      Object.assign(category, { name, description: optional(body['description']), displayOrder: Number(body['displayOrder']), active: body['active'] });
      h.save(db);
      return h.ok(categoryView(category));
    }
    return null;
  }

  // ---------- Produtos ----------
  const menuCategoryIds = categories().map(x => x.id);
  const products = () => c.products.filter(p => menuCategoryIds.includes(p.categoryId));
  if (section === 'products') {
    if (method === 'GET' && !itemId) {
      const url = new URL(req.urlWithParams, 'http://demo');
      const categoryId = url.searchParams.get('categoryId');
      if (categoryId && !menuCategoryIds.includes(categoryId)) return h.fail(404, 'Categoria não encontrada neste cardápio.');
      const order = (p: DemoProduct) => menuCategoryIds.indexOf(p.categoryId);
      return h.ok(products().filter(p => !categoryId || p.categoryId === categoryId)
        .sort((a, b) => order(a) - order(b) || byOrder(a, b)).map(p => productView(menu.id, p)));
    }
    if (menu.status === 'ARCHIVED') return archived();
    if (method === 'PUT' && itemId === 'order') {
      const categoryId = String(body['categoryId'] ?? '');
      const ids = body['productIds'] as string[] | undefined;
      if (!menuCategoryIds.includes(categoryId)) return h.fail(404, 'Categoria não encontrada neste cardápio.');
      const existing = c.products.filter(p => p.categoryId === categoryId).map(p => p.id);
      if (!Array.isArray(ids) || !ids.length || new Set(ids).size !== ids.length || ids.length !== existing.length || ids.some(id => !existing.includes(id))) {
        return h.fail(400, 'A ordenação deve conter todos os produtos desta categoria e nenhum produto externo.');
      }
      ids.forEach((id, index) => { c.products.find(p => p.id === id)!.displayOrder = index; });
      h.save(db);
      return h.ok(c.products.filter(p => p.categoryId === categoryId).sort(byOrder).map(p => productView(menu.id, p)));
    }
    if (method === 'POST' && !itemId) {
      const input = productInput(body, menuCategoryIds, h);
      if ('error' in input) return input.error;
      const product: DemoProduct = { id: h.uid('prod'), lifecycleStatus: 'ACTIVE', availability: 'AVAILABLE', imageUrl: null, ...input.value };
      c.products.push(product);
      h.save(db);
      return h.ok(productView(menu.id, product), 201);
    }
    const product = products().find(p => p.id === itemId);
    if (!product) return h.fail(404, 'Produto não encontrado neste cardápio.');
    if (method === 'PUT' && !itemAction) {
      const input = productInput(body, menuCategoryIds, h);
      if ('error' in input) return input.error;
      if (product.lifecycleStatus === 'ARCHIVED') return h.fail(409, 'Produto arquivado não pode ser editado.');
      Object.assign(product, input.value);
      h.save(db);
      return h.ok(productView(menu.id, product));
    }
    if (method === 'PATCH' && itemAction === 'availability') {
      const availability = body['availability'];
      if (availability !== 'AVAILABLE' && availability !== 'UNAVAILABLE') return h.fail(400, 'Informe a disponibilidade do produto.');
      if (product.lifecycleStatus === 'ARCHIVED') return h.fail(409, 'Produto arquivado não pode ser editado.');
      product.availability = availability;
      h.save(db);
      return h.ok(productView(menu.id, product));
    }
    if (method === 'POST' && (itemAction === 'archive' || itemAction === 'restore')) {
      product.lifecycleStatus = itemAction === 'archive' ? 'ARCHIVED' : 'ACTIVE';
      h.save(db);
      return h.ok(productView(menu.id, product));
    }
    if (itemAction === 'model-3d') return handleModel3d(req, method, subAction, product, db, h);
    if (itemAction === 'image' && method === 'POST') {
      if (product.lifecycleStatus === 'ARCHIVED') return h.fail(409, 'Produto arquivado não pode receber imagens.');
      const file = req.body instanceof FormData ? req.body.get('file') : null;
      if (!(file instanceof File) || file.size === 0) return h.fail(400, 'Selecione uma imagem.');
      if (file.size > 10 * 1024 * 1024) return h.fail(413, 'A imagem do produto pode ter no máximo 10 MB.');
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) return h.fail(400, 'Formato não aceito.');
      if (product.imageUrl) { URL.revokeObjectURL(product.imageUrl); LIVE_IMAGES.delete(product.imageUrl); }
      product.imageUrl = URL.createObjectURL(file);
      LIVE_IMAGES.add(product.imageUrl);
      h.save(db);
      return h.ok(productView(menu.id, product), 201);
    }
    if (itemAction === 'image' && method === 'DELETE') {
      if (!liveImage(product.imageUrl)) { product.imageUrl = null; h.save(db); return h.fail(404, 'Este produto não possui imagem.'); }
      URL.revokeObjectURL(product.imageUrl!);
      LIVE_IMAGES.delete(product.imageUrl!);
      product.imageUrl = null;
      h.save(db);
      return h.ok(null, 204);
    }
  }
  return null;
}

// ---------- modelo 3D (PR #27) ----------
function handleModel3d(req: HttpRequest<unknown>, method: string, subAction: string | undefined, product: DemoProduct, db: DemoDatabase, h: DemoHelpers): Reply | null {
  const view = (m: DemoModel3d) => ({
    id: m.id, productId: product.id, status: m.status, modelUrl: modelUrl(m), contentType: 'model/gltf-binary',
    fileSize: m.url === SEED_MODEL ? seedModelBytes : m.fileSize, widthMm: m.widthMm, heightMm: m.heightMm, depthMm: m.depthMm,
    createdAt: m.createdAt, updatedAt: m.updatedAt
  });
  const current = product.model3d && modelUrl(product.model3d) ? product.model3d : null;
  if (product.lifecycleStatus === 'ARCHIVED') return h.fail(409, 'Produto arquivado não pode ser alterado.');
  if (subAction === 'publication') {
    if (method !== 'PATCH') return null;
    if (!current) return h.fail(404, 'Este produto ainda não possui modelo 3D.');
    const published = (req.body as { published?: unknown } | null)?.published;
    if (typeof published !== 'boolean') return h.fail(400, 'Dados inválidos.');
    current.status = published ? 'PUBLISHED' : 'READY';
    current.updatedAt = new Date().toISOString();
    h.save(db);
    return h.ok(view(current));
  }
  if (method === 'GET') return current ? h.ok(view(current)) : h.fail(404, 'Este produto ainda não possui modelo 3D.');
  if (method === 'DELETE') {
    if (!current) return h.fail(404, 'Este produto ainda não possui modelo 3D.');
    if (current.url !== SEED_MODEL) { URL.revokeObjectURL(current.url); LIVE_IMAGES.delete(current.url); }
    product.model3d = null;
    h.save(db);
    return h.ok(null, 204);
  }
  if (method === 'POST') {
    const form = req.body instanceof FormData ? req.body : null;
    const file = form?.get('file');
    const dims = ['widthMm', 'heightMm', 'depthMm'].map(k => Number(form?.get(k)));
    if (!(file instanceof File) || file.size === 0) return h.fail(400, 'Selecione um arquivo GLB.');
    if (file.size > 10 * 1024 * 1024) return h.fail(413, 'O modelo 3D pode ter no máximo 10 MB.');
    if (!file.name.toLowerCase().endsWith('.glb')) return h.fail(400, 'Envie um arquivo com extensão .glb.');
    if (dims.some(d => !Number.isInteger(d) || d <= 0)) return h.fail(400, 'Informe largura, altura e profundidade em milímetros.');
    return from(file.slice(0, 12).arrayBuffer()).pipe(switchMap(header => {
      const v = new DataView(header);
      if (header.byteLength < 12 || v.getUint32(0, true) !== 0x46546c67 || v.getUint32(4, true) !== 2) return h.fail(400, 'Arquivo GLB inválido.');
      const now = new Date().toISOString();
      if (current && current.url !== SEED_MODEL) { URL.revokeObjectURL(current.url); LIVE_IMAGES.delete(current.url); }
      // A substituição mantém o estado de publicação, como a entidade do backend.
      const url = URL.createObjectURL(file);
      LIVE_IMAGES.add(url);
      product.model3d = {
        id: current?.id ?? h.uid('model'), status: current?.status ?? 'READY', url, fileSize: file.size,
        widthMm: dims[0], heightMm: dims[1], depthMm: dims[2], createdAt: current?.createdAt ?? now, updatedAt: now
      };
      h.save(db);
      return h.ok(view(product.model3d), 201);
    }));
  }
  return null;
}

/** URL utilizável do modelo (object URL vivo ou o prato de exemplo gerado em memória). */
function modelUrl(m: DemoModel3d): string | null {
  if (m.url === SEED_MODEL) {
    if (!seedModelUrl) {
      const bytes = createDemoPlateGlb();
      seedModelBytes = bytes.byteLength;
      seedModelUrl = URL.createObjectURL(new Blob([bytes], { type: 'model/gltf-binary' }));
    }
    return seedModelUrl;
  }
  return LIVE_IMAGES.has(m.url) ? m.url : null;
}

// ---------- utilidades ----------
function catalog(db: DemoDatabase): DemoCatalog {
  const holder = db as DemoDatabase & { catalog?: DemoCatalog };
  holder.catalog ??= createDemoCatalog('demo-r-cantina');
  return holder.catalog;
}

function requireRole(db: DemoDatabase, user: DemoUser, restaurantId: string, manager: boolean, h: DemoHelpers): Observable<never> | null {
  const membership = db.memberships.find(m => m.restaurantId === restaurantId && m.userId === user.id && m.active);
  if (!membership) return h.fail(403, 'Você não tem acesso a este restaurante.');
  if (manager && membership.role === 'STAFF') return h.fail(403, 'Seu papel não permite administrar cardápios.');
  return null;
}

function productInput(body: Record<string, unknown>, categoryIds: string[], h: DemoHelpers):
  { value: Omit<DemoProduct, 'id' | 'lifecycleStatus' | 'availability' | 'imageUrl'> } | { error: Observable<never> } {
  const categoryId = String(body['categoryId'] ?? '');
  if (!categoryIds.includes(categoryId)) return { error: h.fail(404, 'Categoria não encontrada neste cardápio.') };
  const name = text(body['name']);
  const price = body['priceInCents'];
  const ingredients = body['ingredients'];
  const addons = body['addons'];
  const bad = () => ({ error: h.fail(400, 'Dados inválidos.') });
  if (!name || name.length > 160 || !Number.isInteger(price) || (price as number) < 0) return bad();
  if (!Array.isArray(ingredients) || ingredients.length > 40 || ingredients.some(i => !text(i))) return bad();
  if (!Array.isArray(addons) || addons.length > 30) return bad();
  const cleanIngredients = ingredients.map(i => text(i));
  if (new Set(cleanIngredients.map(normalize)).size !== cleanIngredients.length) return bad();
  const cleanAddons: DemoAddon[] = [];
  for (const a of addons as Record<string, unknown>[]) {
    if (!a || !text(a['name']) || !Number.isInteger(a['priceDeltaInCents']) || (a['priceDeltaInCents'] as number) < 0 || typeof a['active'] !== 'boolean') return bad();
    cleanAddons.push({ name: text(a['name']), priceDeltaInCents: a['priceDeltaInCents'] as number, active: a['active'] as boolean });
  }
  if (new Set(cleanAddons.map(a => normalize(a.name))).size !== cleanAddons.length) return bad();
  return { value: { categoryId, name, description: optional(body['description']), priceInCents: price as number, displayOrder: Number(body['displayOrder'] ?? 0), ingredients: cleanIngredients, addons: cleanAddons } };
}

/** PNG ilustrativo: NÃO é um QR Code real (o real é gerado pelo backend). */
function demoQrCode(r: DemoRestaurant, h: DemoHelpers): Reply {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 640;
  const g = canvas.getContext('2d')!;
  g.fillStyle = '#ffffff'; g.fillRect(0, 0, 640, 640);
  g.fillStyle = '#191919';
  let seed = [...r.slug].reduce((s, ch) => s + ch.charCodeAt(0), 7);
  for (let y = 0; y < 25; y++) for (let x = 0; x < 25; x++) {
    seed = (seed * 9301 + 49297) % 233280;
    if (seed / 233280 > 0.55) g.fillRect(70 + x * 20, 70 + y * 20, 20, 20);
  }
  for (const [x, y] of [[70, 70], [430, 70], [70, 430]]) {
    g.fillStyle = '#191919'; g.fillRect(x, y, 140, 140);
    g.fillStyle = '#ffffff'; g.fillRect(x + 20, y + 20, 100, 100);
    g.fillStyle = '#FF7200'; g.fillRect(x + 40, y + 40, 60, 60);
  }
  g.fillStyle = '#ffffff'; g.fillRect(150, 280, 340, 80);
  g.fillStyle = '#191919'; g.font = 'bold 26px sans-serif'; g.textAlign = 'center';
  g.fillText('DEMONSTRAÇÃO', 320, 315);
  g.font = '18px sans-serif'; g.fillText(`/r/${r.slug}`, 320, 343);
  const bytes = atob(canvas.toDataURL('image/png').split(',')[1]);
  const buffer = new Uint8Array(bytes.length);
  for (let i = 0; i < bytes.length; i++) buffer[i] = bytes.charCodeAt(i);
  return h.ok(new Blob([buffer], { type: 'image/png' }));
}

function profileView(r: DemoRestaurant) {
  return {
    id: r.id, name: r.name, slug: r.slug, description: r.description ?? null, active: true, phone: r.phone ?? null,
    whatsapp: r.whatsapp ?? null, instagram: r.instagram ?? null, website: r.website ?? null, address: r.address ?? null,
    latitude: r.latitude ?? null, longitude: r.longitude ?? null, category: r.category ?? null, publicationStatus: r.publicationStatus ?? 'DRAFT'
  };
}
function menuView(m: DemoMenu) { return { id: m.id, name: m.name, description: m.description, status: m.status, displayOrder: m.displayOrder }; }
function categoryView(x: DemoCategory) { return { id: x.id, menuId: x.menuId, name: x.name, description: x.description, active: x.active, displayOrder: x.displayOrder }; }
function productView(menuId: string, p: DemoProduct) {
  return { ...p, menuId, ingredients: [...p.ingredients], addons: p.addons.map(a => ({ ...a })), imageUrl: liveImage(p.imageUrl) };
}
function liveImage(url: string | null): string | null { return url && LIVE_IMAGES.has(url) ? url : null; }
function byOrder<T extends { displayOrder: number; name: string }>(a: T, b: T): number {
  return a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'pt-BR');
}
function text(value: unknown): string { return typeof value === 'string' ? value.trim().replace(/\s+/g, ' ') : ''; }
function optional(value: unknown): string | null { return text(value) || null; }
function normalize(value: string): string { return value.normalize('NFD').replace(/\p{M}/gu, '').replace(/\s+/g, ' ').trim().toLowerCase(); }
